package com.tes.telephotos.di

import android.content.Context
import androidx.room.Room
import com.tes.telephotos.data.local.AppDatabase
import com.tes.telephotos.data.local.MediaDao
import com.tes.telephotos.data.telegram.TelegramClientWrapper
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
    fun provideTelegramClientWrapper(
        @ApplicationContext context: Context
    ): TelegramClientWrapper {
        return TelegramClientWrapper(context)
    }
}