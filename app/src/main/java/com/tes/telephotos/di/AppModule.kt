package com.tes.telephotos.di

import android.content.Context
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
    fun provideTelegramClientWrapper(
        @ApplicationContext context: Context
    ): TelegramClientWrapper {
        return TelegramClientWrapper(context)
    }
}