package com.liuyao.huozhulin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.liuyao.huozhulin.data.local.RecordEntity
import com.liuyao.huozhulin.engine.WebAnalysis
import com.liuyao.huozhulin.ui.components.MarkdownText
import com.liuyao.huozhulin.viewmodel.PaiPanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val records by vm.historyFlow().collectAsState(initial = emptyList())
    Scaffold(
        topBar = { TopAppBar(title = { Text("排盘历史") }, navigationIcon = { BackIcon(nav) }) }
    ) { pad ->
        if (records.isEmpty()) {
            Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("还没有保存的排盘记录。在排盘结果页点击「保存到历史」即可。")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { rec ->
                HistoryCard(
                    vm = vm,
                    nav = nav,
                    rec = rec,
                    apiKey = vm.apiKey.collectAsState().value,
                    baseUrl = vm.baseUrl.collectAsState().value,
                    model = vm.model.collectAsState().value,
                    onClick = {
                        vm.loadFromRecord(rec)
                        nav.navigate("result")
                    },
                    onDelete = { vm.delete(rec) },
                    onSaved = { vm.refreshRecordAi(it) }
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    vm: PaiPanViewModel,
    nav: NavHostController,
    rec: RecordEntity,
    apiKey: String,
    baseUrl: String,
    model: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSaved: (RecordEntity) -> Unit
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    val time = fmt.format(Date(rec.timestamp))

    // 优先使用记录中已保存的 AI 解析结果，下次打开直接查看，无需重新联网
    var expanded by remember { mutableStateOf(false) }
    var savedResult by remember { mutableStateOf(rec.aiResult) }
    var savedModel by remember { mutableStateOf(rec.aiModel) }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 已保存内容（落库结果优先，未保存则看本次生成的）
    val displayText = savedResult ?: resultText
    val displayModel = savedModel ?: model.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${rec.originalName}${if (rec.changedName != null) " 之 ${rec.changedName}" else ""}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    val day = "${rec.dayGanCn}${rec.dayZhiCn ?: ""}"
                    val month = rec.monthZhiCn?.let { " 月建$it" } ?: ""
                    Text("$time  $day$month", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = {
                        expanded = true
                        if (displayText == null && errorText == null) {
                            runAiAnalysis({ vm.buildAiPrompt(rec) }, apiKey, baseUrl, model) { state ->
                                when (state) {
                                    is AiState.Loading -> loading = true
                                    is AiState.Streaming -> {
                                        loading = true
                                        resultText = state.text
                                    }
                                    is AiState.Success -> {
                                        loading = false
                                        resultText = state.text
                                        errorText = null
                                        // 刷新 AI 解析后，自动将最终结果写回该历史记录，下次离线直接查看
                                        savedResult = state.text
                                        savedModel = model.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
                                        onSaved(
                                            rec.copy(
                                                aiResult = state.text,
                                                aiModel = savedModel
                                            )
                                        )
                                    }
                                    is AiState.Error -> {
                                        loading = false
                                        errorText = state.msg
                                    }
                                }
                            }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = "AI解析",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }

            if (expanded) {
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        "AI解析（${displayModel}）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        loading && resultText != null -> {
                            Text(
                                resultText!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "● 生成中…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        loading -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                            Text(
                                "正在联网 AI 解析，即将开始逐字输出…",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        errorText != null -> Text(
                            "AI解析失败：$errorText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        displayText != null -> MarkdownText(
                            displayText,
                            baseStyle = MaterialTheme.typography.bodyMedium
                        )
                        else -> Text("点击上方图标开始 AI解析。", style = MaterialTheme.typography.bodySmall)
                    }
                    if (errorText != null) {
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { nav.navigate("settings") }) {
                            Text("前往 AI解析设置")
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
                    if (savedResult != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "（已保存，离线可查看）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// 简单的 AI解析状态机
private sealed class AiState {
    object Loading : AiState()
    data class Streaming(val text: String) : AiState()
    data class Success(val text: String) : AiState()
    data class Error(val msg: String) : AiState()
}

private fun runAiAnalysis(
    buildPrompt: () -> String?,
    apiKey: String,
    baseUrl: String,
    model: String,
    onState: (AiState) -> Unit
) {
    // 未配置时使用内置默认值
    val key = apiKey.trim().ifBlank { WebAnalysis.DEFAULT_API_KEY }
    val url = baseUrl.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
    val mdl = model.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
    val prompt = buildPrompt()
    if (prompt == null) {
        onState(AiState.Error("该记录缺少完整排盘数据，无法重建卦象。"))
        return
    }
    onState(AiState.Loading)
    CoroutineScope(Dispatchers.Main).launch {
        val sb = StringBuilder()
        var errored = false
        withContext(Dispatchers.IO) {
            WebAnalysis.analyzeStream(
                key, prompt, url, mdl,
                onDelta = { piece ->
                    sb.append(piece)
                    onState(AiState.Streaming(sb.toString()))
                },
                onError = { msg ->
                    errored = true
                    if (sb.isEmpty()) {
                        onState(AiState.Error(msg))
                    } else {
                        onState(AiState.Success(sb.toString() + "\n\n（解析中断：$msg）"))
                    }
                }
            )
        }
        if (errored) return@launch
        if (sb.isEmpty()) {
            onState(AiState.Error("模型未返回内容。"))
        } else {
            onState(AiState.Success(sb.toString()))
        }
    }
}
