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
 */
private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsDataStore {

    private val API_KEY = stringPreferencesKey("ai_api_key")
    private val BASE_URL = stringPreferencesKey("ai_base_url")
    private val MODEL = stringPreferencesKey("ai_model")

    /** AI 模型 API Key，为空时使用内置默认 Key */
    fun apiKeyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[API_KEY] ?: "" }

    /** 自定义接口地址（兼容 OpenAI 格式），为空时使用默认地址 */
    fun baseUrlFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[BASE_URL] ?: "" }

    /** 自定义模型名，为空时使用默认模型 GLM-4.7-Flash */
    fun modelFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[MODEL] ?: "" }

    suspend fun saveApiKey(context: Context, key: String) {
        context.dataStore.edit { it[API_KEY] = key.trim() }
    }

    suspend fun saveBaseUrl(context: Context, url: String) {
        context.dataStore.edit { it[BASE_URL] = url.trim() }
    }

    suspend fun saveModel(context: Context, model: String) {
        context.dataStore.edit { it[MODEL] = model.trim() }
    }
}
