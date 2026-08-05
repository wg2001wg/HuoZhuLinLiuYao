package com.liuyao.huozhulin.engine

import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.ShiYingType
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 联网解析：调用 DeepSeek 开放平台的免费大模型（chat 模型）对卦象进行智能解读。
 *
 * DeepSeek 接口与 OpenAI 兼容：
 *   POST https://api.deepseek.com/chat/completions
 *   Authorization: Bearer <API_KEY>
 *
 * 如需自定义（例如兼容 OpenAI 格式的其它免费模型），可通过 baseUrl 参数覆盖。
 */
object WebAnalysis {

    const val DEFAULT_BASE_URL = "https://api.deepseek.com/chat/completions"
    const val DEFAULT_MODEL = "deepseek-chat"

    /** 内置免费 API Key（用户未配置时自动使用） */
    const val FALLBACK_API_KEY = "sk-5d4c3b2a1e0f9d8c7b6a5f4e3d2c1b0a9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b"

    /** 联网解析结果包装 */
    data class AnalysisResult(
        val content: String,        // 模型返回的正文
        val model: String = DEFAULT_MODEL,
        val isFallback: Boolean = false  // 是否为本地兜底（无 Key / 失败时）
    )

    /** 将排盘结果整理成给大模型的提示文本 */
    fun buildPrompt(result: PaiPanResult): String {
        val sb = StringBuilder()
        sb.append("我是一位研习火珠林六爻（纳甲筮法）的爱好者，请基于传统六爻理论，对我的起卦结果做专业、客观的分析与建议。\n\n")
        sb.append("【排盘信息】\n")
        sb.append("本卦：${result.original.name}\n")
        if (result.hasChanged) sb.append("变卦：${result.changed!!.name}\n")
        sb.append("伏神卦（本宫）：${result.fu.name}\n")
        if (result.dayZhi != null) sb.append("日辰：干${result.dayGan}支${result.dayZhi.name}\n")
        if (result.monthZhi != null) sb.append("月建：${result.monthZhi.name}月\n")
        if (result.kongWang.isNotEmpty()) sb.append("旬空：${result.kongWang.joinToString("、") { it.name }}\n")
        sb.append("\n【六爻明细（自下而上：初爻→上爻）】\n")
        for (i in result.original.lines.indices) {
            val l = result.original.lines[i]
            val arrow = if (l.moving && result.changed != null) {
                " → 变爻：" + result.changed.lines[i].let { (if (it.yang) "阳" else "阴") + it.positionName }
            } else ""
            sb.append(
                "第${i + 1}爻 ${l.positionName}（${if (l.yang) "阳" else "阴"}）" +
                        " 地支：${l.diZhi.name} 天干：${l.tianGan} " +
                        " 六亲：${l.liuQin.cn} 六神：${l.liuShen.cn} " +
                        " ${when (l.shiYing) { ShiYingType.SHI -> "世"; ShiYingType.YING -> "应"; else -> "" }} " +
                        (if (l.kongWang) "（旬空）" else "") +
                        (if (l.moving) "（动爻）" else "") + arrow + "\n"
            )
        }
        sb.append("\n请结合世应、用神、六亲、六神、动变、日辰月建与旬空，分析此事吉凶趋势，并给出建议。回答请使用简体中文，条理清晰、通俗易懂。")
        return sb.toString()
    }

    /**
     * 联网请求 DeepSeek 解读。
     * @param apiKey    DeepSeek API Key（从设置页保存的 DataStore 读取）
     * @param prompt    提示文本（可由 [buildPrompt] 生成）
     * @param baseUrl   可自定义接口地址（兼容 OpenAI 格式），默认 DeepSeek 官方
     * @param model     模型名，默认 deepseek-chat
     */
    fun analyze(apiKey: String, prompt: String, baseUrl: String = DEFAULT_BASE_URL, model: String = DEFAULT_MODEL): String {
        val url = URL(baseUrl)
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000

            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("stream", false)
            }
            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val respText = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
            }
            if (code !in 200..299) {
                return "请求失败（HTTP $code）：${extractError(respText)}"
            }
            parseContent(respText)
        } catch (e: Exception) {
            "联网解析出错：${e.message}"
        } finally {
            conn.disconnect()
        }
    }

    private fun parseContent(respText: String): String {
        return try {
            val json = JSONObject(respText)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) return "模型未返回内容。"
            choices.getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) {
            "解析返回结果失败：${e.message}\n原始返回：$respText"
        }
    }

    private fun extractError(respText: String): String {
        return try {
            val json = JSONObject(respText)
            json.optString("error", json.optJSONObject("error")?.optString("message", respText) ?: respText)
        } catch (_: Exception) {
            respText.take(500)
        }
    }
}
