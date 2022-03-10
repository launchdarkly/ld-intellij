package com.launchdarkly.intellij

import com.mitchellbosecke.pebble.PebbleEngine
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
import com.launchdarkly.intellij.coderefs.FlagAliases
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.sun.jna.StringArray
import java.io.StringWriter

class DocumentationProvider : AbstractDocumentationProvider() {
    // Not sure how this is all working yet but it works for custom documentation in other IDEs than IDEA
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

        val getFlags = contextElement.project.service<com.launchdarkly.intellij.FlagStore>()

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
        val getFlags = element.project.service<com.launchdarkly.intellij.FlagStore>()
        if (getFlags.flags.items == null) return null
        val getAliases = element.project.service<FlagAliases>()
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState

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
            val prereqs = if (env.prerequisites.isNotEmpty()) {
                "<b>Prerequisites</b> ${env.prerequisites.size} • "
            } else ""
            val rules = if (env.rules.isNotEmpty()) {
                "<b>Rules</b> ${env.rules.size}<br />"
            } else if (env.targets.isNotEmpty()) {
                " •"
            } else {
                ""
            }
            var targets = ""
            if (env.targets.isNotEmpty()) {
                targets += "<b>Targets</b><br /> "
                env.targets.forEachIndexed { i, t ->
                    targets += "${flag.variations[t.variation as Int].name ?: flag.variations[t.variation as Int].value} ${t.values.size}"
                    if (i != env.targets.lastIndex) {
                        targets += " \u2022 "
                    }
                }
                targets += "<br />"
            }
            var buildEnvString = ""
            if (prereqs.length > 1) {
                buildEnvString += "$prereqs "
            }
            if (rules.length > 1) {
                buildEnvString += rules
            }
            if (targets != "") {
                buildEnvString += targets
            }
//            result.append("<html>")
//            if (env.version === -1) {
//                result.append("<b>FLAG TARGETING INFORMATION IS NOT AVAILABLE. Below Values are placeholders</b><br />")
//            }
//            result.append("<b>LaunchDarkly Feature Flag \u2022 ${flag.name ?: flag.key}</b><br />")
//            result.append("<a href=\"${settings.baseUri}${flag.environments[settings.environment]!!.site.href}\">Open In LaunchDarkly</a><br />")
//
//            val state = if (env.version === -1) {
//                "Disconnect"
//            } else if (env.on) {
//                "On"
//            } else {
//                "Off"
//            }
//            result.append("Enabled: $state<br />")
//            result.append("${flag.description}<br />")
//            result.append(buildEnvString)
//            result.append("<br /><b>Variations ${if (env.fallthrough?.rollout != null) " ◆ Rollout Configured" else ""}</b><br />")
//            flag.variations.mapIndexed { i, it ->
//                val rolloutPercentage: Double = if (env.fallthrough?.rollout != null) {
//                    val rollout = env.fallthrough?.rollout
//                    val foundVariation = rollout!!.variations.filter { it.variation == i }
//                    (foundVariation[0].weight.toDouble() / 1000)
//                } else -1.000
//                var variationOut = "$i"
//                if (it.name != "" && it.name != null) {
//                    variationOut += " ◆ ${it.name}"
//                }
//                variationOut += " ◆ ${if (rolloutPercentage != null && rolloutPercentage != -1.000) "Rollout $rolloutPercentage% ◆ " else ""}<code>Return value:</code> <code>${it.value}</code><br />"
//                result.append(variationOut)
//                if (env.offVariation != null && env.offVariation == i) {
//                    result.append("<p><b>Off Variation</b></p>")
//                }
//                if (env.fallthrough?.variation != null && env.fallthrough?.variation == i) {
//                    result.append("<p><b>Fallthrough Variation</b></p>")
//                }
//                if (it.description != "" && it.description != null) {
//                    result.append("<p>${it.description ?: ""}</p><br />")
//                }
//            }
//            result.append("</html>")


            // Massage data so we don't perform logic in the html template.
            val displayVariations = ArrayList<Variation>()
            flag.variations.forEach { v ->
                val dv = Variation()
                dv.name = (v.name ?: v.value.toString()).uppercase()
                displayVariations.add(dv)
            }

            // Pass data to html template to be rendered
            val template = PebbleEngine.Builder().build().getTemplate("htmlTemplates/flagKeyHover.html")
            val writer = StringWriter()
            template.evaluate(writer, mapOf("variations" to displayVariations))
            return writer.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {

        return null
    }
}
