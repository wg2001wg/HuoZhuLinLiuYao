package com.liuyao.huozhulin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染组件。
 * 支持：# 标题、**粗体**、*斜体*、~~删除线~~、行内代码、` `-` / `*` 无序列表、`1.` 有序列表。
 * 适用于 AI 解析文本在历史详情页中的排版展示，避免 Markdown 源码直接显示。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = LocalTextStyle.current,
    headingColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        markdown.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                line.startsWith("### ") -> HeadingLine(
                    text = line.drop(4),
                    level = 3,
                    baseStyle = baseStyle,
                    color = headingColor
                )
                line.startsWith("## ") -> HeadingLine(
                    text = line.drop(3),
                    level = 2,
                    baseStyle = baseStyle,
                    color = headingColor
                )
                line.startsWith("# ") -> HeadingLine(
                    text = line.drop(2),
                    level = 1,
                    baseStyle = baseStyle,
                    color = headingColor
                )
                line.startsWith("- ") || line.startsWith("* ") -> BulletLine(
                    text = line.drop(2),
                    baseStyle = baseStyle
                )
                ORDERED_REGEX.matches(line) -> OrderedLine(
                    text = line,
                    baseStyle = baseStyle
                )
                else -> BodyLine(text = line, baseStyle = baseStyle)
            }
        }
    }
}

private val ORDERED_REGEX = Regex("""^(\d+)\.\s+(.*)""")

@Composable
private fun HeadingLine(
    text: String,
    level: Int,
    baseStyle: TextStyle,
    color: Color
) {
    val sizeAdd = when (level) {
        1 -> 5
        2 -> 3
        else -> 1
    }
    val newFontSize = (baseStyle.fontSize.value + sizeAdd).sp
    Text(
        text = parseInlineSpans(text, baseStyle),
        style = baseStyle.copy(
            fontSize = newFontSize,
            fontWeight = FontWeight.Bold,
            color = color
        ),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun BulletLine(text: String, baseStyle: TextStyle) {
    Text(
        text = buildAnnotatedString {
            append("\u2022 ")
            append(parseInlineSpans(text, baseStyle))
        },
        style = baseStyle,
        modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
    )
}

@Composable
private fun OrderedLine(text: String, baseStyle: TextStyle) {
    val match = ORDERED_REGEX.matchEntire(text) ?: return BodyLine(text, baseStyle)
    val number = match.groupValues[1]
    val content = match.groupValues[2]
    Text(
        text = buildAnnotatedString {
            append("$number. ")
            append(parseInlineSpans(content, baseStyle))
        },
        style = baseStyle,
        modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp)
    )
}

@Composable
private fun BodyLine(text: String, baseStyle: TextStyle) {
    Text(
        text = parseInlineSpans(text, baseStyle),
        style = baseStyle,
        modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
    )
}

/**
 * 解析行内样式：**粗体**、*斜体*、~~删除线~~、行内代码。
 * 若标记未成对出现，则按普通文本显示，避免流式输出中标记不完整导致样式错乱。
 */
private fun parseInlineSpans(text: String, baseStyle: TextStyle): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = baseStyle.fontSize
                            )
                        ) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                fontSize = baseStyle.fontSize
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = baseStyle.fontSize
                            )
                        ) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = baseStyle.fontSize.times(0.92f)
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
