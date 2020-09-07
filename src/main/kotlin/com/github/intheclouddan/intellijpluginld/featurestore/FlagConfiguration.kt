package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.api.model.Fallthrough
import com.launchdarkly.api.model.Prerequisite
import com.launchdarkly.api.model.Rule
import com.launchdarkly.api.model.Target

data class FlagConfiguration(
        val key: String,
        var offVariation: Int?,
        var fallthrough: Fallthrough?,
        var prerequisites: List<Prerequisite>,
        var targets: List<Target>,
        var rules: Array<Rule>,
        var on: Boolean,
        var version: Number
) {}