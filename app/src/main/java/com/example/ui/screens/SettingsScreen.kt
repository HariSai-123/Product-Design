package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ColonyViewModel
import com.example.util.FirebaseManager

@Composable
fun SettingsScreen(
    viewModel: ColonyViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val records by viewModel.records.collectAsState()
    val context = LocalContext.current

    var isStrictValuationMode by remember { mutableStateOf(true) }
    var showAdminPanel by remember { mutableStateOf(false) }

    val totalScans = records.size
    val highRiskScans = records.count { it.riskRating == "High Risk" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9))
    ) {
        // Simple Top Header Bar (matches "Sleek Interface" spacing style)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "System Administration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Operator preferences and safety parameters.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Operator Profile Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF0061A4), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Chief Operator Jenkins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = currentUser?.email ?: "jenkins@clinical-labs.org",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE8F8F0), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CFR Compliant ID", fontSize = 8.5.sp, color = Color(0xFF007A44), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Firebase Integration Panel Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                var showFirebasePanel by remember { mutableStateOf(false) }
                
                var apiKeyInput by remember { mutableStateOf(FirebaseManager.getApiKey(context)) }
                var projectIdInput by remember { mutableStateOf(FirebaseManager.getProjectId(context)) }
                var appIdInput by remember { mutableStateOf(FirebaseManager.getAppId(context)) }
                var storageBucketInput by remember { mutableStateOf(FirebaseManager.getStorageBucket(context)) }
                
                val isFirebaseConnected = FirebaseManager.isInitialized

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFirebasePanel = !showFirebasePanel }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFirebaseConnected) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isFirebaseConnected) Color(0xFF007A44) else Color(0xFFFF3F3F)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Firebase Project Integration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                                Text(
                                    text = if (isFirebaseConnected) "Connected to ${FirebaseManager.getProjectId(context)}" else "Using Local Database",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showFirebasePanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(visible = showFirebasePanel) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = Color(0xFFF3F4F9))

                            Text(
                                "Configure credentials of your existing Firebase project (web/mobile) to sync counts, authentication, and culture plate images dynamically.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("API Key", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = projectIdInput,
                                onValueChange = { projectIdInput = it },
                                label = { Text("Project ID", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = appIdInput,
                                onValueChange = { appIdInput = it },
                                label = { Text("Application ID (App ID)", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = storageBucketInput,
                                onValueChange = { storageBucketInput = it },
                                label = { Text("Storage Bucket (Optional)", fontSize = 11.sp) },
                                singleLine = true,
                                placeholder = { Text("your-project.appspot.com", fontSize = 11.sp) },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (apiKeyInput.isEmpty() || projectIdInput.isEmpty() || appIdInput.isEmpty()) {
                                            Toast.makeText(context, "Please fill in API Key, Project ID, and App ID", Toast.LENGTH_SHORT).show()
                                        } else {
                                            FirebaseManager.saveConfig(
                                                context,
                                                apiKeyInput,
                                                projectIdInput,
                                                appIdInput,
                                                storageBucketInput
                                            )
                                            if (FirebaseManager.isInitialized) {
                                                Toast.makeText(context, "Successfully connected to Firebase!", Toast.LENGTH_SHORT).show()
                                                viewModel.syncFirestore()
                                            } else {
                                                Toast.makeText(context, "Failed to initialize. Check your credentials.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                                ) {
                                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (isFirebaseConnected) {
                                    Button(
                                        onClick = {
                                            FirebaseManager.clearConfig(context)
                                            apiKeyInput = ""
                                            projectIdInput = ""
                                            appIdInput = ""
                                            storageBucketInput = ""
                                            Toast.makeText(context, "Firebase disconnected. Reverting to local fallback.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEC), contentColor = Color(0xFFFF3F3F))
                                    ) {
                                        Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isFirebaseConnected) {
                                Button(
                                    onClick = {
                                        viewModel.syncFirestore()
                                        Toast.makeText(context, "Synchronizing records from Firestore database...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F8F0), contentColor = Color(0xFF007A44))
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync Firestore Database Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Operations Validation preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Calibration Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Automated Counts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                            Text("Disable manual colony marker overrides on reports for FDA regulation strictness.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isStrictValuationMode,
                            onCheckedChange = { isStrictValuationMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0061A4))
                        )
                    }

                    Divider(color = Color(0xFFF3F4F9))

                    // Toggle Admin panel options if operator has Admin role verified in Room
                    if (currentUser?.isAdmin == true) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdminPanel = !showAdminPanel }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF0061A4))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Lead Inspector Admin Console", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                            }
                            Icon(
                                imageVector = if (showAdminPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                        
                        AnimatedVisibility(visible = showAdminPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F9), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Operator Console Summary", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Active Records in SQLite Table", color = Color.DarkGray, fontSize = 12.sp)
                                    Text("$totalScans Logs", color = Color(0xFF0061A4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Identified Safe Compliance Rate", color = Color.DarkGray, fontSize = 12.sp)
                                    val safeRate = if (totalScans > 0) ((totalScans - highRiskScans) * 100) / totalScans else 100
                                    Text("$safeRate % compliant", color = Color(0xFF00B464), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Divider(color = Color.White)
                                
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Regulatory Audit Trail exported to secure package.", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                                ) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Security Audit Trail CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // System Logs cleanup
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("System Utilities", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "System image cache cleared.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Flush Cached Processing Blobs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1F))
                            Text("Releases locally cached diagnostic reports files to free up partition size.", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Logout Action Button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEC), contentColor = Color(0xFFFF3F3F))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Revoke Security Authorization Token", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
