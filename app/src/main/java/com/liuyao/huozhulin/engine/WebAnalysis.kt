package com.liuyao.huozhulin.engine

import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.ShiYingType
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AI 解析：调用大模型联网对卦象进行智能解读，替代原有的本地「系统解析」。
 *
 * 默认使用智谱 GLM-4.7-Flash（免费），接口与 OpenAI Chat Completions 格式兼容：
 *   POST https://open.bigmodel.cn/api/paas/v4/chat/completions
 *   Authorization: Bearer <API_KEY>
 *
 * 用户可在设置页自定义 API Key、接口地址与模型名，以对接任意兼容 OpenAI 格式的模型。
 */
object WebAnalysis {

    /** 默认接口地址（智谱开放平台，OpenAI 兼容格式） */
    const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    /** 默认模型名 */
    const val DEFAULT_MODEL = "GLM-4.7-Flash"

    /** 默认 API Key（用户未配置时使用，可在设置页覆盖） */
    const val DEFAULT_API_KEY = "c8911f7e1e064cada93094a1b89fed80.PB2X8egYZFbiF35b"

    /** AI 解析结果包装 */
    data class AnalysisResult(
        val content: String,        // 模型返回的正文
        val model: String = DEFAULT_MODEL,
        val isFallback: Boolean = false  // 是否为本地兜底（无 Key / 失败时）
    )

    /**
     * 将排盘结果整理成给大模型的提示文本。
     * @param result  排盘结果
     * @param guaZhu  卦主（可空，非空时一并告知模型）
     * @param wenShi  问事（可空，非空且非「未填写」时一并告知模型，使解读更具针对性）
     */
    fun buildPrompt(result: PaiPanResult, guaZhu: String? = null, wenShi: String? = null): String {
        val sb = StringBuilder()
        sb.append("我是一位研习火珠林六爻（纳甲筮法）的爱好者，请基于传统六爻理论，对我的起卦结果做专业、客观的分析与建议。\n\n")
        sb.append("【排盘信息】\n")
        sb.append("本卦：${result.original.name}，属${result.original.hexagram.palace.cnName}宫（${result.original.hexagram.palaceElement.cn}）\n")
        if (result.hasChanged) sb.append("变卦：${result.changed!!.name}\n")
        sb.append("伏神卦（本宫）：${result.fu.name}\n")
        val shiIdx = result.original.lines.indexOfFirst { it.shiYing == ShiYingType.SHI }
        val yingIdx = result.original.lines.indexOfFirst { it.shiYing == ShiYingType.YING }
        if (shiIdx >= 0) sb.append("世爻：第${shiIdx + 1}爻\n")
        if (yingIdx >= 0) sb.append("应爻：第${yingIdx + 1}爻\n")
        val dongLines = result.original.lines.filter { it.moving }
        if (dongLines.isEmpty()) {
            sb.append("动爻：无（静卦）\n")
        } else {
            sb.append("动爻：${dongLines.joinToString("、") { "第${it.position + 1}爻（${it.positionName}）" }}\n")
        }
        if (result.dayZhi != null) sb.append("日辰：干${result.dayGan}支${result.dayZhi.name}\n")
        if (result.monthZhi != null) sb.append("月建：${result.monthZhi.name}月\n")
        if (result.kongWang.isNotEmpty()) sb.append("旬空：${result.kongWang.joinToString("、") { it.name }}\n")

        val gz = guaZhu?.trim()?.takeIf { it.isNotBlank() }
        val ws = wenShi?.trim()?.takeIf { it.isNotBlank() && it != "未填写" }
        if (gz != null || ws != null) {
            sb.append("\n【所问之事】\n")
            if (gz != null) sb.append("卦主：$gz\n")
            if (ws != null) sb.append("问事：$ws\n")
        }

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
        if (ws != null) {
            sb.append("\n本次所问之事为「$ws」，请重点围绕此事，结合世应、用神、六亲、六神、动变、日辰月建与旬空，分析其吉凶趋势，并给出具体建议。")
        } else {
            sb.append("\n请结合世应、用神、六亲、六神、动变、日辰月建与旬空，分析此事吉凶趋势，并给出建议。")
        }
        sb.append("\n请按以下结构作答：一、卦象概述（卦宫、世应、动变）；二、用神与六亲分析；三、六神与神煞参考；四、吉凶断语；五、具体建议。")
        sb.append("回答请使用简体中文，条理清晰、通俗易懂。")
        return sb.toString()
    }

    /**
     * 联网请求大模型进行 AI 解析。
     * @param apiKey    API Key（从设置页保存的 DataStore 读取，为空时由调用方回落到 [DEFAULT_API_KEY]）
     * @param prompt    提示文本（可由 [buildPrompt] 生成）
     * @param baseUrl   可自定义接口地址（兼容 OpenAI 格式），默认智谱开放平台
     * @param model     可自定义模型名，默认 [DEFAULT_MODEL]
     */
    fun analyze(apiKey: String, prompt: String, baseUrl: String = DEFAULT_BASE_URL, model: String = DEFAULT_MODEL): String {
        val url = URL(baseUrl.trim().ifBlank { DEFAULT_BASE_URL })
        val useModel = model.trim().ifBlank { DEFAULT_MODEL }
        val useKey = apiKey.trim().ifBlank { DEFAULT_API_KEY }
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $useKey")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000

            val body = JSONObject().apply {
                put("model", useModel)
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
            "AI解析出错：${e.message}"
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
