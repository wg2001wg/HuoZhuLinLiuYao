package com.liuyao.huozhulin.ui.screens

import com.liuyao.huozhulin.data.HeLuoLiShu
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.liuyao.huozhulin.data.Yijing
import com.liuyao.huozhulin.data.model.DiZhi
import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.kongWang
import com.liuyao.huozhulin.engine.GanZhi
import com.liuyao.huozhulin.engine.GanZhiCalendar
import com.liuyao.huozhulin.engine.LunarCalendar
import com.liuyao.huozhulin.engine.ShenSha
import com.liuyao.huozhulin.ui.components.PlateTable
import com.liuyao.huozhulin.engine.WebAnalysis
import com.liuyao.huozhulin.viewmodel.AnalysisState
import com.liuyao.huozhulin.viewmodel.PaiPanViewModel
import java.util.Calendar
import java.util.TimeZone

/** 结果页文字整体放大比例（约 1/3） */
private const val TEXT_SCALE = 1.33f

/** 把某个 TextStyle 的字号按比例放大 */
private fun TextStyle.scaleFont(f: Float): TextStyle = this.copy(fontSize = this.fontSize * f)

@Composable
fun ResultScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val result by vm.result.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("排盘结果") }, navigationIcon = { BackIcon(nav) }) }
    ) { pad ->
        if (result == null) {
            Column(Modifier.padding(pad).padding(16.dp)) { Text("请先起卦。") }
            return@Scaffold
        }
        val r = result!!
        var saved by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableIntStateOf(0) }
        var showEditInfo by remember { mutableStateOf(false) }

        val baseTypo = MaterialTheme.typography
        val scaledTypo = remember(baseTypo) {
            with(baseTypo) {
                Typography(
                    displayLarge = displayLarge.scaleFont(TEXT_SCALE),
                    displayMedium = displayMedium.scaleFont(TEXT_SCALE),
                    displaySmall = displaySmall.scaleFont(TEXT_SCALE),
                    headlineLarge = headlineLarge.scaleFont(TEXT_SCALE),
                    headlineMedium = headlineMedium.scaleFont(TEXT_SCALE),
                    headlineSmall = headlineSmall.scaleFont(TEXT_SCALE),
                    titleLarge = titleLarge.scaleFont(TEXT_SCALE),
                    titleMedium = titleMedium.scaleFont(TEXT_SCALE),
                    titleSmall = titleSmall.scaleFont(TEXT_SCALE),
                    bodyLarge = bodyLarge.scaleFont(TEXT_SCALE),
                    bodyMedium = bodyMedium.scaleFont(TEXT_SCALE),
                    bodySmall = bodySmall.scaleFont(TEXT_SCALE),
                    labelLarge = labelLarge.scaleFont(TEXT_SCALE),
                    labelMedium = labelMedium.scaleFont(TEXT_SCALE),
                    labelSmall = labelSmall.scaleFont(TEXT_SCALE)
                )
            }
        }
        MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = scaledTypo) {
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ===== 顶部：阳历 / 农历 / 节气 =====
            val fp = vm.fourPillars.collectAsState().value
            val ts = vm.castTime.collectAsState().value ?: System.currentTimeMillis()
            val lunar = remember(ts) { LunarCalendar.toLunar(ts) }
            val (prevTerm, nextTerm) = remember(ts) { LunarCalendar.aroundSolarTerms(ts) }

            InfoCard {
                Text("阳历时：${formatGregorian(ts)}", style = MaterialTheme.typography.bodyMedium)
                Text("农历时：${lunar.format()}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${prevTerm.first}：${formatDateTime(prevTerm.second.timeInMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "${nextTerm.first}：${formatDateTime(nextTerm.second.timeInMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ===== 卦主 / 问事 / 神煞 / 干支 / 空亡 / 伏神 =====
            val gz by vm.guaZhu.collectAsState()
            val ws by vm.wenShi.collectAsState()
            val dayGZ = fp?.day
            val monthGZ = fp?.month
            InfoCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "卦主：$gz",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "问事：$ws",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (dayGZ != null) {
                    Text(
                        "神煞：驿马=${ShenSha.yiMa(dayGZ.zhi).cn}  桃花=${ShenSha.taoHua(dayGZ.zhi).cn}  干禄=${ShenSha.ganLu(dayGZ.gan).cn}  贵人=${ShenSha.guiRen(dayGZ.gan).joinToString("、") { it.cn }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                fp?.let { four ->
                    Text(
                        "干支：${four.year.cn}年  ${four.month.cn}月  ${four.day.cn}日  ${four.hour.cn}时",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "空亡：${xunKongText(four.year)}  ${xunKongText(four.month)}  ${xunKongText(four.day)}  ${xunKongText(four.hour)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "伏神：《火珠林》伏神法",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "点击编辑卦主/问事",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { showEditInfo = true }
                )
            }

            if (showEditInfo) {
                EditInfoDialog(
                    guaZhu = gz,
                    wenShi = ws,
                    onSave = { g, w ->
                        vm.guaZhu.value = g
                        vm.wenShi.value = w
                        showEditInfo = false
                    },
                    onDismiss = { showEditInfo = false }
                )
            }

            // ===== 六爻盘 =====
            PlateTable(r)

            // ===== 标签页：爻辞卦象 / 系统解析 =====
            TabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })

            when (selectedTab) {
                0 -> ScripturePanel(r.original.name, r.changed?.name)
                1 -> SystemAnalysisPanel(vm, r)
            }

            // ===== 底部按钮 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* TODO: 复制排盘文本 */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("复制") }
                Button(
                    onClick = { vm.saveCurrent(); saved = true },
                    modifier = Modifier.weight(1f)
                ) { Text("保存到历史") }
            }
            if (saved) Text("已保存到排盘历史。", color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(12.dp))
        }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun TabRow(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TabButton("爻辞卦象", selectedTab == 0, Modifier.weight(1f)) { onSelect(0) }
        TabButton("系统解析", selectedTab == 1, Modifier.weight(1f)) { onSelect(1) }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun ScripturePanel(originalName: String, changedName: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(originalName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        val guaCi = Yijing.guaCi[originalName]
        if (!guaCi.isNullOrBlank()) {
            Text("卦辞：$guaCi", style = MaterialTheme.typography.bodySmall)
        }
        Yijing.yaoCi[originalName]?.forEach { (t, x) ->
            Text("$t：$x", style = MaterialTheme.typography.bodySmall)
        }
        HeLuoSection(originalName)
        if (changedName != null) {
            Spacer(Modifier.height(10.dp))
            Text("变卦：$changedName", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            val chGuaCi = Yijing.guaCi[changedName]
            if (!chGuaCi.isNullOrBlank()) {
                Text("卦辞：$chGuaCi", style = MaterialTheme.typography.bodySmall)
            }
            Yijing.yaoCi[changedName]?.forEach { (t, x) ->
                Text("$t：$x", style = MaterialTheme.typography.bodySmall)
            }
            HeLuoSection(changedName)
        }
    }
}

@Composable
private fun HeLuoSection(guaName: String) {
    val zong = HeLuoLiShu.zongJue[guaName]
    val yao = HeLuoLiShu.yaoJue[guaName]
    if (zong == null && yao == null) return
    Spacer(Modifier.height(10.dp))
    Text(
        "《河洛理数》卦诀",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.tertiary
    )
    if (zong != null) {
        Spacer(Modifier.height(4.dp))
        Text("总诀：$zong", style = MaterialTheme.typography.bodySmall)
    }
    yao?.forEach { (t, j) ->
        Spacer(Modifier.height(2.dp))
        Text("${t}诀：$j", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SystemAnalysisPanel(vm: PaiPanViewModel, r: PaiPanResult) {
    val state by vm.analysisState.collectAsState()
    val apiKey by vm.doubaoApiKey.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            onSave = { vm.setDoubaoApiKey(it) },
            onDismiss = { showApiKeyDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(
            "系统解析",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))

        // —— 本地基础解析（始终展示）——
        Text(
            "本卦：${r.original.name}，属${r.original.hexagram.palace.cnName}宫（${r.original.hexagram.palaceElement.cn}）。",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "世爻在${r.original.lines.indexOfFirst { it.shiYing == com.liuyao.huozhulin.data.model.ShiYingType.SHI } + 1}爻，" +
                    "应爻在${r.original.lines.indexOfFirst { it.shiYing == com.liuyao.huozhulin.data.model.ShiYingType.YING } + 1}爻。",
            style = MaterialTheme.typography.bodySmall
        )
        val dong = r.original.lines.filter { it.moving }
        if (dong.isEmpty()) {
            Text("此卦无动爻，为静卦。", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "动爻：${dong.joinToString("、") { "第${it.position + 1}爻（${it.positionName}）" }}。",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            "伏神以本宫卦${r.fu.name}为伏。",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "联网解析材料",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Row {
                IconButton(onClick = { showApiKeyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置豆包 API Key",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { vm.fetchAnalysis() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新联网解析",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // —— 联网解析结果 ——
        when (val s = state) {
            is AnalysisState.Loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text("正在联网获取解析材料…", style = MaterialTheme.typography.bodySmall)
                }
            }
            is AnalysisState.Error -> {
                Text(
                    "联网解析暂不可用：${s.message}。已切换为本地解析，点击右上角刷新重试。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                val fb = WebAnalysis.fallback(r)
                fb.items.forEach { item -> AnalysisRow(item) }
            }
            is AnalysisState.Success -> {
                Text(
                    "检索词：${s.result.query}（来源：${s.result.items.firstOrNull()?.source ?: "网络"}）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(2.dp))
                if (s.result.items.isEmpty()) {
                    Text("未获取到解析材料，请稍后重试。", style = MaterialTheme.typography.bodySmall)
                } else {
                    s.result.items.forEach { item -> AnalysisRow(item) }
                }
            }
            AnalysisState.Idle -> {
                Button(
                    onClick = { vm.fetchAnalysis() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("点击获取联网解析")
                }
            }
        }
    }
}

@Composable
private fun AnalysisRow(item: com.liuyao.huozhulin.engine.WebAnalysis.AnalysisItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .padding(8.dp)
    ) {
        if (item.title.isNotBlank() && item.title != "相关解析") {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            item.snippet,
            style = MaterialTheme.typography.bodySmall
        )
        if (!item.source.isNullOrBlank() && item.source != "网页") {
            Text(
                "来源：${item.source}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("豆包 API Key 设置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "配置火山方舟 API Key 后可调用豆包大模型生成更详细的六爻解析。未配置时仍可使用本地增强解析。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
            ) { Text("取消") }
            Button(
                onClick = { onSave(key); onDismiss() },
                modifier = Modifier.weight(1f)
            ) { Text("保存") }
        }
    }
}

@Composable
private fun EditInfoDialog(
    guaZhu: String,
    wenShi: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var gz by remember { mutableStateOf(guaZhu) }
    var ws by remember { mutableStateOf(wenShi) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("编辑信息", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = gz, onValueChange = { gz = it }, label = { Text("卦主") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = ws, onValueChange = { ws = it }, label = { Text("问事") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)) { Text("取消") }
            Button(onClick = { onSave(gz, ws) }, modifier = Modifier.weight(1f)) { Text("保存") }
        }
    }
}

/** 四柱中某一柱的旬空文本，例如「寅卯空」 */
private fun xunKongText(gz: GanZhi): String {
    val kw = kongWang(gz.gan, gz.zhi)
    return if (kw.isEmpty()) "—" else kw.joinToString("", transform = { it.cn }) + "空"
}

/** 把时间戳格式化为数字日期时间：2026-08-03 15:20 */
private fun formatGregorian(ts: Long): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = ts }
    val y = c.get(Calendar.YEAR)
    val m = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    val hh = c.get(Calendar.HOUR_OF_DAY)
    val mm = c.get(Calendar.MINUTE)
    return String.format("%04d-%02d-%02d %02d:%02d", y, m, d, hh, mm)
}

/** 带秒的数字日期时间：2026-07-07 09:58:00 */
private fun formatDateTime(ts: Long): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = ts }
    val y = c.get(Calendar.YEAR)
    val m = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    val hh = c.get(Calendar.HOUR_OF_DAY)
    val mm = c.get(Calendar.MINUTE)
    val ss = c.get(Calendar.SECOND)
    return String.format("%04d-%02d-%02d %02d:%02d:%02d", y, m, d, hh, mm, ss)
}
