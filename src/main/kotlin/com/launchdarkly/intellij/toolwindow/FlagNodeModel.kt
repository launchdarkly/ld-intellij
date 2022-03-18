package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.featurestore.FlagConfiguration

class FlagNodeModel(
    val flag: FeatureFlag,
    val flags: FeatureFlags,
    flagConfig: FlagConfiguration?,
) {
    val flagConfig = flagConfig ?: FlagConfiguration(flag.key, null, null, listOf(), listOf(), arrayOf(), false, -1)
    val isDisconnected = flagConfig == null
    val targets = this.flagConfig.targets
    val prereqFlags = this.flagConfig.prerequisites
    val numRules = this.flagConfig.rules.size
    val hasFallthrough = this.flagConfig.fallthrough != null
    val hasOffVariation = this.flagConfig.offVariation != null
    val offVariation = {
        if (hasOffVariation) {
            flag.variations[this.flagConfig.offVariation as Int].name
                ?: flag.variations[this.flagConfig.offVariation as Int].value
        }
        else {
            null
        }
    }
    val flagLabel = flag.name ?: flag.key
}
