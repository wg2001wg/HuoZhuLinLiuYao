package com.liuyao.huozhulin.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /** 对话消息序列化分隔符：ROLE 用于 role 与 content 之间，REC 用于两条消息之间 */
    private val SEP_ROLE = "\u0001"
    private val SEP_REC = "\u0002"
    /** content 中出现分隔符时的转义占位（避免与分隔符冲突） */
    private val ESC_ROLE = "\u0003"
    private val ESC_REC = "\u0004"

    /** AI 解析状态 */
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

    /** AI 多轮对话状态：是否正在发送、对话历史（对话区内嵌于 AI 解析结果页） */
    private val _chatSending = MutableStateFlow(false)
    val chatSending: StateFlow<Boolean> = _chatSending

    private val _chatMessages = MutableStateFlow<List<WebAnalysis.Message>>(emptyList())
    val chatMessages: StateFlow<List<WebAnalysis.Message>> = _chatMessages

    /** 当前正在查看的历史记录 id；非 null 时「保存」将覆盖该记录而非新建 */
    private var currentRecordId: Long? = null

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

    /**
     * 当前设备是否具备可用的网络（Wi-Fi / 蜂窝 / 以太网）。
     * 部分手机 ROM（如 MIUI、EMUI、ColorOS）提供「联网权限」开关，
     * 即使声明了 INTERNET 权限，若该开关被关闭也会导致请求失败，需引导用户在系统设置中开启。
     */
    fun networkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * 给 AI 解析失败信息补充「网络权限」引导。
     * 当检测到设备无可用网络时，提示用户到系统设置开启本应用的联网权限。
     */
    private fun withNetworkHint(message: String): String {
        return if (networkAvailable()) {
            message
        } else {
            "$message\n\n可能是网络未连接或本应用的「联网权限」被关闭。" +
                "请前往「系统设置 → 应用 → 火珠林六爻 → 权限」中开启「网络/联网」权限后重试。"
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
        // 重新起卦时清空上一次的 AI 解析结果，避免结果页残留旧解析
        _analysisState.value = AnalysisState.Idle
        // 同时清空上一卦的多轮对话上下文
        _chatMessages.value = emptyList()
        _chatSending.value = false
        // 新起卦不属于任何历史记录，保存时应新建而非覆盖
        currentRecordId = null
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
                // 记录首轮对话上下文：将本次卦象提示作为 system，便于用户后续追问
                _chatMessages.value = listOf(
                    WebAnalysis.Message("system", prompt),
                    WebAnalysis.Message("assistant", text)
                )
            } else {
                _analysisState.value = AnalysisState.Error(
                    withNetworkHint(text ?: res.exceptionOrNull()?.message ?: "未知错误")
                )
            }
        }
    }

    /**
     * 在解析结果基础上与 AI 继续对话（多轮）。[userMsg] 为用户的新问题。
     * 采用流式输出，逐步把 AI 回复追加到对话历史中实时展示。
     */
    fun sendChat(userMsg: String) {
        val msg = userMsg.trim()
        if (msg.isEmpty() || _chatSending.value) return
        // 尚未初始化对话上下文时（如从历史记录载入），先用当前卦象提示词建立 system 背景
        val base = if (_chatMessages.value.isEmpty()) {
            val r = result.value ?: return
            val p = WebAnalysis.buildPrompt(r, guaZhu = guaZhu.value, wenShi = wenShi.value)
            listOf(WebAnalysis.Message("system", p))
        } else _chatMessages.value
        // 发送给模型的消息（system + 历史 + 本次提问）
        val messages = base + WebAnalysis.Message("user", msg)
        // 在 UI 中再追加一条空的 assistant 占位，便于流式过程中实时填充，
        // 且不会覆盖之前已有的提问与回答。
        val placeholderIdx = messages.size
        _chatMessages.value = messages + WebAnalysis.Message("assistant", "")
        _chatSending.value = true

        val key = _apiKey.value.trim().ifBlank { WebAnalysis.DEFAULT_API_KEY }
        val url = _baseUrl.value.trim().ifBlank { WebAnalysis.DEFAULT_BASE_URL }
        val mdl = _model.value.trim().ifBlank { WebAnalysis.DEFAULT_MODEL }
        viewModelScope.launch {
            val sb = StringBuilder()
            var error: String? = null
            withContext(Dispatchers.IO) {
                WebAnalysis.chatStream(
                    apiKey = key, baseUrl = url, model = mdl, messages = messages,
                    onDelta = { piece ->
                        sb.append(piece)
                        // 始终更新占位这条 assistant 消息，不触碰之前的问答
                        _chatMessages.value = _chatMessages.value.toMutableList().apply {
                            set(placeholderIdx, WebAnalysis.Message("assistant", sb.toString()))
                        }
                    },
                    onError = { error = it }
                )
            }
            _chatSending.value = false
            if (error != null) {
                // 出错时把占位替换为错误提示，仍作为一条独立消息保留在对话中
                _chatMessages.value = _chatMessages.value.toMutableList().apply {
                    set(placeholderIdx, WebAnalysis.Message("assistant", withNetworkHint(error!!)))
                }
            } else {
                _chatMessages.value = _chatMessages.value.toMutableList().apply {
                    set(placeholderIdx, WebAnalysis.Message("assistant", sb.toString()))
                }
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
        // 将用户与 AI 的继续提问对话（仅 user/assistant）序列化保存
        val aiChat = serializeChat(_chatMessages.value)
        val existingId = currentRecordId
        val existing = if (existingId != null) dao.getById(existingId) else null
        val linesStr = r.original.lines.joinToString("") { if (it.yang) "1" else "0" }
        val movingStr = r.original.lines.joinToString("") { if (it.moving) "1" else "0" }
        if (existing != null) {
            // 从历史记录进入：覆盖更新该记录（保留原备注，刷新卦象与 AI 内容）
            dao.updateRecord(
                id = existing.id,
                timestamp = existing.timestamp,
                originalName = r.original.name,
                changedName = r.changed?.name,
                linesStr = linesStr,
                movingStr = movingStr,
                dayGanCn = r.dayGan.cn,
                dayZhiCn = r.dayZhi?.cn,
                monthZhiCn = r.monthZhi?.cn,
                note = existing.note,
                aiResult = aiResult,
                aiModel = aiModel,
                aiChat = aiChat
            )
        } else {
            // 新排盘：新建记录
            val rec = RecordEntity(
                timestamp = System.currentTimeMillis(),
                originalName = r.original.name,
                changedName = r.changed?.name,
                linesStr = linesStr,
                movingStr = movingStr,
                dayGanCn = r.dayGan.cn,
                dayZhiCn = r.dayZhi?.cn,
                monthZhiCn = r.monthZhi?.cn,
                note = "",
                aiResult = aiResult,
                aiModel = aiModel,
                aiChat = aiChat
            )
            val newId = dao.insert(rec)
            // 记录新插入的记录 id，使后续保存（如修改信息后再次保存）覆盖同一条记录而非新建
            currentRecordId = newId
        }
    }

    /** 将对话历史（仅 user/assistant 消息）序列化为可存储字符串 */
    private fun serializeChat(messages: List<WebAnalysis.Message>): String? {
        val parts = messages
            .filter { it.role == "user" || it.role == "assistant" }
            .map { msg ->
                val content = escapeChatContent(msg.content)
                "${msg.role}${SEP_ROLE}${content}"
            }
        return if (parts.isEmpty()) null else parts.joinToString(SEP_REC)
    }

    /** 转义 content 中的分隔符，避免被误判为记录/字段边界（换行等普通字符不受影响） */
    private fun escapeChatContent(text: String): String =
        text.replace(SEP_ROLE, ESC_ROLE).replace(SEP_REC, ESC_REC)

    /** 反转义 */
    private fun unescapeChatContent(text: String): String =
        text.replace(ESC_ROLE, SEP_ROLE).replace(ESC_REC, SEP_REC)

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
        // 清空上一卦的多轮对话上下文
        _chatMessages.value = emptyList()
        _chatSending.value = false
        // 若该记录已保存过 AI 解析内容，直接展示历史解析结果，无需重新联网；
        // 同时把卦象提示词与已保存解析/对话写入对话历史，使「继续提问」可基于旧解析追问。
        val saved = rec.aiResult
        if (!saved.isNullOrBlank()) {
            _analysisState.value = AnalysisState.Success(
                AnalysisResult(
                    content = saved,
                    model = rec.aiModel ?: WebAnalysis.DEFAULT_MODEL,
                    isFallback = false
                )
            )
            val sysPrompt = buildAiPrompt(rec) ?: ""
            // 优先恢复已保存的用户提问对话；否则仅保留首轮解析
            val restored = restoreChat(rec.aiChat, sysPrompt, saved)
            _chatMessages.value = restored
        } else {
            // 无历史解析则回到初始态，避免残留旧解析
            _analysisState.value = AnalysisState.Idle
        }
        // 标记当前查看的是该历史记录，保存时将覆盖而非新建
        currentRecordId = rec.id
    }

    /** 由已保存的对话字符串恢复对话历史：system 提示 + 首轮解析 + 用户/AI 问答 */
    private fun restoreChat(aiChat: String?, sysPrompt: String, aiResult: String): List<WebAnalysis.Message> {
        val list = mutableListOf(WebAnalysis.Message("system", sysPrompt))
        if (!aiChat.isNullOrBlank()) {
            val restored = mutableListOf<WebAnalysis.Message>()
            // 按记录分隔符 SEP_REC 切分每条消息，再按 SEP_ROLE 切分 role/content，
            // 内容中的换行符不会被误拆。兼容旧数据：旧格式用 \n 分隔。
            val records = if (aiChat.contains(SEP_REC)) {
                aiChat.split(SEP_REC)
            } else {
                aiChat.lineSequence().toList()
            }
            records.forEach { line ->
                val sep = line.indexOf(SEP_ROLE)
                if (sep > 0) {
                    val role = line.substring(0, sep)
                    val content = unescapeChatContent(line.substring(sep + SEP_ROLE.length))
                    if (role == "user" || role == "assistant") {
                        restored.add(WebAnalysis.Message(role, content))
                    }
                }
            }
            // 兼容旧数据：若保存的多轮对话未包含首轮解析，则补回首轮 assistant。
            // UI 会在展示时跳过这条紧跟 system 的首轮 assistant，避免与上方 AI 解析面板重复。
            if (restored.firstOrNull()?.role != "assistant" && aiResult.isNotBlank()) {
                list.add(WebAnalysis.Message("assistant", aiResult))
            }
            list.addAll(restored)
        } else {
            list.add(WebAnalysis.Message("assistant", aiResult))
        }
        return list
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
            var interruptedMsg: String? = null
            val err = kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    WebAnalysis.analyzeStream(
                        key, prompt, url, mdl,
                        onDelta = { piece ->
                            sb.append(piece)
                            // 首次收到增量即从 Loading 切到 Streaming，后续持续追加
                            _analysisState.value = AnalysisState.Streaming(sb.toString())
                        },
                        onError = { msg ->
                            // 记录中断信息；若已有部分内容则保留已生成部分并标注中断
                            if (sb.isNotEmpty()) {
                                interruptedMsg = msg
                            }
                        }
                    )
                }
            }.exceptionOrNull()
            if (err != null) {
                if (sb.isEmpty()) {
                    _analysisState.value = AnalysisState.Error(withNetworkHint(err.message ?: "未知错误"))
                } else {
                    finishStream(sb.toString(), mdl, _analysisState, prompt)
                }
                return@launch
            }
            if (interruptedMsg != null) {
                // 流式输出中途出错但已有部分内容：保留已生成部分并标注中断
                val finalText = sb.toString() + "\n\n（解析中断：$interruptedMsg）"
                finishStream(finalText, mdl, _analysisState, prompt)
            } else if (sb.isEmpty()) {
                _analysisState.value = AnalysisState.Error(withNetworkHint("模型未返回内容。"))
            } else {
                finishStream(sb.toString(), mdl, _analysisState, prompt)
            }
        }
    }

    /**
     * 流式解析完成（含正常完成与「已有部分内容但中途中断」）的统一收尾：
     * 写入 [AnalysisState.Success] 并把首轮 system + assistant 写入对话历史，
     * 使后续「继续向 AI 提问」能基于完整首轮解析追问，且保存/恢复时首轮正文不丢失。
     */
    private fun finishStream(
        content: String,
        model: String,
        state: MutableStateFlow<AnalysisState>,
        prompt: String
    ) {
        state.value = AnalysisState.Success(AnalysisResult(content = content, model = model))
        // 仅在尚无对话上下文时初始化首轮（system 提示 + assistant 首轮解析），
        // 避免覆盖用户已在「继续提问」中产生的多轮对话。
        if (_chatMessages.value.isEmpty()) {
            _chatMessages.value = listOf(
                WebAnalysis.Message("system", prompt),
                WebAnalysis.Message("assistant", content)
            )
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
            dao.updateAi(rec.id, rec.aiResult, rec.aiModel ?: "", rec.aiChat)
        }
    }
}
