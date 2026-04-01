package com.example.tasksbot.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoomEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM rooms WHERE name = '3 этаж' AND EXISTS (SELECT 1 FROM rooms WHERE name = 'Арендаторы 3 этаж')",
                )
                db.execSQL("UPDATE rooms SET name = 'Арендаторы 3 этаж' WHERE name = '3 этаж'")
                db.execSQL("UPDATE rooms SET area = 96.3 WHERE name = 'Арендаторы 2 этаж'")
                db.execSQL(
                    "DELETE FROM rooms WHERE name = '2 этаж' AND EXISTS (SELECT 1 FROM rooms WHERE name = 'Арендаторы 2 этаж')",
                )
                db.execSQL(
                    "UPDATE rooms SET name = 'Арендаторы 2 этаж', area = 96.3 WHERE name = '2 этаж'",
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasksbot.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

