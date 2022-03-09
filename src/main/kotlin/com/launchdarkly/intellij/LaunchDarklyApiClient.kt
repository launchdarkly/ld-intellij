package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.launchdarkly.api.ApiClient
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.AccessTokensApi
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.api.ProjectsApi
import com.launchdarkly.api.auth.ApiKeyAuth
import com.launchdarkly.api.model.Tokens
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig

class LaunchDarklyApiClient() {

    companion object {
        @JvmStatic
        fun flagInstance(project: Project, apiKey: String? = null, baseUri: String? = null): FeatureFlagsApi {
            val settings = LaunchDarklyApplicationConfig.getInstance().ldState
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
                    ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$ldBaseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey

            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(apiKey: String? = null, baseUri: String? = null): ProjectsApi {
            val settings = LaunchDarklyApplicationConfig.getInstance().ldState
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
                    ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$ldBaseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey
            return ProjectsApi()
        }

        fun testAccessToken(apiKey: String, baseUri: String): Tokens {
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$baseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = apiKey
            return AccessTokensApi().getTokens(false)
        }
    }
}
