package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.intellij.LDIcons
import java.util.*

const val KEY_PREFIX = "Key:"

class FlagNodeParent(private var viewModel: FlagNodeViewModel) : SimpleNode() {
    private var children: MutableList<SimpleNode> = ArrayList()
    val flag get() = viewModel.flag
    val key: String get() = viewModel.flag.key
    val isEnabled get() = viewModel.isEnabled == true

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
        if (viewModel.hasDescription) children.add(FlagNodeBase("Description: ${viewModel.description}", LDIcons.DESCRIPTION))
        children.add(FlagNodeVariations(flag))
        if (viewModel.hasPrereqs) children.add(FlagNodePrerequisites(viewModel.prereqFlags, viewModel.flags))
        if (viewModel.hasTargets) children.add(FlagNodeTargets(flag, viewModel.targets))
        if (viewModel.hasRules) children.add(FlagNodeBase("Rules: ${viewModel.numRules}", LDIcons.RULES))
        if (viewModel.hasFallthrough) children.add(FlagNodeFallthrough(flag, viewModel.flagConfig!!))
        if (viewModel.hasOffVariation) children.add(FlagNodeBase("Off Variation: ${viewModel.offVariation}", LDIcons.OFF_VARIATION))
        if (viewModel.hasTags) children.add(FlagNodeTags(viewModel.tags))
    }

    override fun update(data: PresentationData) {
        super.update(data)

        data.presentableText = viewModel.flagLabel
        data.setIcon(viewModel.icon)
    }
}
