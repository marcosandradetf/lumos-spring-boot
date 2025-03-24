package com.lumos.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.Material
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.service.NotificationItem

@Database(
    entities = [(PreMeasurementStreet::class), (Deposit::class), (PreMeasurementStreetItem::class), (Material::class), (Contract::class), (NotificationItem::class)],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
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


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumos_op_db"
                )
                    .addMigrations(
//                        MIGRATION_1_2,
                    ) // Certifique-se de que ambas estão aqui
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}