package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.github.intheclouddan.intellijpluginld.featurestore.createClientAndGetStore
import com.github.intheclouddan.intellijpluginld.featurestore.getFlagsAsJSONStrings
import com.github.intheclouddan.intellijpluginld.messaging.ConfigurationNotifier
import com.github.intheclouddan.intellijpluginld.messaging.DefaultMessageBusService
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.EdtExecutorService
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.sdk.server.Components
import com.launchdarkly.sdk.server.LDClient
import com.launchdarkly.sdk.server.LDConfig
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.FlagChangeListener
import java.util.concurrent.TimeUnit


/**
 * Instance to provide other classes access to LaunchDarkly Flag Metadata.
 * Handles refreshing of the flags and notifying attached listeners of changes.
 */
@Service
class FlagStore(project: Project) {
    var flags: FeatureFlags
    var flagConfigs = emptyMap<String, FlagConfiguration>()
    val messageBusService = project.service<DefaultMessageBusService>()
    // Get latest flags via REST API.
    /**
     * This method is gets the latest flags via REST API.
     * @param project the Intellij project open
     * @param settings  LaunchDarkly settings
     * @return FeatureFlags returns the flags, filtered to a specific environment, with summary true.
     */
    private fun flags(project: Project, settings: LaunchDarklyConfig.ConfigState): FeatureFlags {
        val envList = listOf(settings.environment)
        val ldProject: String = settings.project
        val getFlags = LaunchDarklyApiClient.flagInstance(project)
        return getFlags.getFeatureFlags(ldProject, envList, true, null, null, null, null, null, null)
    }

    /**
     * This method wraps {@link #flags} and notifies attached listeners.
     * @param project the Intellij project open
     * @param settings  LaunchDarkly settings
     * @return FeatureFlags returns the flags, filtered to a specific environment, with summary true.
     */
    fun flagsNotify(project: Project, settings: LaunchDarklyConfig.ConfigState): FeatureFlags {
        val publisher = project.messageBus.syncPublisher(messageBusService.flagsUpdatedTopic)
        flags = flags(project, settings)
        publisher.notify(true, "")
        return flags
    }

    /**
     * This method creates a flag listener for the LD Java Server SDK and notifies attached listeners of changes.
     * @param project the Intellij project open
     * @param client  {link #com.launchdarkly.sdk.server.LDClient}
     * @param store  {link #com.launchdarkly.sdk.server.interfaces.DataStore}

     */
    private fun flagListener(project: Project, client: com.launchdarkly.sdk.server.LDClient, store: DataStore) {
        println("listener added")
        val listenForChanges = FlagChangeListener { event ->
            println("flag changed ${event.key}")
            val publisher = project.messageBus.syncPublisher(messageBusService.flagsUpdatedTopic)
            flagTargeting(store)
            publisher.notify(true, event.key as String)
        }
        client.getFlagTracker().addFlagChangeListener(listenForChanges)
    }

    /*
     * Convert the internal SDK representation to JSON to be used in plugin.
     */
    fun flagTargeting(store: DataStore) {
        val g = Gson()
        val flagTargetingJson: String = getFlagsAsJSONStrings(store!!)!!.joinToString(prefix = "[", postfix = "]")
        val listflagTargetingJson = object : TypeToken<List<FlagConfiguration>>() {}.type
        val flagList = g.fromJson<List<FlagConfiguration>>(flagTargetingJson, listflagTargetingJson)
        flagConfigs = flagList.associateBy { it.key }
    }

    init {
        val settings = LaunchDarklyConfig.getInstance(project).ldState
        var refreshRate: Long = settings.refreshRate.toLong()
        flags = flagsNotify(project, settings)
        val ldProject = LaunchDarklyApiClient.projectInstance(project, settings.authorization).getProject(settings.project)
        val (store, client) = createClientAndGetStore(ldProject.environments.find { it.key == settings.environment }!!.apiKey)
        flagTargeting(store!!)
        flagListener(project, client, store!!)

        EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay({ flags = flagsNotify(project, settings) }, refreshRate, refreshRate, TimeUnit.MINUTES)
        project.messageBus.connect().subscribe(messageBusService.configurationEnabledTopic,
                object : ConfigurationNotifier {
                    override fun notify(isConfigured: Boolean) {
                        println("notified")
                        if (isConfigured) {
                            flags = flagsNotify(project, settings)
                        }
                    }
                })
    }

}