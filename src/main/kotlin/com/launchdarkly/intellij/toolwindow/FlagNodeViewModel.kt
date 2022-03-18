package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration

class FlagNodeViewModel(
    val flag: FeatureFlag,
    val flags: FeatureFlags,
    val flagConfig: FlagConfiguration?,
) {
    val description = flag.description
    val isEnabled = flagConfig?.on == true
    val isDisconnected = flagConfig == null
    val targets = flagConfig?.targets ?: listOf()
    val prereqFlags = flagConfig?.prerequisites ?: listOf()
    val numRules = flagConfig?.rules?.size ?: 0
    val hasFallthrough = flagConfig?.fallthrough != null
    val hasOffVariation = flagConfig?.offVariation != null
    private val offVariationIndex = flagConfig?.offVariation as? Int
    val offVariation = when {
        offVariationIndex != null -> flag.variations[offVariationIndex].name ?: flag.variations[offVariationIndex].value
        else -> null
    }
    val flagLabel = flag.name ?: flag.key
    val icon = when {
        isDisconnected -> LDIcons.TOGGLE_DISCONNECTED
        isEnabled -> LDIcons.TOGGLE_ON
        else -> LDIcons.TOGGLE_OFF
    }
}
