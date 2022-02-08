package com.example.happyplaces

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HappyPlaceDao {

    @Insert
    suspend fun insert(happyPlaceEntity: HappyPlaceEntity)

    @Update
    suspend fun update(happyPlaceEntity: HappyPlaceEntity)

    @Delete
    suspend fun delete(happyPlaceEntity: HappyPlaceEntity)

    @Query("Select * from HappyPlacesTable")
    fun fetchAllHappyPlaces(): Flow<List<HappyPlaceEntity>>

    @Query("Select * from HappyPlacesTable where id=:id")
    fun fetchAllHappyPlacesById(id:Int): Flow<HappyPlaceEntity>
}