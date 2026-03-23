package com.jaytt.moveandmeds.di

import android.content.Context
import androidx.room.Room
import com.jaytt.moveandmeds.data.db.AppDatabase
import com.jaytt.moveandmeds.data.db.ContactDao
import com.jaytt.moveandmeds.data.db.ExerciseDao
import com.jaytt.moveandmeds.data.db.HistoryDao
import com.jaytt.moveandmeds.data.db.MedicineDao
import com.jaytt.moveandmeds.data.db.MovementSettingsDao
import com.jaytt.moveandmeds.data.repository.ContactRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "moveandmeds.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .build()

    @Provides
    fun provideMedicineDao(db: AppDatabase): MedicineDao = db.medicineDao()

    @Provides
    fun provideMovementSettingsDao(db: AppDatabase): MovementSettingsDao = db.movementSettingsDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun provideContactRepository(dao: ContactDao): ContactRepository = ContactRepository(dao)
}
