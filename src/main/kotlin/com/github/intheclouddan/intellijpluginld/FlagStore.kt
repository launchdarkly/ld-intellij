package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.coderefs.FlagAliases
import com.github.intheclouddan.intellijpluginld.featurestore.*
import com.github.intheclouddan.intellijpluginld.messaging.AppDefaultMessageBusService
import com.github.intheclouddan.intellijpluginld.messaging.ConfigurationNotifier
import com.github.intheclouddan.intellijpluginld.messaging.DefaultMessageBusService
import com.github.intheclouddan.intellijpluginld.notifications.ConfigNotifier
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.EdtExecutorService
import com.launchdarkly.api.ApiException
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.sdk.server.DataModel
import com.launchdarkly.sdk.server.LDClient
import com.launchdarkly.sdk.server.LDConfig
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.FlagChangeListener
import java.time.Duration
import java.util.concurrent.TimeUnit


/**
 * Instance to provide other classes access to LaunchDarkly Flag Metadata.
 * Handles refreshing of the flags and notifying attached listeners of changes.
 */
@Service
class FlagStore(private var project: Project) {
    var flags: FeatureFlags = FeatureFlags()
    var flagConfigs = mutableMapOf<String, FlagConfiguration>()
    lateinit var flagStore: DataStore
    var flagClient: LDClient = LDClient("sdk-12345", LDConfig.Builder().offline(true).build())
    val messageBusService = project.service<DefaultMessageBusService>()
    val appBusService = service<AppDefaultMessageBusService>()
    private val settings = LaunchDarklyMergedSettings.getInstance(project)
    private var envList = listOf(settings.environment)


    /**
     * This method is gets the latest flags via REST API.
     * @param project the Intellij project open
     * @param settings  LaunchDarkly settings
     * @return FeatureFlags returns the flags, filtered to a specific environment, with summary true.
     */
    private fun flags(): FeatureFlags {
        val ldProject: String = settings.project
        try {
            val getFlags = LaunchDarklyApiClient.flagInstance(project, settings.authorization, settings.baseUri)
            envList = listOf(settings.environment)
            return getFlags.getFeatureFlags(ldProject, envList, true, null, null, null, null, null, null)
        } catch (err: Exception) {
            println(err)
        }
        return FeatureFlags()
    }

    /**
     * This method wraps {@link #flags} and notifies attached listeners.
     * @param project the Intellij project open
     * @param settings  LaunchDarkly settings
     * @return FeatureFlags returns the flags, filtered to a specific environment, with summary true.
     */
    fun flagsNotify(reinit: Boolean = false, rebuild: Boolean = false): FeatureFlags {
        val publisher = project.messageBus.syncPublisher(messageBusService.flagsUpdatedTopic)
        flags = flags()
        when {
            reinit -> {
                publisher.reinit()
            }
            rebuild -> {
                publisher.notify(true, "", true)
            }
            else -> {
                publisher.notify(true)
            }
        }
        return flags
    }

    /**
     * This method creates a flag listener for the LD Java Server SDK and notifies attached listeners of changes.
     * @param project the Intellij project open
     * @param client  {link #com.launchdarkly.sdk.server.LDClient}
     * @param store  {link #com.launchdarkly.sdk.server.interfaces.DataStore}

     */
    private fun flagListener(client: LDClient, store: DataStore) {
        val listenForChanges = FlagChangeListener { event ->
            ApplicationManager.getApplication().executeOnPooledThread {
                val flag: FeatureFlag? = flags.items.find { it.key == event.key }
                if (flag == null) {
                    val getFlags = LaunchDarklyApiClient.flagInstance(project)
                    val newFlag = getFlags.getFeatureFlag(settings.project, event.key, envList)
                    flags.items.add(newFlag)
                }
                if (store.get(DataModel.FEATURES, event.key) == null) {
                    val newFlag = flags.items.find { it.key == event.key }
                    flags.items.remove(newFlag)
                }
                val publisher = project.messageBus.syncPublisher(messageBusService.flagsUpdatedTopic)
                val flagString = getFlagAsJSONString(store, event.key)
                val flagJson = Gson().fromJson(flagString, FlagConfiguration::class.java)
                flagConfigs[event.key] = flagJson
                publisher.notify(true, event.key as String)
            }
        }
        client.flagTracker.addFlagChangeListener(listenForChanges)
    }

    /*
     * Convert the internal SDK representation to JSON to be used in plugin.
     */
    fun flagTargeting(store: DataStore) {
        val flagTargetingJson: String = getFlagsAsJSONStrings(store)!!.joinToString(prefix = "[", postfix = "]")
        val listflagTargetingJson = object : TypeToken<List<FlagConfiguration>>() {}.type
        val flagList = Gson().fromJson<List<FlagConfiguration>>(flagTargetingJson, listflagTargetingJson)
        flagConfigs = flagList.associateBy { it.key } as MutableMap<String, FlagConfiguration>
    }

    fun offlineStore() {
        val (store, client) = createClientAndGetStoreOffline()
        flagStore = store!!
        flagClient = client
    }

    fun setupStore() {
        val ldProject =
            LaunchDarklyApiClient.projectInstance(project, settings.authorization)
                .getProject(settings.project)
        val myStreamBaseURI = System.getenv("LD_STREAMER_CONFIG") ?: settings.baseUri.replace("app", "stream")
        val (store, client) = createClientAndGetStore(
            ldProject.environments.find { it.key == settings.environment }!!.apiKey,
            myStreamBaseURI
        )
        flagStore = store!!
        flagClient = client
        val clientInit = flagClient.dataSourceStatusProvider.waitFor(
            DataSourceStatusProvider.State.VALID,
            Duration.ofMillis(1000)
        )
        if (clientInit) {
            ApplicationManager.getApplication().executeOnPooledThread {
                flagTargeting(store)
                flagListener(client, store)
                flags = flagsNotify(reinit = true, rebuild = true)
            }
        } else {
            // We still want to init, FlagTree will be built without SDK data.
            ApplicationManager.getApplication().executeOnPooledThread {
                flagTargeting(store)
                flagListener(client, store)
                flags = flagsNotify(reinit = true, rebuild = true)
            }

        }
    }

    init {
        val settings = LaunchDarklyMergedSettings.getInstance(project)
        val refreshRate: Long = settings.refreshRate.toLong()
        project.service<FlagAliases>()
        if (settings.project != "" && settings.authorization != "") {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    setupStore()

                } catch (err: ApiException) {
                    val notify = ConfigNotifier()
                    notify.notify(project, "Project: ${settings.project} Error: $err")
                    offlineStore()
                }
            }
        } else {
            offlineStore()
        }

        EdtExecutorService.getScheduledExecutorInstance()
            .scheduleWithFixedDelay({ flags = flagsNotify(rebuild = true) }, refreshRate, refreshRate, TimeUnit.MINUTES)

        project.messageBus.connect().subscribe(appBusService.configurationEnabledTopic,
            object : ConfigurationNotifier {
                override fun notify(isConfigured: Boolean) {
                    if (isConfigured && !settings.projectOverrides()) {
                        try {
                            ApplicationManager.getApplication().executeOnPooledThread {
                                setupStore()
                            }
                        } catch (err: ApiException) {
                            val notify = ConfigNotifier()
                            notify.notify(project, err.toString())
                        }
                    }
                }
            })

        project.messageBus.connect().subscribe(messageBusService.configurationEnabledTopic,
            object : ConfigurationNotifier {
                override fun notify(isConfigured: Boolean) {
                    if (isConfigured) {
                        ApplicationManager.getApplication().executeOnPooledThread {
                            setupStore()
                        }
                    }
                }
            })
    }

}