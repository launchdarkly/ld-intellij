package com.github.intheclouddan.intellijpluginld.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeStructure


class FlagTreeStructure(private val myProject: Project, private val myRootElement: SimpleNode) : SimpleTreeStructure() {
    override fun getRootElement(): SimpleNode {
        return myRootElement
    }

}