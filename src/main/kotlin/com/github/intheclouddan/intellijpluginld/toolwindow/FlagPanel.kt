package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.action.*
import com.github.intheclouddan.intellijpluginld.messaging.FlagNotifier
import com.github.intheclouddan.intellijpluginld.messaging.MessageBusService
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.UIUtil.invokeLaterIfNeeded
import com.intellij.util.ui.tree.TreeUtil
import java.awt.CardLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


private const val SPLITTER_PROPERTY = "BuildAttribution.Splitter.Proportion"

/*
 * FlagPanel renders the ToolWindow Flag Treeview and associated action buttons.
 */
class FlagPanel(private val myProject: Project, messageBusService: MessageBusService) :
    SimpleToolWindowPanel(false, false), Disposable {
    private val settings = LaunchDarklyMergedSettings.getInstance(myProject)
    private var getFlags = myProject.service<FlagStore>()
    private var root = RootNode(getFlags.flags, settings, myProject)
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

    fun updateNodeInfo() {
        root = RootNode(getFlags.flags, settings, myProject)
        treeStructure = createTreeStructure()
        treeModel = StructureTreeModel(treeStructure, this)
        var reviewTreeBuilder = AsyncTreeModel(treeModel, this)
        tree.model = reviewTreeBuilder
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

    fun actions(tree: Tree) {
        val actionManager: ActionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup()
        val actionPopup = DefaultActionGroup()
        val actionToolbar: ActionToolbar = actionManager.createActionToolbar("ACTION_TOOLBAR", actionGroup, true)
        toolbar = actionToolbar.component
        val refreshAction = actionManager.getAction(RefreshAction.ID)
        val copyKeyAction = actionManager.getAction(CopyKeyAction.ID)
        val toggleFlagAction = actionManager.getAction(ToggleFlagAction.ID)
        val openBrowserAction = actionManager.getAction(OpenInBrowserAction.ID)
        val changeFallthroughAction = actionManager.getAction(ChangeFallthroughAction.ID)
        val changeOffVariationAction = actionManager.getAction(ChangeOffVariationAction.ID)

        actionGroup.addAction(refreshAction)


        PopupHandler.installPopupHandler(
            tree,
            actionPopup.apply {
                add(refreshAction)
                add(copyKeyAction)
                add(toggleFlagAction)
                add(openBrowserAction)
                add(changeFallthroughAction)
                add(changeOffVariationAction)
            },
            ActionPlaces.POPUP,
            ActionManager.getInstance()
        )
    }

    fun updateNode(event: String) {
        var getFlags = myProject.service<FlagStore>()
        try {
            val defaultTree = tree.model as AsyncTreeModel
            if (defaultTree.root === null) {
                return
            }
            val root = defaultTree.root as DefaultMutableTreeNode
            val e = root.depthFirstEnumeration()
            var found = false
            while (e.hasMoreElements()) {
                val node = e.nextElement()
                val parent = node as DefaultMutableTreeNode
                if (parent.userObject is FlagNodeParent) {
                    var parentNode = parent.userObject as FlagNodeParent
                    if (parentNode.key == event) {
                        found = true
                        val flag = getFlags.flags.items.find { it.key == parentNode.key }
                        if (flag != null && getFlags.flagConfigs[flag.key] != null) {
                            parentNode = FlagNodeParent(flag, getFlags.flags, myProject)
                            treeModel.invalidate(TreePath(parent), true)
                        } else {
                            // If the flag does not exist in the SDK DataStore it should not be part of Environment.
                            getFlags.flags.items.remove(flag)
                            updateNodeInfo()
                        }
                        break
                    }

                } else {
                    continue
                }
            }
            if (!found) {
                // re-render tree because it does not support adding/removing nodes in TreeStructureModel.
                updateNodeInfo()
            }
        } catch (e: Error) {
            println(e)
        }
    }

    fun updateNodes() {
        var getFlags = myProject.service<FlagStore>()

        try {
            val defaultTree = tree.model as AsyncTreeModel
            if (defaultTree.root != null) {
                val root = defaultTree.root as DefaultMutableTreeNode
                val flagFind = getFlags.flags.items
                for (flag in flagFind) {
                    var found = false
                    var e = root.depthFirstEnumeration()
                    while (e.hasMoreElements()) {
                        val node = e.nextElement()
                        val parent = node as DefaultMutableTreeNode
                        if (parent.userObject is FlagNodeParent) {
                            var parentNode = parent.userObject as FlagNodeParent
                            if (parentNode.key == flag.key && parentNode.flag.version < flag.version && getFlags.flagConfigs[flag.key] !== null) {
                                found = true
                                parentNode = FlagNodeParent(flag, getFlags.flags, myProject)
                                treeModel.invalidate(TreePath(parent), true)
                                break
                            }
                            if (parentNode.key == flag.key) {
                                found = true
                                break
                            }
                        }
                    }

                    if (!found) {
                        invokeLaterIfNeeded() {
                            updateNodeInfo()
                        }
                    }
                    tree.invalidate()
                }
            }
        } catch (e: Error) {
            println(e)
        }
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
            myProject.messageBus.connect().subscribe(messageBusService.flagsUpdatedTopic,
                object : FlagNotifier {
                    override fun notify(isConfigured: Boolean, flag: String, rebuild: Boolean) {
                        if (isConfigured) {
                            if (start) {
                                tree = start()
                                actions(tree)
                            }
                            when {
                                flag != "" -> {
                                    invokeLaterIfNeeded() {
                                        updateNode(flag)
                                    }
                                }
                                rebuild -> {
                                    invokeLaterIfNeeded {
                                        updateNodes()
                                    }
                                }
                                else -> {
                                    invokeLaterIfNeeded {
                                        start()
                                    }
                                }
                            }
                        } else {
                            val notification = Notification(
                                "ProjectOpenNotification", "LaunchDarkly",
                                String.format("LaunchDarkly Plugin is not configured"), NotificationType.WARNING
                            )
                            notification.notify(myProject)
                        }
                    }

                    override fun reinit() {
                        if (start) {
                            tree = start()
                            actions(tree)
                        }
                        invokeLaterIfNeeded {
                            updateNodeInfo()
                        }
                    }
                })
        } catch (err: Error) {
            println(err)
            println("something went wrong")
        }
    }
}