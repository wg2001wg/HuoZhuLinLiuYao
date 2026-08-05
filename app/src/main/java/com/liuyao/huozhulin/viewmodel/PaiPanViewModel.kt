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

/** AI 解析的状态机 */
sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Loading : AnalysisState
    /** 流式生成中：content 为已接收到的增量累积文本 */
    data class Streaming(val content: String) : AnalysisState
    data class Success(val result: AnalysisResult) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class PaiPanViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private val appContext = app.applicationContext

    /** AI 解析状态 */
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    /** AI 模型 API Key（从 DataStore 读取，用户可在设置页配置），为空则用内置默认 Key */
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    /** 自定义接口地址（兼容 OpenAI 格式），为空则用默认地址 */
    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl

    /** 自定义模型名，为空则用默认模型 GLM-4.7-Flash */
    private val _model = MutableStateFlow("")
    val model: StateFlow<String> = _model

    init {
        viewModelScope.launch {
            _baseUrl.value = SettingsDataStore.baseUrlFlow(appContext).first()
            _model.value = SettingsDataStore.modelFlow(appContext).first()
            // 按当前模型加载对应的 API Key（每种模型各自保存）
            _apiKey.value = SettingsDataStore.keyForModelFlow(appContext, _model.value).first()
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
     * 由 UI（结果页「AI解析」面板 / 历史页按钮）触发联网 AI 解析。
     * 默认调用 GLM-4.7-Flash，用户可在设置页自定义 API Key、接口地址与模型名。
     * 仅当排盘结果已就绪时执行；不阻塞本地排盘主流程。
     */
    fun fetchAnalysis() {
        val r = result.value ?: return
        if (_analysisState.value is AnalysisState.Loading) return
        // 未配置时使用内置默认值
        val key = _apiKey.value.trim().ifBlank { WebAnalysis.DEFAULT_API_KEY }
        val url = _baseUrl.value.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
        val mdl = _model.value.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
        // 组装提示词：将卦主与问事一并发送给 AI（问事为空/未填写则不发送）
        val prompt = WebAnalysis.buildPrompt(
            r,
            guaZhu = guaZhu.value,
            wenShi = wenShi.value
        )
        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            val res = kotlin.runCatching {
                withContext(Dispatchers.IO) { WebAnalysis.analyze(key, prompt, url, mdl) }
            }
            val text = res.getOrNull()
            if (res.isSuccess && text != null &&
                !text.startsWith("请求失败") && !text.startsWith("AI解析出错")
            ) {
                _analysisState.value = AnalysisState.Success(
                    AnalysisResult(content = text, model = mdl)
                )
            } else {
                _analysisState.value = AnalysisState.Error(
                    text ?: res.exceptionOrNull()?.message ?: "未知错误"
                )
            }
        }
    }

    /** 保存某模型的 API Key（按模型分别保存，同时写入内存 StateFlow） */
    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        _apiKey.value = trimmed
        viewModelScope.launch { SettingsDataStore.saveKeyForModel(appContext, _model.value, trimmed) }
    }

    /** 读取某模型已保存的 API Key 流（供设置页切换模型时自动填充） */
    fun keyForModelFlow(model: String): Flow<String> =
        SettingsDataStore.keyForModelFlow(appContext, model)

    /** 用户已添加的自定义模型列表（模型名 + 接口地址），供设置页下拉复用 */
    fun savedModelsFlow(): Flow<List<Pair<String, String>>> =
        SettingsDataStore.savedModelsFlow(appContext)

    /** 将用户新输入的模型名保存到本地列表，下次设置时可直接选择 */
    fun addCustomModel(model: String, baseUrl: String) {
        viewModelScope.launch { SettingsDataStore.addCustomModel(appContext, model, baseUrl) }
    }

    /** 保存自定义接口地址 */
    fun saveBaseUrl(url: String) {
        val trimmed = url.trim()
        _baseUrl.value = trimmed
        viewModelScope.launch { SettingsDataStore.saveBaseUrl(appContext, trimmed) }
    }

    /** 保存自定义模型名 */
    fun saveModel(model: String) {
        val trimmed = model.trim()
        _model.value = trimmed
        viewModelScope.launch { SettingsDataStore.saveModel(appContext, trimmed) }
    }

    fun saveCurrent(): kotlinx.coroutines.Job = viewModelScope.launch {
        val r = result.value ?: return@launch
        // 若当前已有 AI 解析结果，则一并保存到记录中，供下次直接查看
        val ai = _analysisState.value
        val aiResult = if (ai is AnalysisState.Success) ai.result.content else null
        val aiModel = if (ai is AnalysisState.Success) ai.result.model else null
        val rec = RecordEntity(
            timestamp = System.currentTimeMillis(),
            originalName = r.original.name,
            changedName = r.changed?.name,
            linesStr = r.original.lines.joinToString("") { if (it.yang) "1" else "0" },
            movingStr = r.original.lines.joinToString("") { if (it.moving) "1" else "0" },
            dayGanCn = r.dayGan.cn,
            dayZhiCn = r.dayZhi?.cn,
            monthZhiCn = r.monthZhi?.cn,
            note = "",
            aiResult = aiResult,
            aiModel = aiModel
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
     * 流式（逐字/逐段）发起 AI 解析：模型每生成一段，UI 即时显示，避免等待整段完成。
     * 与 [fetchAnalysis] 互斥，仅在非进行中时触发。
     */
    fun fetchAnalysisStream() {
        val r = result.value ?: return
        if (_analysisState.value is AnalysisState.Loading ||
            _analysisState.value is AnalysisState.Streaming
        ) return
        val key = _apiKey.value.trim().ifBlank { WebAnalysis.DEFAULT_API_KEY }
        val url = _baseUrl.value.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
        val mdl = _model.value.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
        val prompt = WebAnalysis.buildPrompt(r, guaZhu = guaZhu.value, wenShi = wenShi.value)

        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            val sb = StringBuilder()
            val err = kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    WebAnalysis.analyzeStream(
                        key, prompt, url, mdl,
                        onDelta = { piece ->
                            sb.append(piece)
                            // 首次收到增量即从 Loading 切到 Streaming，后续持续追加
                            if (_analysisState.value !is AnalysisState.Streaming) {
                                _analysisState.value = AnalysisState.Streaming(sb.toString())
                            } else {
                                _analysisState.value = AnalysisState.Streaming(sb.toString())
                            }
                        },
                        onError = { msg ->
                            if (sb.isEmpty()) {
                                _analysisState.value = AnalysisState.Error(msg)
                            } else {
                                // 已有部分内容则保留已生成部分，并标注中断
                                _analysisState.value = AnalysisState.Success(
                                    AnalysisResult(
                                        content = sb.toString() + "\n\n（解析中断：$msg）",
                                        model = mdl
                                    )
                                )
                            }
                        }
                    )
                }
            }.exceptionOrNull()
            if (err != null) {
                if (sb.isEmpty()) {
                    _analysisState.value = AnalysisState.Error(err.message ?: "未知错误")
                } else {
                    _analysisState.value = AnalysisState.Success(
                        AnalysisResult(content = sb.toString(), model = mdl)
                    )
                }
                return@launch
            }
            // 正常完成（非 onError 中断）：有内容则成功，无内容则报错
            if (sb.isEmpty()) {
                _analysisState.value = AnalysisState.Error("模型未返回内容。")
            } else {
                _analysisState.value = AnalysisState.Success(
                    AnalysisResult(content = sb.toString(), model = mdl)
                )
            }
        }
    }

    /**
     * 由历史记录重建排盘并生成给 AI 模型的提示文本。
     * 用于历史页的「AI解析」，无需跳转结果页。
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

    /** 将重新生成的 AI 解析结果写回该历史记录 */
    fun refreshRecordAi(rec: RecordEntity) {
        if (rec.aiResult == null) return
        viewModelScope.launch {
            dao.updateAi(rec.id, rec.aiResult, rec.aiModel ?: "")
        }
    }
}
