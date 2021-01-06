package com.github.intheclouddan.intellijpluginld.settings

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

class LaunchDarklyMergedSettings(private val myProject: Project) : LDSettings {
    val projSettings = LaunchDarklyConfig.getInstance(myProject)
    val appSettings = LaunchDarklyApplicationConfig.getInstance()

    companion object {
        fun getInstance(project: Project): LaunchDarklyMergedSettings {
            return ServiceManager.getService(project, LaunchDarklyMergedSettings(project)::class.java)
        }
    }

    override var baseUri: String
        get() = if (projSettings.ldState.baseUri != "") projSettings.ldState.baseUri else {
            appSettings.ldState.baseUri
        }
        set(value) {}

    override var project: String
        get() = if (projSettings.ldState.project != "") projSettings.ldState.project else {
            appSettings.ldState.project
        }
        set(value) {}

    override var environment: String
        get() = if (projSettings.ldState.environment != "") projSettings.ldState.environment else {
            appSettings.ldState.environment
        }
        set(value) {}

    override var refreshRate: Int
        get() = if (projSettings.ldState.refreshRate == -1) appSettings.ldState.refreshRate else {
            projSettings.ldState.refreshRate
        }
        set(value) {}

    override var authorization: String
        get() = if (projSettings.ldState.authorization != "") projSettings.ldState.authorization else {
            appSettings.ldState.authorization
        }
        set(value) {}

    override var credName: String
        get() = if (projSettings.ldState.credName != "") projSettings.ldState.credName else {
            appSettings.ldState.credName
        }
        set(value) {}

    override var codeReferences: Boolean
        get() = appSettings.ldState.codeReferences
        set(value) {}

    override fun isConfigured(): Boolean {
        if (project == "" || environment == "" || authorization == "") {
            return false
        }
        return true
    }

    override var codeReferencesRefreshRate: Int
        get() = appSettings.ldState.refreshRate
        set(value) {}

    /*
     *   Check if project is using application defaults.
     */
    fun projectOverrides(): Boolean {
        if (projSettings.ldState.credName.isNotEmpty()
            || projSettings.ldState.authorization.isNotEmpty()
            || projSettings.ldState.refreshRate != -1
            || projSettings.ldState.environment.isNotEmpty()
            || projSettings.ldState.project.isNotEmpty()
            || projSettings.ldState.baseUri.isNotEmpty()
        ) {
            return true
        }
        return false
    }
}