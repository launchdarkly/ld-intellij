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
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.and
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.ui.layout.or
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.Project as ApiProject
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.constants.DEFAULT_BASE_URI
import com.launchdarkly.intellij.messaging.AppDefaultMessageBusService
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
        override var baseUri: String = DEFAULT_BASE_URI,
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
    private val baseUriField = JTextField()
    private val settings = LaunchDarklyApplicationConfig.getInstance().ldState
    private var origApiKey = settings.authorization
    private var origBaseUri = settings.baseUri
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
                    .label("Access token:")
                    .bindText(settings::authorization)
                    .columns(COLUMNS_MEDIUM)
                    .validationOnInput(accessTokenValidation())
                icon(AllIcons.General.BalloonWarning)
                    .label("Apply changes")
                    .visibleIf(refreshProjectsPredicate())
            }

            row {
                comment("Input the access token from your LaunchDarkly account. If you don’t have one, you must <a href=\"https://docs.launchdarkly.com/home/account-security/api-access-tokens#creating-api-access-tokens\">create an access token</a> first.")
            }

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
                                .bindItem(settings::project.toNullableProperty())
                                .applyToComponent {
                                    this.addItemListener { _ ->
                                        updateProjectEnvironments()
                                    }
                                }
                                .applyIfEnabled()
                                .component
                        }
                        row("Environment:") {
                            comboBox(environmentBox, renderer)
                                .bindItem(settings::environment.toNullableProperty())
                                .applyIfEnabled()
                        }
                    }.enabledIf(enableProjectsPredicate())
                }
            } catch (err: Exception) {
                println(err)
            }

            collapsibleGroup("Advanced") {
                row {
                    cell(baseUriField)
                        .label("Base URL:")
                        .bindText(settings::baseUri)
                        .columns(COLUMNS_MEDIUM)
                        .component
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
            println("Error updating projects: $err")
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

    private fun isValidAccessToken(apiKey: String, baseUri: String): Boolean {
        return try {
            LaunchDarklyApiClient.testAccessToken(apiKey, baseUri)
            true
        } catch (e: ApiException) {
            println(e)
            false
        }
    }

    private fun accessTokenValidation(): ValidationInfoBuilder.(JPasswordField) -> ValidationInfo? = {
        if (String(it.password).trim() == "") {
            error("Access token is required")
        } else if (!String(it.password).startsWith("api-")) {
            error("Access token should start with \"api-\"")
        } else if (!isValidAccessToken(String(it.password), settings.baseUri)) {
            error("This access token is not authorized to get projects")
        } else {
            null
        }
    }

    private fun refreshProjectsPredicate(): ComponentPredicate {
        return (
            accessTokenField.enteredTextSatisfies { String(accessTokenField.password).trim() != "" } and
                accessTokenField.enteredTextSatisfies { origApiKey != String(accessTokenField.password) }
            ) or
            baseUriField.enteredTextSatisfies { origBaseUri != it }
    }

    private fun enableProjectsPredicate(): ComponentPredicate {
        return accessTokenField.enteredTextSatisfies { String(accessTokenField.password).trim() != "" } and
            accessTokenField.enteredTextSatisfies { origApiKey == String(accessTokenField.password) } and
            baseUriField.enteredTextSatisfies { origBaseUri == it } and
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
