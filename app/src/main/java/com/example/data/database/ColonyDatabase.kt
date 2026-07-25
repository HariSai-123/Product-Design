package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AnalysisRecord
import com.example.data.model.UserRecord

@Database(entities = [AnalysisRecord::class, UserRecord::class], version = 1, exportSchema = false)
abstract class ColonyDatabase : RoomDatabase() {
    abstract fun colonyDao(): ColonyDao

    companion object {
        @Volatile
        private var INSTANCE: ColonyDatabase? = null

        fun getDatabase(context: Context): ColonyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ColonyDatabase::class.java,
                    "colony_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
