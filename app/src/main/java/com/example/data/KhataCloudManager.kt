package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class KhataCloudManager(private val context: Context, private val repository: WayStockRepository) {

    companion object {
        private const val TAG = "KhataCloudManager"
        private const val KHATA_CUSTOMERS_COLLECTION = "khata_customers"
        private const val KHATA_TRANSACTIONS_COLLECTION = "khata_transactions"
        private const val KHATA_MEMOS_COLLECTION = "khata_memos"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                return null
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available: ${e.message}")
            null
        }
    }

    private fun getDoubleField(doc: com.google.firebase.firestore.DocumentSnapshot, vararg keys: String): Double {
        for (k in keys) {
            val v = doc.get(k)
            if (v != null) {
                return when (v) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }
        }
        return 0.0
    }

    private fun getStringField(doc: com.google.firebase.firestore.DocumentSnapshot, vararg keys: String): String {
        for (k in keys) {
            val v = doc.get(k)
            if (v != null) {
                return v.toString().trim()
            }
        }
        return ""
    }

    private fun getLongField(doc: com.google.firebase.firestore.DocumentSnapshot, vararg keys: String): Long {
        for (k in keys) {
            val v = doc.get(k)
            if (v != null) {
                return when (v) {
                    is Number -> v.toLong()
                    is String -> v.toLongOrNull() ?: 0L
                    else -> 0L
                }
            }
        }
        return 0L
    }

    /**
     * Start real-time sync with Firebase Firestore
     */
    fun startRealtimeSync() {
        val firestore = getFirestore() ?: return

        try {
            stopSync()

            // 1. Listen for Customer updates from Cloud (checks khata_customers, customers, parties)
            val customerCollections = listOf("khata_customers", "customers", "parties")
            for (colName in customerCollections) {
                val listener = firestore.collection(colName)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.d(TAG, "Customer collection $colName listener notice: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null && !snapshots.isEmpty) {
                            scope.launch {
                                for (doc in snapshots.documents) {
                                    val id = doc.getString("id") ?: doc.id
                                    val name = getStringField(doc, "name", "customerName", "partyName", "cname", "title")
                                    if (name.isBlank()) continue

                                    val phone = getStringField(doc, "phone", "mobile", "contact", "phoneNumber", "tel")
                                    val address = getStringField(doc, "address", "addr", "city", "location")
                                    var type = getStringField(doc, "customerType", "type", "partyType")
                                    if (type.isBlank()) type = "Customer"
                                    val balance = getDoubleField(doc, "balance", "totalBalance", "dueAmount", "pendingAmount")
                                    val createdAt = getLongField(doc, "createdAt", "created_at", "timestamp").let {
                                        if (it > 0) it else System.currentTimeMillis()
                                    }
                                    val updatedAt = getLongField(doc, "updatedAt", "updated_at").let {
                                        if (it > 0) it else System.currentTimeMillis()
                                    }

                                    val customer = KhataCustomerEntity(
                                        id = id,
                                        name = name,
                                        phone = phone,
                                        address = address,
                                        customerType = type,
                                        balance = balance,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                    repository.insertOrUpdateKhataCustomer(customer)
                                }
                            }
                        }
                    }
                activeListeners.add(listener)
            }

            // 2. Listen for Transactions updates from Cloud (checks khata_transactions, transactions)
            val txnCollections = listOf("khata_transactions", "transactions")
            for (colName in txnCollections) {
                val listener = firestore.collection(colName)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.d(TAG, "Txn collection $colName listener notice: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null && !snapshots.isEmpty) {
                            scope.launch {
                                for (doc in snapshots.documents) {
                                    val id = doc.getString("id") ?: doc.id
                                    val customerId = getStringField(doc, "customerId", "customer_id", "partyId", "party_id")
                                    if (customerId.isBlank()) continue

                                    val customerName = getStringField(doc, "customerName", "partyName", "name")
                                    val amount = getDoubleField(doc, "amount", "amt", "price", "total")
                                    var type = getStringField(doc, "type", "txnType", "transactionType", "dr_cr").uppercase()
                                    if (type != "GAVE" && type != "GOT") {
                                        type = if (type.contains("GAVE") || type.contains("DEBIT") || type.contains("DR")) "GAVE" else "GOT"
                                    }
                                    val date = getStringField(doc, "date", "txnDate")
                                    val time = getStringField(doc, "time", "txnTime")
                                    val note = getStringField(doc, "note", "description", "remark", "notes", "memo")
                                    val paymentMode = getStringField(doc, "paymentMode", "payment_mode", "mode", "method").ifBlank { "Cash" }
                                    val billNumber = getStringField(doc, "billNumber", "bill_no", "invoiceNo", "billNo")
                                    val timestamp = getLongField(doc, "timestamp", "createdAt", "time_millis").let {
                                        if (it > 0) it else System.currentTimeMillis()
                                    }

                                    val txn = KhataTransactionEntity(
                                        id = id,
                                        customerId = customerId,
                                        customerName = customerName,
                                        amount = amount,
                                        type = type,
                                        date = date,
                                        time = time,
                                        note = note,
                                        paymentMode = paymentMode,
                                        billNumber = billNumber,
                                        timestamp = timestamp
                                    )
                                    repository.insertOrUpdateKhataTransactionDirect(txn)
                                }
                            }
                        }
                    }
                activeListeners.add(listener)
            }

            // 3. Listen for Memos updates from Cloud (checks khata_memos, memos, notes)
            val memoCollections = listOf("khata_memos", "memos", "notes")
            for (colName in memoCollections) {
                val listener = firestore.collection(colName)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.d(TAG, "Memos collection $colName listener notice: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshots != null && !snapshots.isEmpty) {
                            scope.launch {
                                for (doc in snapshots.documents) {
                                    val note = getStringField(doc, "note", "text", "memo", "content")
                                    if (note.isBlank()) continue
                                    val timestamp = getLongField(doc, "timestamp", "time", "createdAt").let {
                                        if (it > 0) it else System.currentTimeMillis()
                                    }
                                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L

                                    val memo = KhataMemoEntity(
                                        id = id,
                                        note = note,
                                        timestamp = timestamp
                                    )
                                    repository.insertKhataMemoDirect(memo)
                                }
                            }
                        }
                    }
                activeListeners.add(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Firestore sync: ${e.message}")
        }
    }

    /**
     * Upload Customer data to Firestore Cloud
     */
    suspend fun uploadCustomerToCloud(customer: KhataCustomerEntity) {
        val firestore = getFirestore() ?: return
        try {
            val data = hashMapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phone" to customer.phone,
                "address" to customer.address,
                "customerType" to customer.customerType,
                "balance" to customer.balance,
                "createdAt" to customer.createdAt,
                "updatedAt" to customer.updatedAt
            )
            firestore.collection(KHATA_CUSTOMERS_COLLECTION)
                .document(customer.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Customer ${customer.name} synced to cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload customer: ${e.message}")
        }
    }

    /**
     * Delete Customer from Cloud
     */
    suspend fun deleteCustomerFromCloud(customerId: String) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection(KHATA_CUSTOMERS_COLLECTION)
                .document(customerId)
                .delete()
                .await()
            Log.d(TAG, "Customer $customerId deleted from cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete customer from cloud: ${e.message}")
        }
    }

    /**
     * Upload Transaction to Cloud
     */
    suspend fun uploadTransactionToCloud(txn: KhataTransactionEntity) {
        val firestore = getFirestore() ?: return
        try {
            val data = hashMapOf(
                "id" to txn.id,
                "customerId" to txn.customerId,
                "customerName" to txn.customerName,
                "amount" to txn.amount,
                "type" to txn.type,
                "date" to txn.date,
                "time" to txn.time,
                "note" to txn.note,
                "paymentMode" to txn.paymentMode,
                "billNumber" to txn.billNumber,
                "timestamp" to txn.timestamp
            )
            firestore.collection(KHATA_TRANSACTIONS_COLLECTION)
                .document(txn.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Transaction ${txn.id} synced to cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload transaction: ${e.message}")
        }
    }

    /**
     * Delete Transaction from Cloud
     */
    suspend fun deleteTransactionFromCloud(txnId: String) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection(KHATA_TRANSACTIONS_COLLECTION)
                .document(txnId)
                .delete()
                .await()
            Log.d(TAG, "Transaction $txnId deleted from cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete transaction from cloud: ${e.message}")
        }
    }

    /**
     * Upload Memo / Note to Cloud
     */
    suspend fun uploadMemoToCloud(memo: KhataMemoEntity) {
        val firestore = getFirestore() ?: return
        try {
            val docId = if (memo.id > 0) memo.id.toString() else "memo_${memo.timestamp}"
            val data = hashMapOf(
                "id" to memo.id,
                "note" to memo.note,
                "timestamp" to memo.timestamp
            )
            firestore.collection(KHATA_MEMOS_COLLECTION)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Memo synced to cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload memo: ${e.message}")
        }
    }

    /**
     * Delete Memo from Cloud
     */
    suspend fun deleteMemoFromCloud(memoId: Long) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection(KHATA_MEMOS_COLLECTION)
                .document(memoId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete memo: ${e.message}")
        }
    }

    fun stopSync() {
        for (l in activeListeners) {
            try {
                l.remove()
            } catch (e: Exception) {
                // Ignore
            }
        }
        activeListeners.clear()
    }
}
