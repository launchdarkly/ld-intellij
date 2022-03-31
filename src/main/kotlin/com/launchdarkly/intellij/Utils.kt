package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig

object Utils {
    const val FLAG_KEY_MAX_LENGTH = 256

    fun getFlagUrl(flagKey: String): String {
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        return "${settings.baseUri}/${settings.project}/${settings.environment}/features/$flagKey?${getAppQueryParams()}"
    }

    fun getPluginVersion(): String {
        return PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
            ?: "noversion"
    }

    fun getAppQueryParams(): String {
        return "source=intellij&version=${getPluginVersion()}"
    }

    fun getDocsQueryParams(): String {
        return "utm_source=intellij&utm_medium=ide&utm_campaign=${getPluginVersion()}"
    }
}
