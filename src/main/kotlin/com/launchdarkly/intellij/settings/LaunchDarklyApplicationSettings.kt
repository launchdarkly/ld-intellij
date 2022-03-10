package com.launchdarkly.intellij.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.*
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.Project as ApiProject
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.messaging.AppDefaultMessageBusService
import java.awt.event.ActionEvent
import javax.swing.*

/*
 * Maintain state of what LaunchDarkly Project to connect to.
 */
@State(name = "LaunchDarklyApplicationConfig", storages = [Storage("launchdarkly.xml")])
open class LaunchDarklyApplicationConfig : PersistentStateComponent<LaunchDarklyApplicationConfig.ConfigState> {
    var ldState: ConfigState = ConfigState()

    companion object {
        fun getInstance(): LaunchDarklyApplicationConfig {
            return ApplicationManager.getApplication().getService(LaunchDarklyApplicationConfig()::class.java)
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
        override var project: String = "",
        override var environment: String = "",
        override var refreshRate: Int = 120,
        override var baseUri: String = "https://app.launchdarkly.com",
        override var codeReferences: Boolean = true,
        override var codeReferencesRefreshRate: Int = 240
    ) : LDSettings {
        private val key = "apiKey"
        private val credentialAttributes: CredentialAttributes =
            CredentialAttributes(
                generateServiceName(
                    "launchdarkly-intellij",
                    key
                )
            )

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

class LaunchDarklyApplicationConfigurable : BoundConfigurable(displayName = "LaunchDarkly Application Plugin") {
    private val accessTokenField = JPasswordField()
    private val settings = LaunchDarklyApplicationConfig.getInstance().ldState
    private var origApiKey = settings.authorization
    private var origBaseUri = settings.baseUri
    private var modified = false
    private var panel = DialogPanel()
    private var apiUpdate = false
    private var lastSelectedProject: String? = ""
    private lateinit var projectContainer: MutableList<ApiProject>
    private lateinit var environmentContainer: ApiProject
    private lateinit var projectBox: DefaultComboBoxModel<String>
    private lateinit var environmentBox: DefaultComboBoxModel<String>
    private lateinit var projectComboBox: ComboBox<String>

    init {
        try {
            projectContainer = getProjects(null, null)
            if (projectContainer.size > 0) {
                environmentContainer = getEnvironmentContainer(settings.project)
            }
        } catch (err: Exception) {
            println("Error initializing")
        }
    }

    private fun getEnvironmentContainer(projectKey: String): ApiProject {
        return projectContainer.find { it.key == projectKey }
            ?: projectContainer.firstOrNull() as ApiProject
    }

    private fun onClickGetProjects(event: ActionEvent) {
        val btn = event.source as JButton
        btn.isEnabled = false
        btn.text = "Fetching Projects..."
        modified = true
        apply()
        reset()
        btn.isEnabled = true
        btn.text = "Get Projects"
    }

    override fun createPanel(): DialogPanel {
        val renderer = SimpleListCellRenderer.create<String> { label, value, _ ->
            label.text = value
        }

        panel = panel {
            row {
                text("Access feature flags in the IDE without having to navigate away from your current workflow.")
            }

            row {
                cell(accessTokenField)
                    .label("API Key:")
                    .bindText(settings::authorization)
                    .columns(COLUMNS_MEDIUM)
                    .validationOnInput(apiKeyValidation())
//                button("Get Projects") {
//                    onClickGetProjects(it)
//                }
                icon(AllIcons.General.BalloonWarning)
                    .label("Apply changes")
                    .visibleIf(refreshProjectsPredicate(accessTokenField))
            }.rowComment("Input the API key from your LaunchDarkly account. If you don’t have one, you must <a href=\"https://docs.launchdarkly.com/home/account-security/api-access-tokens#creating-api-access-tokens\">create an access token</a> first.")

            try {
                projectBox = if (::projectContainer.isInitialized) {
                    DefaultComboBoxModel(projectContainer.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel()
                }

                environmentBox = if (::environmentContainer.isInitialized) {
                    DefaultComboBoxModel(environmentContainer.environments.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel()
                }
                environmentBox.selectedItem = settings.environment

                indent {
                    rowsRange {
                        row("Project:") {
                            projectComboBox = comboBox(projectBox, renderer)
                                .bindItem(settings::project)
                                .applyToComponent {
                                    isEditable = true
                                    this.addItemListener { _ ->
                                        updateProjectEnvironments()
                                    }
                                }
                                .applyIfEnabled()
                                .validationOnInput {
                                    println("validation on input")
                                    val match = projectContainer.find { it.key == projectComboBox.selectedItem }
                                    println(match)
                                    if (match == null) {
                                        error("Not a valid project")
                                    } else {
                                        null
                                    }
                                }
                                .component
                        }
                        row("Environment:") {
                            comboBox(environmentBox, renderer)
                                .bindItem(settings::environment)
                                .applyToComponent {
                                    isEditable = true
                                }
                                .applyIfEnabled()
                        }
                    }.enabledIf(enableProjectsPredicate(accessTokenField))
                }
            } catch (err: Exception) {
                println(err)
            }

            collapsibleGroup("Advanced") {
                row {
                    textField()
                        .label("Base URL:")
                        .bindText(settings::baseUri)
                }
                row {
                    intTextField()
                        .label("Refresh flags every")
                        .bindIntText(settings::refreshRate)
                        .gap(RightGap.SMALL)
                    label("minutes")
                }
                lateinit var enableCodeRefs: Cell<JBCheckBox>
                row {
                    enableCodeRefs = checkBox("Use Code References").bindSelected(settings::codeReferences)
                }
                indent {
                    row {
                        intTextField()
                            .label("Refresh every")
                            .bindIntText(settings::codeReferencesRefreshRate)
                            .gap(RightGap.SMALL)
                        label("minutes")
                    }.enabledIf(enableCodeRefs.selected)
                }
            }
        }

        return panel
    }

    private fun updateOptions() {
        if (apiUpdate) {
            updateProjects()
        }

        if (::projectContainer.isInitialized && lastSelectedProject != projectBox.selectedItem?.toString()) {
            updateProjectEnvironments()
        }
    }

    private fun updateProjects() {
        if (!::projectBox.isInitialized) return
        try {
            projectContainer = getProjects(String(accessTokenField.password), settings.baseUri)
            with(projectBox) {
                removeAllElements()
                selectedItem = projectContainer.map { it.key }.firstOrNull()
                projectContainer.map { addElement(it.key) }
            }
            apiUpdate = false
        } catch (err: ApiException) {
            println("caught error")
            println(err)
            with(projectBox) {
                removeAllElements()
            }
            settings.project = ""
            settings.environment = ""
            projectBox.selectedItem = null
            environmentBox.selectedItem = null
        }
    }

    private fun updateProjectEnvironments() {
        if (!::projectContainer.isInitialized) return
        with(environmentBox) {
            removeAllElements()
        }

        lastSelectedProject = projectBox.selectedItem?.toString() ?: return

        try {
            environmentContainer = getEnvironmentContainer(projectBox.selectedItem.toString())
            val envMap = environmentContainer.environments.map { it.key }.sorted()
            with(environmentBox) {
                envMap.map { addElement(it) }
                selectedItem =
                    if (settings.environment != "" && envMap.contains(settings.environment)) settings.environment else envMap.firstOrNull()
            }
        } catch (err: Error) {
            println(err)
        }
    }

    override fun apply() {
        super.apply()

        settings.baseUri = settings.baseUri.replace(Regex("/+$"), "")

        settings.project = projectBox.selectedItem?.toString() ?: settings.project

        settings.environment = environmentBox.selectedItem?.toString() ?: settings.environment

        if (settings.authorization != origApiKey || settings.baseUri != origBaseUri) {
            apiUpdate = true
            origApiKey = settings.authorization
            origBaseUri = settings.baseUri
        }

        updateOptions()
        val appMsgService = ApplicationManager.getApplication().messageBus
        val topic = service<AppDefaultMessageBusService>().configurationEnabledTopic

        val publisher = appMsgService.syncPublisher(topic)
        publisher.notify(true)
       try {
           reset()
       } catch (err: Exception) {
           println(err)
       }
    }

    private fun getProjects(apiKey: String?, baseUri: String?): MutableList<ApiProject> {
        val projectApi = LaunchDarklyApiClient.projectInstance(apiKey, baseUri)
        return projectApi.projects.items.sortedBy { it.key } as MutableList<ApiProject>
    }

    private fun validKey(apiKey: String): Boolean {
        return try {
            LaunchDarklyApiClient.testAccessToken(apiKey, settings.baseUri)
            true
        } catch (e: ApiException) {
            println(e)
            false
        }
    }

    private fun apiKeyValidation(): ValidationInfoBuilder.(JPasswordField) -> ValidationInfo? = {
        if (String(it.password).trim() == "") {
            error("API key is required")
        } else if (!String(it.password).startsWith("api-")) {
            error("Key should start with \"api-\"")
        } else if (!validKey(String(it.password))) {
            // TODO - see if we can move this to an apply level validation
            error("This API key is not authorized to get projects")
        } else {
            null
        }
    }

    private fun refreshProjectsPredicate(component: JPasswordField): ComponentPredicate {
        return component.enteredTextSatisfies { String(component.password).trim() != "" } and
                component.enteredTextSatisfies { origApiKey != String(component.password) }
    }

    private fun enableProjectsPredicate(component: JPasswordField): ComponentPredicate {
        return component.enteredTextSatisfies { String(component.password).trim() != "" } and
                component.enteredTextSatisfies { origApiKey == String(component.password) } and
                projectComboBox.hasOptions { it.itemCount > 0 }
    }
}

fun <T> JComboBox<T>.hasOptions(predicate: (JComboBox<T>) -> Boolean): ComponentPredicate {
    return ComboBoxPredicate(this, predicate)
}

class ComboBoxPredicate<T>(private val comboBox: JComboBox<T>, private val predicate: (JComboBox<T>) -> Boolean) : ComponentPredicate() {
    override fun invoke(): Boolean = predicate(comboBox)

    override fun addListener(listener: (Boolean) -> Unit) {
        comboBox.addActionListener {
            listener(invoke())
        }
    }
}
