package com.woniu0936.verses.sample.tiktok

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton manager for ExoPlayer's video cache.
 * Ensures only one SimpleCache instance exists for the given directory.
 */
@UnstableApi
object VideoCacheManager {
    private const val CACHE_SIZE = 100 * 1024 * 1024L // 100 MB
    private var simpleCache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: run {
            val cacheDir = File(context.cacheDir, "verses_video_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
            }
        }
    }
}
