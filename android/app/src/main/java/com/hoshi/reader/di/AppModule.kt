package com.hoshi.reader.di

import com.hoshi.reader.core.epub.BookProcessor
import com.hoshi.reader.core.epub.EpubParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideEpubParser(): EpubParser = EpubParser()

    @Provides
    fun provideBookProcessor(): BookProcessor = BookProcessor()
}
