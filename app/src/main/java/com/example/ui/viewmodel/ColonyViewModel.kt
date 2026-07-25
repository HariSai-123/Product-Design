package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analyzer.ColonyAnalyzer
import com.example.analyzer.ColonyLoc
import com.example.analyzer.PreprocessResult
import com.example.analyzer.ValidationResult
import com.example.data.database.ColonyDatabase
import com.example.data.model.AnalysisRecord
import com.example.data.model.UserRecord
import com.example.data.repository.ColonyRepository
import com.example.util.GeminiHelper
import com.example.util.ReportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ColonyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ColonyRepository
    val records: StateFlow<List<AnalysisRecord>>

    init {
        val database = ColonyDatabase.getDatabase(application)
        repository = ColonyRepository(database.colonyDao())
        
        // Initialize Firebase programmatically
        com.example.util.FirebaseManager.initialize(application)
        
        records = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Sync Firestore records down to local database if connected
        syncFirestore()
    }

    fun syncFirestore() {
        if (com.example.util.FirebaseManager.isInitialized) {
            com.example.util.FirebaseManager.syncRecordsFromFirestore { firestoreRecords ->
                viewModelScope.launch(Dispatchers.IO) {
                    for (record in firestoreRecords) {
                        try {
                            repository.insertRecord(record)
                        } catch (e: Exception) {
                            Log.e("ColonyViewModel", "Error syncing record: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // --- Authentication States ---
    private val _currentUser = MutableStateFlow<UserRecord?>(null)
    val currentUser: StateFlow<UserRecord?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    private fun translateFirebaseError(message: String): String {
        return when {
            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) || 
            message.contains("configuration not found", ignoreCase = true) -> {
                "Firebase Error: Email/Password sign-in provider is disabled in your Firebase console. Please go to your Firebase Console -> Authentication -> Sign-in method and enable the 'Email/Password' provider."
            }
            message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            message.contains("invalid login credentials", ignoreCase = true) -> {
                "Invalid login credentials. Please check your email and password."
            }
            message.contains("network", ignoreCase = true) -> {
                "Network error. Please check your internet connection."
            }
            else -> message
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        return email.trim().matches(emailRegex)
    }

    fun login(email: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty()) {
                _authError.value = "Laboratory email ID cannot be empty."
                return@launch
            }
            if (!isValidEmail(trimmedEmail)) {
                _authError.value = "Please enter a valid laboratory email address (e.g. user@lab.com)."
                return@launch
            }
            if (passwordHash.isEmpty()) {
                _authError.value = "Access password cannot be empty."
                return@launch
            }

            // Check local predefined accounts and local database first
            if (trimmedEmail == "admin@lab.com" && passwordHash == "admin123") {
                val admin = UserRecord("admin@lab.com", "admin123", "Dr. Sarah Jenkins", true)
                _currentUser.value = admin
                viewModelScope.launch { repository.registerUser(admin) }
                onSuccess()
                return@launch
            }
            if (trimmedEmail == "user@lab.com" && passwordHash == "user123") {
                val defaultUser = UserRecord("user@lab.com", "user123", "Chief Chemist Alice", false)
                _currentUser.value = defaultUser
                viewModelScope.launch { repository.registerUser(defaultUser) }
                onSuccess()
                return@launch
            }

            val localUser = repository.getUserByEmail(trimmedEmail)
            if (localUser != null && localUser.passwordHash == passwordHash) {
                _currentUser.value = localUser
                onSuccess()
                return@launch
            }

            if (com.example.util.FirebaseManager.isInitialized) {
                val auth = com.example.util.FirebaseManager.getAuth()
                if (auth != null) {
                    try {
                        auth.signInWithEmailAndPassword(trimmedEmail, passwordHash)
                            .addOnSuccessListener { authResult ->
                                val userEmail = authResult.user?.email ?: trimmedEmail
                                val firestore = com.example.util.FirebaseManager.getFirestore()
                                if (firestore != null) {
                                    firestore.collection("users").document(userEmail).get()
                                        .addOnSuccessListener { doc ->
                                            val fullName = doc.getString("fullName") ?: "Lab Scientist"
                                            val isAdmin = doc.getBoolean("isAdmin") ?: false
                                            val user = UserRecord(userEmail, passwordHash, fullName, isAdmin)
                                            _currentUser.value = user
                                            
                                            viewModelScope.launch {
                                                repository.registerUser(user)
                                            }
                                            
                                            syncFirestore()
                                            onSuccess()
                                        }
                                        .addOnFailureListener {
                                            val user = UserRecord(userEmail, passwordHash, "Lab Scientist", false)
                                            _currentUser.value = user
                                            syncFirestore()
                                            onSuccess()
                                        }
                                } else {
                                    val user = UserRecord(userEmail, passwordHash, "Lab Scientist", false)
                                    _currentUser.value = user
                                    syncFirestore()
                                    onSuccess()
                                }
                            }
                            .addOnFailureListener { e ->
                                _authError.value = translateFirebaseError(e.message ?: "Authentication failed.")
                            }
                    } catch (e: Exception) {
                        _authError.value = translateFirebaseError(e.message ?: "Firebase login error.")
                    }
                    return@launch
                }
            }

            _authError.value = "Invalid laboratory email or access password."
        }
    }

    fun register(fullName: String, email: String, passwordHash: String, isAdmin: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            
            val trimmedFullName = fullName.trim()
            val trimmedEmail = email.trim()

            if (trimmedFullName.isEmpty()) {
                _authError.value = "Full name and title cannot be empty."
                return@launch
            }
            if (trimmedEmail.isEmpty()) {
                _authError.value = "Laboratory email ID cannot be empty."
                return@launch
            }
            if (!isValidEmail(trimmedEmail)) {
                _authError.value = "Please enter a valid laboratory email address."
                return@launch
            }
            if (passwordHash.length < 6) {
                _authError.value = "Access password must be at least 6 characters long."
                return@launch
            }
            
            if (com.example.util.FirebaseManager.isInitialized) {
                val auth = com.example.util.FirebaseManager.getAuth()
                val firestore = com.example.util.FirebaseManager.getFirestore()
                if (auth != null && firestore != null) {
                    try {
                        auth.createUserWithEmailAndPassword(trimmedEmail, passwordHash)
                            .addOnSuccessListener { authResult ->
                                val newUser = UserRecord(trimmedEmail, passwordHash, trimmedFullName, isAdmin)
                                val userData = hashMapOf(
                                    "email" to trimmedEmail,
                                    "fullName" to trimmedFullName,
                                    "isAdmin" to isAdmin,
                                    "dateRegistered" to newUser.dateRegistered
                                )
                                firestore.collection("users").document(trimmedEmail).set(userData)
                                    .addOnSuccessListener {
                                        _currentUser.value = newUser
                                        viewModelScope.launch {
                                            repository.registerUser(newUser)
                                        }
                                        syncFirestore()
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        _currentUser.value = newUser
                                        viewModelScope.launch {
                                            repository.registerUser(newUser)
                                        }
                                        syncFirestore()
                                        onSuccess()
                                    }
                            }
                            .addOnFailureListener { e ->
                                _authError.value = translateFirebaseError(e.message ?: "Registration failed.")
                            }
                    } catch (e: Exception) {
                        _authError.value = translateFirebaseError(e.message ?: "Firebase registration error.")
                    }
                    return@launch
                }
            }

            val existing = repository.getUserByEmail(trimmedEmail)
            if (existing != null) {
                _authError.value = "User with this email already exists."
            } else {
                val newUser = UserRecord(trimmedEmail, passwordHash, trimmedFullName, isAdmin)
                repository.registerUser(newUser)
                _currentUser.value = newUser
                onSuccess()
            }
        }
    }

    fun logout() {
        if (com.example.util.FirebaseManager.isInitialized) {
            try {
                com.example.util.FirebaseManager.getAuth()?.signOut()
            } catch (e: Exception) {
                Log.e("ColonyViewModel", "Error signing out: ${e.message}")
            }
        }
        _currentUser.value = null
    }

    // --- Current Active Analysis Pipeline States ---
    val sampleId = MutableStateFlow("")
    val deviceType = MutableStateFlow("Catheter Swab")
    val dilutionFactor = MutableStateFlow(1000.0) // 10^3 Default
    val sampleVolume = MutableStateFlow(1.0) // 1.0 mL Default
    val notes = MutableStateFlow("")

    val originalImage = MutableStateFlow<Bitmap?>(null)
    val preprocessResult = MutableStateFlow<PreprocessResult?>(null)
    val detectedColonies = MutableStateFlow<List<ColonyLoc>>(emptyList())
    val userMarkers = MutableStateFlow<List<ColonyLoc>>(emptyList())
    val validationResult = MutableStateFlow<ValidationResult?>(null)

    val isAnalyzing = MutableStateFlow(false)
    val analysisProgress = MutableStateFlow(0f)

    val currentReport = MutableStateFlow<AnalysisRecord?>(null)
    val generatedPdfFile = MutableStateFlow<File?>(null)

    // --- History Screen Settings ---
    val searchQuery = MutableStateFlow("")
    val dateFilter = MutableStateFlow("All") // "All", "Today", "Last 7 Days"
    val typeFilter = MutableStateFlow("All") // "All", "Catheter Swab", "Syringe Rinse", "Endoscope Flush"

    fun setSampleParams(id: String, device: String, dilution: Double, volume: Double, desc: String) {
        sampleId.value = id
        deviceType.value = device
        dilutionFactor.value = dilution
        sampleVolume.value = volume
        notes.value = desc
    }

    fun loadSourceImage(bitmap: Bitmap) {
        originalImage.value = bitmap
        preprocessResult.value = null
        detectedColonies.value = emptyList()
        userMarkers.value = emptyList()
        analysisProgress.value = 0f
        validationResult.value = null
        
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                ColonyAnalyzer.validateImage(bitmap)
            }
            validationResult.value = result
        }
    }

    fun startAnalysisPipeline() {
        viewModelScope.launch {
            val bitmap = originalImage.value ?: return@launch
            isAnalyzing.value = true
            analysisProgress.value = 0.1f
            
            // Re-run validation or fetch background validation
            val vResult = withContext(Dispatchers.Default) {
                ColonyAnalyzer.validateImage(bitmap)
            }
            validationResult.value = vResult
            analysisProgress.value = 0.3f
            
            if (vResult.isValid) {
                // Step 1: Preprocessing (Grayscale, adaptive thresholding)
                withContext(Dispatchers.Default) {
                    val preResult = ColonyAnalyzer.preprocessImage(bitmap)
                    preprocessResult.value = preResult
                }
                analysisProgress.value = 0.6f

                // Step 2: Overlapping colony Hough/Watershed detection
                withContext(Dispatchers.Default) {
                    val colonies = ColonyAnalyzer.detectColonies(bitmap, userMarkers.value)
                    detectedColonies.value = colonies
                }
                analysisProgress.value = 0.9f
            } else {
                // Invalid plate - clear all colony and preprocessed results!
                preprocessResult.value = null
                detectedColonies.value = emptyList()
                userMarkers.value = emptyList()
                analysisProgress.value = 0.9f
            }
            isAnalyzing.value = false
        }
    }

    fun addManualColonyMarker(x: Float, y: Float) {
        // Only allow manual markers when validation successfully passes
        val vResult = validationResult.value
        if (vResult == null || !vResult.isValid) return

        val dx = x - 0.5f
        val dy = y - 0.5f
        val radius = 0.44f
        val distValue = kotlin.math.sqrt(dx*dx + dy*dy) / radius
        val zone = when {
            distValue < 0.33f -> "Center"
            distValue < 0.66f -> "Middle"
            else -> "Outer"
        }

        val newMarker = ColonyLoc(
            x = x,
            y = y,
            radius = 0.025f, // Standard manual marker radius size
            confidence = 1.0f, // Manual confidence is 100%
            zone = zone
        )
        userMarkers.value = userMarkers.value + newMarker
        
        // Recalculate detections taking markers into account
        val img = originalImage.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val colonies = ColonyAnalyzer.detectColonies(img, userMarkers.value)
            detectedColonies.value = colonies
        }
    }

    fun clearManualMarkers() {
        userMarkers.value = emptyList()
        val img = originalImage.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val colonies = ColonyAnalyzer.detectColonies(img, emptyList())
            detectedColonies.value = colonies
        }
    }

    fun removeManualMarkerNear(x: Float, y: Float) {
        userMarkers.value = userMarkers.value.filter {
            val dist = kotlin.math.sqrt((it.x - x)*(it.x - x) + (it.y - y)*(it.y - y))
            dist > 0.04f
        }
        val img = originalImage.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val colonies = ColonyAnalyzer.detectColonies(img, userMarkers.value)
            detectedColonies.value = colonies
        }
    }

    fun finalizeAnalysisAndSave(onComplete: (AnalysisRecord) -> Unit) {
        viewModelScope.launch {
            val bitmap = originalImage.value ?: return@launch
            isAnalyzing.value = true
            
            val finalColonies = detectedColonies.value
            val totalCount = finalColonies.size
            val cfuValue = ColonyAnalyzer.calculateCfu(totalCount, dilutionFactor.value, sampleVolume.value)
            val cfuScientificStr = ColonyAnalyzer.formatCfuScientific(cfuValue)

            // Zone calculations
            val zones = ColonyAnalyzer.analyzeZones(finalColonies)

            // Contamination Risk calculation
            val (risk, colorHex) = when {
                cfuValue >= 100000.0 -> "High Risk" to "#FF3F3F" // Solid red
                cfuValue >= 500.0 -> "Medium Risk" to "#FF9F00" // Solar orange
                else -> "Low Risk" to "#00B464" // Forest green
            }

            // High-fidelity local or server-side Gemini insights
            val zonesSummary = "Center: ${zones.centerZone.count} (${String.format("%.1f", zones.centerZone.percent)}%), " +
                    "Middle: ${zones.middleZone.count} (${String.format("%.1f", zones.middleZone.percent)}%), " +
                    "Outer: ${zones.outerZone.count} (${String.format("%.1f", zones.outerZone.percent)}%)"

            val aiInsights = withContext(Dispatchers.Default) {
                GeminiHelper.getContaminationInsights(
                    sampleId = sampleId.value,
                    deviceType = deviceType.value,
                    totalCount = totalCount,
                    cfuScientific = cfuScientificStr,
                    riskRating = risk,
                    zonesText = zonesSummary
                )
            }

            val record = AnalysisRecord(
                sampleId = sampleId.value.ifEmpty { "MA-" + (1000..9999).random() },
                deviceType = deviceType.value,
                dilutionFactor = dilutionFactor.value,
                sampleVolume = sampleVolume.value,
                totalCount = totalCount,
                cfu = cfuValue,
                cfuScientific = cfuScientificStr,
                centerCount = zones.centerZone.count,
                middleCount = zones.middleZone.count,
                outerCount = zones.outerZone.count,
                centerDensity = zones.centerZone.density,
                middleDensity = zones.middleZone.density,
                outerDensity = zones.outerZone.density,
                centerPercent = zones.centerZone.percent,
                middlePercent = zones.middleZone.percent,
                outerPercent = zones.outerZone.percent,
                riskRating = risk,
                riskColor = colorHex,
                notes = notes.value,
                aiRecommendation = aiInsights
            )

            // Insert into SQLite DB
            val recordId = repository.insertRecord(record)
            var insertedRecord = record.copy(id = recordId.toInt())
            currentReport.value = insertedRecord

            // Generate full PDF document automatically
            withContext(Dispatchers.IO) {
                val pdf = ReportUtils.generatePdfReport(getApplication(), insertedRecord)
                generatedPdfFile.value = pdf
            }

            // Sync with Firebase if initialized
            if (com.example.util.FirebaseManager.isInitialized) {
                com.example.util.FirebaseManager.uploadBitmap(bitmap, "images/${insertedRecord.sampleId}_original.jpg") { originalUrl ->
                    val preprocessedBitmap = preprocessResult.value?.thresholded
                    if (preprocessedBitmap != null) {
                        com.example.util.FirebaseManager.uploadBitmap(preprocessedBitmap, "images/${insertedRecord.sampleId}_processed.jpg") { processedUrl ->
                            val updatedRecord = insertedRecord.copy(
                                imagePath = originalUrl ?: insertedRecord.imagePath,
                                processedImagePath = processedUrl ?: insertedRecord.processedImagePath
                            )
                            viewModelScope.launch {
                                repository.insertRecord(updatedRecord)
                            }
                            insertedRecord = updatedRecord
                            currentReport.value = updatedRecord
                            com.example.util.FirebaseManager.uploadRecordToFirestore(updatedRecord, originalUrl, processedUrl)
                        }
                    } else {
                        val updatedRecord = insertedRecord.copy(
                            imagePath = originalUrl ?: insertedRecord.imagePath
                        )
                        viewModelScope.launch {
                            repository.insertRecord(updatedRecord)
                        }
                        insertedRecord = updatedRecord
                        currentReport.value = updatedRecord
                        com.example.util.FirebaseManager.uploadRecordToFirestore(updatedRecord, originalUrl, null)
                    }
                }
            }

            isAnalyzing.value = false
            onComplete(insertedRecord)
        }
    }

    fun exportExistingReportToPdf(record: AnalysisRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdf = ReportUtils.generatePdfReport(getApplication(), record)
            generatedPdfFile.value = pdf
        }
    }

    fun deleteRecord(record: AnalysisRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }
}
