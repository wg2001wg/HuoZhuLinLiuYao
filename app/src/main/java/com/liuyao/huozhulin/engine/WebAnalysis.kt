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

    /** 预设模型选项：显示名 + 实际模型名 + 对应接口地址 */
    data class ModelOption(val display: String, val model: String, val baseUrl: String)

    /** 可用模型列表：选择后自动填充对应的兼容 OpenAI 格式的接口地址 */
    val MODEL_OPTIONS: List<ModelOption> = listOf(
        ModelOption("智谱 GLM-4.7-Flash", "GLM-4.7-Flash", "https://open.bigmodel.cn/api/paas/v4/"),
        ModelOption("智谱 GLM-4.6", "GLM-4.6", "https://open.bigmodel.cn/api/paas/v4/"),
        ModelOption("智谱 GLM-4-Plus", "glm-4-plus", "https://open.bigmodel.cn/api/paas/v4/"),
        ModelOption("OpenAI GPT-4o", "gpt-4o", "https://api.openai.com/v1/"),
        ModelOption("OpenAI GPT-4o-mini", "gpt-4o-mini", "https://api.openai.com/v1/"),
        ModelOption("DeepSeek Chat", "deepseek-chat", "https://api.deepseek.com/v1/"),
        ModelOption("通义千问 Plus", "qwen-plus", "https://dashscope.aliyuncs.com/compatible-mode/v1/"),
        ModelOption("Moonshot Kimi", "moonshot-v1-8k", "https://api.moonshot.cn/v1/"),
        ModelOption("豆包 Doubao-Pro", "doubao-pro-256k", "https://ark.cn-beijing.volces.com/api/v3/"),
        ModelOption("豆包 Doubao-Lite", "doubao-lite-4k", "https://ark.cn-beijing.volces.com/api/v3/")
    )

    /**
     * 根据模型名（或显示名）查找预设模型，返回对应的接口地址；未匹配时返回 null。
     */
    fun defaultBaseUrlForModel(model: String): String? {
        val m = model.trim()
        if (m.isEmpty()) return null
        return MODEL_OPTIONS.firstOrNull { it.model == m || it.display == m }?.baseUrl
    }

    /** 默认接口地址（智谱开放平台，OpenAI 兼容格式） */
    const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    /** 默认模型名 */
    const val DEFAULT_MODEL = "GLM-4.7-Flash"

    /** 默认 API Key（用户未配置时使用，可在设置页覆盖） */
    const val DEFAULT_API_KEY = "c8911f7e1e064cada93094a1b89fed80.PB2X8egYZFbiF35b"

    /** 建立连接超时（毫秒） */
    private const val CONNECT_TIMEOUT_MS = 30_000

    /**
     * 读取超时（毫秒）。
     * 实测免费共享额度在高峰期排队严重（一次极短回复也可能耗时 2 分钟以上），
     * 解卦这类长文本输出更久，故放宽到 5 分钟，避免正常生成中途被判为「超时」。
     */
    private const val READ_TIMEOUT_MS = 300_000

    /**
     * 遇到 429 / 5xx 等临时性错误时的最大尝试次数。
     * 注意：超时不参与重试（单次已等待很久，再重试会让用户等待过长）。
     */
    private const val MAX_RETRY = 2

    /** 重试基础退避间隔（毫秒），按次数递增 */
    private const val RETRY_DELAY_MS = 2_000L

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
        val useUrl = baseUrl.trim().ifBlank { DEFAULT_BASE_URL }
        val useModel = model.trim().ifBlank { DEFAULT_MODEL }
        val useKey = apiKey.trim().ifBlank { DEFAULT_API_KEY }

        // 免费额度为共享并发，高峰期常返回 429（请求过于频繁）。
        // 此类可恢复错误自动退避重试，避免用户看到无谓的失败。
        var lastError = ""
        repeat(MAX_RETRY) { attempt ->
            val result = requestOnce(useKey, prompt, useUrl, useModel)
            if (!result.retryable) return result.text
            lastError = result.text
            if (attempt < MAX_RETRY - 1) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return lastError
                }
            }
        }
        return lastError
    }

    /** 单次请求结果；[retryable] 为 true 表示属于可重试的临时性错误 */
    private data class Attempt(val text: String, val retryable: Boolean = false)

    private fun requestOnce(apiKey: String, prompt: String, baseUrl: String, model: String): Attempt {
        val url = URL(baseUrl)
        val useModel = model
        val useKey = apiKey
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $useKey")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS

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
                // 限制输出长度：免费模型生成越长越慢，1200 tokens 足够覆盖五段式解卦
                put("max_tokens", 1200)
                // GLM-4.x 系列默认开启「思考模式」，会先生成大量隐藏推理内容，
                // 导致耗时翻倍且极易超时；此处显式关闭，仅输出正式解卦结果。
                // 该字段对不支持的模型服务会被忽略，不影响兼容性。
                put("thinking", JSONObject().apply { put("type", "disabled") })
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
            when {
                code == 429 -> Attempt(
                    "AI解析出错：请求过于频繁（429）。免费额度为共享并发，" +
                            "请稍候重试，或在设置页填入自己的 API Key。",
                    retryable = true
                )
                code in 500..599 -> Attempt(
                    "AI解析出错：模型服务暂时不可用（HTTP $code），请稍后重试。",
                    retryable = true
                )
                code == 401 || code == 403 -> Attempt(
                    "AI解析出错：API Key 无效或无权限（HTTP $code），请在设置页检查 Key。"
                )
                code !in 200..299 -> Attempt("请求失败（HTTP $code）：${extractError(respText)}")
                else -> Attempt(parseContent(respText))
            }
        } catch (e: java.net.SocketTimeoutException) {
            // 不自动重试：单次已等待数分钟，再重试会让用户等待过长，交由用户手动点「重试」
            Attempt(
                "AI解析出错：请求超时。免费模型高峰期排队较久，请稍后重试；" +
                        "也可在设置页填入自己的 API Key 或更换更快的模型。"
            )
        } catch (e: java.net.UnknownHostException) {
            Attempt("AI解析出错：无法连接服务器，请检查网络后重试。")
        } catch (e: Exception) {
            Attempt("AI解析出错：${e.message}")
        } finally {
            conn.disconnect()
        }
    }

    private fun parseContent(respText: String): String {
        return try {
            val json = JSONObject(respText)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) return "模型未返回内容。"
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.optString("content", "")
            if (content.isNotBlank()) return content
            // 个别推理模型只填充 reasoning_content，正文为空时回退取用
            val reasoning = message.optString("reasoning_content", "")
            if (reasoning.isNotBlank()) reasoning else "模型未返回内容。"
        } catch (e: Exception) {
            "解析返回结果失败：${e.message}\n原始返回：${respText.take(500)}"
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
