package com.liuyao.huozhulin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用 DataStore 持久化用户设置（DeepSeek API Key 及可选的接口地址）。
 *
 * 使用方式：在 Composable / ViewModel 中通过 context.dataStore 访问，
 * 调用 [apiKeyFlow] / [baseUrlFlow] 读取，[saveApiKey] / [saveBaseUrl] 保存。
 */
private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsDataStore {

    private val API_KEY = stringPreferencesKey("deepseek_api_key")
    private val BASE_URL = stringPreferencesKey("deepseek_base_url")

    /** DeepSeek API Key */
    fun apiKeyFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[API_KEY] ?: "" }

    /** 自定义接口地址（兼容 OpenAI 格式），为空时使用 DeepSeek 官方默认地址 */
    fun baseUrlFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[BASE_URL] ?: "" }

    suspend fun saveApiKey(context: Context, key: String) {
        context.dataStore.edit { it[API_KEY] = key.trim() }
    }

    suspend fun saveBaseUrl(context: Context, url: String) {
        context.dataStore.edit { it[BASE_URL] = url.trim() }
    }
}
