package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyApplicationConfig
import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyMergedSettings
import com.intellij.openapi.project.Project
import com.launchdarkly.api.ApiClient
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.api.ProjectsApi
import com.launchdarkly.api.auth.ApiKeyAuth


class LaunchDarklyApiClient(project: Project) {

    companion object {
        @JvmStatic
        fun flagInstance(project: Project, apiKey: String? = null, baseUri: String? = null): FeatureFlagsApi {
            val settings = LaunchDarklyMergedSettings.getInstance(project)
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${ldBaseUri}/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey

            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(project: Project?, apiKey: String? = null, baseUri: String? = null): ProjectsApi {
            val settings = if (project != null) LaunchDarklyMergedSettings.getInstance(project) else LaunchDarklyApplicationConfig.getInstance().ldState
            val ldBaseUri = baseUri ?: settings.baseUri
            val ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${ldBaseUri}/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey
            return ProjectsApi()
        }
    }

}