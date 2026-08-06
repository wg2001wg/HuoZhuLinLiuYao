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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.liuyao.huozhulin.data.model.DiZhi
import com.liuyao.huozhulin.data.model.kongWang
import com.liuyao.huozhulin.engine.GanZhi
import com.liuyao.huozhulin.engine.GanZhiCalendar
import com.liuyao.huozhulin.engine.LunarCalendar
import com.liuyao.huozhulin.engine.ShenSha
import com.liuyao.huozhulin.engine.WebAnalysis
import com.liuyao.huozhulin.ui.components.PlateTable
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

            // ===== 标签页：爻辞卦象 / AI解析 =====
            TabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })

            when (selectedTab) {
                0 -> ScripturePanel(r.original.name, r.changed?.name)
                1 -> AiAnalysisPanel(nav, vm)
            }

            // ===== 底部按钮 =====
            Button(
                onClick = { vm.saveCurrent(); saved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存到历史") }
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
        TabButton("AI解析", selectedTab == 1, Modifier.weight(1f)) { onSelect(1) }
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
        // ===== 主卦（本卦）河洛理数卦诀 =====
        Text("主卦：$originalName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HeLuoSection(originalName)

        // ===== 变卦（之卦）河洛理数卦诀 =====
        if (changedName != null) {
            Spacer(Modifier.height(10.dp))
            Text("变卦：$changedName", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            HeLuoSection(changedName)
        }
    }
}

/**
 * 《河洛理数》卦诀区块：以河洛「总诀 + 各爻诀」替换原有《周易》卦辞爻辞。
 * 因河洛诀文已含卦辞、象曰与诀文诗，故不再展示 Yijing 原文爻辞。
 */
@Composable
private fun HeLuoSection(guaName: String) {
    val zong = HeLuoLiShu.zongJue[guaName]
    val yao = HeLuoLiShu.yaoJue[guaName]
    if (zong == null && yao == null) return
    Spacer(Modifier.height(10.dp))
    if (zong != null) {
        Spacer(Modifier.height(4.dp))
        Text("总诀：$zong", style = MaterialTheme.typography.bodySmall)
    }
    yao?.forEach { (t, j) ->
        Spacer(Modifier.height(2.dp))
        Text("${t}诀：$j", style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * AI解析面板：完全由联网大模型解卦，替代原有的本地「系统解析」。
 * 默认模型 GLM-4.7-Flash，可在设置页自定义模型 / 接口地址 / API Key。
 */
@Composable
private fun AiAnalysisPanel(nav: NavHostController, vm: PaiPanViewModel) {
    val state by vm.analysisState.collectAsState()
    val model by vm.model.collectAsState()
    val currentModel = model.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AI解析",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "模型：$currentModel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Row {
                IconButton(onClick = { nav.navigate("settings") }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "AI解析设置",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { vm.fetchAnalysisStream() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新解析",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        when (val s = state) {
            is AnalysisState.Loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text(
                        "正在连接模型，即将开始逐字输出…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is AnalysisState.Streaming -> {
                MarkdownContent(s.content)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "● 生成中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is AnalysisState.Error -> {
                Text(
                    "AI解析失败：${s.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.fetchAnalysisStream() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("重试")
                    }
                    Button(
                        onClick = { nav.navigate("settings") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) { Text("去设置") }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        // 跳转系统应用设置页，引导用户开启本应用的「联网权限」
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("检查并授权网络权限") }
            }
            is AnalysisState.Success -> {
                MarkdownContent(s.result.content)
                Spacer(Modifier.height(6.dp))
                Text(
                    "以上内容由 AI 依据传统六爻理论生成，仅供参考。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Text(
                    "继续向 AI 提问",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                // 多轮对话直接内嵌在本结果页内，不弹窗
                ChatSection(vm = vm)
            }
            AnalysisState.Idle -> {
                Text(
                    "由 AI 联网结合世应、用神、六亲、六神、动变、日辰月建与旬空进行解卦。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { vm.fetchAnalysisStream() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E6B64),
                        contentColor = Color.White
                    )
                ) {
                    Text("开始 AI解析")
                }
            }
        }
    }
}

/**
 * 「继续向 AI 提问」对话区：内嵌在 AI 解析结果页中，展示多轮对话历史并允许用户输入后续问题。
 * 解析结果作为 system 背景与 assistant 首条回复被带入历史，AI 可据此连续追问。
 */
@Composable
private fun ChatSection(vm: PaiPanViewModel) {
    val messages by vm.chatMessages.collectAsState()
    val sending by vm.chatSending.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    // 新消息到达时自动滚动到底部
    LaunchedEffect(messages.size, sending) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // 对话区只展示用户的追问和对应的 AI 回复；
    // 跳过 system 提示，以及紧跟 system 的首轮 assistant 解析（已在上方「AI 解析」面板展示）。
    val visibleMessages = remember(messages) {
        val result = mutableListOf<WebAnalysis.Message>()
        var seenUser = false
        messages.forEach { msg ->
            when {
                msg.role == "system" -> { /* 不展示 */ }
                msg.role == "assistant" && !seenUser -> { /* 首轮解析，不展示 */ }
                else -> {
                    if (msg.role == "user") seenUser = true
                    result.add(msg)
                }
            }
        }
        result
    }

    Column(Modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleMessages.size) { idx ->
                val msg = visibleMessages[idx]
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        tonalElevation = 2.dp,
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        MarkdownContent(
                            msg.content,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("输入你的追问…") },
                enabled = !sending,
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
                singleLine = false,
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val q = input.trim()
                    if (q.isNotEmpty() && !sending) {
                        vm.sendChat(q)
                        input = ""
                        keyboard?.hide()
                    }
                },
                enabled = input.trim().isNotEmpty() && !sending
            ) {
                if (sending) CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                else Text("发送")
            }
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

/** Markdown 语法块：用于把 AI 返回的 Markdown 文本解析成可排版结构 */
private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

/** 解析简单 Markdown：标题、无序列表、普通段落 */
private fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val lines = content.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphBuffer.toString().trim()))
            paragraphBuffer.clear()
        }
    }

    for (rawLine in lines) {
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            flushParagraph()
            continue
        }

        val headingMatch = Regex("""^(#{1,6})\s+(.*)$""").find(line)
        if (headingMatch != null) {
            flushParagraph()
            blocks.add(
                MarkdownBlock.Heading(
                    headingMatch.groupValues[1].length,
                    headingMatch.groupValues[2].trim()
                )
            )
            continue
        }

        val bulletMatch = Regex("""^[*\-+]\s+(.*)$""").find(line)
        if (bulletMatch != null) {
            flushParagraph()
            blocks.add(MarkdownBlock.Bullet(bulletMatch.groupValues[1].trim()))
            continue
        }

        if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append("\n")
        paragraphBuffer.append(line)
    }
    flushParagraph()
    return blocks
}

/** 把字符串中的 **text** 渲染为加粗样式 */
private fun String.withBold(): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\*\*(.+?)\*\*""")
    var cursor = 0
    regex.findAll(this@withBold).forEach { match ->
        append(this@withBold.substring(cursor, match.range.first))
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        append(match.groupValues[1])
        pop()
        cursor = match.range.last + 1
    }
    append(this@withBold.substring(cursor))
}

/** 渲染 Markdown 内容到 Compose，支持标题、列表、加粗 */
@Composable
private fun MarkdownContent(content: String, modifier: Modifier = Modifier) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(
                        text = block.text.withBold(),
                        style = style,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is MarkdownBlock.Bullet -> {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = block.text.withBold(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = block.text.withBold(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
