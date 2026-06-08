package com.swmansion.enriched.markdown.spans

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import com.swmansion.enriched.markdown.renderer.BlockStyle
import com.swmansion.enriched.markdown.renderer.SpanStyleCache
import com.swmansion.enriched.markdown.utils.text.extensions.applyBlockStyleFont
import com.swmansion.enriched.markdown.utils.text.extensions.applyColorPreserving

abstract class BaseListSpan(
  val depth: Int,
  protected val context: Context,
  protected val styleCache: SpanStyleCache,
  protected val blockStyle: BlockStyle,
  protected val marginLeft: Float,
  protected val gapWidth: Float,
) : MetricAffectingSpan(),
  LeadingMarginSpan {
  private var cachedText: CharSequence? = null
  private var cachedHasDeeperSpanByPosition = mutableMapOf<Int, Boolean>()

  protected abstract fun getMarkerWidth(): Float

  override fun updateMeasureState(textPaint: TextPaint) = applyTextStyle(textPaint)

  override fun updateDrawState(textPaint: TextPaint) = applyTextStyle(textPaint)

  protected fun effectiveGap(): Float = gapWidth.coerceAtLeast(DEFAULT_MIN_GAP)

  // Where the text content actually begins, after all accumulated leading
  // margins. Anchoring markers to this (rather than recomputing depth*marginLeft
  // + markerWidth) keeps them clear of the text at every nesting depth. The
  // fallback preserves the depth-0 position when no layout is available.
  protected fun textContentStart(
    layout: Layout?,
    start: Int,
    fallback: Float,
  ): Float = layout?.getPrimaryHorizontal(start) ?: fallback

  override fun getLeadingMargin(first: Boolean): Int =
    if (depth == 0) {
      (getMarkerWidth() + effectiveGap()).toInt()
    } else {
      marginLeft.toInt()
    }

  override fun drawLeadingMargin(
    canvas: Canvas,
    paint: Paint,
    x: Int,
    dir: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    text: CharSequence?,
    start: Int,
    end: Int,
    first: Boolean,
    layout: Layout?,
  ) {
    if (!first || shouldSkipDrawing(text, start) || !hasContent(text, start, end)) return

    // Only draw the marker on the true first line of this span.
    // \n inside list item content creates inner paragraphs, each with first=true,
    // but only the line at the span start should get a bullet/number.
    if (text is Spanned && text.getSpanStart(this) != start) return

    val originalStyle = paint.style
    val originalColor = paint.color
    drawMarker(canvas, paint, x, dir, top, baseline, bottom, layout, start)
    paint.style = originalStyle
    paint.color = originalColor
  }

  protected abstract fun drawMarker(
    canvas: Canvas,
    paint: Paint,
    x: Int,
    dir: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    layout: Layout?,
    start: Int,
  )

  @SuppressLint("WrongConstant")
  private fun applyTextStyle(textPaint: TextPaint) {
    // Skip font override if the paint already carries a monospace typeface set by CodeSpan —
    // otherwise BaseListSpan (which wraps the entire list item) would clobber both the
    // code typeface and code-specific font size inside list items.
    val isMonospace = SpanStyleCache.isMonospaceTypeface(textPaint.typeface)
    if (!isMonospace) {
      textPaint.textSize = blockStyle.fontSize
      val preservedStyle = (textPaint.typeface?.style ?: 0) and BOLD_ITALIC_MASK
      textPaint.applyBlockStyleFont(blockStyle, context)
      if (preservedStyle != 0) {
        textPaint.typeface?.let { base -> textPaint.typeface = Typeface.create(base, preservedStyle) }
      }
    }

    textPaint.applyColorPreserving(blockStyle.color, *styleCache.colorsToPreserve)
  }

  companion object {
    private const val BOLD_ITALIC_MASK = Typeface.BOLD or Typeface.ITALIC
    private const val DEFAULT_MIN_GAP = 4f
  }

  private fun hasContent(
    text: CharSequence?,
    start: Int,
    end: Int,
  ): Boolean {
    if (text == null || end <= start) return false
    return (start until end).any { !text[it].isWhitespace() }
  }

  private fun shouldSkipDrawing(
    text: CharSequence?,
    start: Int,
  ): Boolean {
    if (text !is Spanned) return false

    if (cachedText !== text) {
      cachedText = text
      cachedHasDeeperSpanByPosition.clear()
    }

    return cachedHasDeeperSpanByPosition.getOrPut(start) {
      val spans = text.getSpans(start, start + 1, BaseListSpan::class.java)
      spans.any { it.depth > depth }
    }
  }
}
