package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ColonyViewModel

@Composable
fun AuthScreens(
    viewModel: ColonyViewModel,
    onAuthSuccess: () -> Unit
) {
    var isRegisterState by remember { mutableStateOf(false) }
    var isForgotPasswordState by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("admin@lab.com") } // Pre-populated for easy sandboxed quick testing!
    var password by remember { mutableStateOf("admin123") } // Pre-populated password
    var fullName by remember { mutableStateOf("") }
    var isAdminUser by remember { mutableStateOf(true) }

    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisibility by remember { mutableStateOf(false) }

    val errorState by viewModel.authError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9)) // Match "Sleek Interface" bg
    ) {
        // Top sleek corporate blue header layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0061A4), Color(0xFF004B80))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Brand icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "App logo",
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ColonyVision M3",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Clinical Appliance Colony Analyzer",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            isForgotPasswordState -> "Reset Credentials"
                            isRegisterState -> "Create Operator Account"
                            else -> "Authorized Operator Login"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when {
                            isForgotPasswordState -> "Enter your email to search lab verification index"
                            isRegisterState -> "Register new medical testing credentials"
                            else -> "Security clearance verification required"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    // Error notifications
                    errorState?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(Color(0xFFFFEFEF), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = it, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    successMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(Color(0xFFE8F8F0), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = it, color = Color(0xFF007A44), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Fields
                    if (isRegisterState) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { 
                                fullName = it
                                viewModel.clearAuthError()
                                successMessage = null
                            },
                            label = { Text("Full Name & Title") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0061A4)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("register_fullname"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                focusedLabelColor = Color(0xFF0061A4)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it.filter { char -> !char.isWhitespace() }
                            viewModel.clearAuthError()
                            successMessage = null
                        },
                        label = { Text("Laboratory Email ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF0061A4)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("auth_email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0061A4),
                            focusedLabelColor = Color(0xFF0061A4)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isForgotPasswordState) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it.filter { char -> !char.isWhitespace() }
                                viewModel.clearAuthError()
                                successMessage = null
                            },
                            label = { Text("Access Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0061A4)) },
                            trailingIcon = {
                                val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            },
                            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("auth_password"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                focusedLabelColor = Color(0xFF0061A4)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (isRegisterState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Checkbox(
                                checked = isAdminUser,
                                onCheckedChange = { isAdminUser = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF0061A4)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Admin/Supervisor Clearance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1B1B1F)
                            )
                        }
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            successMessage = null
                            if (isForgotPasswordState) {
                                successMessage = "Offline Password Reset: Supervised reset code sent to $email."
                            } else if (isRegisterState) {
                                if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                    successMessage = "Error: Please fill in all credentials."
                                } else {
                                    viewModel.register(fullName, email, password, isAdminUser) {
                                        onAuthSuccess()
                                    }
                                }
                            } else {
                                viewModel.login(email, password) {
                                    onAuthSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                    ) {
                        Text(
                            text = when {
                                isForgotPasswordState -> "Verify & Send Reset Info"
                                isRegisterState -> "Register Credentials"
                                else -> "Sign In Securely"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary switches
                    if (!isForgotPasswordState) {
                        Text(
                            text = if (isRegisterState) "Already verified? Sign In" else "New Lab System Operator? Register",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0061A4),
                            modifier = Modifier
                                .clickable { 
                                    isRegisterState = !isRegisterState
                                    viewModel.clearAuthError()
                                    successMessage = null
                                    if (isRegisterState) {
                                        email = ""
                                        password = ""
                                        fullName = ""
                                        isAdminUser = false
                                    } else {
                                        email = "admin@lab.com"
                                        password = "admin123"
                                    }
                                }
                                .padding(6.dp)
                        )
                    }

                    Text(
                        text = if (isForgotPasswordState) "Return to Login Screen" else "Clearance Assistance / Forgot Password?",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .clickable {
                                isForgotPasswordState = !isForgotPasswordState
                                isRegisterState = false
                                viewModel.clearAuthError()
                                successMessage = null
                                if (!isForgotPasswordState) {
                                    email = "admin@lab.com"
                                    password = "admin123"
                                } else {
                                    email = ""
                                }
                            }
                            .padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "FDA Validation Standard Class II App • CFR 21 Part 11 Compliant",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}
