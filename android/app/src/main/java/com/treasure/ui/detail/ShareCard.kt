package com.treasure.ui.detail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as ACol
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.treasure.R
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item
import com.treasure.core.domain.PhotoCrop
import java.io.File
import java.io.FileOutputStream

/**
 * Cycle 0038 v3 — Detail 分享卡片。
 *
 * 1920 × 1080（**16:9 横向**）。左 1/3 hero 垂直居中，右 2/3 上标题下参数。
 * 不再有底部水印栏，不再上历史。
 *
 *  ┌─────────────────────────────────────────────────────────────────┐ y=0
 *  │                                                                 │
 *  │   ┌────────────────┐   CATEGORY · ROOM № X                      │ y=140 (cat baseline)
 *  │   │                │                                            │
 *  │   │                │   Brand                                    │ y=240
 *  │   │                │   Model                                    │ y=332
 *  │   │      HERO      │                                            │
 *  │   │   (vertical    │   「nickname」 [STATUS]                    │ y=400
 *  │   │     center)    │   one-liner italic terra                   │ y=448
 *  │   │                │                                            │
 *  │   │                │   ─── 参数 · SPECS ───────────────         │ y=540
 *  │   │                │   label                          value     │ y=605
 *  │   │                │   label                          value     │ y=675
 *  │   │                │   label                          value     │ y=745
 *  │   │                │   label                          value     │ y=815
 *  │   │                │   label                          value     │ y=885
 *  │   └────────────────┘   label                          value     │ y=955
 *  │                                                                 │
 *  └─────────────────────────────────────────────────────────────────┘ y=1080
 *  x=0   x=80      x=700  x=780                                x=1840  x=1920
 *        ←─ 1/3 hero ──→  ←──── gutter 80 ────→ ←─── 右 2/3 ───→
 *
 * 边距 / 行距规格（统一在常量里，方便手调）：
 *   - 卡片外边距 80（上下左右一致 → 等于 paper 留白）
 *   - 左 hero 宽 620（≈ 32% 卡宽），垂直居中
 *   - 中央 gutter 80（视觉气口）
 *   - 右文字栏宽 1060（剩余）
 *   - 标题首行 (`CATEGORY · ROOM`) 距卡顶 140（顶部留白）
 *   - brand+model 行高 92（Cormorant 78sp，1.18 行距）
 *   - 行块间距：title → nickname 60；nickname → one-liner 50
 *   - 标题段 → SECTION header 90（视觉断层）
 *   - SECTION header → 第 1 行 spec 35（divider 之下）
 *   - spec 行高 70（label 24sp + value 28sp 居中，hairline 0.8px）
 *
 * caller 传入 `selectedSpecs`（最多 [MAX_SPECS] 条）；不再接受 history。
 */
object ShareCard {

    /** 卡片像素尺寸：1920 × 1080。 */
    const val CARD_W = 1920
    const val CARD_H = 1080

    /**
     * 上卡参数行数上限。
     *
     * 右栏可用垂直高度 ≈ 430px（从 SECTION header 之下到卡底留白上沿）；行高
     * 70px。6 × 70 = 420px ≈ 刚好。再多（7、8）会把字号压扁或挤进底边，
     * 看着信息过载、丢博物馆图鉴的"克制"调性；少（3、4）则参数稀薄、卡片
     * 整体偏空。
     */
    const val MAX_SPECS = 6

    fun generate(
        context: Context,
        item: Item,
        selectedSpecs: List<HeroSpec>,
    ): File {
        val bmp = renderBitmap(
            context, item,
            specs = selectedSpecs.take(MAX_SPECS),
        )
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

    // ─── Layout constants（卡片几何，方便后续调整） ──────────────────────

    /** 上下左右外边距（卡片到画布边）。 */
    private const val OUTER_PAD = 80f

    /** Hero 正方形边长。720 让 hero 像照片裱框，纵向上下各留 180px 让卡片
     *  整体上下气口比左右更宽，有挂画的"画轴感"。 */
    private const val HERO_SIDE = 720f

    /** Hero 在画布上的几何（垂直居中、左对齐 OUTER_PAD）。 */
    private const val HERO_LEFT = OUTER_PAD
    private const val HERO_TOP = (CARD_H - HERO_SIDE) / 2f              // 180
    private const val HERO_RIGHT = HERO_LEFT + HERO_SIDE                // 800
    private const val HERO_BOTTOM = HERO_TOP + HERO_SIDE                // 900

    /** 中央两栏间气口。 */
    private const val GUTTER = 80f

    /** 右文字栏 left x（= HERO_RIGHT + GUTTER）。 */
    private const val RIGHT_LEFT = HERO_RIGHT + GUTTER                   // 880
    private const val RIGHT_RIGHT = CARD_W - OUTER_PAD                   // 1840
    private const val RIGHT_WIDTH = RIGHT_RIGHT - RIGHT_LEFT             // 960

    /** brand+model 标题首行 baseline — 跟 hero 顶 (y=180) 视觉对齐略低。 */
    private const val TITLE_BASELINE_Y = 210f
    private const val TITLE_LINE_H = 100f                                // 84sp × 1.19

    /** nickname 行 baseline 相对 title block 末；one-liner 跟着。 */
    private const val GAP_TITLE_TO_NICK = 60f
    private const val GAP_NICK_TO_ONELINER = 50f

    /** 标题块 → SECTION header 的视觉断层。 */
    private const val GAP_HEADER_TO_SECTION = 80f
    private const val GAP_SECTION_TO_FIRST_SPEC = 40f

    /** 参数行高（行内垂直居中）。 */
    private const val SPEC_ROW_H = 70f

    // ─── Rendering ──────────────────────────────────────────────────────

    private fun renderBitmap(
        context: Context,
        item: Item,
        specs: List<HeroSpec>,
    ): Bitmap {
        // 永远浅色出图 — 博物馆纸调，不跟系统深色。
        val paper = ACol.parseColor("#F4F1EA")
        val ink = ACol.parseColor("#1A1815")
        val terra = ACol.parseColor("#8A3A1F")
        val card = ACol.parseColor("#FBF9F4")
        val sub = blendAlpha(ink, 0x8C)
        val line = blendAlpha(ink, 0x1A)

        val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(paper)

        val cormorant = loadFont(context, R.font.cormorant_garamond)
        val cormorantItalic = loadFont(context, R.font.cormorant_garamond_italic)
        val space = loadFont(context, R.font.space_grotesk)
        val mono = loadFont(context, R.font.jetbrains_mono)

        // ── 左 hero 区：正方形 720×720 垂直居中 ──────────────────
        val heroRect = RectF(HERO_LEFT, HERO_TOP, HERO_RIGHT, HERO_BOTTOM)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = card }
        c.drawRect(heroRect, fillP)
        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = line; strokeWidth = 1.4f
        }
        c.drawRect(heroRect, borderP)
        drawCornerTicks(c, heroRect, ink = sub, len = 28f, gap = 18f, stroke = 1.4f)

        val heroPath = item.avatarPhotoPath
        if (!heroPath.isNullOrBlank() && File(heroPath).exists()) {
            val crop = item.photoCrops[heroPath] ?: PhotoCrop.Full
            drawCroppedPhoto(c, heroPath, heroRect, crop, paddingPx = 50f)
        } else {
            drawHeroEmblem(c, heroRect, ink = ink, sub = sub, terra = terra,
                font = cormorant, hv = item.heroVector)
        }
        if (item.photos.size > 1) {
            val tag = paintFor(font = mono, size = 20f, color = sub,
                letterSpacing = 0.15f, align = Paint.Align.RIGHT)
            c.drawText("${item.photos.size} PHOTOS", heroRect.right - 28f,
                heroRect.bottom - 28f, tag)
        }

        // ── 右文字栏 ───────────────────────────────────────────────
        // 1) brand + model — Cormorant 84sp 大字，最多 2 行
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = cormorant
            textSize = 84f
            color = ink
            letterSpacing = -0.02f
            textAlign = Paint.Align.LEFT
        }
        val brandModel = "${item.brand} ${item.model}".trim()
            .ifBlank { item.nickname.ifBlank { "Treasure" } }
        val titleLines = wrapForWidth(brandModel, titleP, RIGHT_WIDTH, maxLines = 2)
        var titleY = TITLE_BASELINE_Y
        titleLines.forEach { lineText ->
            c.drawText(lineText, RIGHT_LEFT, titleY, titleP)
            titleY += TITLE_LINE_H
        }
        var cursorY = titleY - TITLE_LINE_H + GAP_TITLE_TO_NICK

        // 2) 「nickname」 — italic，单独一行（STATUS badge cycle 0038 v4 起删）
        if (item.nickname.isNotBlank()) {
            val nick = paintFor(font = cormorantItalic, size = 36f, color = sub, italic = true)
            c.drawText("「${item.nickname}」", RIGHT_LEFT, cursorY, nick)
            cursorY += GAP_NICK_TO_ONELINER
        }

        // 4) one-liner — terra italic 单行 ellipsize
        if (item.oneLiner.isNotBlank()) {
            val pen = paintFor(font = cormorantItalic, size = 28f, color = terra, italic = true)
            c.drawText(ellipsize(item.oneLiner, pen, RIGHT_WIDTH), RIGHT_LEFT, cursorY, pen)
        }
        cursorY += GAP_HEADER_TO_SECTION   // 进入参数段

        // 5) SECTION header — 中文标题 + latin 副字 + hairline divider
        drawSectionHeader(
            c, title = "参数 · SPECS",
            left = RIGHT_LEFT, right = RIGHT_RIGHT, y = cursorY,
            font = mono, fontZh = cormorant,
            sub = sub, ink = ink, line = line,
        )
        cursorY += GAP_SECTION_TO_FIRST_SPEC + 8f   // header 高度 + 间距

        // 6) spec rows — 最多 MAX_SPECS 条；hairline 分隔；行内文字垂直居中
        val specsToShow = specs.filter { it.label.isNotBlank() }.take(MAX_SPECS)
        if (specsToShow.isEmpty()) {
            val empty = paintFor(font = cormorantItalic, size = 26f, color = sub, italic = true)
            c.drawText("（暂无关键参数）", RIGHT_LEFT, cursorY + 32f, empty)
        } else {
            val labelP = paintFor(font = space, size = 24f, color = sub)
            val valueP = paintFor(font = space, size = 28f, color = ink, align = Paint.Align.RIGHT)
            specsToShow.forEachIndexed { idx, sp ->
                val rowTop = cursorY + SPEC_ROW_H * idx
                val rowMid = rowTop + SPEC_ROW_H / 2f
                if (idx > 0) {
                    val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = 0.8f }
                    c.drawLine(RIGHT_LEFT, rowTop, RIGHT_RIGHT, rowTop, hair)
                }
                // label 居中：baseline 用 mid + 字体度量修正
                c.drawText(sp.label, RIGHT_LEFT, baselineCenter(rowMid, labelP), labelP)
                val v = ellipsize(sp.value.ifBlank { "—" }, valueP, RIGHT_WIDTH - 260f)
                c.drawText(v, RIGHT_RIGHT, baselineCenter(rowMid, valueP), valueP)
            }
        }

        return bmp
    }

    /** baseline 算法：把文字"目测中心"放到 [centerY] 上 — 字体 descent/ascent 半角度量修正。 */
    private fun baselineCenter(centerY: Float, paint: Paint): Float =
        centerY - (paint.descent() + paint.ascent()) / 2f

    // ── Section header ────────────────────────────────────────────────
    private fun drawSectionHeader(
        c: Canvas, title: String,
        left: Float, right: Float, y: Float,
        font: Typeface, fontZh: Typeface,
        sub: Int, ink: Int, line: Int,
    ) {
        val parts = title.split(" · ")
        val zh = parts.firstOrNull().orEmpty()
        val en = parts.getOrNull(1).orEmpty()
        val zhP = paintFor(font = fontZh, size = 28f, color = ink, letterSpacing = 0.04f)
        val enP = paintFor(font = font, size = 18f, color = sub, letterSpacing = 0.30f)
        c.drawText(zh, left, y + 22f, zhP)
        val zhW = zhP.measureText(zh)
        c.drawText(en, left + zhW + 14f, y + 22f, enP)
        val divP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendAlpha(sub, 0x55); strokeWidth = 0.8f
        }
        c.drawLine(left, y + 35f, right, y + 35f, divP)
    }

    // ── Hero 4 角 tick ────────────────────────────────────────────────
    private fun drawCornerTicks(
        c: Canvas, rect: RectF, ink: Int,
        len: Float, gap: Float, stroke: Float,
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; strokeWidth = stroke }
        c.drawLine(rect.left + gap, rect.top + gap, rect.left + gap + len, rect.top + gap, p)
        c.drawLine(rect.left + gap, rect.top + gap, rect.left + gap, rect.top + gap + len, p)
        c.drawLine(rect.right - gap, rect.top + gap, rect.right - gap - len, rect.top + gap, p)
        c.drawLine(rect.right - gap, rect.top + gap, rect.right - gap, rect.top + gap + len, p)
        c.drawLine(rect.left + gap, rect.bottom - gap, rect.left + gap + len, rect.bottom - gap, p)
        c.drawLine(rect.left + gap, rect.bottom - gap, rect.left + gap, rect.bottom - gap - len, p)
        c.drawLine(rect.right - gap, rect.bottom - gap, rect.right - gap - len, rect.bottom - gap, p)
        c.drawLine(rect.right - gap, rect.bottom - gap, rect.right - gap, rect.bottom - gap - len, p)
    }

    private fun drawCroppedPhoto(
        c: Canvas, path: String, target: RectF, crop: PhotoCrop, paddingPx: Float,
    ) {
        val raw = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return
        val srcW = raw.width
        val srcH = raw.height
        val left = (crop.x * srcW).toInt().coerceIn(0, srcW - 1)
        val top = (crop.y * srcH).toInt().coerceIn(0, srcH - 1)
        val right = ((crop.x + crop.w) * srcW).toInt().coerceIn(left + 1, srcW)
        val bottom = ((crop.y + crop.h) * srcH).toInt().coerceIn(top + 1, srcH)
        val src = Rect(left, top, right, bottom)

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
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true; isDither = true }
        c.drawBitmap(raw, src, dst, p)
        raw.recycle()
    }

    private fun drawHeroEmblem(
        c: Canvas, rect: RectF,
        ink: Int, sub: Int, terra: Int,
        font: Typeface, hv: HeroVector,
    ) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        // hero 区瘦长（620×920）— 圆环半径取较短边 *0.42 让上下都留气口
        val rOuter = minOf(rect.width(), rect.height()) * 0.42f
        val rInner = rOuter - 18f

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blendAlpha(ink, 0xCC); style = Paint.Style.STROKE; strokeWidth = 2.4f
        }
        c.drawCircle(cx, cy, rOuter, ring)
        val ringIn = Paint(ring).apply { strokeWidth = 1.4f; color = blendAlpha(ink, 0x70) }
        c.drawCircle(cx, cy, rInner, ringIn)

        val initials = hvInitials(hv)
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = if (initials.length <= 2) 220f else 170f
            color = ink
            textAlign = Paint.Align.CENTER
        }
        val baselineOffset = (big.descent() + big.ascent()) / 2f
        c.drawText(initials, cx, cy - baselineOffset, big)

        val sm = paintFor(
            font = Typeface.DEFAULT, size = 26f, color = sub,
            letterSpacing = 0.28f, align = Paint.Align.CENTER,
        )
        c.drawText(hv.name.replace('_', ' '), cx, cy + rOuter + 50f, sm)

        val sparkle = paintFor(font = font, size = 40f, color = terra, align = Paint.Align.CENTER)
        c.drawText("✦", cx, cy - rOuter - 30f, sparkle)
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

    // ── Misc ──────────────────────────────────────────────────────────

    private fun paintFor(
        font: Typeface, size: Float, color: Int,
        letterSpacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT,
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
        text: String, paint: Paint, maxWidth: Float, maxLines: Int,
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

}
