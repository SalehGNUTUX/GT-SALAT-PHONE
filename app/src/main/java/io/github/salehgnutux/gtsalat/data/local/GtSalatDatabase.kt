package io.github.salehgnutux.gtsalat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TimetableEntity::class], version = 1, exportSchema = false)
abstract class GtSalatDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
}
