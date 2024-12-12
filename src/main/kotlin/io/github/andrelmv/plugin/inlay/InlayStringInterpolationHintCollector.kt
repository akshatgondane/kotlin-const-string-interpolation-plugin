package io.github.andrelmv.plugin.inlay

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import java.util.concurrent.atomic.AtomicInteger
import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.icons.AllIcons
import java.net.URLEncoder


fun extractFirstQuotedSubstring(input: String): String {
    val regex = "\"(.*?)\"".toRegex()
    val matchResult = regex.find(input)

    // If a match is found, URL encode the substring inside the quotes
    return matchResult?.groupValues?.get(1)?.let {
        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    } ?: ""
}

fun checkElementText(element: PsiElement): Boolean {
    val loggingKeywords = listOf(
        "logger.info(", "LOGGER.debug(", "LOGGER.error(",
        "LOG.debug(", "logger.debug(", "LOGGER.info(", "LOG.warn(", "LOG.info(", "logger.error("
    )

    return element.text?.let { text ->
        loggingKeywords.any { text.contains(it) } && text.endsWith(")")
    } ?: false
}

@Suppress("UnstableApiUsage")
class InlayStringInterpolationHintCollector(
    private val settings: InlayStringInterpolationSettings,
    editor: Editor,
) : FactoryInlayHintsCollector(editor) {
    override fun collect(
        element: PsiElement,
        editor: Editor,
        sink: InlayHintsSink,
    ): Boolean {
        element.accept(
            object : KotlinRecursiveElementVisitor() {
                override fun visitElement(
                    element: PsiElement,
                ) {
                    val offset: AtomicInteger = AtomicInteger()


                    if (settings.state.withStringInterpolationHint
                        && checkElementText(element)) {
                        element.lastChild?.let { lastChild ->
                            offset.set(lastChild.textOffset)

                            // Define the link you want to add
                            val queryString = extractFirstQuotedSubstring(element.text);
                            val link =
                                "https://app.datadoghq.com/logs?query=$queryString&agg_m=count&agg_m_source=base&agg_t=count&cols=host%2Cservice&fromUser=true&messageDisplay=inline&refresh_mode=sliding&storage=hot&stream_sort=desc&viz=stream&live=true"

                            // Create the default and clicked presentations
                            val defaultPresentation = factory.text("DataDog")
                            val clickedPresentation = factory.text("DataDog") // You can customize this as needed

                            // Define the click listener
                            val clickListener = InlayPresentationFactory.ClickListener { _, _ ->
                                try {
                                    java.awt.Desktop.getDesktop().browse(java.net.URI(link))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // Load the log icon
                            val logIcon = factory.icon(AllIcons.General.Information) // Replace with an appropriate icon

                            // Add spacing before the element
                            val spacer = factory.text("    ") // 10px space

                            // Combine elements: spacer, icon, and text button
                            val combinedPresentation = factory.seq(
                                spacer,
                                factory.button(
                                    defaultPresentation,
                                    clickedPresentation,
                                    clickListener,
                                    null,
                                    false
                                ).first)
                            // Add the combined presentation inline
                            System.out.println(offset.get())
                            sink.addInlineElement(offset.get(), true, combinedPresentation, true)
                        }
                        return
                    }

                }
            }
        )
        return true
    }
}

private fun PsiElement.isKtNameReferenceExpression(): Boolean {
    return this is KtNameReferenceExpression
            && ((this.context is KtCollectionLiteralExpression && this.isConstant())
            || (this.context is KtValueArgument && this.isConstant())
            || this.context is KtNamedFunction
            || this.context is KtBinaryExpression)
}

private fun PsiElement.isKtStringTemplateExpression(): Boolean {
    return this is KtStringTemplateExpression
            && this.isConstant() && this.isPlain().not()
            && this.hasInterpolation()
            && this.isSingleQuoted()
}

@RequiresReadLock
private fun KtExpression.isConstant(): Boolean {
    return org.jetbrains.kotlin.analysis.api.analyze(this) {
        evaluate() != null
    }
}

@RequiresReadLock
private fun KtExpression.getValue(): String? {
    return org.jetbrains.kotlin.analysis.api.analyze(this) {
        evaluate()?.toString()
    }
}