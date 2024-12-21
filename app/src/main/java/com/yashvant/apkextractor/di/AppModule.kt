package com.yashvant.apkextractor.di

import android.content.Context
import com.yashvant.apkextractor.data.storage.CloudStorage
import com.yashvant.apkextractor.data.storage.GoogleDriveStorage
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
    fun provideCloudStorage(googleDriveStorage: GoogleDriveStorage): GoogleDriveStorage = googleDriveStorage
} 