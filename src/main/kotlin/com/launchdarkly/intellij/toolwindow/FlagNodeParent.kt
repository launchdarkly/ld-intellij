package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.intellij.LDIcons
import java.util.*

const val KEY_PREFIX = "Key:"

class FlagNodeParent(private var viewModel: FlagNodeViewModel) : SimpleNode() {
    private var children: MutableList<SimpleNode> = ArrayList()
    val flag get() = viewModel.flag
    val key get() = flag.key
    val isEnabled get() = viewModel.flagConfig.on

    override fun getChildren(): Array<SimpleNode> {
        if (children.isEmpty()) {
            buildChildren()
        } else {
            children = ArrayList()
            buildChildren()
        }
        return children.toTypedArray()
    }

    fun updateViewModel(viewModel: FlagNodeViewModel) {
        this.viewModel = viewModel
    }

    private fun buildChildren() {
        children.add(FlagNodeBase("$KEY_PREFIX ${flag.key}", LDIcons.FLAG_KEY))
        if (viewModel.description != "") children.add(FlagNodeBase("Description: ${viewModel.description}", LDIcons.DESCRIPTION))
        children.add(FlagNodeVariations(flag))
        if (viewModel.prereqFlags.isNotEmpty()) children.add(FlagNodePrerequisites(viewModel.prereqFlags, viewModel.flags))
        if (viewModel.targets.isNotEmpty()) children.add(FlagNodeTargets(flag, viewModel.targets))
        if (viewModel.numRules > 0) children.add(FlagNodeBase("Rules: ${viewModel.numRules}", LDIcons.RULES))
        if (viewModel.hasFallthrough) children.add(FlagNodeFallthrough(flag, viewModel.flagConfig))
        if (viewModel.hasOffVariation) children.add(FlagNodeBase("Off Variation: ${viewModel.offVariation}", LDIcons.OFF_VARIATION))
        if (flag.tags.size > 0) children.add(FlagNodeTags(flag.tags))
    }

    override fun update(data: PresentationData) {
        super.update(data)
        val enabledIcon =
            if (viewModel.isDisconnected) LDIcons.TOGGLE_DISCONNECTED
            else if (viewModel.flagConfig.on) LDIcons.TOGGLE_ON
            else LDIcons.TOGGLE_OFF
        val label = viewModel.flagLabel
        data.presentableText = label
        data.setIcon(enabledIcon)
    }
}
