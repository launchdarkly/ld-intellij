package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.api.model.Fallthrough
import com.launchdarkly.api.model.Prerequisite

data class FlagConfiguration(
        val key: String,
        var offVariation: Int?,
        var fallthrough: Fallthrough?,
        var prerequisites: List<Prerequisite>,
        var targets: Any,
        var rules: Array<Any>,
        var on: Boolean,
        var version: Number
) {}