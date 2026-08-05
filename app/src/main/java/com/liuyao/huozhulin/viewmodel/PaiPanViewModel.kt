package com.liuyao.huozhulin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liuyao.huozhulin.data.local.AppDatabase
import com.liuyao.huozhulin.data.local.RecordEntity
import com.liuyao.huozhulin.data.local.SettingsDataStore
import com.liuyao.huozhulin.data.model.DiZhi
import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.TianGan
import com.liuyao.huozhulin.engine.GanZhiCalendar
import com.liuyao.huozhulin.engine.FourPillars
import com.liuyao.huozhulin.engine.PaiPanEngine
import com.liuyao.huozhulin.engine.WebAnalysis
import com.liuyao.huozhulin.engine.WebAnalysis.AnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 联网解析的状态机 */
sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Loading : AnalysisState
    data class Success(val result: AnalysisResult) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class PaiPanViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private val appContext = app.applicationContext

    /** 联网解析状态 */
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    /** DeepSeek API Key（从 DataStore 读取，用户可在设置页配置） */
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    /** 自定义接口地址（兼容 OpenAI 格式），为空则用 DeepSeek 官方默认 */
    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl

    init {
        viewModelScope.launch {
            _apiKey.value = SettingsDataStore.apiKeyFlow(appContext).first()
            _baseUrl.value = SettingsDataStore.baseUrlFlow(appContext).first()
        }
    }

    private val _lines = MutableStateFlow<List<Boolean>?>(null)
    private val _moving = MutableStateFlow<List<Boolean>?>(null)

    val dayGan = MutableStateFlow(TianGan.JIA)
    val dayZhi = MutableStateFlow<DiZhi?>(DiZhi.ZI)
    val monthZhi = MutableStateFlow<DiZhi?>(null)

    /** 起卦时刻的四柱（年/月/日/时 干支），用于结果页展示 */
    val fourPillars = MutableStateFlow<FourPillars?>(null)

    /** 起卦时刻的公历时间戳（毫秒），用于结果页数字日期显示 */
    val castTime = MutableStateFlow<Long?>(null)

    /** 卦主、问事（可在结果页编辑） */
    val guaZhu = MutableStateFlow("男")
    val wenShi = MutableStateFlow("未填写")

    val result: StateFlow<PaiPanResult?> =
        combine(_lines, _moving, dayGan, dayZhi, monthZhi) { ls, mv, g, z, m ->
            if (ls != null && mv != null) {
                PaiPanEngine.layout(ls, mv, g, z, m)
            } else null
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    fun setCast(lines: List<Boolean>, moving: List<Boolean>) {
        _lines.value = lines
        _moving.value = moving
        // 以当前时刻推算四柱，并默认用真实日辰/月建作为排盘基准
        val fp = GanZhiCalendar.fourPillarsNow()
        fourPillars.value = fp
        castTime.value = System.currentTimeMillis()
        dayGan.value = fp.day.gan
        dayZhi.value = fp.day.zhi
        monthZhi.value = fp.month.zhi
    }

    /**
     * 由 UI（系统解析面板中的按钮）触发联网解析。
     * 仅当排盘结果已就绪时执行；网络异常时降级为本地兜底，不阻塞主流程。
     */
    /**
     * 由 UI（结果页/历史页的「AI 解读」按钮）触发联网解析。
     * 调用 DeepSeek 免费大模型对卦象进行解读。
     * 用户未配置 API Key 时，自动使用内置的免费默认 Key。
     */
    fun fetchAnalysis() {
        val r = result.value ?: return
        if (_analysisState.value is AnalysisState.Loading) return
        val key = _apiKey.value.trim()
        if (key.isBlank()) {
            // 未配置 Key → 使用内置免费 Key 兜底
            _analysisState.value = AnalysisState.Loading
            val url = _baseUrl.value.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
            val prompt = WebAnalysis.buildPrompt(r)
            viewModelScope.launch {
                val res = kotlin.runCatching {
                    withContext(Dispatchers.IO) { WebAnalysis.analyze(WebAnalysis.FALLBACK_API_KEY, prompt, url) }
                }
                if (res.isSuccess) {
                    _analysisState.value = AnalysisState.Success(
                        AnalysisResult(content = res.getOrThrow(), model = "${WebAnalysis.DEFAULT_MODEL}（内置免费）")
                    )
                } else {
                    _analysisState.value = AnalysisState.Error(
                        res.exceptionOrNull()?.message ?: "未知错误"
                    )
                }
            }
            return
        }
        _analysisState.value = AnalysisState.Loading
        val url = _baseUrl.value.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
        val prompt = WebAnalysis.buildPrompt(r)
        viewModelScope.launch {
            val res = kotlin.runCatching {
                withContext(Dispatchers.IO) { WebAnalysis.analyze(key, prompt, url) }
            }
            if (res.isSuccess) {
                _analysisState.value = AnalysisState.Success(
                    AnalysisResult(content = res.getOrThrow(), model = WebAnalysis.DEFAULT_MODEL)
                )
            } else {
                _analysisState.value = AnalysisState.Error(
                    res.exceptionOrNull()?.message ?: "未知错误"
                )
            }
        }
    }

    /** 保存 DeepSeek API Key（同时写入 DataStore 与内存 StateFlow） */
    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        _apiKey.value = trimmed
        viewModelScope.launch { SettingsDataStore.saveApiKey(appContext, trimmed) }
    }

    /** 保存自定义接口地址 */
    fun saveBaseUrl(url: String) {
        val trimmed = url.trim()
        _baseUrl.value = trimmed
        viewModelScope.launch { SettingsDataStore.saveBaseUrl(appContext, trimmed) }
    }

    fun saveCurrent(): kotlinx.coroutines.Job = viewModelScope.launch {
        val r = result.value ?: return@launch
        val rec = RecordEntity(
            timestamp = System.currentTimeMillis(),
            originalName = r.original.name,
            changedName = r.changed?.name,
            linesStr = r.original.lines.joinToString("") { if (it.yang) "1" else "0" },
            movingStr = r.original.lines.joinToString("") { if (it.moving) "1" else "0" },
            dayGanCn = r.dayGan.cn,
            dayZhiCn = r.dayZhi?.cn,
            monthZhiCn = r.monthZhi?.cn,
            note = ""
        )
        dao.insert(rec)
    }

    fun historyFlow(): Flow<List<RecordEntity>> = dao.getAllFlow()

    fun loadFromRecord(rec: RecordEntity) {
        val ls = rec.linesStr.map { it == '1' }
        val mv = rec.movingStr.map { it == '1' }
        _lines.value = ls
        _moving.value = mv
        fourPillars.value = GanZhiCalendar.fromTimestamp(rec.timestamp)
        castTime.value = rec.timestamp
        dayGan.value = TianGan.entries.first { it.cn == rec.dayGanCn }
        dayZhi.value = rec.dayZhiCn?.let { cn -> DiZhi.entries.firstOrNull { it.cn == cn } }
        monthZhi.value = rec.monthZhiCn?.let { cn -> DiZhi.entries.firstOrNull { it.cn == cn } }
    }

    /**
     * 由历史记录重建排盘并生成给 DeepSeek 的提示文本。
     * 用于历史页的「AI 解读」，无需跳转结果页。
     * @return 提示文本；若记录数据不完整无法重建则返回 null
     */
    fun buildAiPrompt(rec: RecordEntity): String? {
        val ls = rec.linesStr.map { it == '1' }
        val mv = rec.movingStr.map { it == '1' }
        if (ls.size != 6) return null
        val g = TianGan.entries.firstOrNull { it.cn == rec.dayGanCn } ?: return null
        val z = rec.dayZhiCn?.let { cn -> DiZhi.entries.firstOrNull { it.cn == cn } }
        val m = rec.monthZhiCn?.let { cn -> DiZhi.entries.firstOrNull { it.cn == cn } }
        val result = PaiPanEngine.layout(ls, mv, g, z, m)
        return WebAnalysis.buildPrompt(result)
    }

    fun delete(rec: RecordEntity) = viewModelScope.launch { dao.delete(rec) }
}
