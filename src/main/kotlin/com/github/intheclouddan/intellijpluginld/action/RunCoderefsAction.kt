package com.github.intheclouddan.intellijpluginld.action

import com.github.intheclouddan.intellijpluginld.toolwindow.FlagToolWindow
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtilRt
import java.nio.charset.Charset
import javax.swing.Icon

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
class RunCoderefsAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to
     * instantiate this class based on plugin.xml declarations. Only needed in PopupDialogAction
     * class because a second constructor is overridden.
     * @see AnAction.AnAction
     */
    constructor() : super()

    companion object {
        const val ID = "com.github.intheclouddan.intellijpluginld.action.RunCoderefsAction"
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     * @param text  The text to be displayed as a menu item.
     * @param description  The description of the menu item.
     * @param icon  The icon to be used with the menu item.
     */
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)

    /**
     * Gives the user feedback when the dynamic action menu is chosen.
     * Pops a simple message dialog. See the psi_demo plugin for an
     * example of how to use AnActionEvent to access data.
     * @param event Event received when the associated menu item is chosen.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!
        val projectPath = project.basePath
        val tmpDir = FileUtilRt.createTempDirectory("ld-", null)
        val commands = arrayListOf("")
        val aliasFile = "${tmpDir}/coderefs_temp_file_scan.csv"
        commands.add("ld-find-code-refs")
        commands.add("--dir=${projectPath}")
        commands.add("--dryRun")
        commands.add("--outDir=${tmpDir}")
        commands.add("--repoName=CHANGEME")
        commands.add("--projectKey=${project.name}")
        commands.add("--baseUri=CHANGEME")
        commands.add("--contextLines=-1")
        commands.add("--branch=scan")
        commands.add("--revision=0")
        val generalCommandLine = GeneralCommandLine(commands)
        val procEnv = mapOf("LD_ACCESS_TOKEN" to "test", "GOMAXPROCS" to "1")
        generalCommandLine.withEnvironment(procEnv)
        generalCommandLine.setCharset(Charset.forName("UTF-8"))
        //generalCommandLine.setWorkDirectory()
        val snykResultJsonStr = ScriptRunnerUtil.getProcessOutput(generalCommandLine)

    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project != null) {
            if (project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent != null) {
                val selectedNode = project.service<FlagToolWindow>().getPanel().tree.lastSelectedPathComponent.toString()
                e.presentation.isEnabledAndVisible = e.presentation.isEnabled && (selectedNode.startsWith("Fallthrough"))
            }
        }
    }
}