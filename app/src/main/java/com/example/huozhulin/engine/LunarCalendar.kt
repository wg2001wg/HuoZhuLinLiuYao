package com.example.huozhulin.engine

import com.example.huozhulin.data.model.DiZhi
import com.example.huozhulin.data.model.TianGan
import java.util.Calendar
import java.util.TimeZone

/**
 * 农历、节气计算（1900–2100）。
 *
 * 说明：
 *  - 农历数据使用传统 bit-packed 表（1900–2099）。
 *  - 节气使用低精度太阳黄经算法（Meeus 近似），误差通常在数小时内，足够显示。
 */
object LunarCalendar {

    /** 节气名（按一年中顺序：小寒、大寒、立春、雨水、惊蛰、春分、清明、谷雨、立夏、小满、芒种、夏至、小暑、大暑、立秋、处暑、白露、秋分、寒露、霜降、立冬、小雪、大雪、冬至） */
    val JIE_QI_NAMES = listOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    /** 农历月份名（正月到腊月，含闰月前缀） */
    private val LUNAR_MONTH_NAMES = listOf(
        "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    )

    /** 农历日名 */
    private val LUNAR_DAY_NAMES = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    /** 1900–2099 农历数据（传统 bit-packed 表，业界标准数组）。
     *  编码：bit0-3 闰月（0=无闰）；bit4-15 表示 12 个月大小，bit15=正月…bit4=腊月（1=大月30天）；
     *  bit16 为闰月大小（1=大月30天）。 */
    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    )

    /** 1900 年春节（农历正月初一）的公历日期：1900-01-31 */
    private val BASE_1900 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
        set(1900, Calendar.JANUARY, 31, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** 一天的毫秒数 */
    private const val DAY_MS = 24L * 3600 * 1000

    /** 农历日期 */
    data class LunarDate(
        val year: Int,           // 农历年（如 2026）
        val month: Int,          // 农历月 1..12
        val day: Int,            // 农历日 1..30
        val isLeap: Boolean,     // 是否闰月
        val hourZhi: DiZhi       // 时辰地支
    ) {
        /** 中文显示，例如「2026年 六月 廿一 申时」 */
        fun format(): String {
            val leapPrefix = if (isLeap) "闰" else ""
            val monthName = leapPrefix + LUNAR_MONTH_NAMES[month - 1] + "月"
            return "${year}年 $monthName ${LUNAR_DAY_NAMES[day - 1]} ${hourZhi.cn}时"
        }
    }

    /** 由公历时间戳转农历日期与时辰 */
    fun toLunar(ts: Long): LunarDate {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = ts }
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        val h = c.get(Calendar.HOUR_OF_DAY)
        return toLunar(y, m, d, h)
    }

    /** 公历（年月日时）转农历 */
    fun toLunar(year: Int, month: Int, day: Int, hour: Int): LunarDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysSince1900 = ((cal.timeInMillis - BASE_1900) / DAY_MS).toInt()

        var offset = daysSince1900
        var lunarYear = 1900
        while (lunarYear < 2100 && offset >= yearDays(lunarYear)) {
            offset -= yearDays(lunarYear)
            lunarYear++
        }

        val leapMonth = leapMonth(lunarYear)
        var lunarMonth = 1
        var isLeap = false
        while (lunarMonth <= 12) {
            // 先扣减该月“正常月”
            val md = monthDays(lunarYear, lunarMonth, false)
            if (offset < md) {
                break
            }
            offset -= md
            // 若该月为闰月年中的闰月序号，再扣减“闰月”
            if (leapMonth == lunarMonth) {
                val ld = monthDays(lunarYear, lunarMonth, true)
                if (offset < ld) {
                    isLeap = true
                    break
                }
                offset -= ld
            }
            lunarMonth++
        }
        val lunarDay = offset + 1
        val hourZhi = DiZhi.entries[((hour + 1) / 2) % 12]
        return LunarDate(lunarYear, lunarMonth, lunarDay, isLeap, hourZhi)
    }

    /** 某农历年总天数 */
    private fun yearDays(year: Int): Int {
        var sum = 0
        val leap = leapMonth(year)
        for (m in 1..12) {
            sum += monthDays(year, m, leap == m)
        }
        return sum
    }

    /** 某农历月天数；isLeap 为 true 时表示取该年的闰月大小（bit16） */
    private fun monthDays(year: Int, month: Int, isLeap: Boolean): Int {
        val info = LUNAR_INFO[year - 1900]
        return if (isLeap) {
            if (((info shr 16) and 1) == 1) 30 else 29   // 闰月大小位 = bit16
        } else {
            val bit = 16 - month   // bit15=正月 … bit4=腊月
            if (((info shr bit) and 1) == 1) 30 else 29
        }
    }

    /** 某年的闰月（1-12），0 表示无闰月 */
    private fun leapMonth(year: Int): Int = LUNAR_INFO[year - 1900] and 0x0f

    // ==================== 节气 ====================

    /** 计算指定节气的 Julian Day（世界时近似，后转北京时间）。
     *  n: 0=小寒, 1=大寒, ..., 23=冬至。
     *  返回该节气的 Calendar（Asia/Shanghai）。
     */
    fun solarTermTime(year: Int, n: Int): Calendar {
        // 目标黄经：小寒=285°，之后每 15°
        val targetLon = (285 + n * 15) % 360.0
        // 先估算该年 1 月 1 日到目标节气的约略天数
        val base = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 用迭代法找太阳黄经 = targetLon 的时刻
        var tLow = jdFromCalendar(base)
        var tHigh = tLow + 30.0 // 一个月内必有一个节气
        while (trueSunLongitude(tHigh) < targetLon) tHigh += 30.0
        // 迭代到 0.001 日精度
        for (i in 0..20) {
            val mid = (tLow + tHigh) / 2
            val lon = trueSunLongitude(mid)
            if (lon < targetLon) tLow = mid else tHigh = mid
            if (tHigh - tLow < 1e-4) break
        }
        return calendarFromJd((tLow + tHigh) / 2 + 8.0 / 24.0) // 加 UTC+8
    }

    /** 计算某公历时刻前后最近的一对节气（上一节气、下一节气）。
     *  返回 Pair(上一节气名 to Calendar, 下一节气名 to Calendar)。
     */
    fun aroundSolarTerms(ts: Long): Pair<Pair<String, Calendar>, Pair<String, Calendar>> {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = ts }
        val year = c.get(Calendar.YEAR)
        // 找当前时刻位于哪两个节气之间
        var prev: Pair<String, Calendar>? = null
        var next: Pair<String, Calendar>? = null
        for (n in 0..23) {
            val t = solarTermTime(year, n)
            if (t.timeInMillis <= ts) {
                prev = JIE_QI_NAMES[n] to t
            } else if (next == null) {
                next = JIE_QI_NAMES[n] to t
                break
            }
        }
        // 跨年时处理
        if (prev == null) {
            val t = solarTermTime(year - 1, 23) // 冬至
            prev = "冬至" to t
        }
        if (next == null) {
            val t = solarTermTime(year + 1, 0) // 小寒
            next = "小寒" to t
        }
        return prev!! to next!!
    }

    private fun jdFromCalendar(c: Calendar): Double {
        return c.timeInMillis / 86400000.0 + 2440587.5
    }

    private fun calendarFromJd(jd: Double): Calendar {
        val ms = ((jd - 2440587.5) * 86400000).toLong()
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = ms }
    }

    /** 太阳真黄经（低精度，Meeus 算法）。 */
    private fun trueSunLongitude(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        val L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T
        val M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T
        val e = 0.016708634 - 0.000042037 * T
        val mRad = Math.toRadians(M)
        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * Math.sin(mRad) +
                (0.019993 - 0.000101 * T) * Math.sin(2 * mRad) +
                0.000289 * Math.sin(3 * mRad)
        var sunLon = L0 + C
        sunLon %= 360.0
        if (sunLon < 0) sunLon += 360.0
        return sunLon
    }
}
