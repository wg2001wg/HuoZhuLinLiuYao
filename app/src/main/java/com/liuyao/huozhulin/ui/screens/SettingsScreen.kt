package com.liuyao.huozhulin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.liuyao.huozhulin.engine.WebAnalysis
import com.liuyao.huozhulin.viewmodel.PaiPanViewModel

@Composable
fun SettingsScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val apiKey by vm.apiKey.collectAsState()
    val baseUrl by vm.baseUrl.collectAsState()
    val model by vm.model.collectAsState()

    var keyInput by remember { mutableStateOf(apiKey) }
    var urlInput by remember { mutableStateOf(baseUrl) }
    var modelInput by remember { mutableStateOf(model) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    // 切换模型时，自动填充该模型已保存的 API Key（每种模型各自保存）
    val savedKeyForModel by vm.keyForModelFlow(modelInput).collectAsState(initial = "")
    LaunchedEffect(modelInput) {
        keyInput = savedKeyForModel
    }

    // 用户已添加的自定义模型列表（与预设一并展示在下拉中）
    val savedModels by vm.savedModelsFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI解析设置") }, navigationIcon = { BackIcon(nav) }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "AI解析：联网调用大模型解卦",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "默认使用智谱 ${WebAnalysis.DEFAULT_MODEL}（免费，已内置可用 Key），无需配置即可使用。\n" +
                        "如需换用自己的模型：填写 API Key、接口地址与模型名即可，" +
                        "支持任何兼容 OpenAI Chat Completions 格式的模型服务。\n" +
                        "三项均可留空，留空则使用默认值。应用仅将卦象排盘文本发送给模型，不会上传其它信息。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it; saved = false },
                label = { Text("API Key（可选，留空用内置默认）") },
                placeholder = { Text("sk-... 或 智谱 Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // 模型名：可输入文本框 + 下拉菜单；选择预设项后自动填充接口地址（仅当用户未手动填写时）
            ExposedDropdownMenuBox(
                expanded = modelMenuExpanded,
                onExpandedChange = { modelMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = {
                        modelInput = it
                        saved = false
                        modelMenuExpanded = true
                        // 模型名被清空时，API Key 与接口地址一并恢复到默认
                        if (it.trim().isEmpty()) {
                            keyInput = ""
                            urlInput = ""
                        }
                    },
                    label = { Text("模型名（可选，留空用默认）") },
                    placeholder = { Text(WebAnalysis.DEFAULT_MODEL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                    }
                )
                val filter = modelInput.trim()
                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("（不指定，使用默认）") },
                        onClick = {
                            modelInput = ""
                            // 模型清空，API Key 与接口地址一并恢复到默认
                            keyInput = ""
                            urlInput = ""
                            saved = false
                            modelMenuExpanded = false
                        }
                    )
                    WebAnalysis.MODEL_OPTIONS
                        .filter {
                            filter.isEmpty() || it.display.contains(filter, true) ||
                                it.model.contains(filter, true)
                        }
                        .forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.display) },
                                onClick = {
                                    modelInput = opt.model
                                    saved = false
                                    if (urlInput.trim().isEmpty()) {
                                        urlInput = opt.baseUrl
                                    }
                                    if (keyInput.trim().isEmpty() && opt.apiKey.isNotBlank()) {
                                        keyInput = opt.apiKey
                                    }
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    // 已添加的自定义模型：不在预设中则展示，供下次直接选择
                    savedModels
                        .filter { (m, _) -> WebAnalysis.MODEL_OPTIONS.none { it.model == m } }
                        .filter { (m, _) -> filter.isEmpty() || m.contains(filter, true) }
                        .forEach { (m, u) ->
                            DropdownMenuItem(
                                text = { Text("$m（自定义）") },
                                onClick = {
                                    modelInput = m
                                    saved = false
                                    if (urlInput.trim().isEmpty()) {
                                        urlInput = u
                                    }
                                    modelMenuExpanded = false
                                }
                            )
                        }
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; saved = false },
                label = { Text("接口地址（可选，留空用默认）") },
                placeholder = { Text(WebAnalysis.DEFAULT_BASE_URL) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                "当前生效：模型 ${modelInput.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }}\n" +
                        "地址 ${urlInput.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }}\n" +
                        "Key ${if (keyInput.isBlank()) "内置默认 Key" else "自定义 Key"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val m = modelInput.trim()
                        // 模型名清空时，API Key 与接口地址一并恢复到默认（留空即使用内置默认值）
                        if (m.isEmpty()) {
                            keyInput = ""
                            urlInput = ""
                        }
                        vm.saveApiKey(keyInput)
                        vm.saveBaseUrl(urlInput)
                        vm.saveModel(modelInput)
                        // 若用户输入了一个新模型名（不在预设、也不在已保存列表中），则保存该模型供下次选择
                        val known = WebAnalysis.MODEL_OPTIONS.any { it.model.equals(m, true) } ||
                            savedModels.any { it.first.equals(m, true) }
                        if (m.isNotEmpty() && !known) {
                            vm.addCustomModel(m, urlInput)
                        }
                        saved = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
                Button(
                    onClick = {
                        keyInput = ""; urlInput = ""; modelInput = ""
                        vm.saveApiKey(""); vm.saveBaseUrl(""); vm.saveModel("")
                        saved = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    )
                ) { Text("恢复默认") }
            }

            if (saved) {
                Text("已保存。可在结果页或历史页使用「AI解析」。", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
