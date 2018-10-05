package io.plastique.core.client

import android.content.Context
import com.squareup.moshi.Moshi
import io.plastique.core.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    context: Context,
    config: ApiConfiguration,
    callAdapterFactory: ApiCallAdapterFactory,
    authInterceptor: AuthInterceptor,
    okHttpClient: OkHttpClient,
    moshi: Moshi
) {
    private val retrofit: Retrofit
    private val responseCacheDir = File(context.cacheDir, "okhttp")

    init {
        retrofit = Retrofit.Builder()
                .baseUrl(config.apiUrl)
                .client(createOkHttpClient(authInterceptor, okHttpClient))
                .addCallAdapterFactory(callAdapterFactory)
                .addConverterFactory(StringConverterFactory())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .validateEagerly(BuildConfig.DEBUG)
                .build()
    }

    fun <T> getService(service: Class<T>): T {
        return retrofit.create(service)
    }

    private fun createOkHttpClient(authInterceptor: AuthInterceptor, baseOkHttpClient: OkHttpClient): OkHttpClient {
        val builder = baseOkHttpClient.newBuilder()
        builder.interceptors().add(0, authInterceptor)
        return builder
                .authenticator(authInterceptor)
                .cache(Cache(responseCacheDir, MAX_RESPONSE_CACHE_SIZE))
                .build()
    }

    companion object {
        private const val MAX_RESPONSE_CACHE_SIZE = 5L * 1024 * 1024
    }
}
