package com.liuyao.huozhulin.ui.screens

import android.content.Context
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
                    onClick = {
                        vm.loadFromRecord(rec)
                        nav.navigate("result")
                    },
                    onDelete = { vm.delete(rec) }
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    val time = fmt.format(Date(rec.timestamp))

    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                        if (resultText == null && errorText == null) {
                            runAiAnalysis({ vm.buildAiPrompt(rec) }, apiKey, baseUrl) { state ->
                                when (state) {
                                    is AiState.Loading -> loading = true
                                    is AiState.Success -> {
                                        loading = false
                                        resultText = state.text
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
                            contentDescription = "AI 解读",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }

            if (expanded) {
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
                        "AI 联网解读（DeepSeek）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        loading -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                            Text("正在联网请求 DeepSeek 解读…", style = MaterialTheme.typography.bodySmall)
                        }
                        errorText != null -> Text(
                            "AI 解读暂不可用：$errorText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        resultText != null -> Text(
                            resultText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        else -> Text("点击上方图标获取 AI 解读。", style = MaterialTheme.typography.bodySmall)
                    }
                    if (errorText != null && errorText!!.contains("DeepSeek API Key")) {
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { nav.navigate("settings") }) {
                            Text("前往设置 API Key")
                        }
                    }
                }
            }
        }
    }
}

// 简单的 AI 解析状态机
private sealed class AiState {
    object Loading : AiState()
    data class Success(val text: String) : AiState()
    data class Error(val msg: String) : AiState()
}

private fun runAiAnalysis(
    buildPrompt: () -> String?,
    apiKey: String,
    baseUrl: String,
    onState: (AiState) -> Unit
) {
    val key = apiKey.trim()
    if (key.isBlank()) {
        onState(AiState.Error("尚未配置 DeepSeek API Key。请前往「设置」页面填写后重试。"))
        return
    }
    val prompt = buildPrompt()
    if (prompt == null) {
        onState(AiState.Error("该记录缺少完整排盘数据，无法重建卦象。"))
        return
    }
    onState(AiState.Loading)
    CoroutineScope(Dispatchers.Main).launch {
        val text = withContext(Dispatchers.IO) {
            WebAnalysis.analyze(key, prompt, baseUrl.ifBlank { WebAnalysis.DEFAULT_BASE_URL })
        }
        if (text.startsWith("请求失败") || text.startsWith("联网解析出错")) {
            onState(AiState.Error(text))
        } else {
            onState(AiState.Success(text))
        }
    }
}
