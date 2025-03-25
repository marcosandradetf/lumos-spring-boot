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
import com.lumos.domain.model.Material
import com.lumos.domain.model.PreMeasurement
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.service.NotificationItem
import java.util.concurrent.Executors

@Database(
    entities = [(PreMeasurement::class), (PreMeasurementStreetItem::class), (Deposit::class), (PreMeasurementStreet::class), (Material::class), (Contract::class), (NotificationItem::class)],
    version = 6,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun preMeasurementDao(): PreMeasurementDao
    abstract fun stockDao(): StockDao
    abstract fun contractDao(): ContractDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN number Varchar(15) NULL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN city Varchar(50) NOT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Verifique se as tabelas existem antes de removê-las
                db.execSQL("DROP TABLE IF EXISTS materials")
                db.execSQL("DROP TABLE IF EXISTS measurements")
                db.execSQL("DROP TABLE IF EXISTS items")

                // Criação das novas tabelas com a estrutura corrigida
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS materials " +
                            "(materialId INTEGER PRIMARY KEY NOT NULL, " +
                            "materialName TEXT, " +
                            "materialPower TEXT, " +
                            "materialAmps TEXT, " +
                            "materialLength TEXT" +
                            ")"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS measurements " +
                            "(measurementId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "lastPower TEXT, " +
                            "latitude REAL NOT NULL, " +
                            "longitude REAL NOT NULL, " +
                            "address TEXT, " +
                            "number TEXT, " +
                            "city TEXT NOT NULL, " +
                            "deviceId TEXT NOT NULL, " +
                            "synced INTEGER NOT NULL DEFAULT 0" +
                            ")"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS items " +
                            "(itemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "materialId TEXT NOT NULL," +
                            "materialQuantity INTEGER NOT NULL," +
                            "measurementId INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Criação das novas tabelas com a estrutura corrigida
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS contracts " +
                            "(contractId INTEGER PRIMARY KEY NOT NULL, " +
                            "contractor TEXT NOT NULL, " +
                            "contractFile TEXT, " +
                            "createdBy TEXT NOT NULL, " +
                            "createdAt TEXT NOT NULL, " +
                            "status TEXT NOT NULL )"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Criação das novas tabelas com a estrutura corrigida
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notificationsItems " +
                            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "title TEXT NOT NULL, " +
                            "body TEXT NOT NUL, " +
                            "action TEXT NOT NULL, " +
                            "time TEXT NOT NULL, " +
                            "type TEXT NOT NULL )"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remover tabelas antigas
                db.execSQL("DROP TABLE IF EXISTS measurements")
                db.execSQL("DROP TABLE IF EXISTS items")
                db.execSQL("DROP TABLE IF EXISTS notificationsItems")

                // Criar novas tabelas com estrutura corrigida
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pre_measurements " +
                            "(preMeasurementId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "contractID INTEGER NOT NULL, " +
                            "deviceId TEXT NOT NULL, " +
                            "status TEXT NOT NULL, " +
                            "synced INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pre_measurement_streets " +
                            "(preMeasurementStreetId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "preMeasurementId INTEGER NOT NULL, " +
                            "lastPower TEXT, " +
                            "latitude REAL NOT NULL, " +
                            "longitude REAL NOT NULL, " +
                            "address TEXT, " +
                            "number TEXT, " +
                            "city TEXT NOT NULL, " +
                            "deviceId TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pre_measurement_street_items " +
                            "(preMeasurementItemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "preMeasurementStreetId INTEGER NOT NULL, " +
                            "materialId INTEGER NOT NULL, " +
                            "materialQuantity INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notifications_items " +
                            "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "title TEXT NOT NULL, " +
                            "body TEXT NOT NULL, " +
                            "action TEXT NOT NULL, " +
                            "time TEXT NOT NULL, " +
                            "type TEXT NOT NULL)"
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
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .setQueryCallback({ sqlQuery, bindArgs -> Log.d("RoomDB", "SQL executed: $sqlQuery with args: $bindArgs") }, Executors.newSingleThreadExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }


    }
}