package com.treasure.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cycle 0036 v2：把用户附加的文件转成纯文本，作为 user-turn 上下文喂给 AI。
 *
 * 走"客户端解析"路径而非"Anthropic document block 之类 provider-specific 协
 * 议"—— 这样所有 BYO key provider（Anthropic / OpenAI / DeepSeek / Kimi /
 * Qwen / GLM ...）都吃同一份文本，不用让某些 provider 静默失败。
 *
 * 支持类型：
 *   - text/任意子类型（plain / markdown / csv / html / xml / javascript / ...）
 *   - application/{json, xml, javascript, typescript, yaml, sql, sh}
 *   - application/pdf（PdfBox-Android 提取）
 *   - 按后缀兜底的源代码 / 配置类文件
 *
 * 不支持：DOCX / XLSX / 二进制 → 返回 [Result.Unsupported]，调用方据此在
 * user-turn 文本里只塞元数据 hint。
 *
 * 大小限制：单文件最多 [MAX_TEXT_BYTES] 字节文本；PDF 最多 [MAX_PDF_PAGES]
 * 页。超出截断，调用方拼提示给用户 / AI。
 */
object FileTextExtractor {

    /** 单文件文本上限 — 太大会撑爆 prompt。256 KB ≈ 12 万英文字 / 8 万中文字。 */
    private const val MAX_TEXT_BYTES = 256 * 1024

    /** PDF 最多解到这一页；剩下截断。 */
    private const val MAX_PDF_PAGES = 50

    private val TEXT_LIKE_MIME = setOf(
        "application/json", "application/xml", "application/javascript",
        "application/typescript", "application/x-yaml", "application/yaml",
        "application/sql", "application/x-sh", "application/x-shell",
        "application/x-httpd-php", "application/toml", "application/x-toml",
    )

    private val TEXT_LIKE_EXTS = setOf(
        "txt", "md", "json", "xml", "yaml", "yml", "csv", "tsv", "log", "ini", "conf", "cfg", "toml",
        "py", "js", "ts", "tsx", "jsx", "kt", "kts", "java", "scala", "groovy",
        "c", "h", "cc", "cpp", "hpp", "cs", "go", "rs", "rb", "php", "swift",
        "sql", "sh", "bash", "zsh", "fish", "ps1", "bat",
        "html", "htm", "css", "scss", "sass", "less", "vue", "svelte",
        "gradle", "properties", "env", "dockerfile",
    )

    sealed interface Result {
        data class Text(val content: String, val truncated: Boolean = false, val pages: Int? = null) : Result
        data class Unsupported(val mime: String?) : Result
        data class Failed(val message: String) : Result
    }

    /**
     * 主入口。decisions：
     *   - text/任意子类型 → 直接 UTF-8 读取，超 256KB 截断；
     *   - application/pdf → PdfBox 提取，超 50 页截断；
     *   - 其它白名单 mime / 后缀 → 当文本读；
     *   - 其它 → Unsupported。
     */
    suspend fun extract(context: Context, uri: Uri, mime: String?): Result = withContext(Dispatchers.IO) {
        val mimeLower = mime?.lowercase()
        val ext = extractExt(uri)
        when {
            mimeLower != null && mimeLower.startsWith("text/") -> extractText(context, uri)
            mimeLower in TEXT_LIKE_MIME -> extractText(context, uri)
            mimeLower == "application/pdf" -> extractPdf(context, uri)
            ext != null && ext in TEXT_LIKE_EXTS -> extractText(context, uri)
            ext == "pdf" -> extractPdf(context, uri)
            else -> Result.Unsupported(mime)
        }
    }

    private fun extractExt(uri: Uri): String? =
        uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() && it.length <= 6 }

    private fun extractText(context: Context, uri: Uri): Result = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            // 多读 1 个 byte 当 truncation 探测：拿到 > MAX_TEXT_BYTES 就说明被截了
            val bytes = input.readBytesCapped(MAX_TEXT_BYTES + 1)
            val truncated = bytes.size > MAX_TEXT_BYTES
            val data = if (truncated) bytes.copyOf(MAX_TEXT_BYTES) else bytes
            Result.Text(content = String(data, Charsets.UTF_8), truncated = truncated)
        } ?: Result.Failed("无法读取")
    } catch (t: Throwable) {
        Result.Failed(t.message ?: t.javaClass.simpleName)
    }

    private fun extractPdf(context: Context, uri: Uri): Result = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val totalPages = doc.numberOfPages
                val pages = totalPages.coerceAtMost(MAX_PDF_PAGES)
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = pages
                }
                val raw = stripper.getText(doc)
                val rawBytes = raw.toByteArray(Charsets.UTF_8)
                val byteTruncated = rawBytes.size > MAX_TEXT_BYTES
                val cropped = if (byteTruncated) {
                    String(rawBytes.copyOf(MAX_TEXT_BYTES), Charsets.UTF_8)
                } else raw
                Result.Text(
                    content = cropped,
                    truncated = byteTruncated || pages < totalPages,
                    pages = totalPages,
                )
            }
        } ?: Result.Failed("无法读取 PDF")
    } catch (t: Throwable) {
        Result.Failed(t.message ?: t.javaClass.simpleName)
    }

    private fun java.io.InputStream.readBytesCapped(cap: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val n = read(buf)
            if (n <= 0) break
            val toCopy = minOf(n, cap - total)
            if (toCopy > 0) out.write(buf, 0, toCopy)
            total += n
            if (total >= cap) break
        }
        return out.toByteArray()
    }
}
