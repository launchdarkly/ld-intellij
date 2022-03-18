package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import java.util.*

const val KEY_PREFIX = "Key:"

class FlagNodeParent(private var model: FlagNodeModel) : SimpleNode() {
    private var children: MutableList<SimpleNode> = ArrayList()
    var flag = model.flag
    var key = flag.key

    override fun getChildren(): Array<SimpleNode> {
        if (children.isEmpty()) {
            buildChildren()
        } else {
            children = ArrayList()
            buildChildren()
        }
        return children.toTypedArray()
    }

    fun updateModel(model: FlagNodeModel) {
        this.model = model
    }

    private fun buildChildren() {
        children.add(FlagNodeBase("$KEY_PREFIX ${flag.key}", LDIcons.FLAG_KEY))
        if (flag.description != "") children.add(FlagNodeBase("Description: ${flag.description}", LDIcons.DESCRIPTION))
        children.add(FlagNodeVariations(flag))
        if (model.prereqFlags.isNotEmpty()) children.add(FlagNodePrerequisites(model.prereqFlags, model.flags))
        if (model.targets.isNotEmpty()) children.add(FlagNodeTargets(flag, model.targets))
        if (model.numRules > 0) children.add(FlagNodeBase("Rules: ${model.numRules}", LDIcons.RULES))
        if (model.hasFallthrough) children.add(FlagNodeFallthrough(flag, model.flagConfig))
        if (model.hasOffVariation) children.add(FlagNodeBase("Off Variation: ${model.offVariation}", LDIcons.OFF_VARIATION))
        if (flag.tags.size > 0) children.add(FlagNodeTags(flag.tags))
    }

    override fun update(data: PresentationData) {
        super.update(data)
        val enabledIcon =
           if (model.isDisconnected) LDIcons.TOGGLE_DISCONNECTED
           else if (model.flagConfig.on) LDIcons.TOGGLE_ON
           else LDIcons.TOGGLE_OFF
        val label = model.flagLabel
        data.presentableText = label
        data.setIcon(enabledIcon)
    }
}
