package com.github.intheclouddan.intellijpluginld.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import java.util.*


class PasswordManager(project: Project) {
    val settings = LaunchDarklyConfig.getInstance(project)

    /**
     * Set the password for accessing the Jenkins build server API.
     *
     * @param password the password to store
     */
    fun setPassword(password: String?) {
        PasswordSafe.instance.setPassword(createCredentialAttributes(), password)
    }

    /**
     * Create the credentials needed to retrieve the password using username and server address
     * from [JBWSettings].
     *
     * @return the credentials
     */
    private fun createCredentialAttributes(): CredentialAttributes {
        if (!settings.hasPasswordCredentials()) {
            throw RuntimeException("API Key has not yet been set, unable to create credentials")
        }
        val apiKey: String = settings.ldState.apiKey
        return CredentialAttributes("LaunchDarkly", apiKey)
    }
//
//    init {
//        settings = LaunchDarklyConfig.getInstance(project)
//    }
}