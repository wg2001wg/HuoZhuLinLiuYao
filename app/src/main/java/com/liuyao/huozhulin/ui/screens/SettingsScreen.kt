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
    var saved by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it; saved = false },
                label = { Text("模型名（可选，留空用默认）") },
                placeholder = { Text(WebAnalysis.DEFAULT_MODEL) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                        vm.saveApiKey(keyInput)
                        vm.saveBaseUrl(urlInput)
                        vm.saveModel(modelInput)
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
