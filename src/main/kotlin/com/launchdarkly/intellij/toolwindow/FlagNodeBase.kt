package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import javax.swing.Icon

class FlagNodeBase(var label: String, var labelIcon: Icon? = null) : SimpleNode() {
    override fun getChildren(): Array<SimpleNode> {
        return NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = label
        if (labelIcon != null) {
            data.setIcon(labelIcon)
        }
    }
}
