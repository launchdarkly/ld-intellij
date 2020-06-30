package com.github.intheclouddan.intellijpluginld

import com.launchdarkly.api.ApiClient
import com.intellij.openapi.project.Project
import com.launchdarkly.api.Configuration
import com.launchdarkly.api.api.FeatureFlagsApi
import com.launchdarkly.api.auth.ApiKeyAuth


class LaunchDarklyApiClient(project: Project) {

    companion object {
        @JvmStatic
        fun flagInstance(project: Project): FeatureFlagsApi {
            val settings = LaunchDarklyConfig.getInstance(project)
            val client: ApiClient = Configuration.getDefaultApiClient()
            val token = client.getAuthentication("Token") as ApiKeyAuth
            token.apiKey = "api-xxx"

            return FeatureFlagsApi()
        }
    }

}