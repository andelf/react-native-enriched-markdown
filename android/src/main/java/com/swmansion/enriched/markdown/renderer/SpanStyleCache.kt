package com.swmansion.enriched.markdown.renderer

import android.content.Context
import android.graphics.Typeface
import com.swmansion.enriched.markdown.styles.StyleConfig

/** Shared style cache for spans to avoid redundant calculations. */
class SpanStyleCache(
  style: StyleConfig,
) {
  // Colors to preserve when applying inline styles (links, code, strong, emphasis)
  val colorsToPreserve: IntArray = buildColorsToPreserve(style)

  val strongFontFamily: String = style.strongStyle.fontFamily
  val strongFontWeight: String = style.strongStyle.fontWeight
  val strongColor: Int? = style.strongStyle.color
  val emphasisFontFamily: String = style.emphasisStyle.fontFamily
  val emphasisFontStyle: String = style.emphasisStyle.fontStyle
  val emphasisColor: Int? = style.emphasisStyle.color
  val strikethroughColor: Int = style.strikethroughStyle.color
  val linkFontFamily: String = style.linkStyle.fontFamily
  val linkColor: Int = style.linkStyle.color
  val linkUnderline: Boolean = style.linkStyle.underline
  val codeFontFamily: String = style.codeStyle.fontFamily
  val codeFontSize: Float = style.codeStyle.fontSize
  val codeColor: Int = style.codeStyle.color

  private fun buildColorsToPreserve(style: StyleConfig): IntArray {
    val paragraphColor = style.paragraphStyle.color
    return buildList {
      style.strongStyle.color
        ?.takeIf { it != 0 }
        ?.let { add(it) }
      style.emphasisStyle.color
        ?.takeIf { it != 0 }
        ?.let { add(it) }
      style.linkStyle.color
        .takeIf { it != 0 && it != paragraphColor }
        ?.let { add(it) }
      style
        .codeStyle
        .color
        .takeIf { it != 0 }
        ?.let { add(it) }
      style.taskListStyle.checkedTextColor
        .takeIf { it != 0 }
        ?.let { add(it) }
    }.toIntArray()
  }

  fun getStrongColorFor(blockColor: Int): Int = strongColor ?: blockColor

  fun getEmphasisColorFor(
    blockColor: Int,
    currentColor: Int,
  ): Int =
    if (currentColor == blockColor) {
      emphasisColor ?: blockColor
    } else {
      currentColor
    }

  companion object {
    private val typefaceCache = mutableMapOf<String, Typeface>()
    private var assetMonoRegular: Typeface? = null
    private var assetMonoBold: Typeface? = null
    private var assetMonoItalic: Typeface? = null
    private var assetMonoBoldItalic: Typeface? = null
    private var assetFontsLoaded = false

    /** Load bundled monospace font from assets (call once with any Context). */
    fun initAssetFonts(context: Context) {
      if (assetFontsLoaded) return
      try {
        val assets = context.assets
        // NL = "No Ligatures" variant: code must render literal characters
        // (`-->`, `!=`, `::`) without JetBrains Mono's calt/liga substitutions.
        assetMonoRegular = Typeface.createFromAsset(assets, "fonts/JetBrainsMonoNL-Regular.ttf")
        assetMonoBold = Typeface.createFromAsset(assets, "fonts/JetBrainsMonoNL-Bold.ttf")
        assetMonoItalic = Typeface.createFromAsset(assets, "fonts/JetBrainsMonoNL-Italic.ttf")
        assetMonoBoldItalic = Typeface.createFromAsset(assets, "fonts/JetBrainsMonoNL-BoldItalic.ttf")
        assetFontsLoaded = true
      } catch (_: Exception) {
        // Font files not bundled — fall back to system monospace
      }
    }

    private fun getAssetMonoForStyle(style: Int): Typeface? {
      if (!assetFontsLoaded) return null
      val isBold = (style and Typeface.BOLD) != 0
      val isItalic = (style and Typeface.ITALIC) != 0
      return when {
        isBold && isItalic -> assetMonoBoldItalic
        isBold -> assetMonoBold
        isItalic -> assetMonoItalic
        else -> assetMonoRegular
      } ?: assetMonoRegular
    }

    /**
     * Direct asset-font path for code rendering. This bypasses the shared
     * typeface cache so an early system "monospace" lookup cannot poison
     * code spans or code blocks on OEM Android builds.
     */
    fun getBundledMonospaceTypeface(
      context: Context,
      style: Int,
    ): Typeface? {
      initAssetFonts(context)
      return getAssetMonoForStyle(style)
    }

    /** Cached typeface for font family + style (BOLD, ITALIC, BOLD_ITALIC) */
    fun getTypeface(
      fontFamily: String,
      style: Int,
    ): Typeface =
      typefaceCache.getOrPut("$fontFamily|$style") {
        val base =
          fontFamily
            .takeIf { it.isNotEmpty() }
            ?.let { Typeface.create(it, Typeface.NORMAL) }
            ?: Typeface.DEFAULT
        Typeface.create(base, style)
      }

    /** Cached typeface using weight string (e.g., "bold", "700") */
    fun getTypefaceWithWeight(
      fontFamily: String,
      fontWeight: String,
    ): Typeface {
      val style =
        when (fontWeight.lowercase()) {
          "bold", "700", "800", "900" -> Typeface.BOLD
          else -> Typeface.NORMAL
        }
      return getTypeface(fontFamily, style)
    }

    /** Cached monospace typeface preserving bold/italic — prefers bundled asset font. */
    fun getMonospaceTypeface(currentStyle: Int): Typeface =
      typefaceCache.getOrPut("monospace|$currentStyle") {
        getAssetMonoForStyle(currentStyle)
          ?: Typeface.create(Typeface.MONOSPACE, currentStyle)
      }

    /** Check if a typeface is one of the known monospace typefaces (asset or system). */
    fun isMonospaceTypeface(typeface: Typeface?): Boolean {
      if (typeface == null) return false
      // Check against cached asset monospace fonts
      if (typeface === assetMonoRegular || typeface === assetMonoBold ||
          typeface === assetMonoItalic || typeface === assetMonoBoldItalic) return true
      // Check against cached monospace typefaces from getMonospaceTypeface()
      return typefaceCache.values.any { it === typeface }
    }
  }
}
