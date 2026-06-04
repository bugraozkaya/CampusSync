package com.bugra.campussync.di

import android.content.Context
import androidx.room.Room
import com.bugra.campussync.data.local.AnnouncementDao
import com.bugra.campussync.data.local.AppDatabase
import com.bugra.campussync.data.local.ScheduleDao
import com.bugra.campussync.utils.ThemePreferences
import com.bugra.campussync.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "campus_sync_db"
        ).build()
    }

    @Provides
    fun provideAnnouncementDao(database: AppDatabase): AnnouncementDao {
        return database.announcementDao()
    }

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }
}
