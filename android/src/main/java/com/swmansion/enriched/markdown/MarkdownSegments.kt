package com.swmansion.enriched.markdown

import android.content.Context
import android.text.SpannableString
import com.swmansion.enriched.markdown.parser.MarkdownASTNode
import com.swmansion.enriched.markdown.renderer.Renderer
import com.swmansion.enriched.markdown.spans.ImageSpan
import com.swmansion.enriched.markdown.styles.StyleConfig

sealed interface MarkdownRenderSegment {
  data class Text(
    val nodes: List<MarkdownASTNode>,
    val styledText: SpannableString,
    val imageSpans: List<ImageSpan>,
    val needsJustify: Boolean,
    val lastElementMarginBottom: Float,
  ) : MarkdownRenderSegment

  data class Table(
    val node: MarkdownASTNode,
  ) : MarkdownRenderSegment

  data class Math(
    val latex: String,
    val node: MarkdownASTNode,
  ) : MarkdownRenderSegment

  data class CodeBlock(
    val node: MarkdownASTNode,
  ) : MarkdownRenderSegment
}

object MarkdownSegmentBuilder {
  fun build(
    root: MarkdownASTNode,
    style: StyleConfig,
    context: Context,
    onLinkPress: ((String) -> Unit)?,
    onLinkLongPress: ((String) -> Unit)?,
  ): List<MarkdownRenderSegment> =
    splitAstIntoSegments(root).map { segment ->
      when (segment) {
        is MarkdownRenderSegment.Text -> renderTextSegment(
          segment.nodes,
          style,
          context,
          onLinkPress,
          onLinkLongPress,
        )
        is MarkdownRenderSegment.Table -> segment
        is MarkdownRenderSegment.Math -> segment
        is MarkdownRenderSegment.CodeBlock -> segment
      }
    }

  private fun renderTextSegment(
    nodes: List<MarkdownASTNode>,
    style: StyleConfig,
    context: Context,
    onLinkPress: ((String) -> Unit)?,
    onLinkLongPress: ((String) -> Unit)?,
  ): MarkdownRenderSegment.Text {
    val documentWrapper = MarkdownASTNode(type = MarkdownASTNode.NodeType.Document, children = nodes)
    val renderer = Renderer().apply { configure(style, context) }

    return MarkdownRenderSegment.Text(
      nodes = nodes,
      styledText = renderer.renderDocument(documentWrapper, onLinkPress, onLinkLongPress),
      imageSpans = renderer.getCollectedImageSpans().toList(),
      needsJustify = style.needsJustify,
      lastElementMarginBottom = renderer.getLastElementMarginBottom(),
    )
  }

  private fun splitAstIntoSegments(root: MarkdownASTNode): List<MarkdownRenderSegment> {
    val segments = mutableListOf<MarkdownRenderSegment>()
    val currentTextNodes = mutableListOf<MarkdownASTNode>()

    fun flushTextNodes() {
      if (currentTextNodes.isNotEmpty()) {
        segments.add(
          MarkdownRenderSegment.Text(
            nodes = currentTextNodes.toList(),
            styledText = SpannableString(""),
            imageSpans = emptyList(),
            needsJustify = false,
            lastElementMarginBottom = 0f,
          )
        )
        currentTextNodes.clear()
      }
    }

    for (child in root.children) {
      when (child.type) {
        MarkdownASTNode.NodeType.Table -> {
          flushTextNodes()
          segments.add(MarkdownRenderSegment.Table(child))
        }

        MarkdownASTNode.NodeType.LatexMathDisplay -> {
          flushTextNodes()
          val latex =
            if (child.children.isNotEmpty()) {
              child.children.first().content
            } else {
              child.content
            }
          segments.add(MarkdownRenderSegment.Math(latex, child))
        }

        MarkdownASTNode.NodeType.CodeBlock -> {
          flushTextNodes()
          segments.add(MarkdownRenderSegment.CodeBlock(child))
        }

        else -> {
          currentTextNodes.add(child)
        }
      }
    }

    flushTextNodes()
    return segments
  }
}
