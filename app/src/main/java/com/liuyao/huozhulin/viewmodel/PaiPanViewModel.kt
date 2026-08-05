package com.liuyao.huozhulin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liuyao.huozhulin.data.local.AppDatabase
import com.liuyao.huozhulin.data.local.RecordEntity
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

    /** 联网解析状态 */
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState

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
    fun fetchAnalysis() {
        val r = result.value ?: return
        if (_analysisState.value is AnalysisState.Loading) return
        _analysisState.value = AnalysisState.Loading
        viewModelScope.launch {
            val res = kotlin.runCatching {
                withContext(Dispatchers.IO) { WebAnalysis.analyze(r) }
            }
            if (res.isSuccess) {
                _analysisState.value = AnalysisState.Success(res.getOrThrow())
            } else {
                _analysisState.value = AnalysisState.Error(
                    res.exceptionOrNull()?.message ?: "未知错误"
                )
            }
        }
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

    fun delete(rec: RecordEntity) = viewModelScope.launch { dao.delete(rec) }
}
