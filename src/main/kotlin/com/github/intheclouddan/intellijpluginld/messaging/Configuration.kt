package com.github.intheclouddan.intellijpluginld.messaging

import com.intellij.util.messages.Topic

interface ConfigurationNotifier {
    fun handle(value: Boolean)
}

val configurationTopic = Topic.create("configuration", ConfigurationNotifier::class.java)