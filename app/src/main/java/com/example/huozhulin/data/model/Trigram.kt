package com.example.huozhulin.data.model

/**
 * 八卦：含阴阳三爻(自下而上)、五行、纳甲天干与地支。
 * diZhiInner 对应 初/二/三 爻；diZhiOuter 对应 四/五/上 爻（按京房纳甲）。
 */
enum class Trigram(
    val cnName: String,
    val symbol: String,
    val lines: List<Boolean>,          // 初,二,三（自下而上），true=阳
    val element: Wuxing,
    val ganInner: String,
    val ganOuter: String,
    val diZhiInner: List<DiZhi>,       // 初,二,三
    val diZhiOuter: List<DiZhi>        // 四,五,上
) {
    QIAN("乾", "☰", listOf(true, true, true), Wuxing.METAL, "甲", "壬",
        listOf(DiZhi.ZI, DiZhi.YIN, DiZhi.CHEN),
        listOf(DiZhi.WU, DiZhi.SHEN, DiZhi.XU)),
    DUI("兑", "☱", listOf(true, true, false), Wuxing.METAL, "丁", "丁",
        listOf(DiZhi.SI, DiZhi.MAO, DiZhi.CHOU),
        listOf(DiZhi.HAI, DiZhi.YOU, DiZhi.WEI)),
    LI("离", "☲", listOf(true, false, true), Wuxing.FIRE, "己", "己",
        listOf(DiZhi.MAO, DiZhi.CHOU, DiZhi.HAI),
        listOf(DiZhi.YOU, DiZhi.WEI, DiZhi.SI)),
    ZHEN("震", "☳", listOf(true, false, false), Wuxing.WOOD, "庚", "庚",
        listOf(DiZhi.ZI, DiZhi.YIN, DiZhi.CHEN),
        listOf(DiZhi.WU, DiZhi.SHEN, DiZhi.XU)),
    XUN("巽", "☴", listOf(false, true, true), Wuxing.WOOD, "辛", "辛",
        listOf(DiZhi.CHOU, DiZhi.HAI, DiZhi.YOU),
        listOf(DiZhi.WEI, DiZhi.SI, DiZhi.MAO)),
    KAN("坎", "☵", listOf(false, true, false), Wuxing.WATER, "戊", "戊",
        listOf(DiZhi.YIN, DiZhi.CHEN, DiZhi.WU),
        listOf(DiZhi.SHEN, DiZhi.XU, DiZhi.ZI)),
    GEN("艮", "☶", listOf(false, false, true), Wuxing.EARTH, "丙", "丙",
        listOf(DiZhi.CHEN, DiZhi.WU, DiZhi.SHEN),
        listOf(DiZhi.XU, DiZhi.ZI, DiZhi.YIN)),
    KUN("坤", "☷", listOf(false, false, false), Wuxing.EARTH, "乙", "癸",
        listOf(DiZhi.WEI, DiZhi.SI, DiZhi.MAO),
        listOf(DiZhi.CHOU, DiZhi.HAI, DiZhi.YOU));

    companion object {
        val ALL: List<Trigram> get() = entries
    }
}
