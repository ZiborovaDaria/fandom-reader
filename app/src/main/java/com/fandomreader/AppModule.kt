package com.fandomreader

import android.content.Context
import androidx.room.Room
import com.fandomreader.data.LibraryRepositoryImpl
import com.fandomreader.data.db.FandomReaderDatabase
import com.fandomreader.data.db.LibraryDao
import com.fandomreader.domain.repository.LibraryRepository
import com.fandomreader.feature.translation.CursorProvider
import com.fandomreader.feature.translation.GeminiProvider
import com.fandomreader.feature.translation.ResolvingTranslationProvider
import com.fandomreader.feature.translation.TranslationPipeline
import com.fandomreader.feature.translation.TranslationProvider
import com.fandomreader.feature.translation.TranslationProviderSettings
import com.fandomreader.source.ao3.Ao3MetaParser
import com.fandomreader.source.api.BookClassifier
import com.fandomreader.source.api.BookMetaParser
import com.fandomreader.source.api.DefaultBookClassifier
import com.fandomreader.source.api.DeviceLibraryScanner
import com.fandomreader.source.api.ImportCoordinator
import com.fandomreader.source.api.OtherMetaParser
import com.fandomreader.source.ficbook.FicbookEpubParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun db(@ApplicationContext context: Context): FandomReaderDatabase =
        Room.databaseBuilder(context, FandomReaderDatabase::class.java, "fandom-reader.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun dao(db: FandomReaderDatabase): LibraryDao = db.libraryDao()

    @Provides
    @Singleton
    fun libraryRepository(impl: LibraryRepositoryImpl): LibraryRepository = impl

    @Provides
    @Singleton
    fun classifier(): BookClassifier = DefaultBookClassifier()

    @Provides
    @Singleton
    fun parsers(): List<@JvmSuppressWildcards BookMetaParser> = listOf(
        Ao3MetaParser(),
        FicbookEpubParser(),
        OtherMetaParser(),
    )

    @Provides
    @Singleton
    @Named("booksDir")
    fun booksDir(@ApplicationContext context: Context): File =
        File(context.filesDir, "books").also { it.mkdirs() }

    @Provides
    @Singleton
    fun importCoordinator(
        classifier: BookClassifier,
        parsers: @JvmSuppressWildcards List<BookMetaParser>,
        libraryRepository: LibraryRepository,
        @Named("booksDir") booksDir: File,
    ): ImportCoordinator = ImportCoordinator(classifier, parsers, libraryRepository, booksDir)

    @Provides
    @Singleton
    fun deviceLibraryScanner(): DeviceLibraryScanner = DeviceLibraryScanner(maxDepth = 3)

    @Provides
    @Singleton
    fun translationProvider(
        settings: TranslationProviderSettings,
        geminiProvider: GeminiProvider,
        cursorProvider: CursorProvider,
    ): TranslationProvider =
        ResolvingTranslationProvider(
            settings = settings,
            geminiProvider = geminiProvider,
            cursorProvider = cursorProvider,
        )

    @Provides
    @Singleton
    fun translationPipeline(
        provider: TranslationProvider,
        libraryRepository: LibraryRepository,
    ): TranslationPipeline = TranslationPipeline(provider, libraryRepository)
}
