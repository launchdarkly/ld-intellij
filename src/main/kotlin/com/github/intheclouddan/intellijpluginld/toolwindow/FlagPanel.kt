package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.action.CopyKeyAction
import com.github.intheclouddan.intellijpluginld.action.OpenInBrowserAction
import com.github.intheclouddan.intellijpluginld.action.RefreshAction
import com.github.intheclouddan.intellijpluginld.action.ToggleFlagAction
import com.github.intheclouddan.intellijpluginld.messaging.FlagNotifier
import com.github.intheclouddan.intellijpluginld.messaging.MessageBusService
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.ide.util.treeView.TreeState
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeLater
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
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


private const val SPLITTER_PROPERTY = "BuildAttribution.Splitter.Proportion"

/*
 * FlagPanel renders the ToolWindow Flag Treeview and associated action buttons.
 */
class FlagPanel(private val myProject: Project, messageBusService: MessageBusService) : SimpleToolWindowPanel(false, false), Disposable {
    private val settings = LaunchDarklyConfig.getInstance(myProject)
    private val getFlags = myProject.service<FlagStore>()
    private var root = RootNode(getFlags.flags, getFlags.flagConfigs, settings, myProject)
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
        TreeSpeedSearch(tree).comparator = SpeedSearchComparator(false)
        TreeUtil.installActions(tree)
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setEditable(true);

        return tree
    }

    fun updateNodeInfo() {
        root = RootNode(getFlags.flags, getFlags.flagConfigs, settings, myProject)
        treeStructure = createTreeStructure()
        treeModel = StructureTreeModel(treeStructure, this)
        var reviewTreeBuilder = AsyncTreeModel(treeModel, this)
        tree.model = reviewTreeBuilder
    }

    fun start(): Tree {
        // var treeStructure = createTreeStructure()

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
        setToolbar(actionToolbar.component)
        val refreshAction = actionManager.getAction(RefreshAction.ID)
        val copyKeyAction = actionManager.getAction(CopyKeyAction.ID)
        val toggleFlagAction = actionManager.getAction(ToggleFlagAction.ID)
        val openBrowserAction = actionManager.getAction(OpenInBrowserAction.ID)
        actionGroup.addAction(refreshAction)


        PopupHandler.installPopupHandler(
                tree,
                actionPopup.apply {
                    add(refreshAction)
                    add(copyKeyAction)
                    add(toggleFlagAction)
                    add(openBrowserAction)
                },
                ActionPlaces.POPUP,
                ActionManager.getInstance()
        )
    }

    /**
     * Invalidates tree nodes, causing IntelliJ to redraw the tree. Preserves node state.
     * Provide an AbstractTreeNode in order to redraw the tree from that point downwards
     * Otherwise redraws the entire tree
     *
     * @param selectedNode AbstractTreeNode to redraw the tree from
     */
    fun invalidateTree(selectedNode: DefaultMutableTreeNode?) {
        withSavedState(tree) {
            if (selectedNode != null) {
                treeModel.invalidate(selectedNode, false)
            } else {
                treeModel.invalidate()
            }
        }
    }

    // Save the state and reapply it after we invalidate (which is the point where the state is wiped).
    // Items are expanded again if their user object is unchanged (.equals()).
    private fun withSavedState(tree: Tree, block: () -> Unit) {
        val state = TreeState.createOn(tree)
        block()
        state.applyTo(tree)
    }

    fun updateNode(event: String) {
        var getFlags = myProject.service<FlagStore>()
        try {
            val defaultTree = tree.model as AsyncTreeModel
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
                        tree.setEditable(true);
                        parentNode = FlagNodeParent(flag!!, settings, getFlags.flags, myProject/*, getFlags.flagConfigs*/)
                        treeModel.invalidate(TreePath(parent), true)
                        break
                    }

                } else {
                    continue
                }
            }
            if (!found) {
                println("should be found")
                val flag = getFlags.flags.items.find { it.key == event }
                val root = tree.model.root as DefaultMutableTreeNode
                val newNode = DefaultMutableTreeNode()
                newNode.userObject = FlagNodeParent(flag!!, settings, getFlags.flags, myProject/*, getFlags.flagConfigs*/)
                tree.setEditable(true)
                root.add(newNode)
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
        try {
            myProject.messageBus.connect().subscribe(messageBusService.flagsUpdatedTopic,
                    object : FlagNotifier {
                        override fun notify(isConfigured: Boolean, flag: String) {
                            if (isConfigured) {
                                if (flag != "") {
                                    invokeLater {
                                        updateNode(flag)
                                    }
                                } else {
                                    start()
                                }
                            } else {
                                val notification = Notification("ProjectOpenNotification", "LaunchDarkly",
                                        String.format("LaunchDarkly Plugin is not configured"), NotificationType.WARNING);
                                notification.notify(myProject);
                            }
                        }

                        override fun reinit() {
                            updateNodeInfo()
                        }
                    })
        } catch (err: Error) {
            println(err)
            println("something went wrong")
        }
    }
}