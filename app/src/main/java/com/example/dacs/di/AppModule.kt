package com.example.dacs.di

import android.content.Context
import com.example.dacs.config.CloudinaryConfig
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
    fun provideCloudinaryConfig(@ApplicationContext context: Context): CloudinaryConfig {
        return CloudinaryConfig(context)
    }
} 