package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.EdtExecutorService
import com.launchdarkly.api.model.FeatureFlags
import java.util.concurrent.TimeUnit

@Service
class FlagStore(project: Project) {
    //private val myProject: Project
    var flags: FeatureFlags

//    fun someServiceMethod(parameter: String?) {
//        val anotherService: AnotherService = myProject.getService(AnotherService::class.java)
//        val result: String = anotherService.anotherServiceMethod(parameter, false)
//        // do some more stuff
//    }

    fun flags (project: Project, settings: LaunchDarklyConfig.ConfigState): FeatureFlags {
        val envList = listOf(settings.environment)
        val ldProject: String = settings.project
        val getFlags = LaunchDarklyApiClient.flagInstance(project)
        return getFlags.getFeatureFlags(ldProject, envList, false, null, null, null, null, null, null)
    }

    init {
        val settings = LaunchDarklyConfig.getInstance(project).ldState
        var refreshRate: Long = settings.refreshRate.toLong()
        flags = flags(project, settings)
        EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay({ flags = flags(project, settings) }, refreshRate, refreshRate, TimeUnit.MINUTES)
    }

}