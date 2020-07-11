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
        private val key = "apiKey"
        private val credentialAttributes: CredentialAttributes =
                CredentialAttributes(generateServiceName(
                        "launchdarkly-intellij",
                        key
                ))
        var project = ""
        var environment = ""
        var apiKey = ""
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

    private val apiField = JPasswordField()
    val projectApi = LaunchDarklyApiClient.projectInstance(project)
    lateinit var projectContainer: MutableList<com.launchdarkly.api.model.Project>
    val origProject = settings.project
    val origEnv = settings.environment
    val origApiKey = settings.authorization
    //private val panel = JPanel()
    private lateinit var projectBox: DefaultComboBoxModel<String>

    //private var comboBox = ComboBox()()

    init {
        try {
            projectContainer = projectApi.projects.items
        } catch(err: ApiException) {
            println(err)
        }
    }

    override fun createPanel(): DialogPanel {
        var panel = panel {

            row("API Key:") { apiField().withTextBinding(PropertyBinding({ settings.authorization }, { settings.authorization = it })) }
            row("Environment:") { textField(settings::environment) }

            //row("Refresh Rate(in Minutes):") { intTextField(settings::refreshRate, 0, 0..1440) }
        }
        if (::projectContainer.isInitialized && projectContainer != null) {
            projectBox = DefaultComboBoxModel<String>(projectContainer.map { it -> it.key }.toTypedArray())

            panel.add(panel {
                row("Test") {
                    comboBox(projectBox, settings::project, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })
                }
            })
//            panel.add(panel {
//                row("Test") {
//                    comboBox(DefaultComboBoxModel<String>(projectContainer.map { it -> it.key }.toTypedArray()), settings::project)
//                }
//            })
        }
//                    comboBox(DefaultComboBoxModel<String>(projectContainer.map { it -> it.key}.toTypedArray()), settings::project, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
//                    label.text =
//                }

                //row("Projects:") { comboBox(projectContainer.map { it -> it.key}.toTypedArray()).selectedItem("default") }
//            })
//        }
        //row("Environment:") { textField(settings::environment) }

        return panel
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
        var modified: Boolean = false
        if(settings.authorization != origApiKey) {
            try {
                projectContainer = projectApi.projects.items
                createPanel()
                println("modified")
                println(settings.project)
                println(projectBox.selectedItem.toString())
            } catch(err: Error) {
                println(err)
            }
        }
        if (settings.project != projectBox.selectedItem.toString()) {
            modified = true
        }
        val sup = super.isModified()
        return modified
    }
    override fun apply() {
        println("New Project: ${settings.project}")
        println("Original Project: ${origProject}")
        println(settings.project)
        println(projectBox.selectedItem.toString())
        if (settings.project != projectBox.selectedItem.toString()) {
            settings.project = projectBox.selectedItem.toString()
        }

        super.apply()
        if (settings.project != projectBox.selectedItem.toString() || settings.environment != origEnv || settings.authorization != origApiKey) {
            val publisher = project.messageBus.syncPublisher(messageBusService.configurationEnabledTopic)
            publisher.notify(true)
            println("notifying")
        }

    }

}