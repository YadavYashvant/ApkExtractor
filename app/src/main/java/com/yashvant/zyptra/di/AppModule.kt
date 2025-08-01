package com.yashvant.zyptra.di

import android.content.Context
import com.yashvant.zyptra.data.storage.CloudStorage
import com.yashvant.zyptra.data.storage.GoogleDriveStorage
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
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideCloudStorage(googleDriveStorage: GoogleDriveStorage): CloudStorage = googleDriveStorage
}

