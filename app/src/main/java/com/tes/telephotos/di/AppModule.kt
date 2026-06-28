package com.tes.telephotos.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.tes.telephotos.data.local.AppDatabase
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.local.prefs.SettingsManager
import com.tes.telephotos.data.telegram.TelegramBotWrapper
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
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "telephotos.db"
        ).build()
    }

    @Provides
    fun provideMediaDao(database: AppDatabase): MediaDao {
        return database.mediaDao()
    }

    @Provides
    @Singleton
    fun provideTelegramBotWrapper(
        @ApplicationContext context: Context,
        settingsManager: SettingsManager
    ): TelegramBotWrapper {
        return TelegramBotWrapper(context, settingsManager)
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}