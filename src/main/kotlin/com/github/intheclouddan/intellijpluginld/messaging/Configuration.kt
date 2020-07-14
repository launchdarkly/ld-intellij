package com.github.intheclouddan.intellijpluginld.messaging

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic

interface ConfigurationNotifier {
    fun notify(isConfigured: Boolean)
}

interface MessageBusService {
    val messageBus: MessageBus

    val configurationEnabledTopic: Topic<ConfigurationNotifier>

    val flagsUpdatedTopic: Topic<FlagNotifier>

}

@Service
class DefaultMessageBusService : MessageBusService {
    override val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    override val configurationEnabledTopic: Topic<ConfigurationNotifier> = Topic.create(
            "LD_CONFIGURATION",
            ConfigurationNotifier::class.java
    )

    override val flagsUpdatedTopic: Topic<FlagNotifier> = Topic.create(
            "LD_FLAGS_UPDATED",
            FlagNotifier::class.java
    )
}