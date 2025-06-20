package com.lumos.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Execution
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.Item
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreetPhoto
import com.lumos.domain.model.Reserve
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.notifications.NotificationItem
import java.util.concurrent.Executors

@Database(
    entities = [
        (PreMeasurementStreetItem::class),
        (Deposit::class),
        (PreMeasurementStreet::class),
        (Item::class),
        (Contract::class),
        (NotificationItem::class),
        (SyncQueueEntity::class),
        (Execution::class),
        (Reserve::class),
        (PreMeasurementStreetPhoto::class),
        (DirectExecution::class)],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun preMeasurementDao(): PreMeasurementDao
    abstract fun contractDao(): ContractDao
    abstract fun notificationDao(): NotificationDao
    abstract fun queueDao(): QueueDao
    abstract fun executionDao(): ExecutionDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("drop table executions")
                db.execSQL("drop table reserves")

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS direct_execution (
                            contractId INTEGER NOT NULL PRIMARY KEY,
                            streetName TEXT,
                            streetNumber TEXT,
                            streetHood TEXT,
                            city TEXT,
                            state TEXT,
                            executionStatus TEXT NOT NULL,
                            type TEXT NOT NULL,
                            itemsQuantity INTEGER NOT NULL,
                            creationDate TEXT NOT NULL,
                            latitude REAL,
                            longitude REAL,
                            photoUri TEXT,
                            contractor TEXT NOT NULL,
                            instructions TEXT
                        );
                    """.trimIndent()
                )
            }
        }


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                    )
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        Log.d("RoomDB", "SQL executed: $sqlQuery with args: $bindArgs")
                    }, Executors.newSingleThreadExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}