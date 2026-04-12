package com.swmansion.enriched.markdown.renderer

import android.os.Build
import android.text.SpannableStringBuilder
import com.swmansion.enriched.markdown.parser.MarkdownASTNode
import com.swmansion.enriched.markdown.spans.LinkSpan
import com.swmansion.enriched.markdown.utils.text.span.SPAN_FLAGS_EXCLUSIVE_EXCLUSIVE

class LinkRenderer(
  private val config: RendererConfig,
) : NodeRenderer {
  override fun render(
    node: MarkdownASTNode,
    builder: SpannableStringBuilder,
    onLinkPress: ((String) -> Unit)?,
    onLinkLongPress: ((String) -> Unit)?,
    factory: RendererFactory,
  ) {
    val url = node.getAttribute("url") ?: return

    val linkContent = renderLinkContent(node, onLinkPress, onLinkLongPress, factory)

    factory.renderWithSpan(builder, { builder.append(linkContent) }) { start, end, blockStyle ->
      builder.setSpan(
        LinkSpan(url, onLinkPress, onLinkLongPress, factory.styleCache, blockStyle, factory.context),
        start,
        end,
        SPAN_FLAGS_EXCLUSIVE_EXCLUSIVE,
      )
    }
  }

  private fun renderLinkContent(
    node: MarkdownASTNode,
    onLinkPress: ((String) -> Unit)?,
    onLinkLongPress: ((String) -> Unit)?,
    factory: RendererFactory,
  ): CharSequence {
    val rendered = SpannableStringBuilder()
    factory.renderChildren(node, rendered, onLinkPress, onLinkLongPress)
    return withSoftWraps(rendered)
  }

  private fun withSoftWraps(source: CharSequence): CharSequence {
    if (source.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return source

    val original = SpannableStringBuilder(source)
    val transformed = SpannableStringBuilder()
    val indexMap = IntArray(original.length + 1)

    var oldIndex = 0
    while (oldIndex < original.length) {
      indexMap[oldIndex] = transformed.length

      val codePoint = Character.codePointAt(original, oldIndex)
      transformed.append(String(Character.toChars(codePoint)))
      val charCount = Character.charCount(codePoint)
      oldIndex += charCount

      if (oldIndex < original.length) {
        transformed.append('\u200B')
      }

      for (boundary in (oldIndex - charCount + 1)..oldIndex) {
        indexMap[boundary] = transformed.length
      }
    }
    indexMap[original.length] = transformed.length

    for (span in original.getSpans(0, original.length, Any::class.java)) {
      val start = original.getSpanStart(span)
      val end = original.getSpanEnd(span)
      if (start < 0 || end < start) continue
      transformed.setSpan(span, indexMap[start], indexMap[end], original.getSpanFlags(span))
    }

    return transformed
  }
}
