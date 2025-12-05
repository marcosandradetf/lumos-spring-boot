package com.lumos.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumos.data.converter.Converters
import com.lumos.domain.model.Contract
import com.lumos.domain.model.ContractItemBalance
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.DirectExecution
import com.lumos.domain.model.DirectExecutionStreet
import com.lumos.domain.model.DirectExecutionStreetItem
import com.lumos.domain.model.DirectReserve
import com.lumos.domain.model.InstallationView
import com.lumos.domain.model.Item
import com.lumos.domain.model.Maintenance
import com.lumos.domain.model.MaintenanceStreet
import com.lumos.domain.model.MaintenanceStreetItem
import com.lumos.domain.model.MaterialStock
import com.lumos.domain.model.OperationalUser
import com.lumos.domain.model.OrderMaterial
import com.lumos.domain.model.OrderMaterialItem
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.Stockist
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.domain.model.Team
import com.lumos.notifications.NotificationItem
import java.util.concurrent.Executors
import com.lumos.domain.model.PreMeasurementInstallation
import com.lumos.domain.model.PreMeasurementInstallationStreet
import com.lumos.domain.model.PreMeasurementInstallationItem

@Database(
    entities = [
        (PreMeasurement::class),
        (PreMeasurementStreet::class),
        (PreMeasurementStreetItem::class),

        (Contract::class),
        (Item::class),
        (ContractItemBalance::class),

        (NotificationItem::class),

        (SyncQueueEntity::class),

        (DirectExecution::class),
        (DirectReserve::class),
        (DirectExecutionStreet::class),
        (DirectExecutionStreetItem::class),

        (MaterialStock::class),
        (OrderMaterial::class),
        (OrderMaterialItem::class),

        (Stockist::class),
        (Deposit::class),

        (Maintenance::class),
        (MaintenanceStreet::class),
        (MaintenanceStreetItem::class),

        (OperationalUser::class),
        (Team::class),
        (PreMeasurementInstallation::class),
        (PreMeasurementInstallationStreet::class),
        (PreMeasurementInstallationItem::class),
    ],
    version = 16,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun preMeasurementDao(): PreMeasurementDao
    abstract fun contractDao(): ContractDao
    abstract fun notificationDao(): NotificationDao
    abstract fun queueDao(): QueueDao
    abstract fun directExecutionDao(): DirectExecutionDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun stockDao(): StockDao
    abstract fun teamDao(): TeamDao
    abstract fun preMeasurementInstallationDao(): PreMeasurementInstallationDao
    abstract fun viewDao(): ViewDao

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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    alter table sync_queue_entity
                    add column errorMessage text null
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS material_stock (
                            materialIdStock INTEGER NOT NULL PRIMARY KEY,
                            materialName TEXT NOT NULL,
                            specs TEXT,
                            stockQuantity REAL NOT NULL,
                            stockAvailable REAL NOT NULL,
                            requestUnit TEXT NOT NULL
                        );
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stockist (
                            stockistId INTEGER NOT NULL PRIMARY KEY,
                            stockistName TEXT NOT NULL,
                            stockistPhone TEXT,
                            depositId INTEGER NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deposit (
                            depositId INTEGER NOT NULL PRIMARY KEY,
                            depositName TEXT NOT NULL,
                            depositAddress TEXT,
                            depositPhone TEXT
                        );
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_material (
                            orderId TEXT NOT NULL PRIMARY KEY,
                            orderCode TEXT NOT NULL,
                            createdAt TEXT NOT NULL,
                            depositId INTEGER NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_material_item (
                            orderId TEXT NOT NULL,
                            materialId INTEGER NOT NULL,
                            PRIMARY KEY (orderId, materialId)
                        );
                """.trimIndent()
                )

                db.execSQL("alter table sync_queue_entity add column relatedUuid TEXT")

                db.execSQL("alter table material_stock RENAME COLUMN materialIdStock TO materialId")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS Maintenance (
                            maintenanceId TEXT NOT NULL,
                            contractId INTEGER NOT NULL,
                            pendingPoints INTEGER NOT NULL,
                            quantityPendingPoints INTEGER,
                            dateOfVisit TEXT NOT NULL,
                            type TEXT NOT NULL,
                            status TEXT NOT NULL,
                            PRIMARY KEY (maintenanceId, contractId)
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS MaintenanceStreet (
                            maintenanceStreetId TEXT NOT NULL,
                            maintenanceId TEXT NOT NULL,
                            address TEXT NOT NULL,
                            latitude REAL,
                            longitude REAL,
                            comment TEXT,
                            lastPower TEXT,
                            lastSupply TEXT,
                            currentSupply TEXT,
                            reason TEXT,
                            PRIMARY KEY (maintenanceStreetId, maintenanceId)
                        );
                """.trimIndent()
                )
                db.execSQL(
                    """
                        CREATE UNIQUE INDEX index_MaintenanceStreet_address_maintenanceId
                        ON MaintenanceStreet(address, maintenanceId);
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS MaintenanceStreetItem (
                            maintenanceId TEXT NOT NULL,
                            maintenanceStreetId TEXT NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            quantityExecuted REAL NOT NULL,
                            PRIMARY KEY (maintenanceId,maintenanceStreetId, materialStockId)
                        );
                """.trimIndent()
                )

                db.execSQL("drop table material_stock")
                db.execSQL("drop table material_stock")
                db.execSQL("drop table material_stock")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS material_stock (
                            materialId INTEGER NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            specs TEXT,
                            stockQuantity REAL NOT NULL,
                            stockAvailable REAL NOT NULL,
                            requestUnit TEXT NOT NULL,
                            type TEXT NOT NULL,
                            PRIMARY KEY (materialId, materialStockId)
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
                            materialQuantity TEXT NOT NULL,
                            requestUnit TEXT NOT NULL
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
                        materialQuantity TEXT NOT NULL,
                        streetId INTEGER NOT NULL,
                        requestUnit TEXT NOT NULL,
                        quantityExecuted TEXT
                    );
                """.trimIndent()
                )

                db.execSQL("delete from contracts")
                db.execSQL("alter table contracts add column hasMaintenance integer not null")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table maintenance add column responsible text")
                db.execSQL("alter table maintenance add column signPath text")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table maintenance add column signDate text")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table direct_execution_street add column finishAt text")
                db.execSQL("alter table direct_execution_street add column currentSupply text")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("drop table material_stock")
                db.execSQL("drop table MaintenanceStreetItem")
                db.execSQL("drop table direct_reserve")
                db.execSQL("drop table indirect_reserve")
                db.execSQL("drop table direct_execution_street_item")

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS material_stock (
                            materialId INTEGER NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            specs TEXT,
                            stockQuantity TEXT NOT NULL,
                            stockAvailable TEXT NOT NULL,
                            requestUnit TEXT NOT NULL,
                            type TEXT NOT NULL,
                            PRIMARY KEY (materialId, materialStockId)
                        );
                """.trimIndent()
                )


                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS MaintenanceStreetItem (
                            maintenanceId TEXT NOT NULL,
                            maintenanceStreetId TEXT NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            quantityExecuted TEXT NOT NULL,
                            PRIMARY KEY (maintenanceId,maintenanceStreetId, materialStockId)
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
                            materialQuantity TEXT NOT NULL,
                            requestUnit TEXT NOT NULL
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
                        materialQuantity TEXT NOT NULL,
                        streetId INTEGER NOT NULL,
                        requestUnit TEXT NOT NULL,
                        quantityExecuted TEXT
                    );
                """.trimIndent()
                )


                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS direct_execution_street_item (
                            directStreetItemId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            reserveId INTEGER NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            contractItemId INTEGER NOT NULL,
                            directStreetId INTEGER NOT NULL,
                            quantityExecuted TEXT NOT NULL
                        );
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS pre_measurement_street")
                db.execSQL("DROP TABLE IF EXISTS pre_measurement_street_item")

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS OperationalUser (
                            userId TEXT NOT NULL PRIMARY KEY,
                            completeName TEXT NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS Team (
                            teamId INTEGER NOT NULL PRIMARY KEY,
                            depositName TEXT NOT NULL,
                            teamName TEXT NOT NULL,
                            plateVehicle TEXT NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS pre_measurement (
                            preMeasurementId TEXT NOT NULL PRIMARY KEY,
                            contractId INTEGER NOT NULL,
                            contractor TEXT NOT NULL
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS pre_measurement_street (
                            preMeasurementStreetId TEXT NOT NULL PRIMARY KEY,
                            preMeasurementId TEXT NOT NULL,
                            lastPower TEXT,
                            latitude REAL,
                            longitude REAL,
                            address TEXT,
                            photoUri TEXT,
                            status TEXT
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS pre_measurement_street_item (
                            preMeasurementStreetId TEXT NOT NULL,
                            contractReferenceItemId INTEGER NOT NULL,
                            preMeasurementId TEXT NOT NULL,
                            measuredQuantity TEXT NOT NULL,
                            PRIMARY KEY (preMeasurementStreetId, contractReferenceItemId, preMeasurementId)
                        );
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table maintenance add column executorsIds TEXT")
                db.execSQL("DROP INDEX IF EXISTS index_direct_execution_street_address")

            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // pre measurement
                db.execSQL("delete from pre_measurement")
                db.execSQL("alter table pre_measurement add column status text not null")
                db.execSQL("alter table team add column notificationTopic text")
                db.execSQL("alter table pre_measurement add column startedAt text not null")

                // direct execution
                db.execSQL("ALTER TABLE direct_execution ADD COLUMN responsible TEXT")
                db.execSQL("ALTER TABLE direct_execution ADD COLUMN signPath TEXT")
                db.execSQL("ALTER TABLE direct_execution ADD COLUMN signDate TEXT")
                db.execSQL("alter table direct_execution ADD COLUMN executorsIds TEXT")
                db.execSQL("alter table direct_execution ADD COLUMN contractId INT")

                // installation
                db.execSQL("DROP TABLE IF EXISTS indirect_execution")
                db.execSQL("DROP TABLE IF EXISTS indirect_reserve")

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS PreMeasurementInstallation (
                            preMeasurementId TEXT NOT NULL PRIMARY KEY,
                            contractId INT NOT NULL,
                            contractor TEXT NOT NULL,
                            instructions TEXT,
                            creationDate TEXT NOT NULL,
                            status TEXT NOT NULL,
                            responsible TEXT,
                            signPath TEXT,
                            signDate TEXT,
                            executorsIds TEXT
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS PreMeasurementInstallationStreet (
                            preMeasurementStreetId TEXT NOT NULL PRIMARY KEY,
                            preMeasurementId TEXT NOT NULL,
                            address TEXT NOT NULL,
                            priority INTEGER NOT NULL,
                            latitude REAL,
                            longitude REAL,
                            lastPower TEXT,
                            photoUrl TEXT,
                            photoExpiration INTEGER,
                            objectUri TEXT,
                            
                            status TEXT NOT NULL,
                            installationPhotoUri TEXT,
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS PreMeasurementInstallationItem (
                            preMeasurementStreetId TEXT NOT NULL,
                            materialStockId INTEGER NOT NULL,
                            contractItemId INTEGER NOT NULL,
                            materialName TEXT NOT NULL,
                            materialQuantity TEXT NOT NULL,
                            executedQuantity TEXT NOT NULL,
                            requestUnit TEXT NOT NULL,
                            specs TEXT,
                            PRIMARY KEY (preMeasurementStreetId, materialStockId, contractItemId)
                        );
                """.trimIndent()
                )

                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS ContractItemBalance (
                            contractItemId INTEGER NOT NULL PRIMARY KEY,
                            currentBalance TEXT NOT NULL,
                            itemName TEXT NOT NULL
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
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                ).setQueryCallback({ sqlQuery, bindArgs ->
                    Log.d("RoomDB", "SQL executed: $sqlQuery with args: $bindArgs")
                }, Executors.newSingleThreadExecutor()).build()

                INSTANCE = instance
                instance
            }
        }
    }
}