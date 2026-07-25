package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.analyzer.ColonyAnalyzer
import com.example.analyzer.ColonyLoc
import com.example.ui.viewmodel.ColonyViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalysisScreens(
    viewModel: ColonyViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Info/Calibrate, 2: Preprocess, 3: Manual Counter & CFU Tuning

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val progressFlow by viewModel.analysisProgress.collectAsState()
    val originalImage by viewModel.originalImage.collectAsState()
    val preprocessedResult by viewModel.preprocessResult.collectAsState()
    val colonies by viewModel.detectedColonies.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()

    val context = LocalContext.current

    // Set parameters
    var inputSampleId by remember { mutableStateOf("MA-" + (4000..9999).random()) }
    var selectedDeviceType by remember { mutableStateOf("Catheter Swab") }
    var selectedDilutionExponent by remember { mutableStateOf(3) } // Default 10^3
    var inputVolume by remember { mutableStateOf("1.0") }
    var labNotes by remember { mutableStateOf("") }

    // Synchronize to ViewModel
    LaunchedEffect(inputSampleId, selectedDeviceType, selectedDilutionExponent, inputVolume, labNotes) {
        val dilutionValue = Math.pow(10.0, selectedDilutionExponent.toDouble())
        val volValue = inputVolume.toDoubleOrNull() ?: 1.0
        viewModel.setSampleParams(inputSampleId, selectedDeviceType, dilutionValue, volValue, labNotes)
    }

    // Automatically load default synthetic sample for testing when step begins if empty
    LaunchedEffect(originalImage, currentStep) {
        if (originalImage == null && currentStep == 1) {
            // Load beautiful sample catheter swab petri dish bitmap
            triggerVirtualSampleLoad(viewModel, "Catheter Swab")
        }
    }

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
                            text = "Automated Bio-Load Scan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = "Patient Appliance Unit Validator",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // High-fidelity Multi-Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicatorItem(stepNumber = 1, title = "Criteria", isActive = currentStep >= 1, isCompleted = currentStep > 1)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), color = if (currentStep > 1) Color(0xFF0061A4) else Color(0xFFE1E2EC))
                StepIndicatorItem(stepNumber = 2, title = "Detection", isActive = currentStep >= 2, isCompleted = currentStep > 2)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), color = if (currentStep > 3) Color(0xFF0061A4) else Color(0xFFE1E2EC))
                StepIndicatorItem(stepNumber = 3, title = "CFU Analysis", isActive = currentStep >= 3, isCompleted = currentStep > 3)
            }

            // Analyzing display overlay loader
            if (isAnalyzing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF0061A4))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Running Computer Vision Segments (${(progressFlow * 100).toInt()}%)...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4)
                    )
                    LinearProgressIndicator(
                        progress = { progressFlow },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF0061A4),
                        trackColor = Color(0xFFE1E2EC),
                    )
                }
            }

            // Active Step content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentStep) {
                    1 -> SetupAndAcquireStep(
                        viewModel = viewModel,
                        inputSampleId = inputSampleId,
                        onSampleIdChange = { inputSampleId = it },
                        selectedDeviceType = selectedDeviceType,
                        onDeviceTypeChange = { selectedDeviceType = it },
                        labNotes = labNotes,
                        onNotesChange = { labNotes = it }
                    )
                    2 -> ImageProcessingStep(
                        viewModel = viewModel
                    )
                    3 -> CfuRefinementStep(
                        viewModel = viewModel,
                        selectedDilutionExponent = selectedDilutionExponent,
                        onDilutionChange = { selectedDilutionExponent = it },
                        inputVolume = inputVolume,
                        onVolumeChange = { inputVolume = it }
                    )
                }
            }

            // Footer navigation buttons matching "Sleek Interface" spacing style
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        Button(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("analysis_back_step_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0061A4)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF0061A4))
                        ) {
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep == 1) {
                                // Automatically triggers segment pipeline
                                viewModel.startAnalysisPipeline()
                                currentStep = 2
                            } else if (currentStep == 2) {
                                currentStep = 3
                            } else {
                                // Save into SQLite DB
                                viewModel.finalizeAnalysisAndSave {
                                    onSaveSuccess()
                                }
                            }
                        },
                        enabled = if (currentStep == 3) {
                            validationResult?.isValid == true
                        } else {
                            true
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .testTag("analysis_next_step_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0061A4),
                            disabledContainerColor = Color(0xFFE1E2EC),
                            contentColor = Color.White,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentStep == 3) "Verify & Save Report" else "Proceed",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep == 3) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicatorItem(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = when {
                        isCompleted -> Color(0xFF0061A4)
                        isActive -> Color(0xFF0061A4)
                        else -> Color(0xFFE1E2EC)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = if (isActive) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color(0xFF1B1B1F) else Color.Gray
        )
    }
}

// -------------------------------------------------------------
// STEP 1: SPECIFICATION & LOADING
// -------------------------------------------------------------
@Composable
fun SetupAndAcquireStep(
    viewModel: ColonyViewModel,
    inputSampleId: String,
    onSampleIdChange: (String) -> Unit,
    selectedDeviceType: String,
    onDeviceTypeChange: (String) -> Unit,
    labNotes: String,
    onNotesChange: (String) -> Unit
) {
    val context = LocalContext.current
    val originalImage by viewModel.originalImage.collectAsState()
    val scrollState = rememberScrollState()

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempPhotoUri
            if (uri != null) {
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    viewModel.loadSourceImage(bitmap)
                    statusMessage = "Photo captured and verified successfully!"
                    statusIsError = false
                } else {
                    statusMessage = "Failed to open or decode captured photo."
                    statusIsError = true
                }
            }
        } else {
            statusMessage = "Photo capture cancelled."
            statusIsError = true
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempPhotoUri(context)
            tempPhotoUri = uri
            if (uri != null) {
                takePictureLauncher.launch(uri)
            } else {
                statusMessage = "Error creating file path inside FileProvider."
                statusIsError = true
            }
        } else {
            statusMessage = "Runtime Camera permission was denied."
            statusIsError = true
        }
    }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (validateImageUri(context, uri)) {
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    viewModel.loadSourceImage(bitmap)
                    statusMessage = "Image successfully validated and loaded!"
                    statusIsError = false
                } else {
                    statusMessage = "Failed to decode loaded image file."
                    statusIsError = true
                }
            } else {
                statusMessage = "Unsupported format. Please choose a JPG, JPEG, or PNG asset."
                statusIsError = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Target Device criteria Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Medical Device Specification",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Select diagnostic source appliance criteria for safety threshold indexation.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputSampleId,
                    onValueChange = onSampleIdChange,
                    label = { Text("Sample ID Barcode") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sample_id_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Appliance Sampling Origin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Catheter Swab", "Syringe Rinse", "Endoscope Flush").forEach { type ->
                        val isSelected = selectedDeviceType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) Color(0xFF0061A4).copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF0061A4) else Color(0xFFE1E2EC),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onDeviceTypeChange(type) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0061A4) else Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Image Acquisition Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Petri Dish Sample Capture",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Take a high-quality photo of your Petri Plate or upload an existing laboratory image.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Action Buttons for Camera / File Choice
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                val uri = createTempPhotoUri(context)
                                tempPhotoUri = uri
                                if (uri != null) {
                                    takePictureLauncher.launch(uri)
                                } else {
                                    statusMessage = "Photo path configuration failed."
                                    statusIsError = true
                                }
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("take_photo_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0061A4),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Take Photo Icon", modifier = Modifier.size(16.dp))
                            Text("Take Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            selectImageLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("upload_file_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE1E2EC),
                            contentColor = Color(0xFF1B1B1F)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Upload Photo Icon", modifier = Modifier.size(16.dp))
                            Text("Upload File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Success or Validation Status Message Banner
                statusMessage?.let { msg ->
                    val bannerBg = if (statusIsError) Color(0xFFFFEFEF) else Color(0xFFE8F8F0)
                    val bannerBorder = if (statusIsError) Color(0xFFFF3F3F) else Color(0xFF00B464)
                    val bannerText = if (statusIsError) Color(0xFFFF3F3F) else Color(0xFF007A44)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(bannerBg, shape = RoundedCornerShape(8.dp))
                            .border(1.dp, bannerBorder, shape = RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (statusIsError) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = if (statusIsError) "Error" else "Success",
                                tint = bannerText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                color = bannerText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Loaded Image Preview below buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFF3F4F9), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE1E2EC), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (originalImage != null) {
                        Image(
                            bitmap = originalImage!!.asImageBitmap(),
                            contentDescription = "Loaded sample plate",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        // Repaint trigger overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                .clickable {
                                    // re-trigger load random
                                    triggerVirtualSampleLoad(viewModel, selectedDeviceType)
                                    statusMessage = "Virtual demo sample loaded successfully!"
                                    statusIsError = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Load Alt Sample", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Petri Plate Loaded", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // High fidelity Preset triggers (Extremely valuable for browser virtualization!)
                Text(
                    text = "Virtual sampling triggers (Recommended for browser sandbox):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            triggerVirtualSampleLoad(viewModel, "Catheter Swab") 
                            statusMessage = "Virtual High Count Preset loaded successfully!"
                            statusIsError = false
                        },
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEC), contentColor = Color(0xFFFF3F3F))
                    ) {
                        Text("High Count Preset", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { 
                            triggerVirtualSampleLoad(viewModel, "Syringe Rinse") 
                            statusMessage = "Virtual Low Count Preset loaded successfully!"
                            statusIsError = false
                        },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2F8EE), contentColor = Color(0xFF007A44))
                    ) {
                        Text("Low Count", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Additional Lab context notes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Auxiliary Lab Metadata",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Additional diagnostics comments or environment parameters.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = labNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("notes_field"),
                    placeholder = { Text("Comment on colony morphology, culture media type, agar thickness, etc...", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

/**
 * Creates and loads pre-rendered mathematical virtual agar samples as real Bitmaps in raw memory!
 * Real biological spot arrays are drawn as real pixel gradients so the watershed/local peak counts are mathematically extracted!
 */
fun triggerVirtualSampleLoad(viewModel: ColonyViewModel, device: String) {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw background translucent amber/agar gel
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    // Outer shadow dish frame
    paint.color = AndroidColor.parseColor("#E0E1E7")
    canvas.drawCircle(size/2f, size/2f, size * 0.46f, paint)
    paint.color = AndroidColor.parseColor("#FFFFFF")
    canvas.drawCircle(size/2f, size/2f, size * 0.44f, paint)

    // Agar plate fill
    val agarColor = when(device) {
        "Syringe Rinse" -> "#FDFBF0" // Pale translucent PCA agar
        "Endoscope Flush" -> "#F4ECE1" // Nutrient medium yeast tint
        else -> "#F6E4D1" // Blood agar tint swab variant
    }
    paint.color = AndroidColor.parseColor(agarColor)
    canvas.drawCircle(size/2f, size/2f, size * 0.435f, paint)

    // Concentric grid zones guide lines
    paint.style = android.graphics.Paint.Style.STROKE
    paint.color = AndroidColor.parseColor("#DDDCE4")
    paint.strokeWidth = 1f
    canvas.drawCircle(size/2f, size/2f, size * 0.435f * 0.33f, paint)
    canvas.drawCircle(size/2f, size/2f, size * 0.435f * 0.66f, paint)

    // Generate deterministic colonies using pseudo randomness based on device selection
    paint.style = android.graphics.Paint.Style.FILL
    val rand = java.util.Random(device.hashCode().toLong())
    val countRange = when(device) {
        "Syringe Rinse" -> 16..22
        "Endoscope Flush" -> 45..68
        else -> 125..152 // Catheter Swab
    }
    val colonyCount = countRange.random()

    val maxRadiusOfDish = size * 0.435f * 0.95f // 95% inside dish edge
    for (i in 0 until colonyCount) {
        val dist = rand.nextFloat() * maxRadiusOfDish
        val angle = rand.nextFloat() * 2 * Math.PI
        val cx = size/2f + dist * cos(angle).toFloat()
        val cy = size/2f + dist * sin(angle).toFloat()

        // Colony properties
        // High count presets have larger overlapping aggregates, low has distinct transparent beads
        val radius = when(device) {
            "Syringe Rinse" -> 4f + rand.nextFloat() * 3f
            "Endoscope Flush" -> 3f + rand.nextFloat() * 5f
            else -> 2f + rand.nextFloat() * 6f // Catheter Swab overlaps
        }

        // Draw multiple gradient transparent layers to simulate biological colonies with darker center nucleolus
        val colonyColorHex = when(device) {
            "Syringe Rinse" -> "#D9AF70" // Golden staph-like translucency
            "Endoscope Flush" -> "#CADCBD" // E-coli greyish green mold
            else -> "#A05A5A" // Yeast red/brown micro aggregates
        }
        val baseColor = AndroidColor.parseColor(colonyColorHex)

        // Glow ring
        paint.color = AndroidColor.argb(70, AndroidColor.red(baseColor), AndroidColor.green(baseColor), AndroidColor.blue(baseColor))
        canvas.drawCircle(cx, cy, radius * 1.5f, paint)
        // Center core
        paint.color = AndroidColor.argb(180, AndroidColor.red(baseColor)-20, AndroidColor.green(baseColor)-20, AndroidColor.blue(baseColor)-20)
        canvas.drawCircle(cx, cy, radius * 0.7f, paint)
    }

    viewModel.loadSourceImage(bitmap)
}

// -------------------------------------------------------------
// STEP 2: PREPROCESSING STATES
// -------------------------------------------------------------
@Composable
fun ImageProcessingStep(viewModel: ColonyViewModel) {
    val preprocessedResult by viewModel.preprocessResult.collectAsState()
    var selectedFilterTab by remember { mutableStateOf("Grayscale") } // "Grayscale", "Contrast", "Adaptive Local Threshold"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Medical Image Preprocessing Pipeline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Visualize the computer vision pipeline layers. Adaptive local thresholding isolates colonies.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render matching active filter Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFFF3F4F9), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (preprocessedResult != null) {
                        val activeBitmap = when (selectedFilterTab) {
                            "Grayscale" -> preprocessedResult!!.grayscale
                            "Contrast" -> preprocessedResult!!.enhanced
                            else -> preprocessedResult!!.thresholded
                        }
                        Image(
                            bitmap = activeBitmap.asImageBitmap(),
                            contentDescription = selectedFilterTab,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = Color(0xFF0061A4))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F9), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Grayscale", "Contrast", "Binary Mask").forEach { tab ->
                        val isSelected = selectedFilterTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilterTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0061A4) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Descriptive algorithm details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = when(selectedFilterTab) {
                            "Grayscale" -> "Greyscale Luma Conversion"
                            "Contrast" -> "Normalized Linear Scaling"
                            else -> "Adaptive Integral Local Thresholding"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = when(selectedFilterTab) {
                            "Grayscale" -> "Converts ARGB channels into 8-bit luma luminance index for noise reduction."
                            "Contrast" -> "Performs histogram stretching between minimum luma to optimize detection gradients."
                            else -> "Examines local pixel areas (15x15 kernel) to offset uneven agar thickness shadows."
                        },
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: MANUAL TUNE, MULTI-ZONE MARKERS & CFU FORMULATION
// -------------------------------------------------------------
@Composable
fun CfuRefinementStep(
    viewModel: ColonyViewModel,
    selectedDilutionExponent: Int,
    onDilutionChange: (Int) -> Unit,
    inputVolume: String,
    onVolumeChange: (String) -> Unit
) {
    val originalImage by viewModel.originalImage.collectAsState()
    val colonies by viewModel.detectedColonies.collectAsState()
    val userMarkers by viewModel.userMarkers.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()

    var showHeatmap by remember { mutableStateOf(false) }

    val isValidOfPlate = validationResult?.isValid == true
    val plateConfidence = validationResult?.confidence ?: 0

    val totalCount = if (isValidOfPlate) colonies.size else 0
    val volumeDouble = inputVolume.toDoubleOrNull() ?: 1.0
    val dilutionDouble = Math.pow(10.0, selectedDilutionExponent.toDouble())
    val computedCfu = ColonyAnalyzer.calculateCfu(totalCount, dilutionDouble, volumeDouble)
    val cfuScientificStr = ColonyAnalyzer.formatCfuScientific(computedCfu)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Red alert error card if plate has failed validation checks
        if (validationResult != null && !isValidOfPlate) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invalid_sample_warning_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFE1)),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF3F3F))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF3F3F),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Invalid Sample Image",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF3F3F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Please upload a clear Petri dish microbial culture image.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC02020)
                        )
                    }
                }
            }
        }

        // Interactive Colony Marker Screen Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Interactive Boundary Calibration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                        Text(
                            text = "Tap image directly to adjust count. Long tap clear.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Heatmap Overlay trigger (only when image is valid)
                    if (isValidOfPlate) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (showHeatmap) Color(0xFFFFECEC) else Color(0xFFF3F4F9),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { showHeatmap = !showHeatmap }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Heatmap Glow",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showHeatmap) Color(0xFFFF3F3F) else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Added detailed validation checklist indicator above the colony image
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isValidOfPlate) Color(0xFFE8F8F0) else Color(0xFFFFEFEF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isValidOfPlate) Color(0xFF00B464) else Color(0xFFFF3F3F),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isValidOfPlate) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isValidOfPlate) Color(0xFF007A44) else Color(0xFFFF3F3F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isValidOfPlate) "✓ Valid Culture Plate ($plateConfidence% Confidence)" else "✗ Invalid Sample Image ($plateConfidence% Confidence)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isValidOfPlate) Color(0xFF007A44) else Color(0xFFFF3F3F)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.HorizontalDivider(
                        color = if (isValidOfPlate) Color(0xFFD0ECCF) else Color(0xFFFCDCDC),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isRing = validationResult?.ringDetected == true
                        val isColonies = validationResult?.structuresDetected == true
                        val isMatch = validationResult?.cultureResembles == true

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Petri Dish Detected:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(text = if (isRing) "Yes" else "No", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isRing) Color(0xFF007A44) else Color(0xFFFF3F3F))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Colonies Detected:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(text = if (isColonies) "Yes" else "No", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isColonies) Color(0xFF007A44) else Color(0xFFFF3F3F))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Culture Plate Match:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(text = if (isMatch) "Yes" else "No", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isMatch) Color(0xFF007A44) else Color(0xFFFF3F3F))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Confidence:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(text = "$plateConfidence%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isValidOfPlate) Color(0xFF007A44) else Color(0xFFFF3F3F))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Text(text = if (isValidOfPlate) "Valid Sample" else "Invalid Sample", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isValidOfPlate) Color(0xFF007A44) else Color(0xFFFF3F3F))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Petri dish Canvas layer mapping clicks responsive
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("interactive_petri_dish"),
                    contentAlignment = Alignment.Center
                ) {
                    val boxWidthPx = constraints.maxWidth.toFloat()
                    val boxHeightPx = constraints.maxHeight.toFloat()

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .border(3.dp, if (isValidOfPlate) Color(0xFF00B464) else Color(0xFFFF3F3F), CircleShape)
                            .pointerInput(isValidOfPlate) {
                                if (isValidOfPlate) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            // Map offset from 240dp constraint space down to raw fractional coordinates
                                            val fx = offset.x / size.width
                                            val fy = offset.y / size.height
                                            viewModel.addManualColonyMarker(fx, fy)
                                        }
                                    )
                                }
                            }
                    ) {
                        if (originalImage != null) {
                            Image(
                                bitmap = originalImage!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        // Rendering the Dynamic Heatmap image if activated and plate is valid
                        if (showHeatmap && originalImage != null && isValidOfPlate) {
                            val heatmapBitmap = ColonyAnalyzer.drawDensityHeatmap(240, 240, colonies)
                            Image(
                                bitmap = heatmapBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        // Drawing concentric zone boundaries and colony coordinate rings in Compose
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.width / 2f
                            
                            // Concentric metric circles
                            drawCircle(color = Color(0x330061A4), radius = radius * 0.33f, style = Stroke(width = 1f))
                            drawCircle(color = Color(0x330061A4), radius = radius * 0.66f, style = Stroke(width = 1f))
                            
                            // Render detected colony positions ONLY if plate is valid
                            if (isValidOfPlate) {
                                colonies.forEach { colony ->
                                    val cx = colony.x * size.width
                                    val cy = colony.y * size.height
                                    // Red halo representing colony segmented bounds
                                    drawCircle(
                                        color = Color(0xFFFF3F3F).copy(alpha = 0.85f),
                                        radius = (colony.radius * size.width).coerceAtLeast(3f),
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Colony accuracy and count statistics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Automated", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isValidOfPlate) "${colonies.size - userMarkers.size} colonies" else "N/A (Invalid)", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Black, 
                            color = Color(0xFF1B1B1F)
                        )
                    }
                    Column {
                        Text(text = "Manual Added", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isValidOfPlate) "${userMarkers.size} markers" else "N/A (Invalid)", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Black, 
                            color = Color(0xFF0061A4)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Validation Index", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (userMarkers.isEmpty()) "100% Auto" else "Adjusted",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00B464)
                        )
                    }
                }

                if (userMarkers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.clearManualMarkers() },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F9), contentColor = Color.DarkGray)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Manual Marks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CFU Formula Settings Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CFU Formula Criteria",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )

                // Dilution Factor Slider/Input
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Dilution Factor Exponent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "10^-$selectedDilutionExponent", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0061A4))
                    }
                    Slider(
                        value = selectedDilutionExponent.toFloat(),
                        onValueChange = { onDilutionChange(it.toInt()) },
                        valueRange = 0f..7f,
                        steps = 6,
                        enabled = isValidOfPlate,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF0061A4),
                            thumbColor = Color(0xFF0061A4)
                        ),
                        modifier = Modifier.testTag("dilution_slider")
                    )
                }

                // Sample Volume Input
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Inoculum Sample Volume (mL)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = inputVolume,
                            onValueChange = onVolumeChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            enabled = isValidOfPlate,
                            modifier = Modifier
                                .width(90.dp)
                                .testTag("volume_field"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFE1E2EC)
                            )
                        )
                    }
                }

                Divider(color = Color(0xFFF3F4F9))

                // Formula calculations outputs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F9), shape = RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Calculated Bio-Load", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isValidOfPlate) cfuScientificStr else "Calculation Disabled", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Black, 
                            color = if (isValidOfPlate) Color(0xFF0061A4) else Color.Gray
                        )
                    }
                    
                    if (isValidOfPlate) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when {
                                        computedCfu >= 100000.0 -> Color(0xFFFFEFEF)
                                        computedCfu >= 500.0 -> Color(0xFFFFF7EB)
                                        else -> Color(0xFFE8F8F0)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    computedCfu >= 100000.0 -> "HIGH RISK"
                                    computedCfu >= 500.0 -> "MEDIUM RISK"
                                    else -> "LOW RISK"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    computedCfu >= 100000.0 -> Color(0xFFFF3F3F)
                                    computedCfu >= 500.0 -> Color(0xFFFF9F00)
                                    else -> Color(0xFF00B464)
                                }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEBEBEF), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "LOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// HELPER METHODS FOR PHOTO CAPTURE & DISK VALIDATION PREFERENCES
// ------------------------------------------------------------------------

private fun createTempPhotoUri(context: Context): Uri? {
    return try {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val tempFile = File(reportsDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun validateImageUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri) ?: ""
    if (mimeType.startsWith("image/", ignoreCase = true)) {
        val sub = mimeType.substringAfter("/").lowercase()
        return sub == "jpeg" || sub == "jpg" || sub == "png"
    }
    val name = getFileName(context, uri).lowercase()
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unnamed_image.png"
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
