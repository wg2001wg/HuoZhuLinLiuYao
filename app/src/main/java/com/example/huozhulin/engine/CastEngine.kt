package com.example.huozhulin.engine

import com.example.huozhulin.data.model.DiZhi
import com.example.huozhulin.data.model.Trigram
import java.util.Calendar
import kotlin.random.Random

/**
 * 起卦引擎：支持七种方式（按图片说明）
 *  - 随机卦：软件随机生成 6 个爻位
 *  - 在线摇（铜钱摇卦）：三枚铜钱正反面，完全模拟实物摇卦
 *  - 指定卦：指定上卦、下卦及动爻；若为历史卦可指定干支
 *  - 数字卦：前半部分字数和除 8 余数为上卦，后半部分数字和除 8 余数为下卦，
 *            总和除 6 余数为动爻（动爻可选加时辰数）
 *  - 日期卦：农历/阳历日期起卦
 *  - 时分卦：时数除 8 余数为上卦，分钟数除 8 余数为下卦，（时数+分钟数）除 6 余数为动爻
 *  - 终身卦：生日年干/年支数 + 农历月 + 农历日 起卦
 * 返回 Pair<六爻阴阳(自下而上), 动爻标记(自下而上)>。
 */
object CastEngine {

    /* ==================== 1. 随机卦 ==================== */

    /** 随机卦：软件模拟手摇，随机生成 6 个爻位。老阴/老阳为动爻。 */
    fun randomCast(): Pair<List<Boolean>, List<Boolean>> {
        val lines = mutableListOf<Boolean>()
        val moving = mutableListOf<Boolean>()
        repeat(6) {
            val coinType = Random.nextInt(4) // 0..3 对应 老阴 少阳 少阴 老阳
            when (coinType) {
                0 -> { lines.add(false); moving.add(true) }   // 老阴
                1 -> { lines.add(true); moving.add(false) }   // 少阳
                2 -> { lines.add(false); moving.add(false) }  // 少阴
                3 -> { lines.add(true); moving.add(true) }    // 老阳
            }
        }
        return lines to moving
    }

    /* ==================== 2. 在线摇 / 铜钱摇卦 ==================== */

    /**
     * 铜钱摇卦：每爻三枚铜钱。
     * 3 正 = 老阳(9, 动, 阳)；2 正 1 反 = 少阴(8, 静, 阴)；
     * 1 正 2 反 = 少阳(7, 静, 阳)；3 反 = 老阴(6, 动, 阴)。
     */
    fun coinCast(): Pair<List<Boolean>, List<Boolean>> {
        val lines = mutableListOf<Boolean>()
        val moving = mutableListOf<Boolean>()
        repeat(6) {
            val heads = (0..2).count { Random.nextBoolean() } // 正面(阳)枚数 0..3
            when (heads) {
                0 -> { lines.add(false); moving.add(true) }   // 老阴 6
                1 -> { lines.add(true); moving.add(false) }   // 少阳 7
                2 -> { lines.add(false); moving.add(false) }  // 少阴 8
                3 -> { lines.add(true); moving.add(true) }    // 老阳 9
            }
        }
        return lines to moving
    }

    /* ==================== 3. 指定卦 ==================== */

    /**
     * 指定卦：指定上卦、下卦（用先天八卦数 1..8）及动爻位置（1..6）。
     * 动爻 = 0 表示无动爻（静卦）。
     */
    fun specifiedCast(
        upperIdx: Int,
        lowerIdx: Int,
        movingYao: Int
    ): Pair<List<Boolean>, List<Boolean>> {
        require(upperIdx in 1..8 && lowerIdx in 1..8) { "上卦、下卦须为 1~8" }
        require(movingYao in 0..6) { "动爻须为 0~6（0 表示静卦）" }
        val up = trigramByIndex(upperIdx)
        val low = trigramByIndex(lowerIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { (it + 1) == movingYao }
        return lines to moving
    }

    /* ==================== 4. 数字卦 ==================== */

    /**
     * 数字卦：前半部分数字和除 8 余数为上卦，后半部分数字和除 8 余数为下卦，
     * 总和除 6 余数为动爻。可额外加时支数。
     *
     * @param first  前半部分数字字符串，如 "1"、"12"
     * @param second 后半部分数字字符串，如 "3"、"23"
     * @param addHourBranch 是否加上时支数参与动爻计算
     * @param hourBranch 当前时支数（1~12），仅当 addHourBranch=true 时使用
     */
    fun numberCast(
        first: String,
        second: String,
        addHourBranch: Boolean = false,
        hourBranch: Int = 1
    ): Pair<List<Boolean>, List<Boolean>> {
        val upSum = digitSum(first)
        val lowSum = digitSum(second)
        require(upSum > 0 || lowSum > 0) { "请输入有效数字" }

        val upIdx = if (upSum == 0) 8 else mod8(upSum)
        val lowIdx = if (lowSum == 0) 8 else mod8(lowSum)
        val total = upSum + lowSum + if (addHourBranch) hourBranch.coerceIn(1, 12) else 0
        val movingPos = mod6(total) - 1 // 0..5

        val up = trigramByIndex(upIdx)
        val low = trigramByIndex(lowIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /** 兼容旧版单数字起卦：下卦 = n%8，上卦 = (n/8)%8，动爻 = n%6（梅花易数式简化）。 */
    fun numberCast(n: Long): Pair<List<Boolean>, List<Boolean>> {
        require(n > 0) { "数字需为正整数" }
        val lowIdx = mod8(n.toInt())
        val upIdx = mod8((n / 8).toInt())
        val movingPos = mod6(n.toInt()) - 1
        val low = trigramByIndex(lowIdx)
        val up = trigramByIndex(upIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /* ==================== 5. 日期卦 ==================== */

    /**
     * 日期卦（阳历）。
     * 上卦 = (年 + 月 + 日) 除 8 余数；
     * 下卦 = (上卦总数 + 时支数 + 附加数) 除 8 余数；
     * 动爻 = 下卦总数 除 6 余数。
     *
     * @param year  阳历年
     * @param month 阳历月
     * @param day   阳历日
     * @param hourBranch 时支数 1~12
     * @param extra 附加数（可选，默认 0）
     */
    fun solarDateCast(
        year: Int,
        month: Int,
        day: Int,
        hourBranch: Int,
        extra: Int = 0
    ): Pair<List<Boolean>, List<Boolean>> {
        require(year > 0 && month in 1..12 && day in 1..31) { "日期不合法" }
        val upSum = year + month + day
        val upIdx = mod8(upSum)
        val lowSum = upIdx + hourBranch.coerceIn(1, 12) + extra
        val lowIdx = mod8(lowSum)
        val movingPos = mod6(lowIdx) - 1

        val up = trigramByIndex(upIdx)
        val low = trigramByIndex(lowIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /**
     * 日期卦（农历）。
     * 上卦 = (农历年支数 + 农历月 + 农历日) 除 8 余数；
     * 下卦 = (上卦总数 + 时支数 + 附加数) 除 8 余数；
     * 动爻 = 下卦总数 除 6 余数。
     */
    fun lunarDateCast(
        lunarYearBranchIndex: Int,
        lunarMonth: Int,
        lunarDay: Int,
        hourBranch: Int,
        extra: Int = 0
    ): Pair<List<Boolean>, List<Boolean>> {
        require(lunarMonth in 1..12 && lunarDay in 1..30) { "农历月日不合法" }
        val upSum = lunarYearBranchIndex.coerceIn(1, 12) + lunarMonth + lunarDay
        val upIdx = mod8(upSum)
        val lowSum = upIdx + hourBranch.coerceIn(1, 12) + extra
        val lowIdx = mod8(lowSum)
        val movingPos = mod6(lowIdx) - 1

        val up = trigramByIndex(upIdx)
        val low = trigramByIndex(lowIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /* ==================== 6. 时分卦 ==================== */

    /**
     * 时分卦：时数除 8 余数为上卦，分钟数除 8 余数为下卦，
     * （时数 + 分钟数）除 6 余数为动爻。
     */
    fun hourMinuteCast(hour: Int, minute: Int): Pair<List<Boolean>, List<Boolean>> {
        require(hour in 0..23 && minute in 0..59) { "时分不合法" }
        val upIdx = mod8(hour)
        val lowIdx = mod8(minute)
        val movingPos = mod6(hour + minute) - 1

        val up = trigramByIndex(upIdx)
        val low = trigramByIndex(lowIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /* ==================== 7. 终身卦 ==================== */

    /**
     * 终身卦：（生日年干或年支数 + 农历月 + 农历日）除 8 余数为上卦，
     * （上卦总数 + 时支数）除 8 余数为下卦，下卦总数除 6 余数为动爻。
     */
    fun lifetimeCast(
        yearStemOrBranchIndex: Int,
        lunarMonth: Int,
        lunarDay: Int,
        hourBranch: Int
    ): Pair<List<Boolean>, List<Boolean>> {
        require(lunarMonth in 1..12 && lunarDay in 1..30) { "农历月日不合法" }
        val upSum = yearStemOrBranchIndex.coerceIn(1, 12) + lunarMonth + lunarDay
        val upIdx = mod8(upSum)
        val lowSum = upIdx + hourBranch.coerceIn(1, 12)
        val lowIdx = mod8(lowSum)
        val movingPos = mod6(lowIdx) - 1

        val up = trigramByIndex(upIdx)
        val low = trigramByIndex(lowIdx)
        val lines = low.lines + up.lines
        val moving = List(6) { it == movingPos }
        return lines to moving
    }

    /* ==================== 兼容旧版时间起卦 ==================== */

    /** 以当前时间生成数字后按数字起卦（简化）。 */
    fun timeCast(): Pair<List<Boolean>, List<Boolean>> {
        val c = Calendar.getInstance()
        val seed = (c.get(Calendar.YEAR) % 100) * 1_000_000L +
                (c.get(Calendar.MONTH) + 1) * 10_000L +
                c.get(Calendar.DAY_OF_MONTH) * 100L +
                c.get(Calendar.HOUR_OF_DAY)
        val n = if (seed <= 0) 1L else seed
        return numberCast(n)
    }

    /* ==================== 工具函数 ==================== */

    private fun trigramByIndex(idx: Int): Trigram { // idx 1..8
        return Trigram.ALL[(idx - 1) % 8]
    }

    /** 数字字符串各位之和。 */
    private fun digitSum(s: String): Int {
        return s.filter { it.isDigit() }.sumOf { it.digitToInt() }
    }

    /** 对 8 取余，结果映射到 1..8。 */
    private fun mod8(n: Int): Int {
        val r = n % 8
        return if (r == 0) 8 else r
    }

    /** 对 6 取余，结果映射到 1..6。 */
    private fun mod6(n: Int): Int {
        val r = n % 6
        return if (r == 0) 6 else r
    }

    /** 获取当前时支数（1~12）。 */
    fun currentHourBranch(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return DiZhi.forHour(hour).ordinal + 1
    }
}
