package com.launchdarkly.intellij.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import com.launchdarkly.api.ApiException
import com.launchdarkly.intellij.LaunchDarklyApiClient
import com.launchdarkly.intellij.messaging.AppDefaultMessageBusService
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.DocumentEvent

const val CHECK_API = "Check API Key"

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
    private val apiField = JPasswordField()
    private val settings = LaunchDarklyApplicationConfig.getInstance().ldState
    private var origApiKey = settings.authorization
    private var origBaseUri = settings.baseUri
    private var modified = false
    private var panel = DialogPanel()
    private var apiUpdate = false
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
            defaultMessage = CHECK_API
        }
    }

    private fun onClickGetProjects(event: ActionEvent) {
        val btn = event.source as JButton
        btn.isEnabled = false
        btn.text = "Fetching Projects..."
        updateProjects()
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
                cell(apiField)
                    .label("API Key:")
                    .applyToComponent {
                        this.addActionListener{ e ->
                            println(e)
                        }
                    }
                    .bindText(settings::authorization)
                    .columns(COLUMNS_MEDIUM).component
                button("Get Projects") {
                    onClickGetProjects(it)
                }.enabledIf(
                    ApiFieldPredicate(apiField, ::validKey)
                )
            }.rowComment("Input the API key from your LaunchDarkly account. If you don’t have one, you must <a href=\"https://docs.launchdarkly.com/home/account-security/api-access-tokens#creating-api-access-tokens\">create an access token</a> first.")

            try {
                projectBox = if (::projectContainer.isInitialized) {
                    DefaultComboBoxModel(projectContainer.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf(defaultMessage))
                }

                environmentBox = if (::environmentContainer.isInitialized) {
                    DefaultComboBoxModel(environmentContainer.environments.map { it.key }.toTypedArray())
                } else {
                    DefaultComboBoxModel(arrayOf("Please select a Project"))
                }
                environmentBox.selectedItem = settings.environment

                indent {
                    rowsRange {
                        row("Project:") {
                            comboBox(projectBox, renderer)
                                .bindItem(settings::project)
                                .applyToComponent {
                                    isEditable = true
                                    this.addItemListener { _ ->
                                        updateProjectEnvironments()
                                    }
                                }
                        }
                        row("Environment:") {
                            comboBox(environmentBox, renderer)
                                .bindItem(settings::environment)
                                .applyToComponent {
                                    isEditable = true
                                }
                        }
                    }.enabledIf(
                        ApiFieldPredicate(apiField, ::validKey)
                    )
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

        val disposable = Disposer.newDisposable()
        panel.registerValidators(disposable)

        return panel
    }

    override fun isModified(): Boolean {
        println("isModified")
        println(super.isModified())
        return super.isModified()
    }

    private fun updateOptions() {
        if ((settings.authorization != origApiKey || settings.baseUri != origBaseUri) || apiUpdate) {
            updateProjects()
        }

        if (::projectContainer.isInitialized && lastSelectedProject != projectBox.selectedItem.toString()) {
            updateProjectEnvironments()
        }
    }

    private fun updateProjects() {
        try {
            projectContainer = getProjects(settings.authorization, settings.baseUri)
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
            ValidationInfo("Invalid API Key", apiField)
        }
    }

    private fun updateProjectEnvironments() {
        settings.environment = ""
        with(environmentBox) {
            removeAllElements()
        }
        if (projectBox.selectedItem == null) {
            return
        }

        lastSelectedProject = projectBox.selectedItem.toString()

        try {
            environmentContainer = projectContainer.find { it.key == projectBox.selectedItem.toString() }!!
            val envMap = environmentContainer.environments.map { it.key }.sorted()
            with(environmentBox) {
                envMap.map { addElement(it) }
                selectedItem = envMap.firstOrNull()
            }
        } catch (err: Error) {
            println(err)
        }
    }

    override fun apply() {
        super.apply()

        settings.baseUri = settings.baseUri.replace(Regex("/+$"), "")

        if (settings.project != projectBox.selectedItem.toString()) {
            settings.project = projectBox.selectedItem.toString()
        }

        if (settings.environment != environmentBox.selectedItem.toString()) {
            settings.environment = environmentBox.selectedItem.toString()
        }

        if (settings.authorization != origApiKey || settings.baseUri != origBaseUri) {
            apiUpdate = true
            origApiKey = settings.authorization
            origBaseUri = settings.baseUri
        }
        if ((projectBox.selectedItem != CHECK_API) && modified) {
            updateOptions()
            val appMsgService = ApplicationManager.getApplication().messageBus
            val topic = service<AppDefaultMessageBusService>().configurationEnabledTopic

            val publisher = appMsgService.syncPublisher(topic)
            publisher.notify(true)
            println("notifying app")
        }
    }

    fun getProjects(apiKey: String?, baseUri: String?): MutableList<com.launchdarkly.api.model.Project> {
        val projectApi = LaunchDarklyApiClient.projectInstance(apiKey, baseUri)
        return projectApi.projects.items.sortedBy { it.key } as MutableList<com.launchdarkly.api.model.Project>
    }

    private fun validKey(apiKey: String): Boolean {
        println("test connection")
        return try {
            LaunchDarklyApiClient.testAccessToken(apiKey, settings.baseUri)
            println("valid token")
            true
        } catch (e: ApiException) {
            println(e)
            println("invalid token")
            false
        }
    }
}

class ApiFieldPredicate(private val component: JTextField, private val predicate: (text: String) -> Boolean) :
    ComponentPredicate() {

    override fun addListener(listener: (Boolean) -> Unit) {
        component.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                println("text changed")
                listener(predicate(component.text))
            }
        })
    }

    override fun invoke(): Boolean = predicate(component.text)
}
