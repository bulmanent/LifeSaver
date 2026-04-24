package com.lifesaver.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifesaver.data.dao.DocumentGroupDao
import com.lifesaver.data.dao.DocumentPageDao
import com.lifesaver.data.entity.DocumentGroupEntity
import com.lifesaver.data.entity.DocumentPageEntity

@Database(
    entities = [DocumentGroupEntity::class, DocumentPageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentGroupDao(): DocumentGroupDao
    abstract fun documentPageDao(): DocumentPageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE document_groups ADD COLUMN description TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifesaver.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
