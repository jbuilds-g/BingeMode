package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Show
import com.example.data.model.Setting

@Database(entities = [Show::class, Setting::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BingeModeDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: BingeModeDatabase? = null

        // Non-destructive migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add the new nullable releaseDate column without deleting the user's existing records
                db.execSQL("ALTER TABLE shows ADD COLUMN releaseDate TEXT DEFAULT NULL")
            }
        }

        // Non-destructive migration from version 2 to 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckTime TEXT NOT NULL DEFAULT '20:00'")
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckType TEXT NOT NULL DEFAULT 'daily'")
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE shows ADD COLUMN autoCheckLastRun INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shows ADD COLUMN watchedEpisodes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shows ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'tv'")
                db.execSQL("UPDATE shows SET mediaType = CASE WHEN status = 'Movie' THEN 'movie' ELSE 'tv' END")
            }
        }

        fun getDatabase(context: Context): BingeModeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BingeModeDatabase::class.java,
                    "bingemode_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
