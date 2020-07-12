package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.messaging.ConfigurationNotifier
import com.github.intheclouddan.intellijpluginld.messaging.DefaultMessageBusService
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.EdtExecutorService
import com.launchdarkly.api.model.FeatureFlags
import java.util.concurrent.TimeUnit

@Service
class FlagStore(project: Project) {
    //private val myProject: Project
    var flags: FeatureFlags
    private val messageBusService = project.service<DefaultMessageBusService>()

//    fun someServiceMethod(parameter: String?) {
//        val anotherService: AnotherService = myProject.getService(AnotherService::class.java)
//        val result: String = anotherService.anotherServiceMethod(parameter, false)
//        // do some more stuff
//    }

    fun flags (project: Project, settings: LaunchDarklyConfig.ConfigState): FeatureFlags {
        val envList = listOf(settings.environment)
        val ldProject: String = settings.project
        println(envList)
        println(ldProject)
        val getFlags = LaunchDarklyApiClient.flagInstance(project)
        return getFlags.getFeatureFlags(ldProject, envList, false, null, null, null, null, null, null)
    }

    fun flagsNotify(project: Project, settings: LaunchDarklyConfig.ConfigState): FeatureFlags {
            val publisher = project.messageBus.syncPublisher(messageBusService.flagsUpdatedTopic)
            flags = flags(project, settings)
            println(flags)
            publisher.notify(true)
            println("notifying Flags")

            return flags
        }

    init {
        val settings = LaunchDarklyConfig.getInstance(project).ldState
        var refreshRate: Long = settings.refreshRate.toLong()
        flags = flagsNotify(project, settings)
        EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay({ flags = flagsNotify(project, settings) }, refreshRate, refreshRate, TimeUnit.MINUTES)
        project.messageBus.connect().subscribe(messageBusService.configurationEnabledTopic,
                object : ConfigurationNotifier {
                    override fun notify(isConfigured: Boolean) {
                        println("notified")
                        if (isConfigured) {
                            flags = flagsNotify(project, settings)
                        }
                    }
                })
    }

}