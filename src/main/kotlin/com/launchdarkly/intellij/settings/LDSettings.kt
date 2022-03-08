package com.launchdarkly.intellij.settings

interface LDSettings {
    var project: String
    var environment: String
    var refreshRate: Int
    var baseUri: String
    var authorization: String
    var codeReferences: Boolean
    var codeReferencesRefreshRate: Int

    fun isConfigured(): Boolean
}
