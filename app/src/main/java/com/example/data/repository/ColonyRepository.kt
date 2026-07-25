package com.example.data.repository

import com.example.data.database.ColonyDao
import com.example.data.model.AnalysisRecord
import com.example.data.model.UserRecord
import kotlinx.coroutines.flow.Flow

class ColonyRepository(private val colonyDao: ColonyDao) {
    val allRecords: Flow<List<AnalysisRecord>> = colonyDao.getAllRecords()

    suspend fun insertRecord(record: AnalysisRecord): Long {
        return colonyDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: AnalysisRecord) {
        colonyDao.deleteRecord(record)
    }

    suspend fun getRecordById(id: Int): AnalysisRecord? {
        return colonyDao.getRecordById(id)
    }

    suspend fun getUserByEmail(email: String): UserRecord? {
        return colonyDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: UserRecord) {
        colonyDao.insertUser(user)
    }
}
