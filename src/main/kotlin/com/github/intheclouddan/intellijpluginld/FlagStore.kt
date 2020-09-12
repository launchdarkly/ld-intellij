package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.github.intheclouddan.intellijpluginld.featurestore.createClientAndGetStore
import com.github.intheclouddan.intellijpluginld.featurestore.createClientAndGetStoreOffline
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
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.sdk.server.DataModel
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
    var flags: FeatureFlags = FeatureFlags()
    var flagConfigs = emptyMap<String, FlagConfiguration>()
    lateinit var flagStore: DataStore
    var flagClient: LDClient = LDClient("sdk-12345", LDConfig.Builder().offline(true).build())
    val messageBusService = project.service<DefaultMessageBusService>()
    private val project = project
    private val settings = LaunchDarklyConfig.getInstance(project).ldState
    private var envList = listOf(settings.environment)


    /**
     * This method is gets the latest flags via REST API.
     * @param project the Intellij project open
     * @param settings  LaunchDarkly settings
     * @return FeatureFlags returns the flags, filtered to a specific environment, with summary true.
     */
    private fun flags(): FeatureFlags {
        var ldProject: String = settings.project
        try {
            var getFlags = LaunchDarklyApiClient.flagInstance(project, settings.authorization, settings.baseUri)
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
        //if (flags.items.size > 0) {
        //    val newFlags = flags()
//            flags.items.forEachIndexed { idx, curFlag ->
//                var foundFlag = newFlags.items.find { it.key == curFlag.key }
//                if (foundFlag != null && curFlag.version < foundFlag.version) {
//                    flags.items[idx] = foundFlag
//                }
//                newFlags.items.remove(foundFlag)
//            }
//            newFlags.items.forEach
        //}
        flags = flags()
        if (reinit) {
            publisher.reinit()
        } else if (rebuild) {
            publisher.notify(true, "", true)
        } else {
            publisher.notify(true)
        }
        return flags
    }

    /**
     * This method creates a flag listener for the LD Java Server SDK and notifies attached listeners of changes.
     * @param project the Intellij project open
     * @param client  {link #com.launchdarkly.sdk.server.LDClient}
     * @param store  {link #com.launchdarkly.sdk.server.interfaces.DataStore}

     */
    private fun flagListener(client: com.launchdarkly.sdk.server.LDClient, store: DataStore) {
        val listenForChanges = FlagChangeListener { event ->
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
        val flagTargetingJson: String = getFlagsAsJSONStrings(store)!!.joinToString(prefix = "[", postfix = "]")
        val listflagTargetingJson = object : TypeToken<List<FlagConfiguration>>() {}.type
        val flagList = g.fromJson<List<FlagConfiguration>>(flagTargetingJson, listflagTargetingJson)
        flagConfigs = flagList.associateBy { it.key }
    }

    init {
        val settings = LaunchDarklyConfig.getInstance(project).ldState
        var refreshRate: Long = settings.refreshRate.toLong()
        if (settings.project != "") {
            flags = flagsNotify()
            val ldProject = LaunchDarklyApiClient.projectInstance(project, settings.authorization).getProject(settings.project)
            val MyStreamBaseURI = settings.baseUri.replace("app", "stream")
            var (store, client) = createClientAndGetStore(ldProject.environments.find { it.key == settings.environment }!!.apiKey, MyStreamBaseURI)
            flagStore = store!!
            flagClient = client
            flagTargeting(store)
            flagListener(client, store!!)
        } else {
            var (store, client) = createClientAndGetStoreOffline()
            flagStore = store!!
            flagClient = client
        }




        EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay({ flags = flagsNotify(rebuild = true) }, refreshRate, refreshRate, TimeUnit.MINUTES)

        project.messageBus.connect().subscribe(messageBusService.configurationEnabledTopic,
                object : ConfigurationNotifier {
                    override fun notify(isConfigured: Boolean) {
                        println("called configuration")
                        if (isConfigured) {
                            val curProject = LaunchDarklyApiClient.projectInstance(project, settings.authorization).getProject(settings.project)
                            val MyStreamBaseURI = settings.baseUri.replace("app", "stream")
                            var (curStore, curClient) = createClientAndGetStore(curProject.environments.find { it.key == settings.environment }!!.apiKey, MyStreamBaseURI)
                            flagClient.close()
                            flagStore = curStore!!
                            flagClient = curClient
                            flagTargeting(curStore)
                            flagListener(curClient, curStore)
                            flags = flagsNotify(true)
                        }
                    }
                })
    }

}