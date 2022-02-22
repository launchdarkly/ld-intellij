package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.launchdarkly.api.ApiClient
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.api.ProjectsApi
import com.launchdarkly.api.auth.ApiKeyAuth
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.settings.LaunchDarklyMergedSettings

class LaunchDarklyApiClient() {

    companion object {
        @JvmStatic
        fun flagInstance(project: Project, apiKey: String? = null, baseUri: String? = null): FeatureFlagsApi {
            val settings = LaunchDarklyMergedSettings.getInstance(project)
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$ldBaseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey

            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(project: Project?, apiKey: String? = null, baseUri: String? = null): ProjectsApi {
            val settings =
                if (project != null) LaunchDarklyMergedSettings.getInstance(project) else LaunchDarklyApplicationConfig.getInstance().ldState
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$ldBaseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey
            return ProjectsApi()
        }
    }
}
