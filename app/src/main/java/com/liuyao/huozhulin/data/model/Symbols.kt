package com.liuyao.huozhulin.data.model

/** 五行 */
enum class Wuxing(val cn: String) {
    WOOD("木"), FIRE("火"), EARTH("土"), METAL("金"), WATER("水");

    companion object {
        /** 相生：返回 x 所生之五行 */
        fun sheng(x: Wuxing): Wuxing = when (x) {
            Wuxing.WOOD -> Wuxing.FIRE
            Wuxing.FIRE -> Wuxing.EARTH
            Wuxing.EARTH -> Wuxing.METAL
            Wuxing.METAL -> Wuxing.WATER
            Wuxing.WATER -> Wuxing.WOOD
        }

        /** 相克：返回 x 所克之五行 */
        fun ke(x: Wuxing): Wuxing = when (x) {
            Wuxing.WOOD -> Wuxing.EARTH
            Wuxing.EARTH -> Wuxing.WATER
            Wuxing.WATER -> Wuxing.FIRE
            Wuxing.FIRE -> Wuxing.METAL
            Wuxing.METAL -> Wuxing.WOOD
        }
    }
}

/** 十二地支 */
enum class DiZhi(
    val cn: String,
    val element: Wuxing,
    val yang: Boolean,
    val order: Int // 子=1 ... 亥=12
) {
    ZI("子", Wuxing.WATER, true, 1),
    CHOU("丑", Wuxing.EARTH, false, 2),
    YIN("寅", Wuxing.WOOD, true, 3),
    MAO("卯", Wuxing.WOOD, false, 4),
    CHEN("辰", Wuxing.EARTH, true, 5),
    SI("巳", Wuxing.FIRE, false, 6),
    WU("午", Wuxing.FIRE, true, 7),
    WEI("未", Wuxing.EARTH, false, 8),
    SHEN("申", Wuxing.METAL, true, 9),
    YOU("酉", Wuxing.METAL, false, 10),
    XU("戌", Wuxing.EARTH, true, 11),
    HAI("亥", Wuxing.WATER, false, 12);

    companion object {
        val ALL: List<DiZhi> get() = entries

        /** 把 0~23 时映射到时支（子丑寅卯...）。子时 23~1，丑时 1~3，依此类推。 */
        fun forHour(hour: Int): DiZhi {
            // 23,0 -> 子(0); 1,2 -> 丑(1); ...
            val idx = ((hour + 1) % 24) / 2
            return ALL[idx]
        }
    }
}

/** 十天干（兼五行、阴阳，用于日干起六神与旬空） */
enum class TianGan(
    val cn: String,
    val element: Wuxing,
    val yang: Boolean
) {
    JIA("甲", Wuxing.WOOD, true),
    YI("乙", Wuxing.WOOD, false),
    BING("丙", Wuxing.FIRE, true),
    DING("丁", Wuxing.FIRE, false),
    WU("戊", Wuxing.EARTH, true),
    JI("己", Wuxing.EARTH, false),
    GENG("庚", Wuxing.METAL, true),
    XIN("辛", Wuxing.METAL, false),
    REN("壬", Wuxing.WATER, true),
    GUI("癸", Wuxing.WATER, false);

    companion object {
        val ALL: List<TianGan> get() = entries
    }
}

/** 六亲 */
enum class LiuQin(val cn: String) {
    BROTHER("兄弟"), PARENT("父母"), OFFICIAL("官鬼"), WEALTH("妻财"), CHILD("子孙");

    companion object {
        /** 以卦宫五行(我) 与 爻支五行 推断六亲 */
        fun of(myElement: Wuxing, branchElement: Wuxing): LiuQin {
            if (myElement == branchElement) return BROTHER            // 同我 -> 兄弟
            if (Wuxing.sheng(branchElement) == myElement) return PARENT // 生我 -> 父母
            if (Wuxing.sheng(myElement) == branchElement) return CHILD  // 我生 -> 子孙
            if (Wuxing.ke(branchElement) == myElement) return OFFICIAL  // 克我 -> 官鬼
            if (Wuxing.ke(myElement) == branchElement) return WEALTH    // 我克 -> 妻财
            error("unreachable liuqin")
        }
    }
}

/** 六神（顺序自初爻向上排） */
enum class LiuShen(val cn: String) {
    QINGLONG("青龙"), ZHUQUE("朱雀"), GOUCHEN("勾陈"), TENG_SHE("螣蛇"), BAIHU("白虎"), XUANWU("玄武");

    companion object {
        val ORDER: List<LiuShen> = listOf(QINGLONG, ZHUQUE, GOUCHEN, TENG_SHE, BAIHU, XUANWU)

        /** 日干 -> 初爻所起六神序号 */
        fun startIndex(gan: TianGan): Int = when (gan) {
            TianGan.JIA, TianGan.YI -> 0
            TianGan.BING, TianGan.DING -> 1
            TianGan.WU -> 2
            TianGan.JI -> 3
            TianGan.GENG, TianGan.XIN -> 4
            TianGan.REN, TianGan.GUI -> 5
        }
    }
}

/** 旺相休囚死 */
enum class WangShuai(val cn: String) {
    WANG("旺"), XIANG("相"), XIU("休"), QIU("囚"), SI("死");

    companion object {
        /** 以月建五行(我) 与 爻支五行 推断旺衰 */
        fun of(monthElement: Wuxing, lineElement: Wuxing): WangShuai {
            if (lineElement == monthElement) return WANG
            if (Wuxing.sheng(monthElement) == lineElement) return XIANG // 我生 -> 相
            if (Wuxing.sheng(lineElement) == monthElement) return XIU   // 生我 -> 休
            if (Wuxing.ke(lineElement) == monthElement) return QIU      // 克我 -> 囚
            return SI                                                       // 我克 -> 死
        }
    }
}

/** 世 / 应 / 无 */
enum class ShiYingType { SHI, YING, NONE }

/** 日柱旬空（以日干支所在旬为准）
 *  甲子旬（1-10）空戌亥；甲戌旬（11-20）空申酉；甲申旬（21-30）空午未；
 *  甲午旬（31-40）空辰巳；甲辰旬（41-50）空寅卯；甲寅旬（51-60）空子丑。
 */
fun kongWang(gan: TianGan, zhi: DiZhi): List<DiZhi> {
    var n = gan.ordinal
    while (n % 12 != zhi.ordinal) n += 10
    return when (n / 10) {
        0 -> listOf(DiZhi.XU, DiZhi.HAI)   // 甲子旬
        1 -> listOf(DiZhi.SHEN, DiZhi.YOU) // 甲戌旬
        2 -> listOf(DiZhi.WU, DiZhi.WEI)   // 甲申旬
        3 -> listOf(DiZhi.CHEN, DiZhi.SI)  // 甲午旬
        4 -> listOf(DiZhi.YIN, DiZhi.MAO)  // 甲辰旬
        5 -> listOf(DiZhi.ZI, DiZhi.CHOU)  // 甲寅旬
        else -> emptyList()
    }
}

/** 仅按地支快速取旬空（旧接口兼容）：返回该地支所在旬的空亡 */
fun kongWang(zhi: DiZhi): List<DiZhi> = kongWang(TianGan.JIA, zhi)

/** 爻位名：初九/初六 ... 上九/上六 */
fun positionName(position: Int, yang: Boolean): String {
    val suffix = if (yang) "九" else "六"
    val prefix = when (position) {
        0 -> "初"
        5 -> "上"
        else -> listOf("二", "三", "四", "五")[position - 1]
    }
    return prefix + suffix
}
