package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisRecord
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ColonyViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: ColonyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("splash") } // "splash", "auth", "dashboard", "analyze", "report", "settings"
                var selectedRecordForReport by remember { mutableStateOf<AnalysisRecord?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Show bottom navigation bar only when logged into interactive areas
                        if (currentScreen == "dashboard" || currentScreen == "analyze" || currentScreen == "settings") {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                containerColor = Color.White,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == "dashboard",
                                    onClick = { currentScreen = "dashboard" },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0061A4),
                                        selectedTextColor = Color(0xFF0061A4),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0061A4).copy(alpha = 0.12f)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "analyze",
                                    onClick = { currentScreen = "analyze" },
                                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                                    label = { Text("Scan", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0061A4),
                                        selectedTextColor = Color(0xFF0061A4),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0061A4).copy(alpha = 0.12f)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "settings",
                                    onClick = { currentScreen = "settings" },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0061A4),
                                        selectedTextColor = Color(0xFF0061A4),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color(0xFF0061A4).copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentScreen == "dashboard" || currentScreen == "analyze" || currentScreen == "settings") innerPadding.calculateBottomPadding() else 0.dp)
                    ) {
                        when (currentScreen) {
                            "splash" -> SplashScreen(
                                onNavigateToAuth = { currentScreen = "auth" }
                            )
                            "auth" -> AuthScreens(
                                viewModel = viewModel,
                                onAuthSuccess = { currentScreen = "dashboard" }
                            )
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAnalyze = { currentScreen = "analyze" },
                                onNavigateToReport = { record ->
                                    selectedRecordForReport = record
                                    currentScreen = "report"
                                }
                            )
                            "analyze" -> AnalysisScreens(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "dashboard" },
                                onSaveSuccess = {
                                    currentScreen = "dashboard"
                                }
                            )
                            "report" -> selectedRecordForReport?.let { record ->
                                ReportScreen(
                                    viewModel = viewModel,
                                    record = record,
                                    onNavigateBack = { currentScreen = "dashboard" }
                                )
                            }
                            "settings" -> SettingsScreen(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logout()
                                    currentScreen = "auth"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
