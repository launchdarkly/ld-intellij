package com.launchdarkly.intellij.messaging

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic

interface AppConfigurationNotifier {
    fun notify(isConfigured: Boolean)
}

//interface AppMessageBusService {
//    val configurationEnabledTopic: Topic<ConfigurationNotifier>
//}

@Service
class AppDefaultMessageBusService : MessageBusService {
    override val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    override val configurationEnabledTopic: Topic<ConfigurationNotifier> = Topic.create(
        "LD_APP_CONFIGURATION",
        ConfigurationNotifier::class.java
    )

    override val flagsUpdatedTopic: Topic<FlagNotifier> = Topic.create(
        "LD_APP_FLAGS_UPDATED",
        FlagNotifier::class.java
    )
}