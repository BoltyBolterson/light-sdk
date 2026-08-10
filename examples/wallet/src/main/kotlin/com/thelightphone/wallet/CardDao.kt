package com.thelightphone.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface CardDao {
    @Insert
    fun insert(card: CardEntity): Long

    @Query("SELECT * FROM cards ORDER BY created_at ASC")
    fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getById(id: Long): CardEntity?
}
