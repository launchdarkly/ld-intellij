package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration

class FlagNodeViewModel(
    val flag: FeatureFlag,
    val flags: FeatureFlags,
    val flagConfig: FlagConfiguration?
) {
    val description = flag.description
    val hasDescription = flag.description != ""
    val isEnabled = flagConfig?.on
    val targets = flagConfig?.targets ?: listOf()
    val hasTargets = targets.isNotEmpty()
    val prereqFlags = flagConfig?.prerequisites ?: listOf()
    val hasPrereqs = prereqFlags.isNotEmpty()
    val numRules = flagConfig?.rules?.size ?: 0
    val hasRules = numRules > 0
    val tags = flag.tags
    val hasTags = flag.tags.isNotEmpty()
    val hasFallthrough = flagConfig?.fallthrough != null
    val hasOffVariation = flagConfig?.offVariation != null
    private val offVariationIndex = flagConfig?.offVariation as? Int
    val offVariation = when {
        offVariationIndex != null -> flag.variations[offVariationIndex].name ?: flag.variations[offVariationIndex].value
        else -> null
    }
    val flagLabel = flag.name ?: flag.key
    val icon = when {
        isEnabled == true -> LDIcons.TOGGLE_ON
        isEnabled == false -> LDIcons.TOGGLE_OFF
        else -> LDIcons.TOGGLE_DISCONNECTED
    }
}
