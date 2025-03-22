package com.lumos.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Item
import com.lumos.domain.model.Material
import com.lumos.domain.model.Measurement

@Database(
    entities = [(Measurement::class), (Deposit::class), (Item::class), (Material::class), (Contract::class)],
    version = 4,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun stockDao(): StockDao
    abstract fun contractDao(): ContractDao

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
                        MIGRATION_3_4
                    ) // Certifique-se de que ambas estão aqui
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}