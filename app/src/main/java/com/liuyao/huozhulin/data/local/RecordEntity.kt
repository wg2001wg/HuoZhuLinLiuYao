package com.liuyao.huozhulin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一次排盘的历史记录 */
@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val originalName: String,
    val changedName: String?,
    val linesStr: String,     // 六爻阴阳，自下而上，"1"=阳
    val movingStr: String,    // 动爻标记，自下而上，"1"=动
    val dayGanCn: String,
    val dayZhiCn: String?,
    val monthZhiCn: String?,
    val note: String,
    // AI 解析结果：保存排盘时若已有解析内容则一并落库，下次打开直接查看，无需重新联网
    val aiResult: String? = null,
    val aiModel: String? = null
)
