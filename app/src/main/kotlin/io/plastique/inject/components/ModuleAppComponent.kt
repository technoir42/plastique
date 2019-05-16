package io.plastique.inject.components

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import io.plastique.BasePlastiqueApplication
import io.plastique.inject.AppComponent
import io.plastique.inject.modules.ApiModule
import io.plastique.inject.modules.AppModule
import io.plastique.inject.modules.DaoModule
import io.plastique.inject.modules.DatabaseModule
import io.plastique.inject.modules.DeviationsModule
import io.plastique.inject.modules.InitializerModule
import io.plastique.inject.modules.NetworkModule
import io.plastique.inject.modules.OkHttpInterceptorModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApiModule::class,
    AppModule::class,
    DatabaseModule::class,
    DaoModule::class,
    DeviationsModule::class,
    InitializerModule::class,
    NetworkModule::class,
    OkHttpInterceptorModule::class
])
interface ModuleAppComponent : AppComponent {
    override fun createActivityComponent(): ModuleActivityComponent

    fun inject(application: BasePlastiqueApplication)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance application: Application): ModuleAppComponent
    }
}
