package com.launchdarkly.intellij.hover

import com.intellij.lang.Language
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.Utils
import com.launchdarkly.intellij.coderefs.FlagAliases
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import com.mitchellbosecke.pebble.PebbleEngine
import java.io.StringWriter

class HoverDocumentationProvider : AbstractDocumentationProvider() {
    private fun getFlagConfig(contextElement: PsiElement, flagKey: String): FlagConfiguration? {
        return contextElement?.project?.service<FlagStore>()?.flagConfigs[flagKey]
    }

    private fun getFlag(contextElement: PsiElement): FeatureFlag? {
        // flags.items can be null if the IDE is not ready during startup
        val flags = contextElement.project.service<FlagStore>().flags.items ?: return null
        val flag = flags.find { contextElement.text.contains(it.key) }

        if (flag == null) {
            val alias =
                contextElement.project.service<FlagAliases>().aliases[contextElement.text.removeSurrounding("\"")]
                    ?: return null
            return flags.find { it.key == alias }
        }

        return flag
    }

    private fun getElementForDocumentation(contextElement: PsiElement?): PsiElement? {
        if (contextElement == null || contextElement == StandardPatterns.not(
                PlatformPatterns.psiElement().notEmpty()
            )
        ) {
            return null
        }

        val flag = getFlag(contextElement)
        if (flag != null) {
            return contextElement
        }

        return contextElement.parent
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        offset: Int
    ): PsiElement? {
        if (contextElement == null) return null

        if (editor.caretModel.currentCaret.offset == contextElement.textRange.startOffset) {
            return getElementForDocumentation(PsiTreeUtil.prevLeaf(contextElement))
        }

        return getElementForDocumentation(contextElement)
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return generateDoc(element, originalElement)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val type = (element as? LeafPsiElement)?.elementType

        // If a language is not supported, it looks like the IDE labels it as "Language.ANY".
        // Secondly it is possible that a language is supported but the node contents is the
        // entire file, hence the second check for max length.
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/360008223759/comments/360001676819
        if (element == null || element.language == Language.ANY || element.textLength > Utils.FLAG_KEY_MAX_LENGTH || type.toString() == "empty token" || element == StandardPatterns.not(
                PlatformPatterns.psiElement().notEmpty()
            )
        ) {
            return null
        }

        val flag = getFlag(element) ?: return null
        val flagConfig: FlagConfiguration = getFlagConfig(element, flag.key) ?: return null

        // Construct view models in kotlin to avoid logic operations in pebble (pain!)
        val flagViewModel = buildMap {
            put("name", flag.name)
            put("description", flag.description)
            put("on", flagConfig.on)
            put("url", Utils.getFlagUrl(flag.key))
            put("variations", flag.variations)
        }

        val template = PebbleEngine.Builder().build().getTemplate("htmlTemplates/flagKeyHover.html")
        val writer = StringWriter()
        template.evaluate(writer, mapOf("flag" to flagViewModel))
        return writer.toString()
    }
}
