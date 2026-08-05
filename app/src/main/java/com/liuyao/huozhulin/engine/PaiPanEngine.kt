package com.liuyao.huozhulin.engine

import com.liuyao.huozhulin.data.model.*

/**
 * 装卦排盘引擎：纳甲、安六亲、安六神、定世应、标动爻、生成变卦。
 */
object PaiPanEngine {

    fun layout(
        lines: List<Boolean>,
        moving: List<Boolean>,
        dayGan: TianGan,
        dayZhi: DiZhi?,
        monthZhi: DiZhi?
    ): PaiPanResult {
        require(lines.size == 6 && moving.size == 6)
        val origHex = findHexagram(lines)
        val palace = origHex.palace
        val originalPlate = buildPlate(origHex, lines, moving, palace, dayGan, dayZhi, monthZhi)

        val changedPlate = if (moving.any { it }) {
            val chLines = lines.mapIndexed { i, y -> if (moving[i]) !y else y }
            val chHex = findHexagram(chLines)
            // 变卦六亲仍以本卦卦宫五行为"我"
            buildPlate(chHex, chLines, List(6) { false }, palace, dayGan, dayZhi, monthZhi)
        } else null

        // 伏神：本宫卦，六亲以本宫五行为"我"，无六神、无世应、无动变
        val fuHex = HEXAGRAMS.first { it.palace == palace && it.shiXi == ShiXi.BENGONG }
        val fuPlate = buildPlate(fuHex, fuHex.lines, List(6) { false }, palace, dayGan, dayZhi, monthZhi)

        val kong = if (dayZhi != null) kongWang(dayGan, dayZhi) else emptyList()
        return PaiPanResult(originalPlate, changedPlate, fuPlate, dayGan, dayZhi, monthZhi, kong)
    }

    private fun buildPlate(
        hex: Hexagram,
        lines: List<Boolean>,
        moving: List<Boolean>,
        palace: Trigram,
        dayGan: TianGan,
        dayZhi: DiZhi?,
        monthZhi: DiZhi?
    ): HexagramPlate {
        val shiPos = hex.shiXi.shiPosition - 1
        val yingPos = (shiPos + 3) % 6
        val startShen = LiuShen.startIndex(dayGan)
        val kong = if (dayZhi != null) kongWang(dayGan, dayZhi) else emptyList()
        val monthElement = monthZhi?.element

        val lineInfos = (0..5).map { p ->
            val trig = if (p < 3) hex.lower else hex.upper
            val inner = p < 3
            val dz = if (inner) trig.diZhiInner[p] else trig.diZhiOuter[p - 3]
            val tg = if (inner) trig.ganInner else trig.ganOuter
            val lq = LiuQin.of(palace.element, dz.element)
            val ls = LiuShen.ORDER[(startShen + p) % 6]
            val sy = when (p) {
                shiPos -> ShiYingType.SHI
                yingPos -> ShiYingType.YING
                else -> ShiYingType.NONE
            }
            val kw = dayZhi != null && dz in kong
            val ws = if (monthElement != null) WangShuai.of(monthElement, dz.element) else null
            LineInfo(
                position = p,
                positionName = positionName(p, lines[p]),
                yang = lines[p],
                moving = moving[p],
                diZhi = dz,
                tianGan = tg,
                liuQin = lq,
                liuShen = ls,
                shiYing = sy,
                kongWang = kw,
                wangShuai = ws
            )
        }
        return HexagramPlate(hex, lineInfos)
    }
}
