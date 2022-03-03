package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.intellij.LDIcons

class InfoNode(var label: String) : SimpleNode() {
    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = label
        data.tooltip = label
        data.setIcon(LDIcons.LOGO)
    }
}
