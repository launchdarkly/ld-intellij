package com.github.intheclouddan.intellijpluginld.messaging

import com.intellij.util.messages.Topic
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.messages.MessageBus

interface ConfigurationNotifier {
    fun notify(value: Boolean)
}

interface MessageBusService {
    val messageBus: MessageBus

    val configurationEnabledTopic: Topic<ConfigurationNotifier>
}

@Service
class DefaultMessageBusService : MessageBusService {
    override val messageBus: MessageBus = ApplicationManager.getApplication().messageBus

    override val configurationEnabledTopic: Topic<ConfigurationNotifier> = Topic.create(
            "LD_CONFIGURATION",
            ConfigurationNotifier::class.java
    )
}