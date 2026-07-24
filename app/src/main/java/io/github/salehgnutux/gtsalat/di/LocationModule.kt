package io.github.salehgnutux.gtsalat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.salehgnutux.gtsalat.data.location.LocationProvider
import io.github.salehgnutux.gtsalat.data.location.PlatformLocationProvider

/**
 * يربط واجهة LocationProvider بالتطبيق الخاصّ بالنكهة (foss أو full).
 * كلا التطبيقين يحملان نفس الاسم المؤهّل، فيُحلّ حسب النكهة عند البناء.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    abstract fun bindLocationProvider(impl: PlatformLocationProvider): LocationProvider
}
