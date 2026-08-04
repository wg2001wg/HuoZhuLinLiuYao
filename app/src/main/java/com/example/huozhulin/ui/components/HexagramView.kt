package com.example.huozhulin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huozhulin.data.model.HexagramPlate
import com.example.huozhulin.data.model.LineInfo
import com.example.huozhulin.data.model.ShiYingType

@Composable
fun HexagramBoard(
    plate: HexagramPlate,
    showOriginalMotion: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${plate.hexagram.name}（${plate.hexagram.palace.cnName}宫 · ${plate.hexagram.shiXi.cn}）",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // 自上而下：上爻(5) -> 初爻(0)
            for (p in 5 downTo 0) {
                LineRow(plate.lines[p], showOriginalMotion)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LineRow(info: LineInfo, showMotion: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 爻位名 + 世应
        Column(modifier = Modifier.width(54.dp)) {
            Text(info.positionName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            if (info.shiYing != ShiYingType.NONE) {
                val (txt, col) = when (info.shiYing) {
                    ShiYingType.SHI -> "世" to Color(0xFF8B1A1A)
                    else -> "应" to Color(0xFF1A5B8B)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(col.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(txt, color = col, fontSize = 11.sp)
                }
            }
        }

        // 爻线
        YaoLine(info.yang, info.moving && showMotion)

        // 右侧信息
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                "${info.liuQin.cn}  ${info.diZhi.cn}${info.tianGan}  ${info.liuShen.cn}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            val tags = buildList {
                if (info.wangShuai != null) add(info.wangShuai!!.cn)
                if (info.kongWang) add("空亡")
            }
            if (tags.isNotEmpty()) {
                Text(tags.joinToString(" · "), fontSize = 11.sp, color = scheme.secondary)
            }
        }
    }
}

@Composable
private fun YaoLine(yang: Boolean, moving: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val barColor = if (moving) Color(0xFFC0392B) else scheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (yang) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
        if (moving) {
            Text(if (yang) "○" else "×", color = Color(0xFFC0392B), fontSize = 16.sp)
        }
    }
}
