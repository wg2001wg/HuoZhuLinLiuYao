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

    /** 更新记录的 AI 解析结果、模型与继续提问对话（重跑 AI 后落库，供下次直接查看） */
    @androidx.room.Query("UPDATE records SET aiResult = :aiResult, aiModel = :aiModel, aiChat = :aiChat WHERE id = :id")
    suspend fun updateAi(id: Long, aiResult: String, aiModel: String, aiChat: String?)

    /** 覆盖更新历史记录的全部字段（从历史记录进入后再次保存时复用同一条记录） */
    @androidx.room.Query(
        "UPDATE records SET timestamp = :timestamp, originalName = :originalName, changedName = :changedName, " +
                "linesStr = :linesStr, movingStr = :movingStr, dayGanCn = :dayGanCn, dayZhiCn = :dayZhiCn, " +
                "monthZhiCn = :monthZhiCn, note = :note, aiResult = :aiResult, aiModel = :aiModel, aiChat = :aiChat " +
                "WHERE id = :id"
    )
    suspend fun updateRecord(
        id: Long,
        timestamp: Long,
        originalName: String,
        changedName: String?,
        linesStr: String,
        movingStr: String,
        dayGanCn: String,
        dayZhiCn: String?,
        monthZhiCn: String?,
        note: String,
        aiResult: String?,
        aiModel: String?,
        aiChat: String?
    )
}
