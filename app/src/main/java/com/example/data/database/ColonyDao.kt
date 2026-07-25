package com.example.data.database

import androidx.room.*
import com.example.data.model.AnalysisRecord
import com.example.data.model.UserRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ColonyDao {
    @Query("SELECT * FROM analysis_records ORDER BY dateCreated DESC")
    fun getAllRecords(): Flow<List<AnalysisRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AnalysisRecord): Long

    @Delete
    suspend fun deleteRecord(record: AnalysisRecord)

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    suspend fun getRecordById(id: Int): AnalysisRecord?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserRecord)
}
