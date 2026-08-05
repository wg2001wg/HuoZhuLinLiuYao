package com.liuyao.huozhulin.engine

import com.liuyao.huozhulin.data.Yijing
import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.ShiYingType
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 排盘后的联网解析引擎。
 *
 * 支持两种模式：
 * 1. 豆包大模型解析：当用户配置火山方舟 API Key 时，直接调用豆包模型生成
 *    包含主卦、变卦、爻辞与整体吉凶的详细六爻解析。
 * 2. 百科/维基解析：无 API Key 时的兜底方案，依次抓取维基百科、百度百科摘要，
 *    并叠加本地易经卦辞爻辞形成较完整的解析材料。
 *
 * 解析材料在「点开系统解析」后由 UI 触发（见 ResultScreen），
 * 网络不可用时降级为本地解析，不影响排盘主流程。
 * 仅使用 JDK 内置 [HttpURLConnection]，兼容 minSdk 24。
 */
object WebAnalysis {

    /** 单条解析材料 */
    data class AnalysisItem(
        val source: String,   // 来源名
        val title: String,     // 标题
        val snippet: String,   // 摘要文本
        val link: String?      // 原文链接（可空）
    )

    /** 一次联网解析的完整结果 */
    data class AnalysisResult(
        val query: String,             // 实际提交的查询词
        val items: List<AnalysisItem>, // 解析材料条目
        val fetchedAt: Long = System.currentTimeMillis()
    )

    private const val WIKI_API =
        "https://zh.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=1&exintro=1&redirects=1&format=json&titles="
    private const val BAIKE = "https://baike.baidu.com/item/"
    private const val DOUBAO_API = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
    private const val DOUBAO_MODEL = "doubao-pro-32k"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 20000
    private const val MAX_ITEMS = 6
    private const val MAX_LEN = 600      // 单条摘要最大字符数

    /**
     * 生成待检索的条目标题列表（按优先级）。
     * 先查「六十四卦」总论，再查本卦与变卦的具体条目。
     */
    fun buildQueries(r: PaiPanResult): List<String> {
        val queries = mutableListOf<String>()
        val origName = r.original.name
        queries += "六十四卦"
        queries += origName
        r.changed?.let { queries += it.name }
        return queries.distinct().take(4)
    }

    /**
     * 执行联网解析。
     *
     * 若 [apiKey] 非空，优先调用豆包大模型生成综合解析；
     * 否则依次尝试维基百科、百度百科，并叠加本地卦辞爻辞材料。
     */
    @Throws(AnalysisException::class)
    fun analyze(r: PaiPanResult, apiKey: String? = null): AnalysisResult {
        // 1) 豆包 AI 解析（需要用户自行配置 API Key）
        if (!apiKey.isNullOrBlank()) {
            try {
                return fetchDoubao(r, apiKey)
            } catch (e: Exception) {
                // 豆包失败时继续走百科兜底，不让 UI 直接报错
                val errors = mutableListOf("doubao:${e.message ?: e.javaClass.simpleName}")
                try {
                    return analyzeBaikeWiki(r, errors)
                } catch (_: AnalysisException) {
                    // 最终 fallback 到本地增强解析
                }
            }
        }

        // 2) 百科 / 维基 + 本地增强
        return try {
            analyzeBaikeWiki(r, mutableListOf())
        } catch (_: AnalysisException) {
            fallback(r)
        }
    }

    private fun analyzeBaikeWiki(r: PaiPanResult, errors: MutableList<String>): AnalysisResult {
        val queries = buildQueries(r)
        val items = mutableListOf<AnalysisItem>()

        // 维基百科（主源）
        for (q in queries) {
            try {
                items += fetchWiki(q)
            } catch (e: Exception) {
                errors += "wiki:$q:${e.message ?: e.javaClass.simpleName}"
            }
        }

        // 百度百科（兜底源）
        val baikeName = r.original.name
        try {
            items += fetchBaike(baikeName)
        } catch (e: Exception) {
            errors += "baike:$baikeName:${e.message ?: e.javaClass.simpleName}"
        }

        if (items.isEmpty()) throw AnalysisException("联网解析失败：${errors.joinToString("; ")}")

        // 叠加本地易经卦辞爻辞解析
        val localItems = buildLocalAnalysisItems(r)
        return AnalysisResult(
            query = r.original.name,
            items = (items.take(MAX_ITEMS) + localItems).distinctBy { it.title + it.snippet }
        )
    }

    /** 本地兜底的解析文本（网络不可用时使用），包含主卦、变卦、爻辞与整体判断 */
    fun fallback(r: PaiPanResult): AnalysisResult {
        return AnalysisResult(
            query = r.original.name,
            items = buildLocalAnalysisItems(r, includeNetworkHint = true)
        )
    }

    /** 基于本地易经数据构建详细解析条目 */
    private fun buildLocalAnalysisItems(
        r: PaiPanResult,
        includeNetworkHint: Boolean = false
    ): List<AnalysisItem> {
        val items = mutableListOf<AnalysisItem>()
        val origName = r.original.name
        val origGuaCi = Yijing.guaCi[origName]

        // 主卦卦辞
        if (!origGuaCi.isNullOrBlank()) {
            items += AnalysisItem(
                source = "《易经》",
                title = "$origName · 卦辞",
                snippet = origGuaCi,
                link = null
            )
        }

        // 动爻爻辞
        val dongYao = r.original.lines.filter { it.moving }.map { it.position + 1 }
        val origYaoCi = Yijing.yaoCi[origName]
        if (!origYaoCi.isNullOrEmpty() && dongYao.isNotEmpty()) {
            val yaoText = dongYao.joinToString("\n") { pos ->
                val pair = origYaoCi.getOrNull(pos - 1)
                if (pair != null) "第${pos}爻 ${pair.first}：${pair.second}" else "第${pos}爻：爻辞未收录"
            }
            items += AnalysisItem(
                source = "《易经》",
                title = "$origName · 动爻爻辞",
                snippet = yaoText,
                link = null
            )
        }

        // 变卦信息
        r.changed?.let { changed ->
            val changedGuaCi = Yijing.guaCi[changed.name]
            if (!changedGuaCi.isNullOrBlank()) {
                items += AnalysisItem(
                    source = "《易经》",
                    title = "变卦 ${changed.name} · 卦辞",
                    snippet = changedGuaCi,
                    link = null
                )
            }
            // 变爻爻辞：变卦中与原动爻位置对应的爻
            val changedYaoCi = Yijing.yaoCi[changed.name]
            if (!changedYaoCi.isNullOrEmpty() && dongYao.isNotEmpty()) {
                val text = dongYao.joinToString("\n") { pos ->
                    val pair = changedYaoCi.getOrNull(pos - 1)
                    if (pair != null) "第${pos}爻 ${pair.first}：${pair.second}" else "第${pos}爻：爻辞未收录"
                }
                items += AnalysisItem(
                    source = "《易经》",
                    title = "变卦 ${changed.name} · 变爻爻辞",
                    snippet = text,
                    link = null
                )
            }
        }

        // 整体吉凶分析
        items += AnalysisItem(
            source = "系统",
            title = "整体吉凶分析",
            snippet = buildFortuneAnalysis(r, includeNetworkHint),
            link = null
        )

        return items
    }

    /** 构造整体吉凶分析文本 */
    private fun buildFortuneAnalysis(r: PaiPanResult, includeNetworkHint: Boolean): String {
        val sb = StringBuilder()
        val origName = r.original.name
        val palace = r.original.hexagram.palace.cnName
        val element = r.original.hexagram.palaceElement.cn
        val shiIdx = r.original.lines.indexOfFirst { it.shiYing == ShiYingType.SHI } + 1
        val yingIdx = r.original.lines.indexOfFirst { it.shiYing == ShiYingType.YING } + 1
        val dong = r.original.lines.filter { it.moving }.map { it.position + 1 }

        sb.append("本卦$origName 属$palace 宫，五行为$element。")
        sb.append("世爻在第${shiIdx}爻，应爻在第${yingIdx}爻。")
        if (dong.isEmpty()) {
            sb.append("此卦无动爻，为静卦，事物当前状态较为稳定，宜以本卦卦辞为主断吉凶。")
        } else {
            sb.append("动爻为第${dong.joinToString("、")}爻，动则变，宜结合本卦动爻爻辞与变卦卦辞综合判断。")
        }
        sb.append("大体而言，")
        val guaCi = Yijing.guaCi[origName] ?: ""
        when {
            guaCi.contains("元亨") || guaCi.contains("元吉") -> sb.append("卦辞见「元亨」「元吉」，多主大吉、亨通。")
            guaCi.contains("亨") && !guaCi.contains("终凶") -> sb.append("卦辞有「亨」，主事可通顺。")
            guaCi.contains("贞吉") || guaCi.contains("利贞") -> sb.append("卦辞见「贞吉」「利贞」，守正则吉。")
            guaCi.contains("凶") || guaCi.contains("厉") -> sb.append("卦辞含凶厉之象，宜谨慎行事、守静待时。")
            else -> sb.append("吉凶须结合所问之事、月建日辰及用神旺衰综合论断。")
        }
        if (includeNetworkHint) {
            sb.append("当前网络解析暂不可用，已切换为本地易经解析；网络恢复后可点击右上角刷新获取联网材料。")
        }
        return sb.toString()
    }

    /** 调用豆包大模型生成综合六爻解析 */
    private fun fetchDoubao(r: PaiPanResult, apiKey: String): AnalysisResult {
        val prompt = buildDoubaoPrompt(r)
        val body = JSONObject().apply {
            put("model", DOUBAO_MODEL)
            put("temperature", 0.7)
            put("max_tokens", 1200)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是一位精通《周易》六爻预测的资深解卦师。请根据用户提供的排盘信息，" +
                            "从主卦卦象、变卦趋势、动爻爻辞、世应关系、整体吉凶与建议等方面给出详细、" +
                            "专业且通俗易懂的解析。不要提及模型身份，只给出解卦内容。")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }.toString()

        val url = URL(DOUBAO_API)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body); it.flush() }

            val responseCode = conn.responseCode
            val reader = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader(java.nio.charset.Charset.forName("UTF-8"))
            } else {
                conn.errorStream?.bufferedReader(java.nio.charset.Charset.forName("UTF-8"))
                    ?: throw AnalysisException("豆包 API 请求失败：HTTP $responseCode")
            }
            val json = reader.use { it.readText() }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw AnalysisException("豆包 API 请求失败：HTTP $responseCode，$json")
            }

            val content = parseDoubaoContent(json)
                ?: throw AnalysisException("豆包返回内容为空")

            return AnalysisResult(
                query = "豆包大模型 · ${r.original.name}",
                items = listOf(
                    AnalysisItem(
                        source = "豆包大模型",
                        title = "综合六爻解析",
                        snippet = content,
                        link = null
                    )
                )
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun parseDoubaoContent(json: String): String? {
        val root = JSONObject(json)
        val choices = root.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.optJSONObject(0)?.optJSONObject("message") ?: return null
        return message.optString("content").trim().takeIf { it.isNotBlank() }
    }

    /** 构造提交给豆包的Prompt */
    private fun buildDoubaoPrompt(r: PaiPanResult): String {
        val origName = r.original.name
        val dong = r.original.lines.filter { it.moving }.map { it.position + 1 }
        val sb = StringBuilder()
        sb.appendLine("请为以下六爻卦象进行详细解析：")
        sb.appendLine("本卦：$origName（${r.original.hexagram.palace.cnName}宫，五行${r.original.hexagram.palaceElement.cn}）")
        sb.appendLine("世爻：第${r.original.lines.indexOfFirst { it.shiYing == ShiYingType.SHI } + 1}爻")
        sb.appendLine("应爻：第${r.original.lines.indexOfFirst { it.shiYing == ShiYingType.YING } + 1}爻")
        if (dong.isNotEmpty()) {
            sb.appendLine("动爻：第${dong.joinToString("、")}爻")
            r.changed?.let { sb.appendLine("变卦：${it.name}") }
        } else {
            sb.appendLine("此卦为静卦，无动爻。")
        }
        sb.appendLine()
        sb.appendLine("请依次包含以下内容：")
        sb.appendLine("1. 主卦卦象与卦辞解读；")
        sb.appendLine("2. 变卦（如有）趋势分析；")
        sb.appendLine("3. 动爻爻辞解析；")
        sb.appendLine("4. 世应关系与用神提示；")
        sb.appendLine("5. 整体吉凶判断与具体建议。")
        return sb.toString()
    }

    // ——————————————————————————— 百科/维基内部实现 ———————————————————————————

    /** 维基百科：按标题取摘要，可返回多条（含总论 + 具体卦） */
    private fun fetchWiki(title: String): List<AnalysisItem> {
        val url = URL(WIKI_API + URLEncoder.encode(title, "UTF-8"))
        val conn = openConn(url)
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val json = conn.inputStream.bufferedReader(java.nio.charset.Charset.forName("UTF-8")).use { it.readText() }
            val root = JSONObject(json)
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
            val items = mutableListOf<AnalysisItem>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val page = pages.optJSONObject(keys.next()) ?: continue
                val pageTitle = page.optString("title")
                val extract = page.optString("extract")
                if (extract.isNotBlank()) {
                    items += AnalysisItem(
                        source = "维基百科",
                        title = pageTitle,
                        snippet = clip(extract.replace("\n", " ")),
                        link = "https://zh.wikipedia.org/wiki/" + URLEncoder.encode(pageTitle, "UTF-8")
                    )
                }
            }
            return items
        } finally {
            conn.disconnect()
        }
    }

    /** 百度百科：抓取词条摘要段落（PC 端页面结构较稳定） */
    private fun fetchBaike(name: String): List<AnalysisItem> {
        val url = URL(BAIKE + URLEncoder.encode(name, "UTF-8"))
        val conn = openConn(url)
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val charset = detectCharset(conn) ?: "UTF-8"
            val html = conn.inputStream.bufferedReader(java.nio.charset.Charset.forName(charset)).use { it.readText() }
            // 摘要段落：百度百科将简介放在 <div class="lemma-summary"> ... </div> 内
            val summaryBlock = Regex("<div[^>]*class=\"lemma-summary\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.getOrNull(1)
                ?: Regex("<div[^>]*class=\"[^\"]*summary[^\"]*\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
                    .find(html)?.groupValues?.getOrNull(1)
                ?: return emptyList()
            val text = stripHtml(summaryBlock).replace(Regex("\\s+"), " ").trim()
            if (text.isBlank()) return emptyList()
            return listOf(
                AnalysisItem(
                    source = "百度百科",
                    title = name,
                    snippet = clip(text),
                    link = BAIKE + URLEncoder.encode(name, "UTF-8")
                )
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun openConn(url: URL): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
        conn.instanceFollowRedirects = true
        return conn
    }

    private fun detectCharset(conn: HttpURLConnection): String? {
        val ct = conn.contentType ?: return null
        val idx = ct.indexOf("charset=", ignoreCase = true)
        if (idx < 0) return null
        return ct.substring(idx + 8).trim().split(";")[0].trim().uppercase()
    }

    private fun stripHtml(s: String): String {
        return s.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;|&apos;"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun clip(s: String): String = if (s.length <= MAX_LEN) s else s.take(MAX_LEN) + "…"

    /** 联网解析异常 */
    class AnalysisException(msg: String) : Exception(msg)
}
