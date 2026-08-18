package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.WayStockBorder
import com.example.ui.theme.WayStockDark
import com.example.ui.theme.WayStockPrimary
import com.example.ui.theme.WayStockTextSec
import kotlinx.coroutines.delay

@Composable
fun AdminGatewayDialog(
    onDismiss: () -> Unit,
    onValidatePin: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(200)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    val submitPin: () -> Unit = {
        val success = onValidatePin(pinInput)
        if (!success) {
            isError = true
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Dialog(onDismissRequest = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("admin_gateway_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isError) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isError) Color(0xFFEF4444) else WayStockPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin Cloud Access",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayStockDark
                )

                Text(
                    text = "Enter Master Security PIN",
                    fontSize = 13.sp,
                    color = WayStockTextSec,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        if (isError) isError = false
                    },
                    placeholder = { Text("••••", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = WayStockTextSec) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitPin() }),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
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
                        .testTag("gateway_pin_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = submitPin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gateway_submit_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Text("ACTIVATE PORTAL ⚡", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
