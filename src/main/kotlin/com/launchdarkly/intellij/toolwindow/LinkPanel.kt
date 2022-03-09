package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import com.launchdarkly.intellij.messaging.FlagNotifier
import com.launchdarkly.intellij.messaging.MessageBusService
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import java.awt.CardLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

private const val SPLITTER_PROPERTY = "BuildAttribution.Splitter.Proportion"

/*
 * FlagPanel renders the ToolWindow Flag Treeview and associated action buttons.
 */
class LinkPanel(private val myProject: Project, messageBusService: MessageBusService) :
    SimpleToolWindowPanel(false, false), Disposable {
    private val settings = LaunchDarklyApplicationConfig.getInstance().ldState
    private var root = LinkNodeRoot(settings)
    private var treeStructure = createTreeStructure()
    private var treeModel = StructureTreeModel(treeStructure, this)
    lateinit var tree: Tree

    private fun createTreeStructure(): SimpleTreeStructure {
        return FlagTreeStructure(myProject, root)
    }

    override fun dispose() {}

    private fun initTree(model: AsyncTreeModel): Tree {
        tree = Tree(model)
        tree.isRootVisible = false
        FlagTreeSearch(tree)
        TreeUtil.installActions(tree)
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        return tree
    }

    fun start(): Tree {
        var reviewTreeBuilder = AsyncTreeModel(treeModel, this)
        tree = initTree(reviewTreeBuilder)

        val componentsSplitter = OnePixelSplitter(SPLITTER_PROPERTY, 0.33f)
        componentsSplitter.setHonorComponentsMinimumSize(true)
        componentsSplitter.setHonorComponentsMinimumSize(true)
        componentsSplitter.firstComponent = JPanel(CardLayout()).apply {
            add(ScrollPaneFactory.createScrollPane(tree, SideBorder.NONE), "Tree")
        }
        setContent(componentsSplitter)
        return tree
    }

    private val listener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.button != BUTTON1) {
                return
            }
            val tree = e.component as Tree
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
            val userNode = node.userObject as LinkNodeBase
            BrowserLauncher.instance.open(userNode.url)
            tree.clearSelection()
        }
    }

    fun actions(tree: Tree) {
        tree.addMouseListener(listener)
    }

    fun updateNodeInfo() {
        root = LinkNodeRoot(settings)
        treeStructure = createTreeStructure()
        treeModel = StructureTreeModel(treeStructure, this)
        var reviewTreeBuilder = AsyncTreeModel(treeModel, this)
        tree.model = reviewTreeBuilder
    }

    init {
        if (settings.isConfigured()) {
            tree = start()
            actions(tree)
        }
        var start = false
        if (!this::tree.isInitialized) {
            start = true
        }

        try {
            myProject.messageBus.connect().subscribe(
                messageBusService.flagsUpdatedTopic,
                object : FlagNotifier {
                    override fun notify(isConfigured: Boolean, flag: String, rebuild: Boolean) {
                    }

                    override fun reinit() {
                        if (start) {
                            tree = start()
                            actions(tree)
                        }
                        UIUtil.invokeLaterIfNeeded {
                            updateNodeInfo()
                        }
                    }
                }
            )
        } catch (err: Error) {
            Utilities.handlePanelError(err, myProject)
        }
    }
}
