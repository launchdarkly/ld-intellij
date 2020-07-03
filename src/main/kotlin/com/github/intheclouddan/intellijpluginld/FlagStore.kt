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

    init {
        val settings = LaunchDarklyConfig.getInstance(project).ldState
        val getFlags = LaunchDarklyApiClient.flagInstance(project)
        //val envList = listOf("dano")
        val envList = listOf(settings.environment)
        var refreshRate: Long = settings.refreshRate.toLong()
        val ldProject: String = settings.project
        flags = getFlags.getFeatureFlags(ldProject, envList, null, null, null, null, null, null, null)
        EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay(object : Runnable {
            override fun run() {
                flags = getFlags.getFeatureFlags( ldProject, envList, null, null, null, null, null, null, null)
            }

        }, refreshRate, refreshRate, TimeUnit.MINUTES)

    }
}