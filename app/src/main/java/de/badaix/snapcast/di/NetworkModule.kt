package de.badaix.snapcast.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.badaix.snapcast.data.remote.SnapcastApiService
import de.badaix.snapcast.data.repository.SnapcastRepositoryImpl
import de.badaix.snapcast.domain.repository.SnapcastRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSnapcastApiService(): SnapcastApiService {
        // Default to localhost, but this can be configured via Settings or user input
        // Example: host = "192.168.1.100", port = 1705
        // Note: Raw TCP socket uses port 1705 (not HTTP port 1780)
        return SnapcastApiService(host = "localhost", port = 1705)
    }

    @Provides
    @Singleton
    fun provideSnapcastRepository(
        apiService: SnapcastApiService
    ): SnapcastRepository {
        return SnapcastRepositoryImpl(apiService)
    }
}
