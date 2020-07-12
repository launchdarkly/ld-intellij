package com.github.intheclouddan.intellijpluginld.settings

import com.github.intheclouddan.intellijpluginld.LaunchDarklyApiClient
import com.github.intheclouddan.intellijpluginld.messaging.ConfigurationNotifier
import com.github.intheclouddan.intellijpluginld.messaging.DefaultMessageBusService
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.Projects
import jdk.jshell.Diag
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel
import javax.swing.JPasswordField


@State(name = "LaunchDarklyConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyConfig (project: Project): PersistentStateComponent<LaunchDarklyConfig.ConfigState> {
    var ldState: ConfigState = ConfigState()
    val project: Project = project

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
        if(ldState.project == "" || ldState.environment == "" || ldState.authorization == "") {
            println(ldState)
            return false
        }
        return true
    }

    fun credentialNamespace(): String {
        val CRED_NAMESPACE = "launchdarkly-intellij"
        println(project.name)
        return CRED_NAMESPACE + "-" + project.name
    }

    inner class ConfigState {
        private val key = "apiKey"
        private val credentialAttributes: CredentialAttributes =
                CredentialAttributes(generateServiceName(
                        credentialNamespace(),
                        key
                ))
        var project = ""
        var environment = ""
        var refreshRate: Int = 120
        var authorization: String
            get() = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
            set(value) {
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(credentialAttributes, credentials)
            }
    }
}

class LaunchDarklyConfigurable(private val project: Project): BoundConfigurable(displayName = "LaunchDarkly Plugin") {
    private val settings = LaunchDarklyConfig.getInstance(project).ldState
    private val messageBusService = project.service<DefaultMessageBusService>()
    private var modified = false

    private val apiField = JPasswordField()
    val projectApi = LaunchDarklyApiClient.projectInstance(project)
    val environmentApi = LaunchDarklyApiClient.environmentInstance(project)

    lateinit var projectContainer: MutableList<com.launchdarkly.api.model.Project>
    lateinit var environmentContainer: com.launchdarkly.api.model.Project

    val origProject = settings.project
    val origEnv = settings.environment
    val origApiKey = settings.authorization
    private var panel = JPanel()
    private lateinit var projectBox: DefaultComboBoxModel<String>
    private lateinit var environmentBox: DefaultComboBoxModel<String>

    //private var comboBox = ComboBox()()

    init {
        try {
            projectContainer = projectApi.projects.items
            if (projectContainer != null) {
                environmentContainer = projectContainer.find { it.key == settings.project }!!
            }
        } catch(err: ApiException) {
            println(err)
        }
    }

    override fun createPanel(): DialogPanel {
        panel = panel {

            row("API Key:") { apiField().withTextBinding(PropertyBinding({ settings.authorization }, { settings.authorization = it })) }
            //row("Environment:") { textField(settings::environment) }

            //row("Refresh Rate(in Minutes):") { intTextField(settings::refreshRate, 0, 0..1440) }

            if (::projectContainer.isInitialized && projectContainer != null) {
                projectBox = DefaultComboBoxModel<String>(projectContainer.map { it -> it.key }.toTypedArray())
                row("Project") {
                    comboBox(projectBox, settings::project, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })
                }
            }

            if (::environmentContainer.isInitialized) {
                println(environmentContainer)
                environmentBox = DefaultComboBoxModel<String>(environmentContainer.environments.map { it -> it.key }.toTypedArray())

                row("Environments:") {
                    comboBox(environmentBox, settings::environment, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })
                }
            }
        }
        return panel as DialogPanel
    }

//    override fun updatePanel(): DialogPanel {
//        if (::projectContainer.isInitialized && projectContainer != null) {
//            panel.add( panel {
//                row("Projects:") { ComboBox(projectContainer.map { it -> it.key}.toTypedArray())() }
//            })
//        }
//
//    }

    override fun isModified(): Boolean {
        if(settings.authorization != origApiKey) {
            try {
                projectContainer = projectApi.projects.items
                println("modified")
                println(settings.project)
                println(projectBox.selectedItem.toString())
                panel.repaint()
            } catch(err: Error) {
                println(err)
            }
        }
        if(settings.project != projectBox.selectedItem.toString()) {
            try {
                environmentContainer = projectContainer.find { it.key == projectBox.selectedItem.toString() }!!
                environmentBox.removeAllElements()
                environmentBox.selectedItem = environmentContainer.environments.map { it -> it.key }.firstOrNull()
                environmentBox.addAll(environmentContainer.environments.map { it -> it.key })
                panel.repaint()
                println("modified environment")
            } catch(err: Error) {
                println(err)
            }
        }

        if (settings.project != projectBox.selectedItem.toString() || settings.environment != environmentBox.selectedItem.toString() ) {
            modified = true
        }
        val sup = super.isModified()
        return modified || sup
    }

    override fun apply() {
        super.apply()
        if (modified == true) {
            val publisher = project.messageBus.syncPublisher(messageBusService.configurationEnabledTopic)
            publisher.notify(true)
            println("notifying")
        }

        if (settings.project != projectBox.selectedItem.toString()) {
            settings.project = projectBox.selectedItem.toString()
        }



    }

}