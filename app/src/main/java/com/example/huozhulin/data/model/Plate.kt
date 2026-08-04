package com.example.huozhulin.data.model

/** 单爻装卦信息（自下而上第 position 爻，position 0..5） */
data class LineInfo(
    val position: Int,          // 0..5（0=初爻）
    val positionName: String,   // 初九/初六...上九/上六
    val yang: Boolean,          // true=阳
    val moving: Boolean,        // 是否动爻
    val diZhi: DiZhi,           // 纳甲地支
    val tianGan: String,        // 纳甲天干
    val liuQin: LiuQin,         // 六亲
    val liuShen: LiuShen,       // 六神
    val shiYing: ShiYingType,   // 世/应
    val kongWang: Boolean,      // 是否旬空
    val wangShuai: WangShuai?   // 旺衰（需月建）
)

/** 一个卦的完整装卦结果（含六爻，自下而上） */
data class HexagramPlate(
    val hexagram: Hexagram,
    val lines: List<LineInfo>    // index 0..5
) {
    val name: String get() = hexagram.name
}

/** 一次完整排盘结果 */
data class PaiPanResult(
    val original: HexagramPlate,
    val changed: HexagramPlate?,   // 无动爻则为 null
    val fu: HexagramPlate,         // 伏神（本宫卦）
    val dayGan: TianGan,
    val dayZhi: DiZhi?,
    val monthZhi: DiZhi?,
    val kongWang: List<DiZhi>
) {
    val hasChanged: Boolean get() = changed != null
}
