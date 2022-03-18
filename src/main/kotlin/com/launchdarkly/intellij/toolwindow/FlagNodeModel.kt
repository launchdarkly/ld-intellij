package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import java.util.ArrayList

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
    val offVariation =
            flag.variations[this.flagConfig.offVariation as Int].name ?:
            flag.variations[this.flagConfig.offVariation as Int].value
    val hasOffVariation = this.offVariation != null
    val flagLabel = flag.name ?: flag.key
}