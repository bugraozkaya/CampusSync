package com.bugra.campussync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AnnouncementEntity::class, ScheduleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun announcementDao(): AnnouncementDao
    abstract fun scheduleDao(): ScheduleDao
}
