package com.novo.drive.core.media

import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.key.Keyer
import coil3.request.Options
import coil3.request.crossfade
import com.novo.drive.core.ocr.AndroidMlKitOcrExtractor
import com.novo.drive.core.ocr.OcrExtractor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

@UnstableApi
val mediaModule = module {
    single {
        ImageLoader.Builder(androidContext())
            .components {
                add(ThumbnailFetcherFactory(get<ThumbnailStore>()))
                add(Keyer<ThumbnailModel> { data, _: Options -> thumbnailCacheKey(data.fileId) })
            }
            .crossfade(true)
            .build()
    }
    singleOf(::AndroidThumbnailStore) bind ThumbnailStore::class
    singleOf(::AndroidMediaMetadataExtractor) bind MediaMetadataExtractor::class
    factoryOf(::TelegramDataSourceFactory)
    single<ThumbnailMemoryCache> { CoilThumbnailMemoryCache(get()) }
    single { PipController(androidContext()) }
    single<OcrExtractor> { AndroidMlKitOcrExtractor(androidContext()) }
    singleOf(::AndroidIplusAudioPlayer) bind com.novo.drive.core.bridge.MediaBridge::class
}
