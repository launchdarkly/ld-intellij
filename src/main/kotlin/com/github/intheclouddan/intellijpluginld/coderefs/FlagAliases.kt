package com.github.intheclouddan.intellijpluginld.coderefs

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


@Service
class FlagAliases(private var project: Project) {
    var aliases = mutableMapOf<String, String>()
    val cr = CodeRefs()
    val settings = LaunchDarklyMergedSettings.getInstance(project)

    fun readAliases(file: File) {
        csvReader().open(file) {
            readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                if (row["aliases"] !== "") {
                    //aliases.set(row["aliases"], row["flagKey"])
                    aliases[row["aliases"]!!] = row["flagKey"]!!
                }
            }
        }
    }

    fun runCodeRefs(project: Project) {
        try {
            val settings = LaunchDarklyMergedSettings.getInstance(project)
            val tmpDir = File(PathManager.getTempPath())

            val cmds = ArrayList<String>()
            val aliasesPath = File(project.basePath + "/.launchdarkly/coderefs.yaml")
            if (!aliasesPath.exists()) {
                return
            }
            cmds.add(PathManager.getPluginsPath() + "/intellij-plugin-ld/bin/coderefs/2.1.0/ld-find-code-refs")
            cmds.add("--dir=${project.basePath}")
            cmds.add("--dryRun")
            cmds.add("--outDir=$tmpDir")
            cmds.add("--projKey=${settings.project}")
            cmds.add("--repoName=${project.name}")
            cmds.add("--baseUri=${settings.baseUri}")
            cmds.add("--contextLines=-1")
            cmds.add("--branch=scan")
            cmds.add("--revision=0")
            val generalCommandLine = GeneralCommandLine(cmds)
            generalCommandLine.charset = Charset.forName("UTF-8")
            generalCommandLine.setWorkDirectory(project.basePath)
            generalCommandLine.withEnvironment("LD_ACCESS_TOKEN", settings.authorization)

            try {
                val process: Process = generalCommandLine.createProcess()
                val processHandler: OSProcessHandler =
                    KillableProcessHandler(process, generalCommandLine.commandLineString)
                processHandler.startNotify()
                var completed = processHandler.waitFor()
                while (completed) {
                    val aliasPath =
                        File(PathManager.getTempPath() + "/coderefs_${settings.project}_${project.name}_scan.csv")
                    readAliases(aliasPath)
                    break
                }
                Runtime.getRuntime().addShutdownHook(Thread(process::destroy));
            } catch (exception: ExecutionException) {
                println(exception)
            }

        } catch (err: Exception) {
            println(err)
        }

    }

    fun checkCodeRefs(): Boolean {
        val pluginPath = File("${cr.codeRefsPath}${cr.codeRefsVerion}")
        if (!pluginPath.exists()) {
            // If this version of CodeRefs is not downloaded. Wipe out bin dir of all versions and redownload.
            val binDir = File("${cr.codeRefsPath}")
            try {
                binDir.deleteRecursively()
                cr.downloadCodeRefs()
            } catch (err: Exception) {
                println(err)
                return false
            }
        }
        return true
    }

    init {

        val codeRefsFile = File(project.basePath + "/.launchdarkly/coderefs.yaml")
        val codeRefsConfig = LocalFileSystem.getInstance().findFileByIoFile(codeRefsFile)
        if (codeRefsConfig != null) {
            if (settings.codeReferences && codeRefsConfig.exists()) {
                AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                    Runnable {
                        val runnerCheck = checkCodeRefs()
                        if (runnerCheck) {
                            ApplicationManager.getApplication().executeOnPooledThread {
                                runCodeRefs(project)
                            }
                        }
                    },
                    0, settings.codeReferencesRefreshRate.toLong(), TimeUnit.MINUTES
                )
            }
        }
    }
}