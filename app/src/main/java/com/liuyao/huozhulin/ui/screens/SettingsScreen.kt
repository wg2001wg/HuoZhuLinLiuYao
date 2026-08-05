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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.liuyao.huozhulin.viewmodel.PaiPanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val apiKey by vm.apiKey.collectAsState()
    val baseUrl by vm.baseUrl.collectAsState()

    var keyInput by remember { mutableStateOf(apiKey) }
    var urlInput by remember { mutableStateOf(baseUrl) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { BackIcon(nav) }) }
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
                "AI 解读模型：智谱 GLM-4.7-Flash（免费）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "默认已内置可用的智谱 GLM-4.7-Flash 免费 Key，可直接使用 AI 解读；\n" +
                        "如需更换，可在智谱开放平台（open.bigmodel.cn）注册并创建自己的 API Key 后粘贴到下方覆盖。\n" +
                        "应用仅将卦象排盘文本发送给模型用于解读，不会上传其它信息。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it; saved = false },
                label = { Text("DeepSeek API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; saved = false },
                label = { Text("接口地址（可选，留空用默认）") },
                placeholder = { Text("https://api.deepseek.com/chat/completions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                        saved = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
                Button(
                    onClick = { keyInput = ""; urlInput = ""; vm.saveApiKey(""); vm.saveBaseUrl("") },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    )
                ) { Text("清除") }
            }

            if (saved) {
                Text("已保存。可在结果页或历史页点击「AI 解读」使用。", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
