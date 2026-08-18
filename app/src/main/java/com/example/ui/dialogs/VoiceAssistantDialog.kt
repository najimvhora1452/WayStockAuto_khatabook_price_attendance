package com.example.ui.dialogs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.WayStockPrimary
import com.example.ui.theme.WayStockTextMain
import com.example.ui.theme.WayStockTextSec

data class VoiceCommandExample(
    val category: String,
    val icon: ImageVector,
    val englishCmd: String,
    val hindiCmd: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantDialog(
    onDismiss: () -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Listening for commands...") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var manualInputText by remember { mutableStateOf("") }

    // Android Speech Recognizer Setup
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-US", "gu-IN"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak command for WayStock...")
        }
    }

    fun startListening() {
        if (speechRecognizer != null) {
            try {
                spokenText = ""
                isListening = true
                statusMessage = "Listening... Speak now 🎙️"
                speechRecognizer.startListening(recognitionIntent)
            } catch (e: Exception) {
                isListening = false
                statusMessage = "Tap mic to speak or select a command"
            }
        } else {
            isListening = false
            statusMessage = "Speech recognizer unavailable. Tap command below."
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
        statusMessage = "Tap mic to speak or choose a command"
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            statusMessage = "Microphone permission required for voice"
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                statusMessage = "Listening... Speak now 🎙️"
            }

            override fun onBeginningOfSpeech() {
                statusMessage = "Recording audio..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                statusMessage = "Processing voice..."
            }

            override fun onError(error: Int) {
                isListening = false
                statusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No command detected. Try again or tap below."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue. Tap a command below."
                    else -> "Tap mic to try again, or pick a command below."
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim() ?: ""
                if (recognized.isNotBlank()) {
                    spokenText = recognized
                    isListening = false
                    onExecuteCommand(recognized)
                    onDismiss()
                } else {
                    isListening = false
                    statusMessage = "No match found. Try again."
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    spokenText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        // Check permission and auto-start on open
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Categories of voice commands
    val commandCategories = listOf(
        "🛒 Add to Bucket" to listOf(
            VoiceCommandExample("Add to Bucket", Icons.Outlined.ShoppingCart, "Add 5 Classic Salted", "Vimal 10 packet daalo"),
            VoiceCommandExample("Add to Bucket", Icons.Outlined.ShoppingCart, "Add 2 boxes of Marlboro", "5 Classic Salted bucket me add karo"),
            VoiceCommandExample("Add to Bucket", Icons.Outlined.ShoppingCart, "Add 10 Lays to cart", "2 packet Marlboro cart me dalo"),
            VoiceCommandExample("Add to Bucket", Icons.Outlined.ShoppingCart, "Put 3 Red Bull in bucket", "10 Lays packet add karo")
        ),
        "📁 Open Folder" to listOf(
            VoiceCommandExample("Open Folder", Icons.Outlined.Folder, "Open Snacks", "Snacks folder kholo"),
            VoiceCommandExample("Open Folder", Icons.Outlined.Folder, "Go to Cigarettes", "Cigarettes me jao"),
            VoiceCommandExample("Open Folder", Icons.Outlined.Folder, "Navigate to Paan Masala", "Paan Masala open karo"),
            VoiceCommandExample("Open Folder", Icons.Outlined.Folder, "Go back", "Piche jao / Back jao")
        ),
        "📦 Bucket Actions" to listOf(
            VoiceCommandExample("Bucket Actions", Icons.Outlined.Inventory2, "Show my bucket", "Mera bucket dikhao"),
            VoiceCommandExample("Bucket Actions", Icons.Outlined.Inventory2, "Open cart", "Cart kholo"),
            VoiceCommandExample("Bucket Actions", Icons.Outlined.Inventory2, "Clear my bucket", "Cart khali karo"),
            VoiceCommandExample("Bucket Actions", Icons.Outlined.Inventory2, "How many items in cart?", "Cart me kitne items hain?")
        ),
        "📄 Bill & Slip" to listOf(
            VoiceCommandExample("Bill & Slip", Icons.Outlined.Description, "Create order slip", "Bill generate karo"),
            VoiceCommandExample("Bill & Slip", Icons.Outlined.Description, "Share on WhatsApp", "WhatsApp par share karo"),
            VoiceCommandExample("Bill & Slip", Icons.Outlined.Description, "Generate PDF slip", "Order slip banao")
        ),
        "🔍 Stock Check" to listOf(
            VoiceCommandExample("Stock Check", Icons.Outlined.Search, "Do we have Red Bull?", "Kya Red Bull hai?"),
            VoiceCommandExample("Stock Check", Icons.Outlined.Search, "Check stock for Fogg", "Fogg perfume ka stock check karo"),
            VoiceCommandExample("Stock Check", Icons.Outlined.Search, "Find Classic Salted", "Classic Salted dhundo")
        )
    )

    // Pulsing Mic Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Dialog(
        onDismissRequest = {
            stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("voice_assistant_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WayStockPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WayStock Voice Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WayStockTextMain
                            )
                            Text(
                                text = "English • हिन्दी • Hinglish",
                                fontSize = 11.sp,
                                color = WayStockTextSec
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopListening()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WayStockTextSec
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mic Pulse Orb Area
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            WayStockPrimary.copy(alpha = 0.25f),
                                            WayStockPrimary.copy(alpha = 0.05f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (isListening) {
                                    stopListening()
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        startListening()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                            .testTag("mic_toggle_btn"),
                        shape = CircleShape,
                        color = if (isListening) WayStockPrimary else Color(0xFFF1F5F9),
                        shadowElevation = if (isListening) 8.dp else 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (isListening) "Listening" else "Mic Off",
                                tint = if (isListening) Color.White else WayStockTextSec,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Text
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) WayStockPrimary else WayStockTextSec,
                    textAlign = TextAlign.Center
                )

                // Spoken Transcript Box
                AnimatedVisibility(
                    visible = spokenText.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = borderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Heard:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WayStockPrimary
                                )
                                Text(
                                    text = "\"$spokenText\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WayStockTextMain
                                )
                            }
                            IconButton(
                                onClick = {
                                    onExecuteCommand(spokenText)
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = WayStockPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Commands Section
                Text(
                    text = "Try these Voice Commands:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockTextSec,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips Horizontal Scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commandCategories.forEachIndexed { index, (catTitle, _) ->
                        FilterChip(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            label = {
                                Text(
                                    text = catTitle,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WayStockPrimary.copy(alpha = 0.12f),
                                selectedLabelColor = WayStockPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Examples in selected Category (English & Hindi cards)
                val currentExamples = commandCategories.getOrNull(selectedCategoryIndex)?.second ?: emptyList()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentExamples.forEach { ex ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // English option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            stopListening()
                                            onExecuteCommand(ex.englishCmd)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "🇺🇸", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "\"${ex.englishCmd}\"",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = WayStockTextMain
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Run",
                                        tint = WayStockPrimary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Hindi / Hinglish option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            stopListening()
                                            onExecuteCommand(ex.hindiCmd)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "🇮🇳", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "\"${ex.hindiCmd}\"",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF0F766E)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Run",
                                        tint = Color(0xFF0F766E).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Text Command Input Fallback
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = { Text("Or type command (e.g. Add 5 Lays)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_voice_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (manualInputText.isNotBlank()) {
                                keyboardController?.hide()
                                stopListening()
                                onExecuteCommand(manualInputText.trim())
                                onDismiss()
                            }
                        }
                    ),
                    trailingIcon = {
                        if (manualInputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    stopListening()
                                    onExecuteCommand(manualInputText.trim())
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Submit",
                                    tint = WayStockPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        stopListening()
                        onDismiss()
                    }
                ) {
                    Text("Cancel", color = WayStockTextSec)
                }
            }
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
