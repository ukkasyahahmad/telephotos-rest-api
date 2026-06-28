package com.tes.telephotos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}