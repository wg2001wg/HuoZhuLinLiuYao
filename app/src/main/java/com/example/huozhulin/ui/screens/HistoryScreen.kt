package com.example.huozhulin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.huozhulin.data.local.RecordEntity
import com.example.huozhulin.viewmodel.PaiPanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                HistoryCard(rec, onClick = {
                    vm.loadFromRecord(rec)
                    nav.navigate("result")
                }, onDelete = { vm.delete(rec) })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    rec: RecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    val time = fmt.format(Date(rec.timestamp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${rec.originalName}${if (rec.changedName != null) " 之 ${rec.changedName}" else ""}",
                    style = MaterialTheme.typography.titleSmall
                )
                val day = "${rec.dayGanCn}${rec.dayZhiCn ?: ""}"
                val month = rec.monthZhiCn?.let { " 月建$it" } ?: ""
                Text("$time  $day$month", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
    }
}
