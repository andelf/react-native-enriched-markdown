package com.swmansion.enriched.markdown.renderer

import android.os.Build
import android.text.SpannableStringBuilder
import com.swmansion.enriched.markdown.parser.MarkdownASTNode
import com.swmansion.enriched.markdown.spans.CodeBackgroundSpan
import com.swmansion.enriched.markdown.spans.CodeSpan
import com.swmansion.enriched.markdown.utils.text.span.SPAN_FLAGS_EXCLUSIVE_EXCLUSIVE

class CodeRenderer(
  private val config: RendererConfig,
) : NodeRenderer {
  override fun render(
    node: MarkdownASTNode,
    builder: SpannableStringBuilder,
    onLinkPress: ((String) -> Unit)?,
    onLinkLongPress: ((String) -> Unit)?,
    factory: RendererFactory,
  ) {
    if (node.children.isEmpty() || node.children.all { it.content.isEmpty() }) return

    factory.renderWithSpan(builder, {
      node.children.forEach { child ->
        builder.append(inlineCodeTextWithSoftWraps(child.content))
      }
    }) { start, end, blockStyle ->
      builder.setSpan(
        CodeSpan(factory.styleCache, blockStyle, factory.context),
        start,
        end,
        SPAN_FLAGS_EXCLUSIVE_EXCLUSIVE,
      )
      builder.setSpan(
        CodeBackgroundSpan(config.style),
        start,
        end,
        SPAN_FLAGS_EXCLUSIVE_EXCLUSIVE,
      )
    }
  }

  private fun inlineCodeTextWithSoftWraps(content: String): CharSequence {
    if (content.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return content

    val result = StringBuilder(content.length * 2)
    var index = 0
    while (index < content.length) {
      val codePoint = content.codePointAt(index)
      result.appendCodePoint(codePoint)
      index += Character.charCount(codePoint)
      if (index < content.length) {
        result.append('\u200B')
      }
    }
    return result
  }
}
