package com.launchdarkly.intellij.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure

class FlagTreeStructure(private val myRootElement: SimpleNode) : SimpleTreeStructure() {
    override fun getRootElement(): SimpleNode {
        return myRootElement
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return true
    }
}
