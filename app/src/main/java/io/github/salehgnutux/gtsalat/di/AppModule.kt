package io.github.salehgnutux.gtsalat.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.salehgnutux.gtsalat.data.local.GtSalatDatabase
import io.github.salehgnutux.gtsalat.data.local.TimetableDao
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): GtSalatDatabase =
        Room.databaseBuilder(ctx, GtSalatDatabase::class.java, "gt_salat.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun timetableDao(db: GtSalatDatabase): TimetableDao = db.timetableDao()
}
