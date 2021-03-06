package io.plastique.inject.modules

import com.facebook.flipper.core.FlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named

@Module
object DebugOkHttpInterceptorModule {
    @Provides
    fun provideInterceptors(): List<Interceptor> =
        listOf(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })

    @Provides
    @Named("network")
    fun provideNetworkInterceptors(flipperClient: FlipperClient): List<Interceptor> =
        listOf(FlipperOkhttpInterceptor(flipperClient.getPlugin(NetworkFlipperPlugin.ID)))
}
