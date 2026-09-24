package com.jbuilds.bingemode.data.database

import androidx.room.*
import com.jbuilds.bingemode.data.model.Show
import com.jbuilds.bingemode.data.model.Setting
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows ORDER BY updated DESC")
    fun getAllShowsFlow(): Flow<List<Show>>

    @Query("SELECT * FROM shows")
    suspend fun getAllShows(): List<Show>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: Int): Show?

    @Query("SELECT * FROM shows WHERE id = :id")
    fun getShowFlowById(id: Int): Flow<Show?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: Show): Long

    @Delete
    suspend fun deleteShow(show: Show)

    @Query("DELETE FROM shows")
    suspend fun deleteAllShows()

    @Query("DELETE FROM shows WHERE id = :id")
    suspend fun deleteShowById(id: Int)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): Setting?

    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSettingValueFlow(key: String): Flow<Setting?>

    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<Setting>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: Setting)
}
