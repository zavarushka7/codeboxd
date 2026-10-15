package ru.savushkina.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.savushkina.core.data.AuthRepositoryImpl
import ru.savushkina.core.data.FavouriteRepositoryImpl
import ru.savushkina.core.data.ItemRepositoryImpl
import ru.savushkina.core.data.ReviewRepositoryImpl
import ru.savushkina.core.data.SessionRepositoryImpl
import ru.savushkina.core.data.SettingsRepositoryImpl
import ru.savushkina.core.data.UserRepositoryImpl
import ru.savushkina.core.domain.AuthRepository
import ru.savushkina.core.domain.FavouriteRepository
import ru.savushkina.core.domain.ItemRepository
import ru.savushkina.core.domain.ReviewRepository
import ru.savushkina.core.domain.SessionRepository
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.domain.UserRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindFavouriteRepository(impl: FavouriteRepositoryImpl): FavouriteRepository


}