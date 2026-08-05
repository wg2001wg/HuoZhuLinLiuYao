package com.liuyao.huozhulin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用 DataStore 持久化 AI 解析的相关设置（API Key、接口地址、模型名）。
 *
 * 三项均可留空，留空时由 WebAnalysis 使用内置默认值
 * （默认模型 GLM-4.7-Flash、默认智谱开放平台地址与内置免费 Key）。
 *
 * API Key 按模型分别保存：每种模型各自记录 Key，切换模型时自动填充回对应 Key。
 * 这些 Key 统一存放在 [KEYS]（以 "model\u0001key" 逐条、用 "\n" 分隔的映射字符串）中。
 */
private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsDataStore {

    private val KEYS = stringPreferencesKey("ai_keys")
    private val BASE_URL = stringPreferencesKey("ai_base_url")
    private val MODEL = stringPreferencesKey("ai_model")
    private val MODELS = stringPreferencesKey("ai_models")

    /**
     * 读取某模型已保存的 API Key（按模型分别保存）。
     * @param model 模型名（trim 后）；为空（表示用默认模型）时返回空字符串。
     */
    fun keyForModelFlow(context: Context, model: String): Flow<String> {
        val m = model.trim()
        return context.dataStore.data.map { prefs ->
            if (m.isEmpty()) {
                ""
            } else {
                val map = parseKeys(prefs[KEYS] ?: "")
                map[m] ?: ""
            }
        }
    }

    /** 自定义接口地址（兼容 OpenAI 格式），为空时使用默认地址 */
    fun baseUrlFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[BASE_URL] ?: "" }

    /** 自定义模型名，为空时使用默认模型 GLM-4.7-Flash */
    fun modelFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[MODEL] ?: "" }

    /**
     * 用户添加的自定义模型列表（下拉框可复用）。
     * 以 "model\u0001baseUrl" 逐条、用 "\n" 分隔的映射字符串存储。
     */
    fun savedModelsFlow(context: Context): Flow<List<Pair<String, String>>> =
        context.dataStore.data.map { prefs -> parseModels(prefs[MODELS] ?: "") }

    /**
     * 保存一个自定义模型（若该模型名已存在则更新其接口地址）。
     * @param model 模型名（trim 后，非空）
     * @param baseUrl 该模型对应的接口地址（可空/空）
     */
    suspend fun addCustomModel(context: Context, model: String, baseUrl: String) {
        val m = model.trim()
        if (m.isEmpty()) return
        val url = baseUrl.trim()
        context.dataStore.edit { prefs ->
            val list = parseModels(prefs[MODELS] ?: "").toMutableList()
            val idx = list.indexOfFirst { it.first == m }
            if (idx >= 0) list[idx] = m to url else list.add(m to url)
            prefs[MODELS] = list.joinToString("\n") { "${it.first}\u0001${it.second}" }
        }
    }

    /**
     * 保存某模型的 API Key（按模型分别保存）。
     * 若 key 为空则删除该模型的记录（恢复使用内置默认 Key）。
     */
    suspend fun saveKeyForModel(context: Context, model: String, key: String) {
        val m = model.trim()
        val k = key.trim()
        context.dataStore.edit { prefs ->
            val map = parseKeys(prefs[KEYS] ?: "").toMutableMap()
            if (k.isEmpty()) {
                map.remove(m)
            } else {
                if (m.isBlank()) return@edit // 默认模型不单独保存，沿用内置 Key
                map[m] = k
            }
            prefs[KEYS] = map.entries.joinToString("\n") { "${it.key}\u0001${it.value}" }
        }
    }

    suspend fun saveBaseUrl(context: Context, url: String) {
        context.dataStore.edit { it[BASE_URL] = url.trim() }
    }

    suspend fun saveModel(context: Context, model: String) {
        context.dataStore.edit { it[MODEL] = model.trim() }
    }

    /** 将 "model\u0001key\nmodel\u0001key" 解析为 Map */
    private fun parseKeys(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.lines()
            .mapNotNull { line ->
                val idx = line.indexOf('\u0001')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .toMap()
    }

    /** 将 "model\u0001baseUrl\nmodel\u0001baseUrl" 解析为列表 */
    private fun parseModels(raw: String): List<Pair<String, String>> {
        if (raw.isBlank()) return emptyList()
        return raw.lines()
            .mapNotNull { line ->
                val idx = line.indexOf('\u0001')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
    }
}
