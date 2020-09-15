package com.github.intheclouddan.intellijpluginld.settings

import com.github.intheclouddan.intellijpluginld.LaunchDarklyApiClient
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel
import javax.swing.JPasswordField

val CHECK_API = "Check API Key"

/*
 * Maintain state of what LaunchDarkly Project to connect to.
 */
@State(name = "LaunchDarklyApplicationConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyApplicationConfig() : PersistentStateComponent<LaunchDarklyApplicationConfig.ConfigState> {
    var ldState: ConfigState = ConfigState()

    companion object {
        fun getInstance(): LaunchDarklyApplicationConfig {
            return ServiceManager.getService(LaunchDarklyApplicationConfig()::class.java)
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
        if (ldState.project == "" || ldState.environment == "" || ldState.authorization == "") {
            return false
        }
        return true
    }

    fun creds(key: String) {
        var setKey = ConfigState::credName.javaClass as String
        setKey = key
    }

    data class ConfigState(
            override var credName: String = "",
            override var project: String = "",
            override var environment: String = "",
            override var refreshRate: Int = 120,
            override var baseUri: String = "https://app.launchdarkly.com"
    ) : LDSettings {
        private val key = "apiKey"
        private val credentialAttributes: CredentialAttributes =
                CredentialAttributes(generateServiceName(
                        "launchdarkly-intellij",
                        key
                ))


        // Stored in System Credential store
        override var authorization: String
            get() = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
            set(value) {
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(credentialAttributes, credentials)
            }

        override fun isConfigured(): Boolean {
            if (project == "" || environment == "" || authorization == "") {
                return false
            }
            return true
        }
    }
}

class LaunchDarklyApplicationConfigurable() : BoundConfigurable(displayName = "LaunchDarkly Application Plugin") {
    private val apiField = JPasswordField()
    private val settings = LaunchDarklyApplicationConfig.getInstance().ldState
    private val origApiKey = settings.authorization
    private val origBaseUri = settings.baseUri
    private var modified = false
    private var panel = JPanel()
    private var apiUpdate = false
    private var lastSelectedProject = ""
    lateinit var projectContainer: MutableList<com.launchdarkly.api.model.Project>
    lateinit var environmentContainer: com.launchdarkly.api.model.Project

    private lateinit var defaultMessage: String
    private lateinit var projectBox: DefaultComboBoxModel<String>
    private lateinit var environmentBox: DefaultComboBoxModel<String>

    init {
        try {
            projectContainer = getProjects(null, null)
            if (projectContainer.size > 0) {
                environmentContainer = projectContainer.find { it.key == settings.project }
                        ?: projectContainer.firstOrNull() as com.launchdarkly.api.model.Project
            }
        } catch (err: Exception) {
            println(err)
            println(settings.authorization)
            println(settings.baseUri)
            defaultMessage = CHECK_API
        }
    }

    override fun createPanel(): DialogPanel {
        panel = panel {
            commentRow("Add your LaunchDarkly API Key and click Apply. Project and Environment selections will populate based on key permissions.")
            row("API Key:") { apiField().withTextBinding(PropertyBinding({ settings.authorization }, { settings.authorization = it })) }
            row("Refresh Rate(in Minutes):") { intTextField(settings::refreshRate) }
            row("Base URL:") { textField(settings::baseUri) }
            try {
                projectBox = if (::projectContainer.isInitialized) {
                    DefaultComboBoxModel(projectContainer.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf(defaultMessage))
                }
                row("Project") {
                    comboBox(projectBox, settings::project, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })

                }

                if (::environmentContainer.isInitialized) {
                    environmentBox = DefaultComboBoxModel(environmentContainer.environments.map { it.key }.toTypedArray())
                } else {
                    environmentBox = DefaultComboBoxModel(arrayOf("Please select a Project"))
                }
                row("Environments:") {
                    comboBox(environmentBox, settings::environment, renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
                        label.text = value
                    })
                }

            } catch (err: Exception) {
                println(err)
            }
        }
        return panel as DialogPanel
    }

    override fun isModified(): Boolean {
        if ((settings.authorization != origApiKey || settings.baseUri != origBaseUri) && !apiUpdate) {
            try {
                println(settings.baseUri)
                println(settings.authorization)
                projectContainer = getProjects(settings.authorization, settings.baseUri)
                with(projectBox) {
                    removeAllElements()
                    if (selectedItem == null || selectedItem.toString() == CHECK_API) {
                        selectedItem = projectContainer.map { it.key }.firstOrNull()
                    }
                    projectContainer.map { addElement(it.key) }
                }
                apiUpdate = true
            } catch (err: Error) {
                println(err)
            }
        }
//
//        if () {
//            try {
//                projectContainer = getProjects()
//                with(projectBox) {
//                    removeAllElements()
//                    if (selectedItem == null || selectedItem.toString() == CHECK_API) {
//                        selectedItem = projectContainer.map { it.key }.firstOrNull()
//                    }
//                    projectContainer.map { addElement(it.key) }
//                }
//                apiUpdate = true
//            } catch (err: Error) {
//                println(err)
//            }
//        }
        if (::projectContainer.isInitialized && lastSelectedProject != projectBox.selectedItem.toString()) {
            lastSelectedProject = projectBox.selectedItem.toString()
            try {
                environmentContainer = projectContainer.find { it.key == projectBox.selectedItem.toString() }!!
                val envMap = environmentContainer.environments.map { it.key }.sorted()
                if (::environmentBox.isInitialized) {
                    with(environmentBox) {
                        removeAllElements()
                        envMap.map { addElement(it) }
                        if (selectedItem == null || selectedItem.toString() == "Please select a Project") {
                            selectedItem = if (settings.environment != "") settings.environment else envMap.firstOrNull()
                        }
                    }
                }
            } catch (err: Error) {
                println(err)
            }
        }

        if (::projectBox.isInitialized || settings.project != projectBox.selectedItem.toString()) {
            modified = true
        }

        if (::environmentBox.isInitialized) {
            if (settings.environment != environmentBox.selectedItem.toString()) {
                modified = true
            }
        }

        val sup = super.isModified()
        return modified || sup
    }

    override fun apply() {
        super.apply()
        if ((projectBox.selectedItem != CHECK_API) && modified && origApiKey != "") {
            //val publisher = project.messageBus.syncPublisher(messageBusService.configurationEnabledTopic)
            //publisher.notify(true)
            println("notifying")
        }

        if (settings.project != projectBox.selectedItem.toString()) {
            settings.project = projectBox.selectedItem.toString()
        }
        if (settings.environment != environmentBox.selectedItem.toString()) {
            settings.environment = environmentBox.selectedItem.toString()
        }

    }

    fun getProjects(apiKey: String?, baseUri: String?): MutableList<com.launchdarkly.api.model.Project> {
        val projectApi = LaunchDarklyApiClient.projectInstance(null, apiKey, baseUri)
        return projectApi.projects.items.sortedBy { it.key } as MutableList<com.launchdarkly.api.model.Project>
    }

}
