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
    /**
     * OkHttpClient for API calls. No HTTP cache — API responses are dynamic and
     * should always reflect the latest server state.
     */
    fun createApiClient(context: Context): OkHttpClient {
        return baseBuilder(context).build()
    }

    /**
     * OkHttpClient for image loading. Has a dedicated HTTP cache so that image
     * responses don't get evicted by API or video traffic. Coil maintains its own
     * disk cache on top of this for decoded/transformed bitmaps.
     */
    fun createImageClient(context: Context): OkHttpClient {
        val appContext = context.applicationContext
        val imageHttpCache = Cache(
            File(appContext.cacheDir, "image_http_cache"),
            IMAGE_HTTP_CACHE_BYTES,
        )
        return baseBuilder(context)
            .cache(imageHttpCache)
            .build()
    }

    /**
     * OkHttpClient for video streaming (playback + offline downloads). No HTTP
     * cache — video streams are large, one-time, and would evict everything else.
     *
     * The base callTimeout of 60s is intentionally cleared here: direct-play video
     * streams and offline downloads are single HTTP calls that routinely exceed 60s
     * while data is actively flowing. `readTimeout` still protects against a silent
     * server stall, which is the failure mode we actually want to surface.
     */
    fun createStreamingClient(context: Context): OkHttpClient {
        return baseBuilder(context)
            .callTimeout(0, TimeUnit.SECONDS)
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

    private fun baseBuilder(context: Context): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
    }

    private const val IMAGE_HTTP_CACHE_BYTES = 48L * 1024L * 1024L
}
