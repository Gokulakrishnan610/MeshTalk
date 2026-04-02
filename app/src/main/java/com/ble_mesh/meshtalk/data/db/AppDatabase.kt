package com.ble_mesh.meshtalk.data.db

import android.content.Context
import androidx.room.*
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus

/**
 * Room Database singleton for MeshTalk.
 * Includes a TypeConverter for the [MessageStatus] enum.
 */
@Database(
    entities = [MeshMessage::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    class Converters {
        @TypeConverter
        fun fromStatus(status: MessageStatus): String = status.name

        @TypeConverter
        fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meshtalk.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
