package com.treasure.ui.detail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as ACol
import android.graphics.Paint
import android.graphics.Path as APath
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.treasure.R
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.domain.PhotoCrop
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Cycle 0038 — Detail 分享卡片。
 *
 * 路径 A：纯 [android.graphics.Bitmap] + [android.graphics.Canvas] 手绘。
 * 目标尺寸 1080×1350（4:5，微信朋友圈 / Instagram 友好）。
 *
 * 视觉风格借鉴 prototype/project/Treasure.html — 博物馆图鉴版面：上下窄边
 * 罗马指南针式 Ornament 横线、中央 hero 框（card 浅底 + 1px line 边）、英
 * 文小字 latin label + 中文物品名（Cormorant）、4 行 hero specs 表（hairline
 * 分隔）、底部水印 "TREASURE · est. 2025" 与 acquired 日期左右对称。
 *
 * 中文用系统默认字体（思源黑体 / PingFang）— 整个 app 都这样，符合调性。
 */
object ShareCard {

    /** 卡片像素尺寸：1080 × 1350（4:5）。 */
    const val CARD_W = 1080
    const val CARD_H = 1350

    /**
     * 生成单张分享卡片。运行在 IO 线程上。
     *
     * 写到 `filesDir/share-cards/<itemId>-<timestamp>.png`，返回该 File。
     * 同 item / 同时间戳的多次调用会各写一份（毫秒级时间戳基本不会撞），
     * 抽屉关闭后不主动删，下次同物品打开可复用。
     */
    fun generate(
        context: Context,
        item: Item,
        categoryNameZh: String,
        categoryNameEn: String,
    ): File {
        val bmp = renderBitmap(context, item, categoryNameZh, categoryNameEn)
        val dir = File(context.filesDir, "share-cards").apply { mkdirs() }
        val safeBrand = item.brand.replace("[^A-Za-z0-9\\u4e00-\\u9fa5]+".toRegex(), "_").ifBlank { "treasure" }
        val safeModel = item.model.replace("[^A-Za-z0-9\\u4e00-\\u9fa5]+".toRegex(), "_").ifBlank { item.id.take(6) }
        val ts = System.currentTimeMillis()
        val file = File(dir, "Treasure-${safeBrand}-${safeModel}-${ts}.png")
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        return file
    }

    /**
     * 把 [card] 文件存到系统相册。Android 10+ 走 MediaStore scoped storage，
     * 不需要 WRITE_EXTERNAL_STORAGE permission；老版本同样走 MediaStore，但写
     * 入 `Pictures/Treasure/` 子目录靠 RELATIVE_PATH（Q+），9- 直接落到默认
     * Pictures。返回写成功后的 content:// uri。
     */
    fun saveToGallery(context: Context, card: File): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, card.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Treasure")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                card.inputStream().use { it.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }.onFailure {
            resolver.delete(uri, null, null)
            return null
        }
        return uri
    }

    /** 通过 FileProvider 包成 content:// uri 走系统分享面板（image/png）。 */
    fun shareIntent(context: Context, card: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, card)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "分享卡片").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // ─── Rendering ──────────────────────────────────────────────────────

    private fun renderBitmap(
        context: Context,
        item: Item,
        categoryNameZh: String,
        categoryNameEn: String,
    ): Bitmap {
        // 视觉走"浅色一套" — paper 底，墨色字，不跟随系统深色（卡片是出图，
        // 出去给别人看，永远是博物馆纸调）。
        val paper = ACol.parseColor("#F4F1EA")
        val ink = ACol.parseColor("#1A1815")
        val terra = ACol.parseColor("#8A3A1F")
        val card = ACol.parseColor("#FBF9F4")
        val sub = blendAlpha(ink, 0x8C)
        val line = blendAlpha(ink, 0x1A)

        val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(paper)

        // 字体：拉项目自带的西文字体；中文系统默认。
        val cormorant = loadFont(context, R.font.cormorant_garamond)
        val cormorantItalic = loadFont(context, R.font.cormorant_garamond_italic)
        val space = loadFont(context, R.font.space_grotesk)
        val mono = loadFont(context, R.font.jetbrains_mono)

        val pad = 88f

        // ── 顶部装饰条：左右细线 + 中心菱形 + 双圆环 ─────────────
        drawOrnament(
            c, sub = sub, ink = ink, line = line,
            cy = 112f,
            left = pad, right = CARD_W - pad,
        )

        // ── Latin small caps：CATEGORY 名 ─────────────────────────
        val latin = (categoryNameEn.ifBlank { Category.fromId(item.category).nameEn }).uppercase(Locale.ROOT)
        val cat = paintFor(font = mono, size = 22f, color = sub, letterSpacing = 0.20f, align = Paint.Align.CENTER)
        c.drawText("$latin  ·  ROOM № ${roomNumeral(item.category)}", CARD_W / 2f, 168f, cat)

        // ── Hero 框：940 × 560，居中、淡 card 底 + 0.6dp 线框 ────
        val heroW = (CARD_W - pad * 2)
        val heroH = 620f
        val heroLeft = (CARD_W - heroW) / 2f
        val heroTop = 200f
        val heroRect = RectF(heroLeft, heroTop, heroLeft + heroW, heroTop + heroH)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = card
        }
        c.drawRect(heroRect, fillP)
        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = line
            strokeWidth = 1.4f
        }
        c.drawRect(heroRect, borderP)

        // 左上 / 右下 角的小 tick — 博物馆图鉴边角徽记
        drawCornerTicks(c, heroRect, ink = sub, len = 18f, gap = 12f, stroke = 1.4f)

        // Hero 内容：有 avatarPhotoPath 走真实照片（应用 photoCrops）；否则走线描占位徽章
        val heroPath = item.avatarPhotoPath
        if (!heroPath.isNullOrBlank() && File(heroPath).exists()) {
            val crop = item.photoCrops[heroPath] ?: PhotoCrop.Full
            drawCroppedPhoto(c, heroPath, heroRect, crop, paddingPx = 28f)
        } else {
            drawHeroEmblem(
                c, heroRect, ink = ink, sub = sub, terra = terra,
                font = cormorant, hv = item.heroVector,
            )
        }

        // 物品张数 PHOTOS 角标（右下）
        if (item.photos.isNotEmpty()) {
            val tag = paintFor(font = mono, size = 18f, color = sub, letterSpacing = 0.15f, align = Paint.Align.RIGHT)
            c.drawText("${item.photos.size} PHOTOS", heroRect.right - 22f, heroRect.bottom - 22f, tag)
        }

        // ── 标题 brand + model（Cormorant titleLarge 同 spec, 放大）
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cormorant
            textSize = 80f
            color = ink
            letterSpacing = -0.02f
            textAlign = Paint.Align.LEFT
        }
        val titleY = heroTop + heroH + 96f
        val brandModel = "${item.brand} ${item.model}".trim()
        // 单行截断：测一下宽度；超就把 model 移下一行
        val maxTitleW = CARD_W - pad * 2
        val titleLines = wrapForWidth(brandModel, titleP, maxTitleW, maxLines = 2)
        var ty = titleY
        titleLines.forEach { line ->
            c.drawText(line, pad, ty, titleP)
            ty += 86f
        }

        // ── 副行：nickname italic 「」 + status pill
        val subRowY = ty + 4f
        var cursorX = pad
        if (item.nickname.isNotBlank()) {
            val nick = paintFor(font = cormorantItalic, size = 38f, color = sub, italic = true)
            val txt = "「${item.nickname}」"
            c.drawText(txt, cursorX, subRowY, nick)
            cursorX += nick.measureText(txt) + 18f
        }
        // status badge
        drawStatusBadge(c, cursorX, subRowY - 32f, item.status, sub = sub, font = mono)

        // ── 一句话简介 oneLiner（terra italic 一行 ellipsize）
        if (item.oneLiner.isNotBlank()) {
            val pen = paintFor(font = cormorantItalic, size = 30f, color = terra, italic = true)
            val text = ellipsize(item.oneLiner, pen, maxTitleW)
            c.drawText(text, pad, subRowY + 56f, pen)
        }

        // ── 4 行 hero specs 表（hairline 分隔）
        val specs = item.heroSpecs.filter { it.label.isNotBlank() }.take(4)
        var specTop = subRowY + 110f
        val specRowH = 78f
        val specRect = RectF(pad, specTop, CARD_W - pad, specTop + specRowH * specs.size.coerceAtLeast(1))
        c.drawRect(specRect, borderP)
        val labelP = paintFor(font = space, size = 26f, color = sub)
        val valueP = paintFor(font = space, size = 32f, color = ink)
        if (specs.isEmpty()) {
            val empty = paintFor(font = cormorantItalic, size = 28f, color = sub, italic = true, align = Paint.Align.CENTER)
            c.drawText("（暂无关键参数）", CARD_W / 2f, specTop + specRowH / 2f + 10f, empty)
        } else {
            specs.forEachIndexed { idx, sp ->
                val rowY = specTop + specRowH * idx
                if (idx > 0) {
                    val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
                    c.drawLine(pad + 28f, rowY, CARD_W - pad - 28f, rowY, hair)
                }
                c.drawText(sp.label, pad + 32f, rowY + specRowH / 2f + 10f, labelP)
                val vp = Paint(valueP).apply { textAlign = Paint.Align.RIGHT }
                val v = ellipsize(sp.value.ifBlank { "—" }, vp, maxTitleW - 280f)
                c.drawText(v, CARD_W - pad - 32f, rowY + specRowH / 2f + 12f, vp)
            }
        }

        // ── 底部装饰条 + 水印行（acquired 日期左 / TREASURE 中 / category 右）
        val bottomY = CARD_H - 132f
        drawOrnament(
            c, sub = sub, ink = ink, line = line,
            cy = bottomY - 36f,
            left = pad, right = CARD_W - pad,
        )

        // 左：入手日期 (acquired) Cormorant italic
        val dateText = formatAcquired(item.acquired)
        val datePaint = paintFor(font = cormorantItalic, size = 26f, color = sub, italic = true)
        c.drawText(dateText, pad, bottomY + 12f, datePaint)

        // 中：TREASURE 字 + est.
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cormorant
            textSize = 44f
            color = ink
            letterSpacing = 0.06f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("Treasure", CARD_W / 2f, bottomY + 4f, brandPaint)
        val estPaint = paintFor(font = mono, size = 16f, color = sub, letterSpacing = 0.30f, align = Paint.Align.CENTER)
        c.drawText("EST · MMXXV", CARD_W / 2f, bottomY + 36f, estPaint)

        // 右：category 中文名（如 "羽毛球 / 摄影"）
        val catRight = paintFor(font = cormorantItalic, size = 26f, color = sub, italic = true, align = Paint.Align.RIGHT)
        c.drawText(categoryNameZh.ifBlank { Category.fromId(item.category).nameZh }, CARD_W - pad, bottomY + 12f, catRight)

        return bmp
    }

    // ── Drawing helpers ────────────────────────────────────────────────

    private fun drawOrnament(
        c: Canvas,
        sub: Int, ink: Int, line: Int,
        cy: Float,
        left: Float,
        right: Float,
    ) {
        val center = (left + right) / 2f
        val gap = 28f
        val ringR = 11f
        val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendAlpha(sub, 0x99)
            strokeWidth = 1.2f
        }
        c.drawLine(left, cy, center - ringR - gap, cy, lineP)
        c.drawLine(center + ringR + gap, cy, right, cy, lineP)

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = sub
            strokeWidth = 1.4f
        }
        c.drawCircle(center, cy, ringR, ring)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = sub
        }
        c.drawCircle(center, cy, 2.4f, dot)

        // 上下小菱形
        val d = 5.2f
        listOf(cy - 22f, cy + 22f).forEach { y ->
            val p = APath().apply {
                moveTo(center, y - d)
                lineTo(center + d, y)
                lineTo(center, y + d)
                lineTo(center - d, y)
                close()
            }
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = blendAlpha(ink, 0x80)
                style = Paint.Style.FILL
            }
            c.drawPath(p, fill)
        }
    }

    /**
     * Hero 框 4 角的细小 tick — 走博物馆装裱风（不画完整框，画 L 形短线）。
     */
    private fun drawCornerTicks(
        c: Canvas,
        rect: RectF,
        ink: Int,
        len: Float,
        gap: Float,
        stroke: Float,
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            strokeWidth = stroke
        }
        // 四角：每角两条 L 线，向内偏 gap
        // 左上
        c.drawLine(rect.left + gap, rect.top + gap, rect.left + gap + len, rect.top + gap, p)
        c.drawLine(rect.left + gap, rect.top + gap, rect.left + gap, rect.top + gap + len, p)
        // 右上
        c.drawLine(rect.right - gap, rect.top + gap, rect.right - gap - len, rect.top + gap, p)
        c.drawLine(rect.right - gap, rect.top + gap, rect.right - gap, rect.top + gap + len, p)
        // 左下
        c.drawLine(rect.left + gap, rect.bottom - gap, rect.left + gap + len, rect.bottom - gap, p)
        c.drawLine(rect.left + gap, rect.bottom - gap, rect.left + gap, rect.bottom - gap - len, p)
        // 右下
        c.drawLine(rect.right - gap, rect.bottom - gap, rect.right - gap - len, rect.bottom - gap, p)
        c.drawLine(rect.right - gap, rect.bottom - gap, rect.right - gap, rect.bottom - gap - len, p)
    }

    private fun drawCroppedPhoto(
        c: Canvas,
        path: String,
        target: RectF,
        crop: PhotoCrop,
        paddingPx: Float,
    ) {
        val raw = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return
        val srcW = raw.width
        val srcH = raw.height
        // 应用 photoCrops（归一化 rect → 像素 rect）
        val left = (crop.x * srcW).toInt().coerceIn(0, srcW - 1)
        val top = (crop.y * srcH).toInt().coerceIn(0, srcH - 1)
        val right = ((crop.x + crop.w) * srcW).toInt().coerceIn(left + 1, srcW)
        val bottom = ((crop.y + crop.h) * srcH).toInt().coerceIn(top + 1, srcH)
        val src = Rect(left, top, right, bottom)

        // 留 padding 后的目标区域，居中、保持 src 宽高比 fit（不裁，怕切到关键内容）
        val cellW = target.width() - paddingPx * 2
        val cellH = target.height() - paddingPx * 2
        val srcRatio = (right - left).toFloat() / (bottom - top)
        val cellRatio = cellW / cellH
        val (drawW, drawH) = if (srcRatio > cellRatio) {
            cellW to cellW / srcRatio
        } else {
            cellH * srcRatio to cellH
        }
        val dst = RectF(
            target.centerX() - drawW / 2f,
            target.centerY() - drawH / 2f,
            target.centerX() + drawW / 2f,
            target.centerY() + drawH / 2f,
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        c.drawBitmap(raw, src, dst, p)
        raw.recycle()
    }

    /**
     * 没真实照片时的 hero 占位 — 不重做 16 张 Compose 线描，画一枚简洁的
     * 博物馆徽章：双重圆环 + 中心 HeroVector 的英文名缩写 + 下方分类英文。
     * 风格统一 line-stroke，跟整张卡的"图鉴版面"自洽。
     */
    private fun drawHeroEmblem(
        c: Canvas,
        rect: RectF,
        ink: Int,
        sub: Int,
        terra: Int,
        font: Typeface,
        hv: HeroVector,
    ) {
        val cx = rect.centerX()
        val cy = rect.centerY() - 12f
        val rOuter = (rect.height() / 2f) - 64f
        val rInner = rOuter - 16f

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendAlpha(ink, 0xCC)
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        c.drawCircle(cx, cy, rOuter, ring)
        val ringIn = Paint(ring).apply { strokeWidth = 1.2f; color = blendAlpha(ink, 0x70) }
        c.drawCircle(cx, cy, rInner, ringIn)

        // 中心字符（HeroVector 的首字母 / 双字母）
        val initials = hvInitials(hv)
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = if (initials.length <= 2) 220f else 170f
            color = ink
            textAlign = Paint.Align.CENTER
        }
        // baseline 居中：textSize 一半再加些视觉补偿
        val baselineOffset = (big.descent() + big.ascent()) / 2f
        c.drawText(initials, cx, cy - baselineOffset, big)

        // 下方一行小字 — HeroVector 英文
        val sm = paintFor(
            font = Typeface.DEFAULT,
            size = 26f,
            color = sub,
            letterSpacing = 0.25f,
            align = Paint.Align.CENTER,
        )
        c.drawText(hv.name.replace('_', ' '), cx, cy + rOuter + 50f, sm)

        // 顶部 ✦ 标记
        val sparkle = paintFor(font = font, size = 36f, color = terra, align = Paint.Align.CENTER)
        c.drawText("✦", cx, cy - rOuter - 28f, sparkle)
    }

    private fun hvInitials(hv: HeroVector): String = when (hv) {
        HeroVector.RACKET -> "R"
        HeroVector.SHOES -> "S"
        HeroVector.CAMERA_DSLR -> "C"
        HeroVector.CAMERA_RANGEFINDER -> "Cr"
        HeroVector.LENS_PRIME -> "L"
        HeroVector.TRIPOD -> "T"
        HeroVector.CAR_SEDAN -> "A"
        HeroVector.CAR_SUV -> "V"
        HeroVector.LAPTOP -> "M"
        HeroVector.TABLET -> "P"
        HeroVector.KINDLE -> "K"
        HeroVector.EARBUDS -> "E"
        HeroVector.WATCH -> "W"
        HeroVector.ESPRESSO_MACHINE -> "Es"
        HeroVector.COFFEE_GRINDER -> "Gr"
        HeroVector.COFFEE_BEAN -> "B"
        HeroVector.WINE_BOTTLE -> "Wb"
        HeroVector.COCKTAIL_GLASS -> "Cg"
        HeroVector.GENERIC -> "T"
    }

    private fun drawStatusBadge(
        c: Canvas,
        x: Float,
        y: Float,
        status: ItemStatus,
        sub: Int,
        font: Typeface,
    ) {
        val label = when (status) {
            ItemStatus.OWNED -> "OWNED"
            ItemStatus.PARTED -> "PARTED"
            ItemStatus.RENTED -> "RENTED"
        }
        val p = paintFor(font = font, size = 18f, color = sub, letterSpacing = 0.25f)
        val padH = 12f
        val padV = 7f
        val tw = p.measureText(label)
        val rect = RectF(x, y, x + tw + padH * 2, y + 32f + padV * 2)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = sub
            strokeWidth = 1f
        }
        c.drawRect(rect, border)
        c.drawText(label, x + padH, rect.bottom - padV - 6f, p)
    }

    // ── Misc ───────────────────────────────────────────────────────────

    private fun paintFor(
        font: Typeface,
        size: Float,
        color: Int,
        letterSpacing: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
        italic: Boolean = false,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = font
        this.textSize = size
        this.color = color
        this.letterSpacing = letterSpacing
        this.textAlign = align
        if (italic) this.textSkewX = -0.18f
    }

    private fun loadFont(context: Context, fontResId: Int): Typeface {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT
        } catch (_: Throwable) {
            Typeface.DEFAULT
        }
    }

    private fun blendAlpha(rgbColor: Int, alpha: Int): Int {
        // rgbColor 可能本身有 alpha；这里强制把 alpha 改成参数值
        val r = (rgbColor shr 16) and 0xFF
        val g = (rgbColor shr 8) and 0xFF
        val b = rgbColor and 0xFF
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }

    private fun wrapForWidth(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLines: Int,
    ): List<String> {
        if (paint.measureText(text) <= maxWidth || maxLines <= 1) {
            return listOf(ellipsize(text, paint, maxWidth))
        }
        val out = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty() && out.size < maxLines - 1) {
            var end = remaining.length
            while (end > 0 && paint.measureText(remaining.substring(0, end)) > maxWidth) {
                end--
            }
            if (end == 0) break
            // 尽量在最近的空格断行（西文友好）
            val space = remaining.lastIndexOf(' ', end - 1)
            val cut = if (space > end / 2) space else end
            out += remaining.substring(0, cut).trim()
            remaining = remaining.substring(cut).trim()
        }
        if (remaining.isNotEmpty()) {
            out += ellipsize(remaining, paint, maxWidth)
        }
        return out
    }

    private fun formatAcquired(raw: String): String {
        // raw 是 YYYY-MM-DD；输出 "2026 / 05 / 12 · ACQUIRED"
        val parsed = runCatching { LocalDate.parse(raw) }.getOrNull()
        return if (parsed != null) {
            val df = DateTimeFormatter.ofPattern("yyyy / MM / dd", Locale.ROOT)
            "${parsed.format(df)} · ACQUIRED"
        } else if (raw.isNotBlank()) {
            "$raw · ACQUIRED"
        } else {
            "ACQUIRED"
        }
    }

    /**
     * 给分类一个稳定的"展厅 №" — 6 个内建 enum 各占 I..VI；自定义分类按
     * id hash 落 VII..XII 之间，纯装饰。
     */
    private fun roomNumeral(categoryId: String): String {
        val builtIn = listOf("badminton" to "I", "photo" to "II", "cars" to "III",
            "tech" to "IV", "coffee" to "V", "wine" to "VI")
        builtIn.firstOrNull { it.first == categoryId }?.let { return it.second }
        val extra = listOf("VII", "VIII", "IX", "X", "XI", "XII")
        val idx = ((categoryId.hashCode() and 0x7FFFFFFF) % extra.size)
        return extra[idx]
    }
}
