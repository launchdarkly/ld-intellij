package com.launchdarkly.intellij

import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig

object Utils {
    const val FLAG_KEY_MAX_LENGTH = 256

    fun getFlagUrl(flagKey: String): String {
        val settings = LaunchDarklyApplicationConfig.getInstance().ldState
        return "${settings.baseUri}/${settings.project}/${settings.environment}/features/$flagKey"
    }
}
