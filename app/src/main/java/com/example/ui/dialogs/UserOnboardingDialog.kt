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
    onNameSubmitted: (String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    isGoogleLoading: Boolean = false
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

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
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
                    text = "Sign in with Google or enter your name",
                    fontSize = 13.sp,
                    color = WayStockTextSec,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                // Google One-Tap Sign-In Button
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_signin_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF8FAFC),
                        contentColor = WayStockDark
                    )
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = WayStockPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Signing in...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        // Google "G" Icon representation
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WayStockDark)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = WayStockBorder)
                    Text(
                        "  OR  ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WayStockTextSec
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = WayStockBorder)
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Enter your name manually...", color = WayStockTextSec, fontSize = 13.sp) },
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = submitName,
                    enabled = nameInput.trim().length >= 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("user_submit_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WayStockPrimary)
                ) {
                    Text("Let's Go 🚀", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
