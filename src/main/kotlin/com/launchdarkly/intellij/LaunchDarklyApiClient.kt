package com.launchdarkly.intellij

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.launchdarkly.api.ApiClient
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.AccessTokensApi
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.api.ProjectsApi
import com.launchdarkly.api.auth.ApiKeyAuth
import com.launchdarkly.api.model.Tokens
import com.launchdarkly.intellij.settings.LaunchDarklyApplicationConfig
import com.launchdarkly.intellij.constants.DEFAULT_BASE_URI

class LaunchDarklyApiClient() {

    companion object {
        @JvmStatic
        fun flagInstance(apiKey: String? = null, baseUri: String? = null): FeatureFlagsApi {
            configureClient(apiKey, baseUri)
            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(apiKey: String? = null, baseUri: String? = null): ProjectsApi {
            configureClient(apiKey, baseUri)
            return ProjectsApi()
        }

        fun testAccessToken(apiKey: String, baseUri: String): Tokens {
            configureClient(apiKey, baseUri)
            return AccessTokensApi().getTokens(false)
        }

        private fun configureClient(apiKey: String?, baseUri: String?) {
            val settings = LaunchDarklyApplicationConfig.getInstance().ldState
            val ldBaseUri = if (!baseUri.isNullOrEmpty()) baseUri else getUri(settings.baseUri)
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            val pluginVersion =
                PluginManagerCore.getPlugin(PluginId.getId("com.github.intheclouddan.intellijpluginld"))?.version
                    ?: "noversion"
            client.setUserAgent("launchdarkly-intellij/$pluginVersion")
            client.basePath = "$ldBaseUri/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey
        }
    }
}

fun getUri(baseUri: String?): String {
    return if (baseUri.isNullOrEmpty()) DEFAULT_BASE_URI else baseUri
}
