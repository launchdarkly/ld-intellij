package com.github.intheclouddan.intellijpluginld.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import javax.swing.JPasswordField


@State(name = "LaunchDarklyConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyConfig : PersistentStateComponent<LaunchDarklyConfig.ConfigState> {
    var ldState: ConfigState = ConfigState()

    companion object {
        fun getInstance(project: Project): LaunchDarklyConfig {
            return ServiceManager.getService(project, LaunchDarklyConfig::class.java)
        }
    }


    override fun getState(): ConfigState {
        return ldState
    }

    override fun loadState(state: ConfigState) {
        ldState = state
    }

    // Not in working state.
    fun isConfigured(): Boolean {
        if(ldState.project == "" || ldState.environment == "" || ldState.apiKey == "") {
            return false
        }
        return true
    }


    class ConfigState {

        var project = ""
        var environment = ""
        var apiKey = ""
        var refreshRate: Int = 120

    }
}

class LaunchDarklyConfigurable(private val project: Project): BoundConfigurable(displayName = "LaunchDarkly Plugin") {
    private val settings = LaunchDarklyConfig.getInstance(project).ldState
    val key = "apiKey"
    //val credentialAttributes = CredentialAttributes(generateServiceName("MySystem", key))
    private val credentialAttributes: CredentialAttributes =
            CredentialAttributes(generateServiceName(
                    "launchdarkly",
                    key
            ))
    private val apiField = JPasswordField()

    var authorization: String
        get() = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
        set(value) {
            val credentials = Credentials("", value)
            PasswordSafe.instance.set(credentialAttributes, credentials)
        }

    override fun createPanel(): DialogPanel {
        return panel {
            row("Project:") { textField(settings::project) }
            row("Environment:") { textField(settings::environment) }
            row("API Key:") { apiField().withTextBinding(PropertyBinding({ authorization }, { authorization = it })) }
            //row("Refresh Rate(in Minutes):") { intTextField(settings::refreshRate, 0, 0..1440) }
        }
    }

}