package com.liuyao.huozhulin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<RecordEntity>>

    @Insert
    suspend fun insert(record: RecordEntity): Long

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): RecordEntity?

    /** 更新记录的 AI 解析结果与模型（重跑 AI 后落库，供下次直接查看） */
    @androidx.room.Query("UPDATE records SET aiResult = :aiResult, aiModel = :aiModel WHERE id = :id")
    suspend fun updateAi(id: Long, aiResult: String, aiModel: String)
}
