package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyConfig
import com.intellij.openapi.project.Project
import com.launchdarkly.api.ApiClient
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.EnvironmentsApi
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.api.ProjectsApi
import com.launchdarkly.api.auth.ApiKeyAuth


class LaunchDarklyApiClient(project: Project) {

    companion object {
        @JvmStatic
        fun flagInstance(project: Project): FeatureFlagsApi {
            val settings = LaunchDarklyConfig.getInstance(project)
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${settings.ldState.baseUri}/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = settings.ldState.authorization

            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(project: Project, apiKey: String? = null): ProjectsApi {
            val settings = LaunchDarklyConfig.getInstance(project)
            var ldApiKey = apiKey
            if (ldApiKey == null) {
                val settings = LaunchDarklyConfig.getInstance(project)
                ldApiKey = settings.ldState.authorization
            }
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${settings.ldState.baseUri}/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey
            return ProjectsApi()
        }

        fun environmentInstance(project: Project): EnvironmentsApi {
            val settings = LaunchDarklyConfig.getInstance(project)
            val client: ApiClient = Configuration.getDefaultApiClient()
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = settings.ldState.authorization
            return EnvironmentsApi()
        }
    }

}