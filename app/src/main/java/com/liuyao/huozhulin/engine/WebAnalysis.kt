package com.liuyao.huozhulin.engine

import com.liuyao.huozhulin.data.model.LineInfo
import com.liuyao.huozhulin.data.model.PaiPanResult
import com.liuyao.huozhulin.data.model.ShiYingType
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 排盘后的联网解析引擎。
 *
 * 数据源（无需鉴权、HTTPS、返回结构化结果）：
 * 1. 维基百科中文 API：以卦名为关键字检索「六十四卦」「本卦名」等条目，
 *    直接拿到纯文本摘要（extracts），稳定可靠，作为主来源。
 * 2. 百度百科 PC 端页面：作为兜底，正则抓取词条摘要段落。
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
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 10000
    private const val MAX_ITEMS = 6
    private const val MAX_LEN = 220      // 单条摘要最大字符数（百科摘要较长）

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
     * 执行联网解析。依次尝试维基百科（多个条目），
     * 再尝试百度百科，返回首个成功获取到材料的结果。
     */
    @Throws(AnalysisException::class)
    fun analyze(r: PaiPanResult): AnalysisResult {
        val queries = buildQueries(r)
        val errors = mutableListOf<String>()

        // 1) 维基百科（主源）
        for (q in queries) {
            try {
                val items = fetchWiki(q)
                if (items.isNotEmpty()) return AnalysisResult(query = q, items = items)
            } catch (e: Exception) {
                errors += "wiki:$q:${e.message ?: e.javaClass.simpleName}"
            }
        }

        // 2) 百度百科（兜底源）
        val baikeName = r.original.name
        try {
            val items = fetchBaike(baikeName)
            if (items.isNotEmpty()) return AnalysisResult(query = baikeName, items = items)
        } catch (e: Exception) {
            errors += "baike:$baikeName:${e.message ?: e.javaClass.simpleName}"
        }

        throw AnalysisException("联网解析失败：${errors.joinToString("; ")}")
    }

    /** 本地兜底的解析文本（网络不可用时使用） */
    fun fallback(r: PaiPanResult): AnalysisResult {
        val q = buildQueries(r).firstOrNull() ?: r.original.name
        val items = listOf(
            AnalysisItem(
                source = "本地",
                title = "${r.original.name}（${r.original.hexagram.palace.cnName}宫）",
                snippet = "本卦属${r.original.hexagram.palaceElement.cn}，世爻在" +
                        "${r.original.lines.indexOfFirst { it.shiYing == ShiYingType.SHI } + 1}爻、" +
                        "应爻在${r.original.lines.indexOfFirst { it.shiYing == ShiYingType.YING } + 1}爻。" +
                        "当前无网络连接，未能获取联网解析材料，请检查网络后重试。",
                link = null
            )
        )
        return AnalysisResult(query = q, items = items)
    }

    // ——————————————————————————— 内部实现 ———————————————————————————

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
