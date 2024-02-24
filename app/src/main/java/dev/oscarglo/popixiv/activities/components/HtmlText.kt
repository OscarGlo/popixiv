package dev.oscarglo.popixiv.activities.components

import android.graphics.Typeface
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onBackground
) {
    val annotatedString = buildAnnotatedString {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        val spans = spanned.getSpans(0, spanned.length, Any::class.java)

        append(spanned.toString())

        spans
            .filter { it !is BulletSpan }
            .forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                when (span) {
                    is RelativeSizeSpan -> SpanStyle(fontSize = span.sizeChange.sp)
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                        Typeface.BOLD_ITALIC -> SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        )

                        else -> null
                    }

                    is UnderlineSpan -> SpanStyle(textDecoration = TextDecoration.Underline)
                    is ForegroundColorSpan -> SpanStyle(color = Color(span.foregroundColor))
                    is StrikethroughSpan -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    is SuperscriptSpan -> SpanStyle(baselineShift = BaselineShift.Superscript)
                    is SubscriptSpan -> SpanStyle(baselineShift = BaselineShift.Subscript)
                    is URLSpan -> {
                        addStringAnnotation(
                            tag = "url",
                            annotation = span.url,
                            start = start,
                            end = end
                        )
                        SpanStyle(
                            color = MaterialTheme.colors.primary,
                            //textDecoration = TextDecoration.Underline,
                        )
                    }

                    else -> null
                }?.let { spanStyle ->
                    addStyle(spanStyle, start, end)
                }
            }
    }

    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedString,
        style = TextStyle(color = color),
        modifier = modifier,
        onClick = {
            annotatedString
                .getStringAnnotations("url", it, it)
                .firstOrNull()
                ?.let { stringAnnotation -> uriHandler.openUri(stringAnnotation.item) }
        }
    )
}