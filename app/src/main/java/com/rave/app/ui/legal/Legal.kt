package com.rave.app.ui.legal

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.components.RaveScreen

/** Legal documents bundled with the app (res/raw, simple Markdown). */
enum class LegalDoc(@RawRes val raw: Int, @StringRes val title: Int) {
    PRIVACY(R.raw.privacy_policy, R.string.legal_privacy),
    TERMS(R.raw.terms_of_use, R.string.legal_terms),
    CREDITS(R.raw.credits, R.string.legal_credits),
}

@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    RaveScreen(title = stringResource(doc.title), onBack = onBack) {
        LegalDocContent(doc, Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding())
    }
}

/** Reads a document and fills in the developer name, contact email and date. */
@Composable
private fun rememberLegalText(doc: LegalDoc): String {
    val context = LocalContext.current
    val developer = stringResource(R.string.legal_developer_name)
    val email = stringResource(R.string.legal_contact_email)
    val updated = stringResource(R.string.legal_last_updated)
    return remember(doc, developer, email, updated) {
        context.resources.openRawResource(doc.raw).bufferedReader().use { it.readText() }
            .replace("{{DEVELOPER}}", developer).replace("{{EMAIL}}", email).replace("{{UPDATED}}", updated)
    }
}

@Composable
fun LegalDocContent(doc: LegalDoc, modifier: Modifier = Modifier) {
    val text = rememberLegalText(doc)
    val blocks = remember(text) { parseBlocks(text) }
    val body = MaterialTheme.typography.bodyMedium
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        blocks.forEach { b ->
            when (b) {
                is Block.Title -> Unit // shown in the top bar
                is Block.Heading -> {
                    Spacer(Modifier.height(18.dp))
                    Text(b.text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = onSurface)
                    Spacer(Modifier.height(6.dp))
                }
                is Block.Paragraph -> {
                    Text(inline(b.text), style = body, color = onSurface, modifier = Modifier.padding(vertical = 4.dp))
                }
                is Block.Bullet -> Row(Modifier.padding(vertical = 3.dp, horizontal = 2.dp)) {
                    Text("•", style = body, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(inline(b.text), style = body, color = onSurface, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private sealed interface Block {
    data class Title(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullet(val text: String) : Block
}

private fun parseBlocks(md: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = mutableListOf<String>()
    fun flush() { if (para.isNotEmpty()) { out += Block.Paragraph(para.joinToString("\n")); para.clear() } }
    md.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> flush()
            line.startsWith("## ") -> { flush(); out += Block.Heading(line.removePrefix("## ")) }
            line.startsWith("# ") -> { flush(); out += Block.Title(line.removePrefix("# ")) }
            line.startsWith("- ") -> { flush(); out += Block.Bullet(line.removePrefix("- ")) }
            else -> para += line
        }
    }
    flush()
    return out
}

private val LINK = Regex("""https?://[^\s)]+[^\s).,]|[\w.+-]+@[\w-]+\.[\w.-]*\w""")

/** **bold** spans plus clickable web links and email addresses. */
@Composable
private fun inline(text: String): AnnotatedString {
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    return remember(text, linkStyle) {
        buildAnnotatedString {
            text.split("**").forEachIndexed { i, part ->
                val bold = i % 2 == 1
                if (bold) pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                var last = 0
                LINK.findAll(part).forEach { m ->
                    append(part.substring(last, m.range.first))
                    val url = if (m.value.contains('@') && !m.value.startsWith("http")) "mailto:${m.value}" else m.value
                    withLink(LinkAnnotation.Url(url, linkStyle)) { append(m.value) }
                    last = m.range.last + 1
                }
                append(part.substring(last))
                if (bold) pop()
            }
        }
    }
}

/**
 * "By continuing you agree to the Terms of Use and Privacy Policy", where both open the document
 * in a sheet. Used on sign-in and the paywall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalAgreementText(
    modifier: Modifier = Modifier,
    @StringRes lead: Int = R.string.legal_agree_lead,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    linkColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
) {
    var open by rememberSaveable { mutableStateOf<LegalDoc?>(null) }
    val terms = stringResource(R.string.legal_terms)
    val privacy = stringResource(R.string.legal_privacy)
    val leadText = stringResource(lead)
    val andText = stringResource(R.string.legal_and)
    val linkStyle = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        append(leadText)
        append(" ")
        withLink(LinkAnnotation.Clickable("terms", linkStyle) { open = LegalDoc.TERMS }) { append(terms) }
        append(" ")
        append(andText)
        append(" ")
        withLink(LinkAnnotation.Clickable("privacy", linkStyle) { open = LegalDoc.PRIVACY }) { append(privacy) }
        append(".")
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, textAlign = textAlign, modifier = modifier)
    open?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { open = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding(), verticalArrangement = Arrangement.Top) {
                Text(
                    stringResource(doc.title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 20.dp),
                )
                LegalDocContent(doc)
            }
        }
    }
}
