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
    private var customersListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var memosListener: ListenerRegistration? = null

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

    /**
     * Start real-time sync with Firebase Firestore
     */
    fun startRealtimeSync() {
        val firestore = getFirestore() ?: return

        try {
            // 1. Listen for Customer updates from Cloud
            customersListener?.remove()
            customersListener = firestore.collection(KHATA_CUSTOMERS_COLLECTION)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Customers listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch {
                            for (doc in snapshots.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val name = doc.getString("name") ?: continue
                                val phone = doc.getString("phone") ?: ""
                                val address = doc.getString("address") ?: ""
                                val type = doc.getString("customerType") ?: "Customer"
                                val balance = doc.getDouble("balance") ?: 0.0
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

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

            // 2. Listen for Transactions updates from Cloud
            transactionsListener?.remove()
            transactionsListener = firestore.collection(KHATA_TRANSACTIONS_COLLECTION)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Transactions listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch {
                            for (doc in snapshots.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val customerId = doc.getString("customerId") ?: continue
                                val customerName = doc.getString("customerName") ?: ""
                                val amount = doc.getDouble("amount") ?: 0.0
                                val type = doc.getString("type") ?: "GAVE"
                                val date = doc.getString("date") ?: ""
                                val time = doc.getString("time") ?: ""
                                val note = doc.getString("note") ?: ""
                                val paymentMode = doc.getString("paymentMode") ?: "Cash"
                                val billNumber = doc.getString("billNumber") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

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

            // 3. Listen for Memos updates from Cloud
            memosListener?.remove()
            memosListener = firestore.collection(KHATA_MEMOS_COLLECTION)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Memos listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch {
                            for (doc in snapshots.documents) {
                                val note = doc.getString("note") ?: continue
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
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
        customersListener?.remove()
        transactionsListener?.remove()
        memosListener?.remove()
    }
}
