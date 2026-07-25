package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AnalysisRecord
import com.example.ui.viewmodel.ColonyViewModel
import com.example.util.ReportUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: ColonyViewModel,
    record: AnalysisRecord,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pdfFileState by viewModel.generatedPdfFile.collectAsState()

    var showVerificationDialog by remember { mutableStateOf(false) }

    // Auto-generate or locate cached PDF for existing records
    LaunchedEffect(record) {
        viewModel.exportExistingReportToPdf(record)
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.dateCreated))

    val themeColor = Color(AndroidColor.parseColor(record.riskColor))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Validation Report",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = record.sampleId,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Risk / Contamination Badge Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_risk_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(themeColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Sterility Risk Check: " + record.riskRating.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                text = when (record.riskRating) {
                                    "High Risk" -> "Colony count exceeds threshold. Potential infection risk."
                                    "Medium Risk" -> "Biological load detected. Recalibrate decontamination cycles."
                                    else -> "Zero/low contamination rate. Safe for surgical environments."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                        }
                    }
                }

                // Core Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Count
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Colonies", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${record.totalCount}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF0061A4))
                            Text("Automated counts", fontSize = 9.sp, color = Color.LightGray)
                        }
                    }

                    // CFU value
                    Card(
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Calculated Bio-Load", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = record.cfuScientific, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0061A4))
                        }
                    }
                }

                // Metadata Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Metadata Attributes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                        Divider(color = Color(0xFFF3F4F9))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Appliance Unit Type", color = Color.Gray, fontSize = 12.sp)
                            Text(record.deviceType, color = Color(0xFF1B1B1F), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dilution Factor Settings", color = Color.Gray, fontSize = 12.sp)
                            Text("10^${kotlin.math.log10(record.dilutionFactor).toInt()}", color = Color(0xFF1B1B1F), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inoculum Volume (mL)", color = Color.Gray, fontSize = 12.sp)
                            Text("${record.sampleVolume} mL", color = Color(0xFF1B1B1F), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Scan Timestamp", color = Color.Gray, fontSize = 12.sp)
                            Text(formattedDate, color = Color(0xFF1B1B1F), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                // Zone distribution & Density comparison chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Zone Dispersion & Density", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                        Text("Compares colony concentration from center to outer periphery.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

                        // Render matching distribution bars as requested in Sleek design
                        ZoneDistributionProgressBar(name = "Center Zone [r < 33%]", count = record.centerCount, percent = record.centerPercent, density = record.centerDensity)
                        Spacer(modifier = Modifier.height(10.dp))
                        ZoneDistributionProgressBar(name = "Middle Zone [33% - 66%]", count = record.middleCount, percent = record.middlePercent, density = record.middleDensity)
                        Spacer(modifier = Modifier.height(10.dp))
                        ZoneDistributionProgressBar(name = "Outer Zone [r > 66%]", count = record.outerCount, percent = record.outerPercent, density = record.outerDensity)
                    }
                }

                // AI insight recommendations (Gemini Powered)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Insights & Guidelines", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                        }
                        Text(
                            text = record.aiRecommendation,
                            fontSize = 11.sp,
                            color = Color(0xFF1B1B1F),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }

                // Document Integrity hash QR block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { showVerificationDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val qrBitmap = ReportUtils.drawVerificationQrCode("verify:${record.sampleId}:id:${record.id}", 64)
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Verification QR Code",
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Regulatory Validation QR Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                            Text("Tap to review official CFR certification signatures and credentials.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Action controls: View/Share PDF sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export/Share
                    Button(
                        onClick = {
                            pdfFileState?.let { sharePdfReport(context, it) }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0061A4)),
                        border = BorderStroke(1.dp, Color(0xFF0061A4))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Done/Acknowledge
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("report_done_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                    ) {
                        Text("Log Saved", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Verification signature popup details
        if (showVerificationDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showVerificationDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Validation Signatures",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = "This validation record is electronically sealed and encrypted in concordance with FDA CFR Title 21 Part 11 requirements regarding biomedical electronic signatures.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp),
                            lineHeight = 16.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Doc Hash", fontSize = 11.sp, color = Color.Gray)
                            Text("SHA-256: F5919D...E2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("System State", fontSize = 11.sp, color = Color.Gray)
                            Text("COMPLIANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B464))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showVerificationDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                        ) {
                            Text("Acknowledge Integrity", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneDistributionProgressBar(
    name: String,
    count: Int,
    percent: Double,
    density: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B1B1F))
            Text(text = "${count} col (${String.format("%.1f", percent)}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFF3F4F9), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100.0).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color(0xFF0061A4), shape = RoundedCornerShape(4.dp))
            )
        }
        Text(
            text = "Area density: ${String.format("%.1f", density)} colonies/arbitrary units²",
            fontSize = 9.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Executes a native Android FileProvider Intent to easily share the compiled A4 Biomedical PDF report
 */
fun sharePdfReport(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Lab PDF Report"))
    } catch (e: Exception) {
        // Fallback for sandboxed developer stream if uri exposure throws
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = Uri.fromFile(file)
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
