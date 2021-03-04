package com.launchdarkly.intellij.messaging

interface FlagNotifier {
    fun notify(isConfigured: Boolean, flag: String = "", rebuild: Boolean = false)
    fun reinit()
}

