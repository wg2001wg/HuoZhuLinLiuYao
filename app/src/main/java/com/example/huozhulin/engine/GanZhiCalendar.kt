package com.example.huozhulin.engine

import com.example.huozhulin.data.model.DiZhi
import com.example.huozhulin.data.model.TianGan
import java.util.Calendar

/** 一个干支组合（天干 + 地支） */
data class GanZhi(val gan: TianGan, val zhi: DiZhi) {
    val cn: String get() = gan.cn + zhi.cn
}

/** 完整四柱：年 / 月 / 日 / 时 */
data class FourPillars(
    val year: GanZhi,
    val month: GanZhi,
    val day: GanZhi,
    val hour: GanZhi
)

/**
 * 公历（年/月/日/时）转四柱干支。
 *
 * 说明：
 *  - 年柱以立春（约 2 月 4 日）为界；立春前属上一年干支。
 *  - 月柱以十二"节"近似公历日期为界；月干依年干"五虎遁"。
 *  - 日柱以 2000-01-01（戊午日）为基准，按儒略日差推算（准确）。
 *  - 时柱按时辰地支 + 日干"五鼠遁"。
 */
object GanZhiCalendar {

    /** 各"节"起始的公历日期（近似值，用于推算月支）。
     *  以"寅月"为干支岁首，数组元素顺序：月支序 0=寅 … 11=丑。 */
    private val JIE = arrayOf(
        intArrayOf(1, 6, 11),   // 小寒 -> 丑月(11)
        intArrayOf(2, 4, 0),    // 立春 -> 寅月(0)
        intArrayOf(3, 6, 1),    // 惊蛰 -> 卯月(1)
        intArrayOf(4, 5, 2),    // 清明 -> 辰月(2)
        intArrayOf(5, 6, 3),    // 立夏 -> 巳月(3)
        intArrayOf(6, 6, 4),    // 芒种 -> 午月(4)
        intArrayOf(7, 7, 5),    // 小暑 -> 未月(5)
        intArrayOf(8, 8, 6),    // 立秋 -> 申月(6)
        intArrayOf(9, 8, 7),    // 白露 -> 酉月(7)
        intArrayOf(10, 8, 8),   // 寒露 -> 戌月(8)
        intArrayOf(11, 7, 9),   // 立冬 -> 亥月(9)
        intArrayOf(12, 7, 10)   // 大雪 -> 子月(10)
    )

    /** 月支（DiZhi），按公历月日近似节气判断 */
    private fun monthZhi(m: Int, d: Int): DiZhi {
        val md = m * 100 + d
        var idx = 10 // 1/1-1/5 落在大雪(12/7)之后、小寒(1/6)之前，属子月(10)
        for (j in JIE) if (j[0] * 100 + j[1] <= md) idx = j[2]
        // idx 约定：0=寅 … 11=丑；映射到 DiZhi.entries 序（子0…亥11）：entriesIndex=(idx+2)%12
        return DiZhi.entries[(idx + 2) % 12]
    }

    /** 年柱：以立春为界 */
    private fun yearGanZhi(y: Int, m: Int, d: Int): GanZhi {
        val gzYear = if (m < 2 || (m == 2 && d < 4)) y - 1 else y
        val seq = ((gzYear - 4) % 60 + 60) % 60 // 公元 4 年为甲子年
        return GanZhi(TianGan.entries[seq % 10], DiZhi.entries[seq % 12])
    }

    /** 日柱：以 2000-01-01（戊午日，序号 54）为基准按天数差推算 */
    private fun dayGanZhi(y: Int, m: Int, d: Int): GanZhi {
        val base = Calendar.getInstance().apply {
            set(2000, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val target = Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val diffDays = ((target - base) / (24L * 3600 * 1000)).toInt()
        val seq = ((54 + diffDays) % 60 + 60) % 60
        return GanZhi(TianGan.entries[seq % 10], DiZhi.entries[seq % 12])
    }

    /** 月干：依年干"五虎遁"。寅月起，甲/己年丙为首。 */
    fun monthGanZhi(yearGan: TianGan, monthZhi: DiZhi): GanZhi {
        val start = when (yearGan) {
            TianGan.JIA, TianGan.JI -> 2
            TianGan.YI, TianGan.GENG -> 4
            TianGan.BING, TianGan.XIN -> 6
            TianGan.DING, TianGan.REN -> 8
            TianGan.WU, TianGan.GUI -> 0
        }
        val monthSeq = (monthZhi.ordinal - 2 + 12) % 12 // 寅=0 … 丑=11
        val ganIdx = (start + monthSeq) % 10
        return GanZhi(TianGan.entries[ganIdx], monthZhi)
    }

    /** 时柱：时辰地支 + 日干"五鼠遁" */
    private fun hourGanZhi(dayGZ: GanZhi, hour: Int): GanZhi {
        val zhiIdx = ((hour + 1) / 2) % 12 // 子0(23-1) 丑1(1-3) …
        val zhi = DiZhi.entries[zhiIdx]
        val start = when (dayGZ.gan) {
            TianGan.JIA, TianGan.JI -> 0
            TianGan.YI, TianGan.GENG -> 2
            TianGan.BING, TianGan.XIN -> 4
            TianGan.DING, TianGan.REN -> 6
            TianGan.WU, TianGan.GUI -> 8
        }
        val ganIdx = (start + zhiIdx) % 10
        return GanZhi(TianGan.entries[ganIdx], zhi)
    }

    fun fromDateTime(y: Int, m: Int, d: Int, hour: Int, minute: Int = 0): FourPillars {
        val yg = yearGanZhi(y, m, d)
        val mz = monthZhi(m, d)
        val mg = monthGanZhi(yg.gan, mz)
        val dg = dayGanZhi(y, m, d)
        val hg = hourGanZhi(dg, hour)
        return FourPillars(yg, mg, dg, hg)
    }

    /** 取当前时刻的四柱 */
    fun fourPillarsNow(): FourPillars {
        val c = Calendar.getInstance()
        return fromDateTime(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE)
        )
    }

    /** 由时间戳（毫秒）还原四柱，用于历史记录回看 */
    fun fromTimestamp(ts: Long): FourPillars {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return fromDateTime(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE)
        )
    }
}
