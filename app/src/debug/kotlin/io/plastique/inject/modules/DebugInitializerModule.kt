package io.plastique.inject.modules

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.plastique.core.init.FlipperInitializer
import io.plastique.core.init.Initializer
import io.plastique.core.init.LeakCanaryInitializer
import io.plastique.core.init.StethoInitializer
import io.plastique.core.init.TimberInitializer

@Module(includes = [InitializerModule::class])
interface DebugInitializerModule {
    @Binds
    @IntoSet
    fun bindFlipperInitializer(impl: FlipperInitializer): Initializer

    @Binds
    @IntoSet
    fun bindLeakCanaryInitializer(impl: LeakCanaryInitializer): Initializer

    @Binds
    @IntoSet
    fun bindStethoInitializer(impl: StethoInitializer): Initializer

    @Binds
    @IntoSet
    fun bindTimberInitializer(impl: TimberInitializer): Initializer
}