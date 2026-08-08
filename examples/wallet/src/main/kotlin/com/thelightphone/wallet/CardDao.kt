package com.thelightphone.wallet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface CardDao {
    /** Returns the generated row id. */
    @Insert
    fun insert(card: CardEntity): Long

    @Query("SELECT * FROM cards ORDER BY created_at ASC")
    fun getAll(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getById(id: Long): CardEntity?

    @Query("DELETE FROM cards WHERE id = :id")
    fun delete(id: Long)
}
