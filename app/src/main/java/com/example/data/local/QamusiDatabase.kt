package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.WordEntry

@Database(
    entities = [WordEntry::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(WordTypeConverters::class)
abstract class QamusiDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: QamusiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsRepetitions INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsIntervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsEaseFactor REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsNextReviewAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsLastReviewedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsTotalReviews INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN srsCorrectCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE word_entries ADD COLUMN ipa TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): QamusiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QamusiDatabase::class.java,
                    "qamusi_dictionary.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
