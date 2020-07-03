package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.*
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.CardLayout
import javax.swing.JPanel

private const val SPLITTER_PROPERTY = "BuildAttribution.Splitter.Proportion"


class FlagPanel(private val myProject: Project) : SimpleToolWindowPanel(false, false), Disposable {
    private fun createTreeStructure(): SimpleTreeStructure {
        val getFlags = myProject.service<FlagStore>()
        val rootNode = RootNode(getFlags.flags)
        return FlagTreeStructure(myProject, rootNode)
    }

    override fun dispose() {}

    private fun initTree(model: AsyncTreeModel): Tree {
        val tree = Tree(model)
        tree.isRootVisible = false
        TreeSpeedSearch(tree).comparator = SpeedSearchComparator(false)
        TreeUtil.installActions(tree)
        return tree
    }

    init {
        val treeStucture = createTreeStructure()
        val treeModel = StructureTreeModel(treeStucture, this)

        var reviewTreeBuilder = AsyncTreeModel(treeModel, this)
        val tree = initTree(reviewTreeBuilder)

        val componentsSplitter = OnePixelSplitter(SPLITTER_PROPERTY, 0.33f)
        componentsSplitter.setHonorComponentsMinimumSize(true)
        componentsSplitter.setHonorComponentsMinimumSize(true)
        componentsSplitter.firstComponent = JPanel(CardLayout()).apply {
            add(ScrollPaneFactory.createScrollPane(tree, SideBorder.NONE), "Tree")
        }
        setContent(componentsSplitter)
    }
}