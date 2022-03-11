package com.launchdarkly.intellij.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon

class LinkNodeBase(var label: String, val url: String, var labelIcon: Icon? = null) : SimpleNode() {
    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = label
        data.tooltip = label
        data.setIcon(AllIcons.Ide.External_link_arrow)
    }
}
