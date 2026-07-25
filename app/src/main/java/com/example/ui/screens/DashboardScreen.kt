package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisRecord
import com.example.ui.viewmodel.ColonyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ColonyViewModel,
    onNavigateToAnalyze: () -> Unit,
    onNavigateToReport: (AnalysisRecord) -> Unit
) {
    val records by viewModel.records.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var deviceFilter by remember { mutableStateOf("All") }
    var riskFilter by remember { mutableStateOf("All") }

    val filteredRecords = records.filter { record ->
        val matchesSearch = record.sampleId.contains(searchQuery, ignoreCase = true) ||
                record.deviceType.contains(searchQuery, ignoreCase = true)
        val matchesDevice = deviceFilter == "All" || record.deviceType == deviceFilter
        val matchesRisk = riskFilter == "All" || record.riskRating == riskFilter
        matchesSearch && matchesDevice && matchesRisk
    }

    // Statistics computation
    val totalCount = records.size
    val highRiskCount = records.count { it.riskRating == "High Risk" }
    val mediumRiskCount = records.count { it.riskRating == "Medium Risk" }
    val lowRiskCount = records.count { it.riskRating == "Low Risk" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9)) // Match target bg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Lab Header Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 54.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF0061A4), shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Lab Logo",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser?.fullName ?: "Guest Operator",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = "Verification Lab Station",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Small Operator Badge Card
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE1E2EC), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentUser?.isAdmin == true) "Lead Inspector" else "Assistant",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0061A4)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Intro Analytics Dashboard Card
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Valuation Analytics",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total App. Checked",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$totalCount Units",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0061A4)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("High Risk", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("$highRiskCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3F3F))
                                    }
                                    Column {
                                        Text("Moderate", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("$mediumRiskCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F00))
                                    }
                                    Column {
                                        Text("Acceptable", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("$lowRiskCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B464))
                                    }
                                }
                            }

                            // Dynamic pure Compose micro-donut chart representing safe proportions
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 10.dp.toPx()
                                    val safeRatio = if (totalCount > 0) lowRiskCount.toFloat() / totalCount else 1f
                                    val dangerRatio = if (totalCount > 0) highRiskCount.toFloat() / totalCount else 0.0f
                                    val medRatio = if (totalCount > 0) mediumRiskCount.toFloat() / totalCount else 0.0f

                                    // Base empty circle
                                    drawCircle(
                                        color = Color(0xFFE1E2EC),
                                        radius = size.width / 2 - strokeWidth,
                                        style = Stroke(width = strokeWidth)
                                    )

                                    var startAngle = -90f
                                    // Safe slice (Green)
                                    if (safeRatio > 0) {
                                        drawArc(
                                            color = Color(0xFF00B464),
                                            startAngle = startAngle,
                                            sweepAngle = 360f * safeRatio,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                        startAngle += 360f * safeRatio
                                    }

                                    // Med slice (Orange)
                                    if (medRatio > 0) {
                                        drawArc(
                                            color = Color(0xFFFF9F00),
                                            startAngle = startAngle,
                                            sweepAngle = 360f * medRatio,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                        startAngle += 360f * medRatio
                                    }

                                    // Danger slice (Red)
                                    if (dangerRatio > 0) {
                                        drawArc(
                                            color = Color(0xFFFF3F3F),
                                            startAngle = startAngle,
                                            sweepAngle = 360f * dangerRatio,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (totalCount > 0) (lowRiskCount * 100) / totalCount else 100}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1B1B1F)
                                )
                            }
                        }
                    }
                }

                // Filtering Area
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Laboratory Logs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field"),
                        placeholder = { Text("Search by ID or device...", fontSize = 14.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE1E2EC),
                            focusedBorderColor = Color(0xFF0061A4)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row-wise pills for appliance and contamination filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Device list filter pills
                        listOf("All", "Catheter Swab", "Syringe Rinse", "Endoscope Flush").forEach { device ->
                            val isSelected = deviceFilter == device
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF0061A4) else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { deviceFilter = device }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = device,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Empty state if no records matches search criteria
                if (filteredRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFFF3F4F9), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No validation logs found",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                                Text(
                                    text = "Change parameters or tap '+' to analyze a sample.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Dynamic list of reports
                items(filteredRecords) { record ->
                    DashboardRecordCard(record = record, onClick = { onNavigateToReport(record) }, onDelete = { viewModel.deleteRecord(record) })
                }

                // Offset bottom space for floating action bar
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button to triggers camera scans pipeline
        FloatingActionButton(
            onClick = onNavigateToAnalyze,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_sample_fab"),
            containerColor = Color(0xFF0061A4),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Scan New Sample")
        }
    }
}

@Composable
fun DashboardRecordCard(
    record: AnalysisRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.dateCreated))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .testTag("record_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-contrast rating vertical colored bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(Color(AndroidColor.parseColor(record.riskColor)), shape = RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.sampleId,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    
                    // Risk rating badge label
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(AndroidColor.parseColor(record.riskColor)).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = record.riskRating,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(AndroidColor.parseColor(record.riskColor))
                        )
                    }
                }
                
                Text(
                    text = record.deviceType,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 1.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${record.totalCount} colonies",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0061A4)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${record.cfuScientific})",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Delete record option for the laboratory supervisors
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
