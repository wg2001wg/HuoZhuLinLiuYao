package com.liuyao.huozhulin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecordEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RecordDao

    companion object {
        /** 从 v1 升级到 v2：新增 aiChat 列，保存用户与 AI 的继续提问对话 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE records ADD COLUMN aiChat TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "huozhulin.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
