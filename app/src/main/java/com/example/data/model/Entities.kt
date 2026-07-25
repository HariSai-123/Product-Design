package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "analysis_records")
data class AnalysisRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sampleId: String,
    val deviceType: String,
    val dilutionFactor: Double,
    val sampleVolume: Double,
    val totalCount: Int,
    val cfu: Double,
    val cfuScientific: String,
    val centerCount: Int,
    val middleCount: Int,
    val outerCount: Int,
    val centerDensity: Double,
    val middleDensity: Double,
    val outerDensity: Double,
    val centerPercent: Double,
    val middlePercent: Double,
    val outerPercent: Double,
    val riskRating: String, // Low Risk, Medium Risk, High Risk
    val riskColor: String,  // Hex color representation
    val notes: String = "",
    val aiRecommendation: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val processedImagePath: String? = null
) : Serializable

@Entity(tableName = "users")
data class UserRecord(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val fullName: String,
    val isAdmin: Boolean = false,
    val dateRegistered: Long = System.currentTimeMillis()
) : Serializable
