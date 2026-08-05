package com.example.huozhulin.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.huozhulin.data.model.DiZhi
import com.example.huozhulin.data.model.TianGan
import com.example.huozhulin.data.model.Trigram
import com.example.huozhulin.engine.CastEngine
import com.example.huozhulin.engine.LunarCalendar
import com.example.huozhulin.viewmodel.PaiPanViewModel
import java.util.Calendar

@Composable
fun HomeScreen(nav: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("火珠林六爻排盘") }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("请选择起卦方式", style = MaterialTheme.typography.titleMedium)
            CastButton(nav, "random", "随机卦（软件模拟手摇）")
            CastButton(nav, "coin", "铜钱摇卦（逐爻摇三枚铜钱）")
            CastButton(nav, "manual", "指定卦（手动点爻）")
            CastButton(nav, "number", "数字卦")
            CastButton(nav, "date", "日期卦（农历 / 阳历）")
            CastButton(nav, "hourMinute", "时分卦")
            CastButton(nav, "lifetime", "终身卦")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { nav.navigate("history") }, Modifier.fillMaxWidth()) { Text("排盘历史") }
        }
    }
}

@Composable
private fun CastButton(nav: NavHostController, route: String, text: String) {
    Button(onClick = { nav.navigate(route) }, Modifier.fillMaxWidth()) { Text(text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomScreen(nav: NavHostController, vm: PaiPanViewModel) {
    CastScaffold(nav, "随机卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("软件随机模拟手摇，生成 6 个爻位。老阴、老阳为动爻。")
            ActionButton("随机起卦") {
                val (l, m) = CastEngine.randomCast()
                vm.setCast(l, m)
                nav.navigate("result")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinScreen(nav: NavHostController, vm: PaiPanViewModel) {
    // 已摇出的爻（索引 0 = 初爻，自下而上）
    val yaos = remember { mutableStateListOf<CastEngine.SingleYao>() }
    var currentBacks by remember { mutableStateOf<List<Boolean>?>(null) }
    var rolling by remember { mutableStateOf(false) }

    fun rollOnce() {
        if (rolling || yaos.size >= 6) return
        rolling = true
        currentBacks = List(3) { kotlin.random.Random.nextBoolean() } // 动画用随机面
    }

    // 动画结束后用引擎结果定格并提交本爻
    LaunchedEffect(rolling) {
        if (rolling) {
            kotlinx.coroutines.delay(700)
            val yao = CastEngine.castYao()
            currentBacks = yao.backs
            yaos.add(yao)
            rolling = false
        }
    }

    CastScaffold(nav, "铜钱摇卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val idx = yaos.size
            Text(
                text = if (yaos.size < 6) "第 ${idx + 1} 爻（${YAO_POSITION_CN[idx]}爻）" else "六爻已成",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "字(有字)=阴　背(无字)=阳\n1背2字=少阳　2背1字=少阴\n3背=老阳(○动)　3字=老阴(×动)",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 三枚铜钱
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val backs = currentBacks ?: List(3) { false }
                repeat(3) { c ->
                    CoinView(back = backs.getOrNull(c) ?: false, rolling = rolling, spin = c * 0.3f)
                }
            }

            // 本次结果
            if (currentBacks != null && !rolling) {
                yaos.lastOrNull()?.let { y ->
                    Text("本次：${y.symbol}　${yaoDesc(y)}", style = MaterialTheme.typography.titleMedium)
                }
            } else if (rolling) {
                Text("摇动中…", style = MaterialTheme.typography.bodyMedium)
            }

            // 操作
            if (yaos.size < 6) {
                ActionButton(if (yaos.isEmpty()) "开始摇卦" else "摇下一爻", enabled = !rolling) { rollOnce() }
                if (yaos.isNotEmpty()) {
                    TextButton(onClick = {
                        if (!rolling) { yaos.removeAt(yaos.lastIndex); currentBacks = null }
                    }, enabled = !rolling) { Text("撤销上一爻") }
                }
            } else {
                ActionButton("前往排盘") {
                    vm.setCast(yaos.map { it.yang }, yaos.map { it.moving })
                    nav.navigate("result")
                }
                OutlinedButton(onClick = {
                    yaos.clear(); currentBacks = null; rolling = false
                }) { Text("重摇") }
            }

            // 已摇爻记录（上→下展示）
            if (yaos.isNotEmpty()) {
                Text("卦象（上→下）", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in (yaos.size - 1) downTo 0) {
                        val y = yaos[i]
                        Text(
                            text = "${YAO_POSITION_CN[i]}爻 ${y.symbol}　${yaoDesc(y)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private val YAO_POSITION_CN = listOf("初", "二", "三", "四", "五", "上")

private fun yaoDesc(y: CastEngine.SingleYao): String = when {
    y.moving && y.yang -> "老阳（动）"
    y.moving && !y.yang -> "老阴（动）"
    y.yang -> "少阳（静）"
    else -> "少阴（静）"
}

/**
 * 单枚铜钱视图。
 * 不再使用 rotationY 3D 翻转，避免文字被镜像颠倒。
 * 仅通过 2D 缩放+旋转动画模拟摇晃，文字始终正向。
 */
@Composable
private fun CoinView(back: Boolean, rolling: Boolean, spin: Float) {
    // 摇晃动画：2D 缩放 + 平面旋转，文字始终正向，不再使用 rotationY 导致镜像颠倒
    val coinScale by animateFloatAsState(
        targetValue = if (rolling) 0.8f else 1f,
        animationSpec = repeatable(
            iterations = if (rolling) 4 else 1,
            animation = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinScale"
    )
    val coinRotation by animateFloatAsState(
        targetValue = if (rolling) (360f * 3 + spin * 360f) else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "coinRotation"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = coinScale
                scaleY = coinScale
                rotationZ = coinRotation
            }
            .clip(CircleShape)
            .background(if (back) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (back) "背" else "字",
            color = if (back) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(nav: NavHostController, vm: PaiPanViewModel) {
    var lines by remember { mutableStateOf(List(6) { true }) }
    var moving by remember { mutableStateOf(List(6) { false }) }
    CastScaffold(nav, "指定卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("依次设置初爻到上爻的阴阳与动静：", style = MaterialTheme.typography.titleSmall)
            val names = listOf("初爻", "二爻", "三爻", "四爻", "五爻", "上爻")
            for (p in 0..5) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(names[p], modifier = Modifier.width(56.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (lines[p]) "阳" else "阴")
                        Switch(checked = lines[p], onCheckedChange = {
                            lines = lines.toMutableList().also { it[p] = it[p].not() }
                        })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (moving[p]) "动" else "静")
                        Switch(checked = moving[p], onCheckedChange = {
                            moving = moving.toMutableList().also { it[p] = it[p].not() }
                        })
                    }
                }
            }
            ActionButton("前往排盘") {
                vm.setCast(lines, moving)
                nav.navigate("result")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberScreen(nav: NavHostController, vm: PaiPanViewModel) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var addHour by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    CastScaffold(nav, "数字卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("前半部分数字和除 8 余数为上卦，后半部分数字和除 8 余数为下卦，总和除 6 余数为动爻。")
            OutlinedTextField(
                value = first,
                onValueChange = { first = it.filter { c -> c.isDigit() } },
                label = { Text("前半部分数字") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = second,
                onValueChange = { second = it.filter { c -> c.isDigit() } },
                label = { Text("后半部分数字") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = addHour, onCheckedChange = { addHour = it })
                Text("动爻加时支数")
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ActionButton("按数字起卦") {
                if (first.isBlank() && second.isBlank()) {
                    error = "请至少输入一组数字"; return@ActionButton
                }
                error = null
                val hourBranch = CastEngine.currentHourBranch()
                val (l, m) = CastEngine.numberCast(first, second, addHour, hourBranch)
                vm.setCast(l, m)
                nav.navigate("result")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val c = Calendar.getInstance()
    var useLunar by remember { mutableStateOf(false) }
    var year by remember { mutableStateOf(c.get(Calendar.YEAR).toString()) }
    var month by remember { mutableStateOf((c.get(Calendar.MONTH) + 1).toString()) }
    var day by remember { mutableStateOf(c.get(Calendar.DAY_OF_MONTH).toString()) }
    var hour by remember { mutableStateOf(c.get(Calendar.HOUR_OF_DAY).toString()) }
    var extra by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    CastScaffold(nav, "日期卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("按图片说明：上卦=(年+月+日)除8余数；下卦=(上卦总数+时支数+附加数)除8余数；动爻=下卦总数除6余数。")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useLunar, onCheckedChange = { useLunar = it })
                Text(if (useLunar) "使用农历日期" else "使用阳历日期")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedField(year, { year = it.filter { ch -> ch.isDigit() } }, "年", Modifier.weight(1f))
                SmallOutlinedField(month, { month = it.filter { ch -> ch.isDigit() } }, "月", Modifier.weight(1f))
                SmallOutlinedField(day, { day = it.filter { ch -> ch.isDigit() } }, "日", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedField(hour, { hour = it.filter { ch -> ch.isDigit() } }, "时(0-23)", Modifier.weight(1f))
                SmallOutlinedField(extra, { extra = it.filter { ch -> ch.isDigit() } }, "附加数", Modifier.weight(1f))
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ActionButton("日期起卦") {
                error = null
                val y = year.toIntOrNull() ?: 0
                val m = month.toIntOrNull() ?: 0
                val d = day.toIntOrNull() ?: 0
                val h = hour.toIntOrNull() ?: 0
                val ex = extra.toIntOrNull() ?: 0
                if (y <= 0 || m !in 1..12 || d !in 1..31 || h !in 0..23) {
                    error = "日期/时间输入不合法"; return@ActionButton
                }
                try {
                    val (l, mov) = if (useLunar) {
                        // 需要农历年支数；这里先用公历转农历得到农历年月日
                        val lunar = LunarCalendar.toLunar(y, m, d, h)
                        CastEngine.lunarDateCast(
                            lunarYear = lunar.year,
                            lunarMonth = lunar.month,
                            lunarDay = lunar.day,
                            hourBranch = DiZhi.forHour(h).ordinal + 1,
                            extra = ex
                        )
                    } else {
                        CastEngine.solarDateCast(y, m, d, DiZhi.forHour(h).ordinal + 1, ex)
                    }
                    vm.setCast(l, mov)
                    nav.navigate("result")
                } catch (e: Exception) {
                    error = e.message ?: "起卦失败"
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourMinuteScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val c = Calendar.getInstance()
    var hour by remember { mutableStateOf(c.get(Calendar.HOUR_OF_DAY).toString()) }
    var minute by remember { mutableStateOf(c.get(Calendar.MINUTE).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    CastScaffold(nav, "时分卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("时数除 8 余数为上卦，分钟数除 8 余数为下卦，（时数+分钟数）除 6 余数为动爻。")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedField(hour, { hour = it.filter { ch -> ch.isDigit() } }, "时(0-23)", Modifier.weight(1f))
                SmallOutlinedField(minute, { minute = it.filter { ch -> ch.isDigit() } }, "分(0-59)", Modifier.weight(1f))
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ActionButton("时分起卦") {
                val h = hour.toIntOrNull() ?: -1
                val min = minute.toIntOrNull() ?: -1
                if (h !in 0..23 || min !in 0..59) {
                    error = "时分不合法"; return@ActionButton
                }
                error = null
                val (l, m) = CastEngine.hourMinuteCast(h, min)
                vm.setCast(l, m)
                nav.navigate("result")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifetimeScreen(nav: NavHostController, vm: PaiPanViewModel) {
    val c = Calendar.getInstance()
    var useYearStem by remember { mutableStateOf(false) }
    var year by remember { mutableStateOf(c.get(Calendar.YEAR).toString()) }
    var month by remember { mutableStateOf((c.get(Calendar.MONTH) + 1).toString()) }
    var day by remember { mutableStateOf(c.get(Calendar.DAY_OF_MONTH).toString()) }
    var hour by remember { mutableStateOf(c.get(Calendar.HOUR_OF_DAY).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    CastScaffold(nav, "终身卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("（年干或年支数 + 农历月 + 农历日）除 8 余数为上卦，（上卦总数 + 时支数）除 8 余数为下卦，下卦总数除 6 余数为动爻。")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useYearStem, onCheckedChange = { useYearStem = it })
                Text("使用年干序数（否则用年支序数）")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedField(year, { year = it.filter { ch -> ch.isDigit() } }, "生年", Modifier.weight(1f))
                SmallOutlinedField(month, { month = it.filter { ch -> ch.isDigit() } }, "农历月", Modifier.weight(1f))
                SmallOutlinedField(day, { day = it.filter { ch -> ch.isDigit() } }, "农历日", Modifier.weight(1f))
                SmallOutlinedField(hour, { hour = it.filter { ch -> ch.isDigit() } }, "出生时(0-23)", Modifier.weight(1f))
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ActionButton("终身起卦") {
                val y = year.toIntOrNull() ?: 0
                val m = month.toIntOrNull() ?: 0
                val d = day.toIntOrNull() ?: 0
                val h = hour.toIntOrNull() ?: -1
                if (y <= 1900 || m !in 1..12 || d !in 1..30 || h !in 0..23) {
                    error = "请输入合法的生年、农历月日、时辰"; return@ActionButton
                }
                error = null
                try {
                    val yearIndex = if (useYearStem) {
                        TianGan.entries[(y - 4) % 10].ordinal + 1
                    } else {
                        (y - 4) % 12 + 1
                    }
                    val (l, mov) = CastEngine.lifetimeCast(
                        yearStemOrBranchIndex = yearIndex,
                        lunarMonth = m,
                        lunarDay = d,
                        hourBranch = DiZhi.forHour(h).ordinal + 1
                    )
                    vm.setCast(l, mov)
                    nav.navigate("result")
                } catch (e: Exception) {
                    error = e.message ?: "起卦失败"
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecifiedScreen(nav: NavHostController, vm: PaiPanViewModel) {
    var upper by remember { mutableIntStateOf(1) }
    var lower by remember { mutableIntStateOf(1) }
    var movingYao by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    CastScaffold(nav, "指定卦") { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("指定上卦、下卦（先天八卦数 1~8）及动爻（1~6）；选“0”表示静卦。")
            TrigramPicker("上卦", upper) { upper = it }
            TrigramPicker("下卦", lower) { lower = it }
            NumberPickerRow("动爻", 0..6, movingYao) { movingYao = it }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ActionButton("生成卦象") {
                error = null
                try {
                    val (l, m) = CastEngine.specifiedCast(upper, lower, movingYao)
                    vm.setCast(l, m)
                    nav.navigate("result")
                } catch (e: Exception) {
                    error = e.message ?: "生成失败"
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastScaffold(nav: NavHostController, title: String, content: @Composable (padding: PaddingValues) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { BackIcon(nav) }) },
        content = content
    )
}

@Composable
private fun ActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun SmallOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true
    )
}

@Composable
private fun TrigramPicker(label: String, selected: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Trigram.ALL.forEachIndexed { idx, tri ->
                val num = idx + 1
                val isSel = num == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(num) }
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$num.${tri.cnName}",
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPickerRow(label: String, range: IntRange, selected: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            range.forEach { num ->
                val isSel = num == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(num) }
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        num.toString(),
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun BackIcon(nav: NavHostController) {
    IconButton(onClick = { nav.popBackStack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
    }
}
