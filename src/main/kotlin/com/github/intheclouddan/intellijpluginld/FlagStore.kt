package com.github.intheclouddan.intellijpluginld

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.launchdarkly.api.model.FeatureFlags

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
        val settings = LaunchDarklyConfig.getInstance(project)
        val getFlags = LaunchDarklyApiClient.flagInstance(project)
        val envList = listOf("dano")
        flags = getFlags.getFeatureFlags("support-service", envList, null, null, null, null, null, null, null)
    }
}