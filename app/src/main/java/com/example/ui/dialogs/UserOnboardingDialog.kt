package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.WayStockBorder
import com.example.ui.theme.WayStockDark
import com.example.ui.theme.WayStockPrimary
import com.example.ui.theme.WayStockTextSec
import kotlinx.coroutines.delay

@Composable
fun UserOnboardingDialog(
    onNameSubmitted: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(250)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val emojiAvatar = remember(nameInput) {
        if (nameInput.trim().isEmpty()) "👋" else "🤩"
    }

    val submitName: () -> Unit = {
        if (nameInput.trim().length >= 2) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onNameSubmitted(nameInput.trim())
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("user_onboarding_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.example.ui.components.WayStockAnimatedLogo(
                    size = 72.dp,
                    interactive = true,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Welcome to WayStock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WayStockDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Apna naam daal kar apni personal bucket chalu karein!",
                    fontSize = 13.sp,
                    color = WayStockTextSec,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Enter your name...", color = WayStockTextSec) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitName() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WayStockDark,
                        unfocusedTextColor = WayStockDark,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = WayStockPrimary,
                        unfocusedBorderColor = WayStockBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("user_name_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = submitName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("user_submit_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Text("Let's Go 🚀", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
