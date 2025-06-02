package com.lumos.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lumos.domain.model.Contract
import com.lumos.domain.model.Deposit
import com.lumos.domain.model.Execution
import com.lumos.domain.model.Material
import com.lumos.domain.model.PreMeasurementStreet
import com.lumos.domain.model.PreMeasurementStreetItem
import com.lumos.domain.model.Reserve
import com.lumos.domain.model.SyncQueueEntity
import com.lumos.notifications.NotificationItem
import java.util.concurrent.Executors

@Database(
    entities = [
        (PreMeasurementStreetItem::class),
        (Deposit::class),
        (PreMeasurementStreet::class),
        (Material::class),
        (Contract::class),
        (NotificationItem::class),
        (SyncQueueEntity::class),
        (Execution::class),
        (Reserve::class)],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun preMeasurementDao(): PreMeasurementDao
    abstract fun contractDao(): ContractDao
    abstract fun notificationDao(): NotificationDao
    abstract fun queueDao(): QueueDao
    abstract fun executionDao(): ExecutionDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                Log.d("RoomDB", "Executando MIGRATION_1_2")
//
//                // Desativa restrições de Foreign Key temporariamente
//                db.execSQL("PRAGMA foreign_keys=OFF")
//

//
//                Log.d("RoomDB", "MIGRATION_1_2 concluída com sucesso")
//            }
//        }


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(
//                        MIGRATION_1_2,
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