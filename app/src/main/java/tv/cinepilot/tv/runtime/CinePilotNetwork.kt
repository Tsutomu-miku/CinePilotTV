package tv.cinepilot.tv.runtime

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

object CinePilotNetwork {
    fun createClient(context: Context): OkHttpClient {
        val appContext = context.applicationContext
        val httpCache = Cache(
            File(appContext.cacheDir, "http_cache"),
            HTTP_CACHE_BYTES,
        )
        return OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun installImageLoader(context: Context, okHttpClient: OkHttpClient) {
        val appContext = context.applicationContext
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(appContext)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                okHttpClient
                            },
                        ),
                    )
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(appContext, 0.22)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(appContext.cacheDir, "coil_image_cache").toOkioPath())
                        .maxSizePercent(0.08)
                        .build()
                }
                .build()
        }
    }

    private const val HTTP_CACHE_BYTES = 64L * 1024L * 1024L
}
