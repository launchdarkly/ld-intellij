import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.4.20-M2"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.8.0-SNAPSHOT"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "0.6.0"
    // detekt linter - read more: https://detekt.github.io/detekt/kotlindsl.html
    id("io.gitlab.arturbosch.detekt") version "1.10.0-RC1"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.intellij.plugins:structure-base:3.139")
        classpath("org.jetbrains.intellij.plugins:structure-intellij:3.139")
    }
}

// Import variables from gradle.properties file
val pluginGroup: String by project
val pluginName: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project

val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
    jcenter()
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.10.0-RC1")
    implementation("com.launchdarkly:api-client:3.10.0")
    implementation("com.launchdarkly:launchdarkly-java-server-sdk:5.+")
    implementation("com.google.code.gson:gson:2.7")
    implementation("com.googlecode.json-simple", "json-simple", "1.1.1")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.14.0")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.4.20-M2")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    //compileOnly(kotlin("stdlib-jdk8"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = pluginName
    version = platformVersion
    type = platformType
    //alternativeIdePath = "/Applications/GoLand.app"
    downloadSources = platformDownloadSources.toBoolean()
    updateSinceUntilBuild = true
//  Plugin Dependencies:
//  https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html
//
    setPlugins("java") //201.6668.1.98
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
    config = files("./detekt-config.yml")

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    listOf("compileKotlin", "compileTestKotlin").forEach {
        getByName<KotlinCompile>(it) {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    withType<Detekt> {
        jvmTarget = "1.8"
    }

    changelog {
        version = "${project.version}"
        path = "${project.projectDir}/CHANGELOG.md"
        header = closure { "[${project.version}] - ${date()}" }
        itemPrefix = "-"
        keepUnreleasedSection = true
        unreleasedTerm = "[Unreleased]"
        groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription(closure {
            File("./README.md").readText().lines().run {
                subList(indexOf("<!-- Plugin description -->") + 1, indexOf("<!-- Plugin description end -->"))
            }.joinToString("\n").run { markdownToHTML(this) }
        })

        // Get the latest available change notes from the changelog file
        changeNotes(closure {
            changelog.getLatest().toHTML()
        })
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("PUBLISH_TOKEN"))
        @Suppress("NonNullable")
        channels((System.getenv("GIT_RELEASE") ?: "").split('-').getOrElse(1) { "default" }.split('.').first())
    }

}