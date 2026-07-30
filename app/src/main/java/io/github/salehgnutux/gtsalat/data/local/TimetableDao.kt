package io.github.salehgnutux.gtsalat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimetableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<TimetableEntity>)

    @Query("SELECT * FROM timetable WHERE dateIso = :dateIso AND methodId = :methodId AND locKey = :locKey LIMIT 1")
    suspend fun day(dateIso: String, methodId: Int, locKey: String): TimetableEntity?

    @Query("SELECT * FROM timetable WHERE methodId = :methodId AND locKey = :locKey AND dateIso BETWEEN :from AND :to ORDER BY dateIso")
    suspend fun range(from: String, to: String, methodId: Int, locKey: String): List<TimetableEntity>

    @Query("SELECT COUNT(DISTINCT substr(dateIso,1,7)) FROM timetable WHERE methodId = :methodId AND locKey = :locKey")
    suspend fun cachedMonthsCount(methodId: Int, locKey: String): Int

    @Query("DELETE FROM timetable WHERE dateIso < :beforeIso")
    suspend fun deleteOlderThan(beforeIso: String)

    /** كلّ الأيّام المخزّنة (للتصدير/النسخ الاحتياطيّ). */
    @Query("SELECT * FROM timetable")
    suspend fun all(): List<TimetableEntity>

    @Query("SELECT COUNT(*) FROM timetable")
    suspend fun count(): Int
}
