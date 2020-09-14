package com.github.intheclouddan.intellijpluginld

import com.github.intheclouddan.intellijpluginld.settings.LaunchDarklyApplicationConfig
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
        fun flagInstance(project: Project, apiKey: String? = null, baseUri: String? = null): FeatureFlagsApi {
            val settings = LaunchDarklyConfig.getInstance(project)
            var ldBaseUri = baseUri ?: settings.ldState.baseUri
            var ldApiKey = apiKey ?: settings.ldState.authorization
            println("inside flag instance")
            println(ldBaseUri)
            println(ldApiKey)
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${ldBaseUri}/api/v2"
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = ldApiKey

            return FeatureFlagsApi()
        }

        @JvmStatic
        fun projectInstance(project: Project?, apiKey: String? = null, baseUri: String? = null): ProjectsApi {
            var settings = if (project != null) LaunchDarklyConfig.getInstance(project!!).ldState else {
                LaunchDarklyApplicationConfig.getInstance().ldState
            }
            var ldBaseUri = baseUri ?: settings.baseUri
            var ldApiKey = apiKey ?: settings.authorization
            val client: ApiClient = Configuration.getDefaultApiClient()
            client.basePath = "${ldBaseUri}/api/v2"
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