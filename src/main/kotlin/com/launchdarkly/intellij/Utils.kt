package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig

object Utils {
    const val FLAG_KEY_MAX_LENGTH = 256

    fun getFlagUrl(flagKey: String): String {
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        return "${settings.baseUri}/${settings.project}/${settings.environment}/features/$flagKey?${getQueryParams()}"
    }

    fun getUserAgent(): String {
        return "${getPluginInfo()} ${ApplicationInfo.getInstance().versionName}/${ApplicationInfo.getInstance().fullVersion}"
    }

    fun getIDEVersion(): String {
        return ApplicationInfo.getInstance().fullApplicationName
    }

    fun getPluginInfo(): String {
        return "launchdarkly-intellij/${PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
            ?: "noversion"}"
    }

    fun getQueryParams(): String {
        return "utm_source=${getIDEVersion()}&utm_medium=ide&utm_campaign=${getPluginInfo()}"
    }
}
