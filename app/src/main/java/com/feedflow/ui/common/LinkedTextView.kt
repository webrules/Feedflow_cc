package com.feedflow.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val IMAGE_REGEX = """\[IMAGE:([^\]]+)]""".toRegex()
private val LINK_REGEX = """\[LINK:([^|]+)\|([^\]]+)]""".toRegex()
private val URL_REGEX = """https?://[^\s\]]+""".toRegex()

@Composable
fun LinkedTextView(
    content: String,
    onLinkClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val parts = remember(content) { parseContent(content) }

    Column(modifier = modifier) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is ContentPart.Text -> {
                    if (part.text.isNotBlank()) {
                        if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                        ClickableTextWithLinks(
                            text = part.text,
                            onLinkClick = onLinkClick,
                            lineHeight = lineHeight
                        )
                    }
                }
                is ContentPart.Image -> {
                    AsyncImage(
                        model = part.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(part.url) },
                        contentScale = ContentScale.FillWidth
                    )
                }
                is ContentPart.Link -> {
                    Text(
                        text = part.title,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onLinkClick(part.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClickableTextWithLinks(
    text: String,
    onLinkClick: (String) -> Unit,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val urlMatches = URL_REGEX.findAll(text).toList()
    val textStyle = if (lineHeight != TextUnit.Unspecified) {
        MaterialTheme.typography.bodyMedium.copy(lineHeight = lineHeight)
    } else {
        MaterialTheme.typography.bodyMedium
    }

    if (urlMatches.isEmpty()) {
        Text(
            text = text,
            style = textStyle
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0

        urlMatches.forEach { match ->
            // Append text before the URL
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }

            // Append the URL with styling
            pushStringAnnotation(tag = "URL", annotation = match.value)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(match.value)
            }
            pop()

            lastIndex = match.range.last + 1
        }

        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
        }
    )
}

private sealed class ContentPart {
    data class Text(val text: String) : ContentPart()
    data class Image(val url: String) : ContentPart()
    data class Link(val url: String, val title: String) : ContentPart()
}

private fun parseContent(content: String): List<ContentPart> {
    val parts = mutableListOf<ContentPart>()
    var remaining = content

    while (remaining.isNotEmpty()) {
        val imageMatch = IMAGE_REGEX.find(remaining)
        val linkMatch = LINK_REGEX.find(remaining)

        val nextMatch = listOfNotNull(imageMatch, linkMatch)
            .minByOrNull { it.range.first }

        if (nextMatch == null) {
            // No more matches, add remaining text
            if (remaining.isNotBlank()) {
                parts.add(ContentPart.Text(remaining.trim()))
            }
            break
        }

        // Add text before the match
        if (nextMatch.range.first > 0) {
            val textBefore = remaining.substring(0, nextMatch.range.first)
            if (textBefore.isNotBlank()) {
                parts.add(ContentPart.Text(textBefore.trim()))
            }
        }

        // Add the match
        when (nextMatch) {
            imageMatch -> {
                val url = imageMatch!!.groupValues[1]
                if (url.isNotBlank()) {
                    parts.add(ContentPart.Image(url))
                }
            }
            linkMatch -> {
                val url = linkMatch!!.groupValues[1]
                val title = linkMatch.groupValues[2]
                if (url.isNotBlank()) {
                    parts.add(ContentPart.Link(url, title.ifBlank { url }))
                }
            }
        }

        remaining = remaining.substring(nextMatch.range.last + 1)
    }

    return parts
}
