package com.liuyao.huozhulin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuyao.huozhulin.data.model.LineInfo
import com.liuyao.huozhulin.data.model.LiuQin
import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.ShiYingType

/** 六爻表内文字统一字号（固定值，不随结果页整体放大，保证窄屏单列可容纳、不溢出压字） */
private val lineTextStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)

/** 参考传统排盘风格配色 */
private val shenColorValue = Color(0xFF8D6E63)      // 六神：棕褐色
private val fuColorValue = Color(0xFF2E7D32)        // 主伏/变伏：墨绿
private val fuMissingColorValue = Color(0xFFD32F2F) // 伏神六亲在卦中无对应：红色
private val shiYingColorValue = Color(0xFF1565C0)   // 世/应：蓝色

/**
 * 按传统六爻排盘模板展示：
 * 六神 | 主伏 | 本卦(爻条·六亲天干地支) | 动变 | 变卦(爻条·六亲天干地支) | 变伏
 */
@Composable
fun PlateTable(result: PaiPanResult) {
    val orig = result.original
    val changed = result.changed
    val fu = result.fu
    val palaceName = orig.hexagram.palace.cnName
    val changedPalaceName = changed?.hexagram?.palace?.cnName ?: ""
    val palaceElement = orig.hexagram.palaceElement.cn
    val changedPalaceElement = changed?.hexagram?.palaceElement?.cn ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // 表头：六神 | (主伏留空) | 本卦 | (动变留空) | 变卦 | 变伏
        Row(
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            HeaderText("六神", Modifier.weight(0.7f))
            HeaderText("主伏", Modifier.weight(1f))
            HeaderCell(
                "${orig.name}\n${palaceName}宫·$palaceElement",
                Modifier.weight(2f),
                align = TextAlign.Start,
                color = MaterialTheme.colorScheme.primary
            )
            Box(Modifier.weight(0.4f))
            HeaderCell(
                if (changed != null) "${changed.name}\n${changedPalaceName}宫·$changedPalaceElement" else "变卦\n(静卦无变)",
                Modifier.weight(2f),
                align = TextAlign.Start,
                color = shiYingColorValue
            )
            HeaderText("变伏", Modifier.weight(1f))
        }

        Spacer(Modifier.height(2.dp))

        // 主卦中实际出现的六亲集合，用于判断伏神六亲是否缺显
        val originalLiuQinSet = orig.lines.map { it.liuQin }.toSet()

        // 六爻，从上爻（position 5）向下到初爻（position 0）
        for (p in 5 downTo 0) {
            PlateRow(
                liuShen = orig.lines[p].liuShen.cn,
                original = orig.lines[p],
                changed = changed?.lines?.get(p),
                fu = fu.lines[p],
                originalLiuQinSet = originalLiuQinSet
            )
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = lineTextStyle,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Center,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(modifier.clipToBounds()) {
        Text(
            text = text,
            textAlign = align,
            style = lineTextStyle,
            lineHeight = 14.sp,
            softWrap = false,
            color = color
        )
    }
}

@Composable
private fun PlateRow(
    liuShen: String,
    original: LineInfo,
    changed: LineInfo?,
    fu: LineInfo,
    originalLiuQinSet: Set<LiuQin>
) {
    Row(
        modifier = Modifier.fillMaxWidth().clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 六神
        Box(
            modifier = Modifier.weight(0.7f).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                liuShen,
                style = lineTextStyle,
                maxLines = 1,
                softWrap = false,
                color = shenColorValue
            )
        }

        // 主卦中已显现的六亲，用于判断伏神六亲是否在主卦里出现
        val fuMissing = fu.liuQin !in originalLiuQinSet

        // 主伏（伏神六亲·天干·地支）
        Box(
            modifier = Modifier.weight(1f).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            LineText(fu, isFu = true, missing = fuMissing)
        }

        // 本卦：爻条 + 六亲（天干地支）
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(38.dp)) {
                YaoBar(
                    yang = original.yang,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(2.dp))
            Box(Modifier.clipToBounds()) {
                LineText(original, isFu = false)
            }
        }

        // 动变标记
        Box(
            modifier = Modifier.weight(0.4f).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            MovingMarker(original)
        }

        // 变卦：爻条 + 六亲（天干地支）
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (changed != null) {
                Box(Modifier.width(38.dp)) {
                    YaoBar(
                        yang = changed.yang,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(2.dp))
                Box(Modifier.clipToBounds()) {
                    LineText(changed, isFu = false)
                }
            } else {
                // 静卦：变卦列用极淡的爻条填满空白，表明“无变”
                Box(Modifier.width(38.dp)) {
                    YaoBar(
                        yang = original.yang,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 变伏（伏神六亲·天干·地支）：始终绿色，不按主卦缺显判断
        Box(
            modifier = Modifier.weight(1f).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            LineText(fu, isFu = true)
        }
    }
}

/**
 * 六亲·天干·地支描述；伏神用绿色，本卦/变卦用黑色，世/应为蓝色小字。
 * 伏神的六亲若在卦中（本卦+变卦）未出现，则以红色显示，提示“伏神无对应”。
 * 文字放入受约束且裁切的格子中，超宽则裁切，绝不重叠到相邻列。
 */
@Composable
private fun LineText(line: LineInfo, isFu: Boolean, missing: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${line.liuQin.cn}${line.tianGan}${line.diZhi.cn}",
            style = lineTextStyle,
            maxLines = 1,
            softWrap = false,
            color = when {
                isFu && missing -> fuMissingColorValue
                isFu -> fuColorValue
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        if (!isFu && line.shiYing != ShiYingType.NONE) {
            Spacer(Modifier.width(1.dp))
            Text(
                text = if (line.shiYing == ShiYingType.SHI) "世" else "应",
                style = lineTextStyle,
                maxLines = 1,
                softWrap = false,
                color = shiYingColorValue
            )
        }
    }
}

/**
 * 动变标记（按传统六爻排盘风格，仅标记“动爻”，静爻留空，避免与前后爻条列重复）：
 * - 阳爻动（老阳）→ 变阴：o
 * - 阴爻动（老阴）→ 变阳：×
 * - 静爻：留空
 */
@Composable
private fun MovingMarker(original: LineInfo) {
    val text = when {
        original.moving && original.yang -> "o"
        original.moving && !original.yang -> "×"
        else -> ""
    }
    Text(
        text = text,
        style = lineTextStyle,
        maxLines = 1,
        softWrap = false,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 绘制爻：阳爻 = 一条实心长条；阴爻 = 中间留缝的两段条。
 */
@Composable
private fun YaoBar(yang: Boolean, color: Color, modifier: Modifier = Modifier) {
    val barHeight = 8.dp
    if (yang) {
        Box(
            modifier = modifier
                .height(barHeight)
                .background(color, RoundedCornerShape(3.dp))
        )
    } else {
        Row(
            modifier = modifier.height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}
