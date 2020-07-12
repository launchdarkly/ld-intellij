package com.github.intheclouddan.intellijpluginld.messaging

import com.intellij.util.messages.Topic
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.messages.MessageBus

interface FlagNotifier {
    fun notify(value: Boolean)
}

