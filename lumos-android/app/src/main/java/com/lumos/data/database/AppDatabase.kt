package com.lumos.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.IndirectExecution
import com.lumos.domain.model.IndirectReserve
import com.lumos.domain.model.Item
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.PreMeasurementStreetPhoto
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.notifications.NotificationItem
import java.util.concurrent.Executors

@Database(
    entities = [
        (PreMeasurementStreet::class),
        (PreMeasurementStreetItem::class),
        (PreMeasurementStreetPhoto::class),

        (Contract::class),
        (Item::class),

        (NotificationItem::class),

        (SyncQueueEntity::class),

        (IndirectExecution::class),
        (IndirectReserve::class),

        (DirectExecution::class),
        (DirectReserve::class),
        (DirectExecutionStreet::class),
        (DirectExecutionStreetItem::class),
    ],
    version = 4,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun preMeasurementDao(): PreMeasurementDao
    abstract fun contractDao(): ContractDao
    abstract fun notificationDao(): NotificationDao
    abstract fun queueDao(): QueueDao
    abstract fun indirectExecutionDao(): IndirectExecutionDao
    abstract fun directExecutionDao(): DirectExecutionDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS executions;")
                db.execSQL("DROP TABLE IF EXISTS reserves;")
                db.execSQL("DROP TABLE IF EXISTS deposits;")

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS direct_reserve (
                            materialStockId INTEGER NOT NULL,
                            contractItemId INTEGER NOT NULL,
                            contractId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            materialQuantity REAL NOT NULL,
                            requestUnit TEXT NOT NULL,
                            PRIMARY KEY(materialStockId, contractItemId)
                        );
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_execution (
                            contractId INTEGER NOT NULL PRIMARY KEY,
                            executionStatus TEXT NOT NULL,
                            type TEXT NOT NULL,
                            itemsQuantity INTEGER NOT NULL,
                            creationDate TEXT NOT NULL,
                            contractor TEXT NOT NULL,
                            instructions TEXT
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_execution_street (
                            directStreetId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            address TEXT NOT NULL,
                            latitude REAL,
                            longitude REAL,
                            photoUri TEXT,
                            deviceId TEXT NOT NULL,
                            contractId INTEGER NOT NULL,
                            contractor TEXT NOT NULL,
                            lastPower TEXT,
                            UNIQUE(address)
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_direct_execution_street_address 
                        ON direct_execution_street(address);
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_execution_street_item (
                            directStreetItemId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            materialStockId INTEGER NOT NULL,
                            contractItemId INTEGER NOT NULL,
                            directStreetId INTEGER NOT NULL,
                            quantityExecuted REAL NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS indirect_reserve (
                        reserveId INTEGER NOT NULL PRIMARY KEY,
                        contractId INTEGER NOT NULL,
                        contractItemId INTEGER NOT NULL,
                        materialName TEXT NOT NULL,
                        materialQuantity REAL NOT NULL,
                        streetId INTEGER NOT NULL,
                        requestUnit TEXT NOT NULL,
                        quantityExecuted REAL
                    );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS indirect_execution (
                        streetId INTEGER NOT NULL PRIMARY KEY,
                        contractId INTEGER NOT NULL,
                        streetName TEXT NOT NULL,
                        streetNumber TEXT,
                        streetHood TEXT,
                        city TEXT,
                        state TEXT,
                        executionStatus TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        itemsQuantity INTEGER NOT NULL,
                        creationDate TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        photoUri TEXT,
                        contractor TEXT NOT NULL
                    );
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS direct_execution")
                db.execSQL("DROP TABLE IF EXISTS direct_reserve")
                db.execSQL("delete from direct_execution_street")
                db.execSQL("delete from direct_execution_street_item")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_execution (
                            directExecutionId INTEGER NOT NULL PRIMARY KEY,
                            description TEXT NOT NULL,
                            instructions TEXT,
                            executionStatus TEXT NOT NULL,
                            type TEXT NOT NULL,
                            itemsQuantity INTEGER NOT NULL,
                            creationDate TEXT NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_reserve (
                            reserveId INTEGER NOT NULL PRIMARY KEY,
                            directExecutionId INTEGER NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            contractItemId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            materialQuantity REAL NOT NULL,
                            requestUnit TEXT NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    alter table direct_execution_street
                    rename column contractId to directExecutionId;
                """.trimIndent()
                )

                db.execSQL(
                    """
                    alter table direct_execution_street
                    rename column contractor to description;
                """.trimIndent()
                )

                db.execSQL(
                    """
                    alter table direct_execution_street_item
                    add column reserveId INTEGER NOT NULL;
                """.trimIndent()
                )


            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM direct_execution_street")

                db.execSQL("DELETE FROM direct_execution_street_item")

                db.execSQL(
                    """
                        ALTER TABLE direct_execution_street_item
                        ADD COLUMN materialName TEXT NOT NULL
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
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                ).setQueryCallback({ sqlQuery, bindArgs ->
                    Log.d("RoomDB", "SQL executed: $sqlQuery with args: $bindArgs")
                }, Executors.newSingleThreadExecutor()).build()

                INSTANCE = instance
                instance
            }
        }
    }
}