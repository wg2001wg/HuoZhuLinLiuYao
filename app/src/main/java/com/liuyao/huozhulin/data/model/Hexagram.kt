package com.liuyao.huozhulin.data.model

/** 世系（八宫）：决定世爻位置 */
enum class ShiXi(val cn: String, val shiPosition: Int /* 1..6，1=初爻 */) {
    BENGONG("本宫", 6),
    YISHI("一世", 1),
    ERSHI("二世", 2),
    SANSHI("三世", 3),
    SISHI("四世", 4),
    WUSHI("五世", 5),
    YOUHUN("游魂", 4),
    GUIHUN("归魂", 3)
}

/** 六十四卦之一 */
data class Hexagram(
    val index: Int,        // 1..64（八宫序）
    val name: String,     // 如 "乾为天"
    val upper: Trigram,
    val lower: Trigram,
    val palace: Trigram,  // 所属卦宫
    val shiXi: ShiXi
) {
    /** 六爻阴阳（自下而上，true=阳） */
    val lines: List<Boolean>
        get() = lower.lines + upper.lines

    val palaceElement: Wuxing get() = palace.element
}

private fun h(
    index: Int, name: String,
    up: Trigram, low: Trigram, palace: Trigram, shi: ShiXi
): Hexagram = Hexagram(index, name, up, low, palace, shi)

/** 六十四卦（八宫序） */
val HEXAGRAMS: List<Hexagram> = listOf(
    // —— 乾宫（金）——
    h(1, "乾为天", Trigram.QIAN, Trigram.QIAN, Trigram.QIAN, ShiXi.BENGONG),
    h(2, "天风姤", Trigram.QIAN, Trigram.XUN, Trigram.QIAN, ShiXi.YISHI),
    h(3, "天山遁", Trigram.QIAN, Trigram.GEN, Trigram.QIAN, ShiXi.ERSHI),
    h(4, "天地否", Trigram.QIAN, Trigram.KUN, Trigram.QIAN, ShiXi.SANSHI),
    h(5, "风地观", Trigram.XUN, Trigram.KUN, Trigram.QIAN, ShiXi.SISHI),
    h(6, "山地剥", Trigram.GEN, Trigram.KUN, Trigram.QIAN, ShiXi.WUSHI),
    h(7, "火地晋", Trigram.LI, Trigram.KUN, Trigram.QIAN, ShiXi.YOUHUN),
    h(8, "火天大有", Trigram.LI, Trigram.QIAN, Trigram.QIAN, ShiXi.GUIHUN),
    // —— 兑宫（金）——
    h(9, "兑为泽", Trigram.DUI, Trigram.DUI, Trigram.DUI, ShiXi.BENGONG),
    h(10, "泽水困", Trigram.DUI, Trigram.KAN, Trigram.DUI, ShiXi.YISHI),
    h(11, "泽地萃", Trigram.DUI, Trigram.KUN, Trigram.DUI, ShiXi.ERSHI),
    h(12, "泽山咸", Trigram.DUI, Trigram.GEN, Trigram.DUI, ShiXi.SANSHI),
    h(13, "水山蹇", Trigram.KAN, Trigram.GEN, Trigram.DUI, ShiXi.SISHI),
    h(14, "地山谦", Trigram.KUN, Trigram.GEN, Trigram.DUI, ShiXi.WUSHI),
    h(15, "雷山小过", Trigram.ZHEN, Trigram.GEN, Trigram.DUI, ShiXi.YOUHUN),
    h(16, "雷泽归妹", Trigram.ZHEN, Trigram.DUI, Trigram.DUI, ShiXi.GUIHUN),
    // —— 离宫（火）——
    h(17, "离为火", Trigram.LI, Trigram.LI, Trigram.LI, ShiXi.BENGONG),
    h(18, "火山旅", Trigram.LI, Trigram.GEN, Trigram.LI, ShiXi.YISHI),
    h(19, "火风鼎", Trigram.LI, Trigram.XUN, Trigram.LI, ShiXi.ERSHI),
    h(20, "火水未济", Trigram.LI, Trigram.KAN, Trigram.LI, ShiXi.SANSHI),
    h(21, "山水蒙", Trigram.GEN, Trigram.KAN, Trigram.LI, ShiXi.SISHI),
    h(22, "风水涣", Trigram.XUN, Trigram.KAN, Trigram.LI, ShiXi.WUSHI),
    h(23, "天水讼", Trigram.QIAN, Trigram.KAN, Trigram.LI, ShiXi.YOUHUN),
    h(24, "天火同人", Trigram.QIAN, Trigram.LI, Trigram.LI, ShiXi.GUIHUN),
    // —— 震宫（木）——
    h(25, "震为雷", Trigram.ZHEN, Trigram.ZHEN, Trigram.ZHEN, ShiXi.BENGONG),
    h(26, "雷地豫", Trigram.ZHEN, Trigram.KUN, Trigram.ZHEN, ShiXi.YISHI),
    h(27, "雷水解", Trigram.ZHEN, Trigram.KAN, Trigram.ZHEN, ShiXi.ERSHI),
    h(28, "雷风恒", Trigram.ZHEN, Trigram.XUN, Trigram.ZHEN, ShiXi.SANSHI),
    h(29, "地风升", Trigram.KUN, Trigram.XUN, Trigram.ZHEN, ShiXi.SISHI),
    h(30, "水风井", Trigram.KAN, Trigram.XUN, Trigram.ZHEN, ShiXi.WUSHI),
    h(31, "泽风大过", Trigram.DUI, Trigram.XUN, Trigram.ZHEN, ShiXi.YOUHUN),
    h(32, "泽雷随", Trigram.DUI, Trigram.ZHEN, Trigram.ZHEN, ShiXi.GUIHUN),
    // —— 巽宫（木）——
    h(33, "巽为风", Trigram.XUN, Trigram.XUN, Trigram.XUN, ShiXi.BENGONG),
    h(34, "风天小畜", Trigram.XUN, Trigram.QIAN, Trigram.XUN, ShiXi.YISHI),
    h(35, "风火家人", Trigram.XUN, Trigram.LI, Trigram.XUN, ShiXi.ERSHI),
    h(36, "风雷益", Trigram.XUN, Trigram.ZHEN, Trigram.XUN, ShiXi.SANSHI),
    h(37, "天雷无妄", Trigram.QIAN, Trigram.ZHEN, Trigram.XUN, ShiXi.SISHI),
    h(38, "火雷噬嗑", Trigram.LI, Trigram.ZHEN, Trigram.XUN, ShiXi.WUSHI),
    h(39, "山雷颐", Trigram.GEN, Trigram.ZHEN, Trigram.XUN, ShiXi.YOUHUN),
    h(40, "山风蛊", Trigram.GEN, Trigram.XUN, Trigram.XUN, ShiXi.GUIHUN),
    // —— 坎宫（水）——
    h(41, "坎为水", Trigram.KAN, Trigram.KAN, Trigram.KAN, ShiXi.BENGONG),
    h(42, "水泽节", Trigram.KAN, Trigram.DUI, Trigram.KAN, ShiXi.YISHI),
    h(43, "水雷屯", Trigram.KAN, Trigram.ZHEN, Trigram.KAN, ShiXi.ERSHI),
    h(44, "水火既济", Trigram.KAN, Trigram.LI, Trigram.KAN, ShiXi.SANSHI),
    h(45, "泽火革", Trigram.DUI, Trigram.LI, Trigram.KAN, ShiXi.SISHI),
    h(46, "雷火丰", Trigram.ZHEN, Trigram.LI, Trigram.KAN, ShiXi.WUSHI),
    h(47, "地火明夷", Trigram.KUN, Trigram.LI, Trigram.KAN, ShiXi.YOUHUN),
    h(48, "地水师", Trigram.KUN, Trigram.KAN, Trigram.KAN, ShiXi.GUIHUN),
    // —— 艮宫（土）——
    h(49, "艮为山", Trigram.GEN, Trigram.GEN, Trigram.GEN, ShiXi.BENGONG),
    h(50, "山火贲", Trigram.GEN, Trigram.LI, Trigram.GEN, ShiXi.YISHI),
    h(51, "山天大畜", Trigram.GEN, Trigram.QIAN, Trigram.GEN, ShiXi.ERSHI),
    h(52, "山泽损", Trigram.GEN, Trigram.DUI, Trigram.GEN, ShiXi.SANSHI),
    h(53, "火泽睽", Trigram.LI, Trigram.DUI, Trigram.GEN, ShiXi.SISHI),
    h(54, "天泽履", Trigram.QIAN, Trigram.DUI, Trigram.GEN, ShiXi.WUSHI),
    h(55, "风泽中孚", Trigram.XUN, Trigram.DUI, Trigram.GEN, ShiXi.YOUHUN),
    h(56, "风山渐", Trigram.XUN, Trigram.GEN, Trigram.GEN, ShiXi.GUIHUN),
    // —— 坤宫（土）——
    h(57, "坤为地", Trigram.KUN, Trigram.KUN, Trigram.KUN, ShiXi.BENGONG),
    h(58, "地雷复", Trigram.KUN, Trigram.ZHEN, Trigram.KUN, ShiXi.YISHI),
    h(59, "地泽临", Trigram.KUN, Trigram.DUI, Trigram.KUN, ShiXi.ERSHI),
    h(60, "地天泰", Trigram.KUN, Trigram.QIAN, Trigram.KUN, ShiXi.SANSHI),
    h(61, "雷天大壮", Trigram.ZHEN, Trigram.QIAN, Trigram.KUN, ShiXi.SISHI),
    h(62, "泽天夬", Trigram.DUI, Trigram.QIAN, Trigram.KUN, ShiXi.WUSHI),
    h(63, "水天需", Trigram.KAN, Trigram.QIAN, Trigram.KUN, ShiXi.YOUHUN),
    h(64, "水地比", Trigram.KAN, Trigram.KUN, Trigram.KUN, ShiXi.GUIHUN)
)

/** 依六爻阴阳(自下而上)查找对应卦 */
fun findHexagram(lines: List<Boolean>): Hexagram {
    require(lines.size == 6)
    val lower = Trigram.ALL.first { it.lines == listOf(lines[0], lines[1], lines[2]) }
    val upper = Trigram.ALL.first { it.lines == listOf(lines[3], lines[4], lines[5]) }
    return HEXAGRAMS.first { it.upper == upper && it.lower == lower }
}
