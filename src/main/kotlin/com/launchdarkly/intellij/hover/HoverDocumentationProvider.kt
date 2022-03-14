package com.launchdarkly.intellij.hover

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.Variation
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.Utils
import com.launchdarkly.intellij.coderefs.FlagAliases
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import com.mitchellbosecke.pebble.PebbleEngine
import java.io.StringWriter

class HoverDocumentationProvider : AbstractDocumentationProvider() {
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

    private fun getElementForDocumentation(contextElement: PsiElement?): PsiElement? {
        if (contextElement == null || contextElement == StandardPatterns.not(
                PlatformPatterns.psiElement().notEmpty()
            )
        ) return null

        val getFlags = contextElement.project.service<FlagStore>()

        var flag: FeatureFlag? =
            getFlags.flags?.items?.find { contextElement.text.contains(it.key) }
        if (flag != null) {
            return contextElement
        }
        val getAliases = contextElement.project.service<FlagAliases>()
        val aliasFlag = getAliases.aliases[contextElement.text.removeSurrounding("\"")]
        if (aliasFlag != null) return contextElement
        return contextElement?.parent
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return generateDoc(element, originalElement)
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || element == StandardPatterns.not(
                PlatformPatterns.psiElement().notEmpty()
            )
        ) return null

        val getFlags = element.project.service<FlagStore>()
        if (getFlags.flags.items == null) return null

        val getAliases = element.project.service<FlagAliases>()
        var flag: FeatureFlag? =
            getFlags.flags?.items?.find { element.text.contains(it.key) }
        var alias: String?
        if (flag == null) {
            alias = getAliases.aliases[element.text.removeSurrounding("\"")]
            flag = getFlags.flags.items.find { it.key == alias }
        }

        // TODO: gracefully handle API call working and Datastore being unavailable
        if (flag != null) {
            val env: FlagConfiguration = getFlags.flagConfigs[flag.key]
                ?: FlagConfiguration(flag.key, null, null, listOf(), listOf(), arrayOf(), false, -1)

            // Construct view models in kotlin so we can use them in the html template
            // to minimise logic operations in pebble (pain!)
            val variationsViewModel = ArrayList<Variation>()
            flag.variations.forEach { v ->
                val model = Variation()
                model.name = (v.name ?: v.value.toString()).uppercase()
                model.value = v.value
                model.description = v.description
                variationsViewModel.add(model)
            }

            // Build the parent flag object and add the above variations object to it
            val flagViewModel = buildMap {
                put("name", flag.name)
                put("description", flag.description)
                put("on", env.on)
                put("url", Utils.getFlagUrl(flag.key))
                put("variations", variationsViewModel)
            }

            val template = PebbleEngine.Builder().build().getTemplate("htmlTemplates/flagKeyHover.html")
            val writer = StringWriter()
            template.evaluate(writer, mapOf("flag" to flagViewModel))
            return writer.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {

        return null
    }
}