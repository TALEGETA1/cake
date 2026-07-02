package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        OrderEntity::class,
        PaymentEntity::class,
        NotificationEntity::class,
        SavedReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CakeDatabase : RoomDatabase() {
    abstract fun cakeDao(): CakeDao

    companion object {
        @Volatile
        private var INSTANCE: CakeDatabase? = null

        fun getDatabase(context: Context): CakeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CakeDatabase::class.java,
                    "cake_scheduler_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
