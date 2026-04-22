package com.swmansion.enriched.markdown.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import com.swmansion.enriched.markdown.parser.MarkdownASTNode
import com.swmansion.enriched.markdown.renderer.Renderer
import com.swmansion.enriched.markdown.renderer.SpanStyleCache
import com.swmansion.enriched.markdown.styles.CodeBlockStyle
import com.swmansion.enriched.markdown.styles.StyleConfig
import kotlin.math.ceil

/**
 * A block-level code container that wraps code content in a HorizontalScrollView,
 * allowing wide code lines to scroll horizontally instead of wrapping.
 */
class CodeBlockContainerView(
  context: Context,
  private val styleConfig: StyleConfig,
) : FrameLayout(context),
  BlockSegmentView {

  private val codeStyle: CodeBlockStyle = styleConfig.codeBlockStyle
  override val segmentMarginTop: Int get() = codeStyle.marginTop.toInt()
  override val segmentMarginBottom: Int get() = codeStyle.marginBottom.toInt()

  private val density = resources.displayMetrics.density
  private val padding = codeStyle.padding.toInt()

  private val scrollView =
    object : HorizontalScrollView(context) {
      private var startX = 0f
      private var startY = 0f
      private var didDisallow = false

      override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
          MotionEvent.ACTION_DOWN -> {
            startX = ev.x
            startY = ev.y
            didDisallow = false
            parent?.requestDisallowInterceptTouchEvent(true)
          }
          MotionEvent.ACTION_MOVE -> {
            val dx = Math.abs(ev.x - startX)
            val dy = Math.abs(ev.y - startY)
            if (!didDisallow && dx > dy && dx > 8f * density) {
              didDisallow = true
              parent?.requestDisallowInterceptTouchEvent(true)
            } else if (!didDisallow && dy > dx && dy > 8f * density) {
              parent?.requestDisallowInterceptTouchEvent(false)
            }
          }
          MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            parent?.requestDisallowInterceptTouchEvent(false)
            didDisallow = false
          }
        }
        return super.dispatchTouchEvent(ev)
      }

      init {
        isHorizontalScrollBarEnabled = true
        overScrollMode = View.OVER_SCROLL_NEVER
      }
    }

  private val backgroundView = BackgroundView(context)
  private var contentHeight = 0

  init {
    backgroundView.configure(codeStyle)
    addView(backgroundView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
  }

  fun applyCodeBlockNode(node: MarkdownASTNode) {
    scrollView.removeAllViews()

    val text = extractCodeText(node)
    val typeface = resolveMonospaceTypeface()

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = codeStyle.fontSize
      color = codeStyle.color
      this.typeface = typeface
    }

    // Measure natural (unwrapped) width by finding the longest line
    val lines = text.split("\n")
    var maxLineWidth = 0f
    for (line in lines) {
      val w = textPaint.measureText(line)
      if (w > maxLineWidth) maxLineWidth = w
    }
    val naturalWidth = ceil(maxLineWidth).toInt() + padding * 2

    // Build layout with natural width (no wrapping)
    val layout = StaticLayout.Builder
      .obtain(text, 0, text.length, textPaint, naturalWidth)
      .setIncludePad(false)
      .build()

    contentHeight = layout.height + padding * 2

    val codeTextView = CodeTextView(context).apply {
      this.text = text
      this.textSize = codeStyle.fontSize / resources.displayMetrics.scaledDensity
      this.typeface = typeface
      setTextColor(codeStyle.color)
      setPadding(padding, padding, padding, padding)
      // Disable line wrapping — content scrolls horizontally
      isSingleLine = false
      maxLines = Int.MAX_VALUE
      setHorizontallyScrolling(true)
    }

    val innerFrame = FrameLayout(context).apply {
      addView(codeTextView, LayoutParams(naturalWidth, LayoutParams.WRAP_CONTENT))
    }
    scrollView.addView(innerFrame, LayoutParams(naturalWidth, LayoutParams.WRAP_CONTENT))
  }

  private fun extractCodeText(node: MarkdownASTNode): String {
    val sb = StringBuilder()
    fun walk(n: MarkdownASTNode) {
      if (n.content.isNotEmpty()) sb.append(n.content)
      for (child in n.children) walk(child)
    }
    walk(node)
    // Trim trailing newline that md4c appends
    if (sb.isNotEmpty() && sb.last() == '\n') sb.deleteCharAt(sb.length - 1)
    return sb.toString()
  }

  private fun resolveMonospaceTypeface(): Typeface {
    SpanStyleCache.initAssetFonts(context)
    if (codeStyle.fontFamily.isNotEmpty() && codeStyle.fontFamily != "monospace") {
      val cached = SpanStyleCache.getTypeface(codeStyle.fontFamily, Typeface.NORMAL)
      if (cached != Typeface.DEFAULT) return cached
    }
    return SpanStyleCache.getMonospaceTypeface(Typeface.NORMAL)
  }

  override fun onMeasure(widthSpec: Int, heightSpec: Int) {
    val measuredWidth = MeasureSpec.getSize(widthSpec)
    val h = contentHeight.coerceAtLeast(1)
    backgroundView.measure(
      MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY),
    )
    scrollView.measure(
      MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY),
    )
    setMeasuredDimension(measuredWidth, h)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val w = right - left
    val h = bottom - top
    backgroundView.layout(0, 0, w, h)
    scrollView.layout(0, 0, w, h)
  }

  companion object {
    fun measureCodeBlockNodeHeight(
      node: MarkdownASTNode,
      config: StyleConfig,
      context: Context,
    ): Float {
      val style = config.codeBlockStyle
      val padding = style.padding.toInt()
      SpanStyleCache.initAssetFonts(context)
      val typeface = SpanStyleCache.getMonospaceTypeface(Typeface.NORMAL)
      val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = style.fontSize
        this.typeface = typeface
      }

      val text = buildString {
        fun walk(n: MarkdownASTNode) {
          if (n.content.isNotEmpty()) append(n.content)
          for (child in n.children) walk(child)
        }
        walk(node)
        if (isNotEmpty() && last() == '\n') deleteCharAt(length - 1)
      }

      var maxLineWidth = 0f
      for (line in text.split("\n")) {
        val width = paint.measureText(line)
        if (width > maxLineWidth) maxLineWidth = width
      }
      val naturalWidth = maxOf(1, ceil(maxLineWidth).toInt() + padding * 2)

      // Match the actual code block rendering path: no wrapping, horizontal scroll.
      val layout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, naturalWidth)
        .setIncludePad(false)
        .build()

      return (layout.height + padding * 2).toFloat()
    }
  }

  /** Draws the code block background with rounded corners and optional border. */
  private class BackgroundView(context: Context) : View(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private var radius = 0f
    private val rect = RectF()

    fun configure(style: CodeBlockStyle) {
      bgPaint.color = style.backgroundColor
      borderPaint.color = style.borderColor
      borderPaint.strokeWidth = style.borderWidth
      radius = style.borderRadius
    }

    override fun onDraw(canvas: Canvas) {
      rect.set(0f, 0f, width.toFloat(), height.toFloat())
      if (radius > 0f) {
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        if (borderPaint.strokeWidth > 0f) {
          val half = borderPaint.strokeWidth / 2
          rect.inset(half, half)
          canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }
      } else {
        canvas.drawRect(rect, bgPaint)
        if (borderPaint.strokeWidth > 0f) {
          val half = borderPaint.strokeWidth / 2
          rect.inset(half, half)
          canvas.drawRect(rect, borderPaint)
        }
      }
    }
  }

  private class CodeTextView(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {
    init {
      includeFontPadding = false
    }
  }
}
