package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.featurestore.FlagConfiguration

class FlagNodeViewModel(
    val flag: FeatureFlag,
    val flags: FeatureFlags,
    flagConfig: FlagConfiguration?,
) {
    val flagConfig = flagConfig ?: FlagConfiguration(flag.key, null, null, listOf(), listOf(), arrayOf(), false, -1)
    val description = flag.description
    val isDisconnected = flagConfig == null
    val targets = this.flagConfig.targets
    val prereqFlags = this.flagConfig.prerequisites
    val numRules = this.flagConfig.rules.size
    val hasFallthrough = this.flagConfig.fallthrough != null
    val hasOffVariation = this.flagConfig.offVariation != null
    private val offVariationIndex = this.flagConfig.offVariation as Int
    val offVariation = when {
        hasOffVariation -> flag.variations[offVariationIndex].name ?: flag.variations[offVariationIndex].value
        else -> null
    }
    val flagLabel  = flag.name ?: flag.key
}
