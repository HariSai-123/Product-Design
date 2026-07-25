package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AnalysisRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PREFS_NAME = "firebase_prefs"

    var isInitialized = false
        private set

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("api_key", "") ?: ""
        if (saved.isNotEmpty()) return saved
        return try { BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
    }

    fun getProjectId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("project_id", "") ?: ""
        if (saved.isNotEmpty()) return saved
        return try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }
    }

    fun getAppId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("app_id", "") ?: ""
        if (saved.isNotEmpty()) return saved
        return try { BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Exception) { "" }
    }

    fun getStorageBucket(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString("storage_bucket", "") ?: ""
        if (saved.isNotEmpty()) return saved
        return try { BuildConfig.FIREBASE_STORAGE_BUCKET } catch (e: Exception) { "" }
    }

    fun saveConfig(context: Context, apiKey: String, projectId: String, appId: String, storageBucket: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("api_key", apiKey.trim())
            .putString("project_id", projectId.trim())
            .putString("app_id", appId.trim())
            .putString("storage_bucket", storageBucket.trim())
            .apply()

        initialize(context, force = true)
    }

    fun clearConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        try {
            val existingApps = FirebaseApp.getApps(context)
            for (app in existingApps) {
                if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
                    app.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning Firebase app instance: ${e.message}")
        }
        isInitialized = false
    }

    fun initialize(context: Context, force: Boolean = false): Boolean {
        if (isInitialized && !force) return true

        val apiKey = getApiKey(context)
        val projectId = getProjectId(context)
        val appId = getAppId(context)
        val storageBucket = getStorageBucket(context)

        // If no API key or Project ID is provided, treat as unconfigured/fallback mode.
        if (apiKey.isEmpty() || apiKey == "MY_FIREBASE_API_KEY" || projectId.isEmpty() || appId.isEmpty()) {
            Log.w(TAG, "Firebase configuration is incomplete. Standard local fallback mode active.")
            isInitialized = false
            return false
        }

        try {
            val existingApps = FirebaseApp.getApps(context)
            if (existingApps.isNotEmpty()) {
                if (force) {
                    for (app in existingApps) {
                        if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
                            app.delete()
                        }
                    }
                } else {
                    isInitialized = true
                    return true
                }
            }

            val optionsBuilder = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)

            if (storageBucket.isNotEmpty()) {
                optionsBuilder.setStorageBucket(storageBucket)
            } else {
                optionsBuilder.setStorageBucket("$projectId.appspot.com")
            }

            FirebaseApp.initializeApp(context, optionsBuilder.build())
            isInitialized = true
            Log.d(TAG, "Firebase initialized programmatically with success.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error programmatically initializing Firebase: ${e.message}", e)
            isInitialized = false
            return false
        }
    }

    fun getAuth(): FirebaseAuth? {
        return if (isInitialized) {
            try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        } else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isInitialized) {
            try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        } else null
    }

    fun getStorage(): FirebaseStorage? {
        return if (isInitialized) {
            try { FirebaseStorage.getInstance() } catch (e: Exception) { null }
        } else null
    }

    // --- High-fidelity image upload to Firebase Storage ---
    fun uploadBitmap(bitmap: Bitmap, pathOnStorage: String, onComplete: (String?) -> Unit) {
        val storage = getStorage()
        if (storage == null) {
            onComplete(null)
            return
        }
        try {
            val ref = storage.reference.child(pathOnStorage)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()
            ref.putBytes(data)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        onComplete(downloadUri.toString())
                    }.addOnFailureListener {
                        onComplete(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed putting image bytes: ${e.message}")
                    onComplete(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading bitmap: ${e.message}")
            onComplete(null)
        }
    }

    // --- Record sync and mapping to Cloud Firestore ---
    fun uploadRecordToFirestore(record: AnalysisRecord, originalUrl: String?, processedUrl: String?) {
        val firestore = getFirestore() ?: return
        try {
            val data = hashMapOf(
                "id" to record.id,
                "sampleId" to record.sampleId,
                "deviceType" to record.deviceType,
                "dilutionFactor" to record.dilutionFactor,
                "sampleVolume" to record.sampleVolume,
                "totalCount" to record.totalCount,
                "cfu" to record.cfu,
                "cfuScientific" to record.cfuScientific,
                "centerCount" to record.centerCount,
                "middleCount" to record.middleCount,
                "outerCount" to record.outerCount,
                "centerDensity" to record.centerDensity,
                "middleDensity" to record.middleDensity,
                "outerDensity" to record.outerDensity,
                "centerPercent" to record.centerPercent,
                "middlePercent" to record.middlePercent,
                "outerPercent" to record.outerPercent,
                "riskRating" to record.riskRating,
                "riskColor" to record.riskColor,
                "notes" to record.notes,
                "aiRecommendation" to record.aiRecommendation,
                "dateCreated" to record.dateCreated,
                "imagePath" to (originalUrl ?: record.imagePath ?: ""),
                "processedImagePath" to (processedUrl ?: record.processedImagePath ?: "")
            )

            firestore.collection("analysis_records")
                .document(record.sampleId)
                .set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Record successfully written to Firestore: ${record.sampleId}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore write error: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error packaging record: ${e.message}")
        }
    }

    fun syncRecordsFromFirestore(onSyncComplete: (List<AnalysisRecord>) -> Unit) {
        val firestore = getFirestore() ?: return
        try {
            firestore.collection("analysis_records")
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<AnalysisRecord>()
                    for (doc in result) {
                        try {
                            val record = AnalysisRecord(
                                id = (doc.getLong("id") ?: 0).toInt(),
                                sampleId = doc.getString("sampleId") ?: doc.id,
                                deviceType = doc.getString("deviceType") ?: "Catheter Swab",
                                dilutionFactor = doc.getDouble("dilutionFactor") ?: 1000.0,
                                sampleVolume = doc.getDouble("sampleVolume") ?: 1.0,
                                totalCount = (doc.getLong("totalCount") ?: 0).toInt(),
                                cfu = doc.getDouble("cfu") ?: 0.0,
                                cfuScientific = doc.getString("cfuScientific") ?: "0.0",
                                centerCount = (doc.getLong("centerCount") ?: 0).toInt(),
                                middleCount = (doc.getLong("middleCount") ?: 0).toInt(),
                                outerCount = (doc.getLong("outerCount") ?: 0).toInt(),
                                centerDensity = doc.getDouble("centerDensity") ?: 0.0,
                                middleDensity = doc.getDouble("middleDensity") ?: 0.0,
                                outerDensity = doc.getDouble("outerDensity") ?: 0.0,
                                centerPercent = doc.getDouble("centerPercent") ?: 0.0,
                                middlePercent = doc.getDouble("middlePercent") ?: 0.0,
                                outerPercent = doc.getDouble("outerPercent") ?: 0.0,
                                riskRating = doc.getString("riskRating") ?: "Low Risk",
                                riskColor = doc.getString("riskColor") ?: "#00B464",
                                notes = doc.getString("notes") ?: "",
                                aiRecommendation = doc.getString("aiRecommendation") ?: "",
                                dateCreated = doc.getLong("dateCreated") ?: System.currentTimeMillis(),
                                imagePath = doc.getString("imagePath"),
                                processedImagePath = doc.getString("processedImagePath")
                            )
                            list.add(record)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping single document to record: ${e.message}")
                        }
                    }
                    onSyncComplete(list)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore query error: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Firestore sync: ${e.message}")
        }
    }
}
