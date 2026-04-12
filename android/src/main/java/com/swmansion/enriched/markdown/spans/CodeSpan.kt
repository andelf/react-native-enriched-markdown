package com.swmansion.enriched.markdown.spans

import android.content.Context
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.markdown.renderer.BlockStyle
import com.swmansion.enriched.markdown.renderer.SpanStyleCache

class CodeSpan(
  private val styleCache: SpanStyleCache,
  private val blockStyle: BlockStyle,
  private val context: Context? = null,
) : MetricAffectingSpan() {
  override fun updateDrawState(tp: TextPaint) {
    applyMonospacedFont(tp)
    tp.color = styleCache.codeColor
  }

  override fun updateMeasureState(tp: TextPaint) {
    applyMonospacedFont(tp)
  }

  private fun applyMonospacedFont(paint: TextPaint) {
    paint.textSize = if (styleCache.codeFontSize > 0) styleCache.codeFontSize else blockStyle.fontSize
    val preservedStyle = (paint.typeface?.style ?: 0) and (Typeface.BOLD or Typeface.ITALIC)

    // Try loading bundled monospace font directly from assets
    val assetTypeface = context?.let { loadAssetMonospace(it, preservedStyle) }
    paint.typeface = assetTypeface
      ?: if (styleCache.codeFontFamily.isNotEmpty()) {
        SpanStyleCache.getTypeface(styleCache.codeFontFamily, preservedStyle)
      } else {
        SpanStyleCache.getMonospaceTypeface(preservedStyle)
      }
  }

  companion object {
    private var cachedRegular: Typeface? = null
    private var cachedBold: Typeface? = null
    private var cachedItalic: Typeface? = null
    private var cachedBoldItalic: Typeface? = null
    private var loadAttempted = false

    private fun loadAssetMonospace(context: Context, style: Int): Typeface? {
      if (!loadAttempted) {
        loadAttempted = true
        try {
          val assets = context.assets
          cachedRegular = Typeface.createFromAsset(assets, "fonts/JetBrainsMono-Regular.ttf")
          cachedBold = Typeface.createFromAsset(assets, "fonts/JetBrainsMono-Bold.ttf")
          cachedItalic = Typeface.createFromAsset(assets, "fonts/JetBrainsMono-Italic.ttf")
          cachedBoldItalic = Typeface.createFromAsset(assets, "fonts/JetBrainsMono-BoldItalic.ttf")
        } catch (_: Exception) {
          // Font files not bundled
        }
      }
      val isBold = (style and Typeface.BOLD) != 0
      val isItalic = (style and Typeface.ITALIC) != 0
      return when {
        isBold && isItalic -> cachedBoldItalic
        isBold -> cachedBold
        isItalic -> cachedItalic
        else -> cachedRegular
      } ?: cachedRegular
    }
  }
}
