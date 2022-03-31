package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig

object Utils {
    const val FLAG_KEY_MAX_LENGTH = 256

    fun getFlagUrl(flagKey: String): String {
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        return "${settings.baseUri}/${settings.project}/${settings.environment}/features/$flagKey?${getAppQueryParams()}"
    }

    fun getIDEVersion(): String {
        return ApplicationInfo.getInstance().fullApplicationName
    }

    fun getPluginVersion(): String {
        return "launchdarkly-intellij/${PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
            ?: "noversion"}"
    }

    fun getAppQueryParams(): String {
        return "source=${getIDEVersion()}&pluginVersion=${getPluginVersion()}"
    }

    fun getDocsQueryParams(): String {
        return "utm_source=${getIDEVersion()}&utm_medium=ide&utm_campaign=${getPluginVersion()}"
    }
}
