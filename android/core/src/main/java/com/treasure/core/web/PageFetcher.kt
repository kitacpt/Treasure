package com.treasure.core.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 抓取结果：success / blocked（防爬 / 登录墙）/ network 错误。
 * Cycle 0021 加：让 ViewModel 能区分三种情况，给 AI 不同的 prompt 提示。
 */
sealed class FetchResult {
    data class Success(val text: String) : FetchResult()
    /** HTTP 状态成功但页面是登录 / 验证码页 / 内容稀薄。 */
    data class Blocked(val host: String, val reason: String) : FetchResult()
    /** 网络 / HTTP 错误。 */
    data class Failed(val host: String, val message: String) : FetchResult()
}

/**
 * 把京东 / 淘宝 / 拼多多 / 浏览器分享过来的 URL 真去拉一下页面，剥掉脚本
 * 样式后给 AI 当上下文。AI 不会真访问 web，所以我们先拉文本喂回去。
 *
 * 设备直连 — [ADR-0004](docs/adr/0004-byo-ai-key.md) 规定 AI 不走代理；
 * 这里 fetch 也是设备直连商家站点。失败就回退到只用分享文本。
 *
 * 不试图理解 JD / 淘宝的具体 DOM；不同商家页结构差太大，加上做反爬识别。
 * 简单走 OkHttp + 移动 UA + 取头部 N KB + 全文 strip tag 给 AI 看摘要。
 *
 * Cycle 0021：返回 [FetchResult] 区分 success / blocked / failed，让上层
 * 能告诉 AI 实际情况，避免 AI 拿到没内容时回 "我无法访问外部链接"。
 */
class PageFetcher(httpClient: OkHttpClient? = null) {

    private val client: OkHttpClient = httpClient ?: defaultHttpClient()

    /**
     * 抓 [url] 的页面文字，返回结构化结果。
     * @param maxBytes 最多读多少字节的 raw body，避免大页面把 prompt 塞爆
     * @param maxTextChars 抽完文本后再截到这个字符数喂 AI
     */
    suspend fun fetchText(
        url: String,
        maxBytes: Int = 96 * 1024,
        maxTextChars: Int = 4_000,
    ): FetchResult = withContext(Dispatchers.IO) {
        val host = runCatching { java.net.URI(url).host.orEmpty() }.getOrDefault("")
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MOBILE_UA)
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                )
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext FetchResult.Failed(host, "HTTP ${resp.code}")
                }
                val body = resp.body
                    ?: return@withContext FetchResult.Failed(host, "empty body")
                val raw = body.byteStream().use { input ->
                    val buf = ByteArray(maxBytes)
                    var read = 0
                    while (read < buf.size) {
                        val n = input.read(buf, read, buf.size - read)
                        if (n <= 0) break
                        read += n
                    }
                    val charset = body.contentType()?.charset()
                        ?: Charsets.UTF_8
                    String(buf, 0, read, charset)
                }
                val stripped = stripHtml(raw).take(maxTextChars)
                val blockedReason = detectBlock(stripped, host)
                if (blockedReason != null) {
                    FetchResult.Blocked(host, blockedReason)
                } else {
                    FetchResult.Success(stripped)
                }
            }
        } catch (t: Throwable) {
            FetchResult.Failed(host, t.javaClass.simpleName + ": " + (t.message ?: ""))
        }
    }

    companion object {
        // 装成主流移动 Chrome — 部分电商站会按 UA 决定返回桌面或移动版，
        // 移动版结构相对干净。
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; vivo X200 Pro mini) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.6478.122 Mobile Safari/537.36"

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

/**
 * Best-effort 把 HTML 摘成纯文本：
 * 1. `<script>` / `<style>` / `<noscript>` 整段干掉（含里面的 JS / CSS 噪声）
 * 2. 抓 `<title>` 内容塞到开头
 * 3. 抓 og:title / og:description / description meta 信息塞到开头
 * 4. 剩下所有 tag 替成空格
 * 5. 折叠空白
 *
 * 输出大致是一段 "标题 + 描述 + 全页可见文本" 的乱炖，AI 自己挑里面的
 * 品牌 / 型号 / 价格出来。
 */
internal fun stripHtml(html: String): String {
    if (html.isBlank()) return ""

    fun removeBlock(input: String, tag: String): String =
        Regex(
            "<$tag\\b[^>]*>[\\s\\S]*?</$tag>",
            setOf(RegexOption.IGNORE_CASE),
        ).replace(input, " ")

    var s = html
    s = removeBlock(s, "script")
    s = removeBlock(s, "style")
    s = removeBlock(s, "noscript")

    val title = Regex(
        "<title[^>]*>(.*?)</title>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).find(s)?.groupValues?.getOrNull(1)?.trim()

    val metas = mutableListOf<String>()
    Regex(
        "<meta[^>]+(?:property|name)=[\"'](?:og:title|og:description|description|keywords)[\"'][^>]*>",
        setOf(RegexOption.IGNORE_CASE),
    ).findAll(s).forEach { m ->
        val tag = m.value
        Regex("content=[\"']([^\"']*)[\"']", setOf(RegexOption.IGNORE_CASE))
            .find(tag)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(metas::add)
    }

    s = s.replace(Regex("<[^>]+>"), " ")
        .replace(Regex("&nbsp;", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("&amp;", RegexOption.IGNORE_CASE), "&")
        .replace(Regex("&lt;", RegexOption.IGNORE_CASE), "<")
        .replace(Regex("&gt;", RegexOption.IGNORE_CASE), ">")
        .replace(Regex("&quot;", RegexOption.IGNORE_CASE), "\"")
        .replace(Regex("\\s+"), " ")
        .trim()

    val parts = buildList {
        title?.takeIf { it.isNotBlank() }?.let { add("【标题】 $it") }
        if (metas.isNotEmpty()) add("【描述】 " + metas.joinToString(" / "))
        if (s.isNotBlank()) add(s)
    }
    return parts.joinToString("\n\n")
}

private val URL_REGEX = Regex(
    "(https?://[\\w.\\-]+(?:/[\\w./?=&%#\\-+~]*)?)",
    RegexOption.IGNORE_CASE,
)

/** Returns first http(s) URL inside a free-form string, or null. */
fun firstUrlIn(text: String): String? =
    URL_REGEX.find(text)?.value

/**
 * 启发式判 "拉到的页面其实是登录 / 验证码 / 反爬墙"。
 *
 * 拼多多的分享链接（mobile.yangkeduo.com / yangkeduo.com / pinduoduo.com）
 * 直接 fetch 经常拿到 "请打开拼多多 App" / "活动正在加载" 之类的占位页；
 * 京东 item 页可能要登录拿价格；淘宝直接被风控挡。这些页面 strip 后字数
 * 少 + 含特定关键词，能识别出来。
 */
internal fun detectBlock(text: String, host: String): String? {
    val len = text.length

    // 长度极短直接当被屏蔽（一般正常商品页 strip 后至少几百字）
    if (len < 200) return "page too short ($len chars) — likely empty / blocked"

    // 关键词命中：登录墙 / captcha / 拼多多打开 App 提示
    val lower = text.lowercase()
    val keywords = listOf(
        "请登录" to "login wall",
        "请先登录" to "login wall",
        "需要登录" to "login wall",
        "captcha" to "captcha",
        "verify you are human" to "captcha",
        "robot check" to "captcha",
        "访问受限" to "rate limited",
        "frequent access" to "rate limited",
        "打开拼多多" to "pinduoduo app gate",
        "打开淘宝" to "taobao app gate",
        "打开京东" to "jd app gate",
        "请使用" to "app required",
        "在 app 内打开" to "app required",
        "活动已结束" to "event ended",
    )
    for ((kw, reason) in keywords) {
        // 关键词命中 + 总长不超过 1000 字 → 八成是壳页
        if (lower.contains(kw) && len < 1000) {
            return "$reason ($host)"
        }
    }
    return null
}
