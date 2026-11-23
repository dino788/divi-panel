package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        CategoryEntity::class,
        MediaItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DiviDatabase : RoomDatabase() {
    abstract fun diviDao(): DiviDao

    companion object {
        @Volatile
        private var INSTANCE: DiviDatabase? = null

        fun getInstance(context: Context): DiviDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiviDatabase::class.java,
                    "divi_iptv_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
