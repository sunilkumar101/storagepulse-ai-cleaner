package com.storagepulse.cleaner.di

import android.content.Context
import androidx.room.Room
import com.storagepulse.cleaner.data.db.AppDatabase
import com.storagepulse.cleaner.data.dedup.MediaFingerprintDao
import com.storagepulse.cleaner.data.trash.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideTrashDao(db: AppDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideVaultDao(db: AppDatabase): com.storagepulse.cleaner.data.vault.VaultDao = db.vaultDao()

    @Provides
    fun provideFingerprintDao(db: AppDatabase): MediaFingerprintDao = db.fingerprintDao()
}
