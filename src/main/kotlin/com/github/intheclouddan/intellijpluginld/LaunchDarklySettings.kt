package com.github.intheclouddan.intellijpluginld

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.ui.layout.panel
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name = "LaunchDarklyConfig")
class LaunchDarklyConfig : PersistentStateComponent<LaunchDarklyConfig> {
    //var myState = State()
    var project: String = ""
    var environment: String = ""
    var apiKey: String = ""

    companion object {
        fun getInstance(project: Project): LaunchDarklyConfig {
            return ServiceManager.getService(project, LaunchDarklyConfig::class.java)
        }
    }

    override fun getState() = this

    override fun loadState(state: LaunchDarklyConfig) = XmlSerializerUtil.copyBean(LaunchDarklyConfig, this)

}

class LaunchDarklyConfigurable(private val project: Project): BoundConfigurable(displayName = "LaunchDarkly Plugin") {
    private val settings = LaunchDarklyConfig.getInstance(project)

    override fun createPanel(): DialogPanel {
        return panel {
            row("Project:") { textField(settings::project) }
            row("Environment:") { textField(settings::environment) }
            row("API Key:") { textField(settings::apiKey) }
        }
    }

}