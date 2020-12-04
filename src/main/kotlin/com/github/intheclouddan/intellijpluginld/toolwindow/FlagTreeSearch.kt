package com.github.intheclouddan.intellijpluginld.toolwindow

import com.intellij.ui.TreeSpeedSearch
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class FlagTreeSearch(tree: JTree) : TreeSpeedSearch(tree) {
    override fun getElementText(element: Any?): String? {
        val path: TreePath = element as TreePath
        val node = path.getLastPathComponent() as DefaultMutableTreeNode;
        val flagNode = node.userObject;
        if (flagNode is FlagNodeParent) {
            return flagNode.key + " " + flagNode.toString()
        } else {
            return flagNode.toString()
        }
    }
}