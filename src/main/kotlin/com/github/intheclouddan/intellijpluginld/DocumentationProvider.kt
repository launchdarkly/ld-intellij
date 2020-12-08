package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.launchdarkly.api.model.FeatureFlag

class LDDocumentationProvider : AbstractDocumentationProvider() {

    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): List<String>? {
        if (element == null) {
            return null
        }
        val getFlags = element.project.service<FlagStore>()
        val flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.drop(1).dropLast(1) }

        if (flag != null) {
            val settings = LaunchDarklyMergedSettings.getInstance(element.project)
            return listOf("${settings.baseUri.removePrefix("https://")}${flag.environments[settings.environment]!!.site.href}")
        }

        return null
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) {
            return null
        }
        val getFlags = element.project.service<FlagStore>()

        val flag: FeatureFlag? = getFlags.flags.items.find { it.key == element.text.drop(1).dropLast(1) }
        if (flag != null) {
            val env: FlagConfiguration = getFlags.flagConfigs[element.text.drop(1).dropLast(1)]!!
            val result = StringBuilder()
            val prereqs = if (env.prerequisites.isNotEmpty()) {
                "<b>Prerequisites</b> ${env.prerequisites.size} • "
            } else ""
            val rules = if (env.rules.isNotEmpty()) {
                "<b>Rules</b> ${env.rules.size}<br />"
            } else " •"
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
            result.append("<html>")
            result.append("<img src=\"${LDIcons.FLAG}\"> <b>LaunchDarkly Feature Flag \u2022 ${flag.name ?: flag.key}</b><br />")
            val enabledIcon = if (env.on) {
                "<img src=\"${LDIcons.TOGGLE_ON}\" alt=\"On\">"
            } else {
                "<img src=\"${LDIcons.TOGGLE_OFF}\" alt=\"Off\">"
            }
            result.append("$enabledIcon ${flag.description}<br />")
            result.append(buildEnvString)
            result.append("<br /><b>Variations ${if (env.fallthrough?.rollout != null) " ◆ Rollout Configured" else ""}</b><br />")
            flag.variations.mapIndexed { i, it ->
                val rolloutPercentage: Double = if (env.fallthrough?.rollout != null) {
                    val rollout = env.fallthrough?.rollout
                    val foundVariation = rollout!!.variations.filter { it.variation == i }
                    (foundVariation[0].weight.toDouble() / 1000)
                } else -1.000
                var variationOut = "$i"
                if (it.name != "" && it.name != null) {
                    variationOut += " ◆ ${it.name}"
                }
                variationOut += " ◆ ${if (rolloutPercentage != null && rolloutPercentage != -1.000) "Rollout $rolloutPercentage% ◆ " else ""}<code>Return value:</code> <code>${it.value}</code><br />"
                result.append(variationOut)
                if (env.offVariation != null && env.offVariation == i) {
                    result.append("<p><b>Off Variation</b></p>")
                }
                if (env.fallthrough?.variation != null && env.fallthrough?.variation == i) {
                    result.append("<p><b>Fallthrough Variation</b></p>")
                }
                if (it.description != "" && it.description != null) {
                    result.append("<p>${it.description ?: ""}</p><br />")
                }
            }
            result.append("</html>")

            return result.toString()
        }

        return null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {

        return null
    }

}