package com.alteryx.plugins.kotlin.inlay

import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

@Suppress("UnstableApiUsage")
class InlayStringInterpolationProvider : InlayHintsProvider<InlayStringInterpolationSettings> {

    private val settingsKey: SettingsKey<InlayStringInterpolationSettings> = SettingsKey(STR_INTERPOLATION_HINT)

    override val key: SettingsKey<InlayStringInterpolationSettings>
        get() = settingsKey

    override val name: String
        get() = STR_INTERPOLATION_HINT

    override val previewText: String
        get() = """
            LOGGER.info("Remote job complete");
            """

    override fun createConfigurable(
        settings: InlayStringInterpolationSettings
    ): ImmediateConfigurable = InlayStringInterpolationConfig(settings)

    override fun isLanguageSupported(
        language: Language
    ): Boolean = language == JavaLanguage.INSTANCE

    override fun createSettings(): InlayStringInterpolationSettings = service<InlayStringInterpolationSettings>()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: InlayStringInterpolationSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector = InlayStringInterpolationHintCollector(settings, editor)
}

private const val STR_INTERPOLATION_HINT = "String Interpolation Hint"
