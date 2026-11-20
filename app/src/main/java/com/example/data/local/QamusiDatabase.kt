package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.WordEntry
import com.example.data.model.WordRevision

@Database(
    entities = [WordEntry::class, WordRevision::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(WordTypeConverters::class)
abstract class QamusiDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun wordRevisionDao(): WordRevisionDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE word_entries ADD COLUMN prepositions TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN collocations TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN linguisticNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN examples TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN personalHint TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN difficulty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN studyTags TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE word_entries ADD COLUMN lastWrongAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS word_revisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        wordId INTEGER NOT NULL,
                        snapshotJson TEXT NOT NULL,
                        changeSummaryAr TEXT NOT NULL,
                        editedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_word_revisions_wordId_editedAt ON word_revisions(wordId, editedAt)"
                )
            }
        }

        fun getInstance(context: Context): QamusiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QamusiDatabase::class.java,
                    "qamusi_dictionary.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
