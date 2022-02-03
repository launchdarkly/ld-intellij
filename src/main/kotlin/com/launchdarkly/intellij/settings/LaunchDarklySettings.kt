package com.launchdarkly.intellij.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.*
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.Environment
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.messaging.DefaultMessageBusService
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel

/*
 * Maintain state of what LaunchDarkly Project to connect to.
 */
@State(name = "LaunchDarklyConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyConfig(project: Project) : PersistentStateComponent<LaunchDarklyConfig.ConfigState> {
    val project: Project = project
    var ldState: ConfigState = ConfigState()

    companion object {
        fun getInstance(project: Project): LaunchDarklyConfig {
            return project.getService(LaunchDarklyConfig(project)::class.java)
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

    data class ConfigState(
        override var credName: String = "",
        override var project: String = "",
        override var environment: String = "",
        override var refreshRate: Int = -1,
        override var baseUri: String = "",
        override var codeReferences: Boolean = true,
        override var codeReferencesRefreshRate: Int = 240
    ) : LDSettings {
        private val key: String = "apiKey"

        // Stored in System Credential store
        override var authorization: String
            get() = PasswordSafe.instance.getPassword(
                CredentialAttributes(
                    generateServiceName(
                        "launchdarkly-intellij-$credName",
                        key
                    )
                )
            ) ?: ""
            set(value) {
                if (credName == "") {
                    return
                }
                val credentials = Credentials("", value)
                PasswordSafe.instance.set(
                    CredentialAttributes(
                        generateServiceName(
                            "launchdarkly-intellij-$credName",
                            key
                        )
                    ), credentials
                )
            }

        override fun isConfigured(): Boolean {
            if (project == "" || environment == "" || authorization == "") {
                return false
            }
            return true
        }

    }
}

class LaunchDarklyConfigurable(private val project: Project) : BoundConfigurable(displayName = "LaunchDarkly Plugin") {
    //private val apiField = JPasswordField()
    private val messageBusService = project.service<DefaultMessageBusService>()
    private val mergedSettings = project.service<LaunchDarklyMergedSettings>()
    private val settings = LaunchDarklyConfig.getInstance(project).ldState
    private var origApiKey = settings.authorization
    private var origBaseUri = settings.baseUri
    private var modified = false
    private var panel = JPanel()
    private var apiUpdate = false
    private var projectUpdatedSelection = false
    private var envUpdatedSelection = false
    private var lastSelectedProject = ""
    private lateinit var projectContainer: MutableList<com.launchdarkly.api.model.Project>
    private lateinit var environmentContainer: com.launchdarkly.api.model.Project

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
            defaultMessage = "Check API Key"
        }
    }

    override fun createPanel(): DialogPanel {
        val renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
            label.text = value
        }
        panel = panel {
            row {
                comment("Any settings manually selected here will override the corresponding Application settings.")
            }
            row {
                comment("Project and Environment selections will populate based on key permissions.")
            }
            try {
                projectBox = if (::projectContainer.isInitialized) {
                    DefaultComboBoxModel(projectContainer.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf(defaultMessage))
                }
                row("Project") {
                    comboBox(projectBox, renderer).bindItem(settings::project).component.addActionListener {
                        projectUpdatedSelection = true
                    }
                }

                environmentBox = if (::environmentContainer.isInitialized) {
                    DefaultComboBoxModel(environmentContainer.environments.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf("Please select a Project"))
                }
                row("Environments:") {
                    comboBox(environmentBox, renderer).bindItem(settings::environment)
                        .component.addActionListener {
                            envUpdatedSelection = true
                        }
                }

            } catch (err: Exception) {
                println(err)
            }
            collapsibleGroup("Advanced") {
                row {
                    comment("Leaving Refresh Rate as -1 will inherit value from Application settings.")
                }
                row("Refresh Rate(in Minutes):") { intTextField().bindIntText(settings::refreshRate) }
                row("Base URL:") { textField().bindText(settings::baseUri) }
            }
        }
        return panel as DialogPanel
    }

    override fun isModified(): Boolean {
        try {
            if (settings.authorization != origApiKey) {
                if (settings.credName != project.name) settings.credName = project.name
                settings.credName = project.name
            }
            val uri = if (settings.baseUri != origBaseUri) {
                if (settings.baseUri != "") settings.baseUri else mergedSettings.baseUri
            } else {
                mergedSettings.baseUri
            }
            try {
                if (::projectContainer.isInitialized && projectContainer?.size <= 2 && (projectContainer[0].key == "Check API and baseURL" || projectContainer[0].key == "Check API Key")) {
                    projectContainer = getProjects(settings.authorization, uri)
                } else if (settings.baseUri != origBaseUri) {
                    projectContainer = getProjects(settings.authorization, uri)
                } else {
                    if (!::projectContainer.isInitialized) {
                        val tempProj = tmpProj()
                        projectContainer = mutableListOf<com.launchdarkly.api.model.Project>(tempProj)
                    }
                }
            } catch (err: ApiException) {
                if (!::projectContainer.isInitialized) {
                    val tempProj = tmpProj()
                    projectContainer = mutableListOf<com.launchdarkly.api.model.Project>(tempProj)
                }
            }
            with(projectBox) {
                if (settings.authorization != origApiKey || settings.baseUri != origBaseUri) {
                    removeAllElements()
                    // After updating set everything to the same so it does not keep deleting elements.
                    origBaseUri = settings.baseUri
                    origApiKey = settings.authorization
                }
                if (selectedItem !== null && (selectedItem.toString() == "Check API Key" || selectedItem.toString() == "Check API and baseURL")) {
                    selectedItem = projectContainer.map { it.key }.firstOrNull()
                }
                if (projectContainer.size <= 2 && projectContainer[0].key == "Check API and baseURL") {
                    removeAllElements()
                }
                projectContainer.map { addElement(it.key) }
            }
            apiUpdate = true
        } catch (err: Error) {
            println(err)
        }

        if (::projectContainer.isInitialized && lastSelectedProject != projectBox?.selectedItem?.toString()) {
            lastSelectedProject = projectBox.selectedItem.toString()
            try {
                val tempProj = tmpProj()
                val projCont = projectContainer.find { it.key == projectBox?.selectedItem?.toString() } ?: tempProj

                environmentContainer = projCont
                val envMap = environmentContainer.environments.map { it.key }.sorted()
                if (::environmentBox.isInitialized) {
                    environmentBox.selectedItem = settings::environment
                    with(environmentBox) {
                        removeAllElements()
                        envMap.map { addElement(it) }
                        selectedItem =
                            if (settings.environment != "" && envMap.contains(settings.environment)) settings.environment else envMap.firstOrNull()
                    }
                    envUpdatedSelection = false // Set back to false because a user did not take an action
                }
            } catch (err: Error) {
                println(err)
            }
        }

        if ((::projectBox.isInitialized && projectBox.selectedItem != "") && projectUpdatedSelection) {
            if (settings.project != projectBox.selectedItem.toString()) {
                modified = true
            }
        }

        if ((::environmentBox.isInitialized && environmentBox.selectedItem != "") && envUpdatedSelection) {
            if (settings.environment != environmentBox.selectedItem.toString()) {
                modified = true
            }
        }

        val sup = super.isModified()
        return modified || sup
    }

    override fun apply() {
        super.apply()

        if (settings.project != projectBox.selectedItem.toString() && projectBox.selectedItem.toString() != defaultMessage) {
            settings.project = projectBox.selectedItem.toString()
        }

        if (settings.environment != environmentBox.selectedItem.toString() && environmentBox.selectedItem.toString() != "Please select a Project") {
            settings.environment = environmentBox.selectedItem.toString()
        }

        settings.credName = project.name
        if ((projectBox.selectedItem != "Check API Key" && projectBox.selectedItem != "Check API and baseURL") && modified) {
            println(projectBox.selectedItem)
            val publisher = project.messageBus.syncPublisher(messageBusService.configurationEnabledTopic)
            publisher.notify(true)
            println("notifying")
        }

    }

    private fun getProjects(apiKey: String?, baseUri: String?): MutableList<com.launchdarkly.api.model.Project> {
        val projectApi = LaunchDarklyApiClient.projectInstance(project, apiKey, baseUri)
        return projectApi.projects.items.sortedBy { it.key } as MutableList<com.launchdarkly.api.model.Project>
    }

    private fun tmpProj(): com.launchdarkly.api.model.Project {
        val tempProj = com.launchdarkly.api.model.Project()
        tempProj.key = "Check API and baseURL"
        val tempEnv = Environment()
        tempEnv.key("Check API and baseURL")
        tempProj.environments = listOf<Environment>(tempEnv)
        return tempProj
    }

}