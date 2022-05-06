import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.6.0"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.5.3"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // detekt linter - read more: https://detekt.github.io/detekt/kotlindsl.html
    id("io.gitlab.arturbosch.detekt") version "1.10.0-RC1"
    // ktlint
    id("org.jmailen.kotlinter") version "3.9.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
    maven("https://plugins.gradle.org/m2")
}

dependencies {
    implementation("io.pebbletemplates:pebble:3.1.5")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.10.0-RC1")
    implementation("com.launchdarkly:api-client:3.10.0")
    implementation("com.launchdarkly:launchdarkly-java-server-sdk:5.+")
    implementation("com.google.code.gson:gson:2.7")
    implementation("com.googlecode.json-simple", "json-simple", "1.1.1")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.14.0")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.4.20-M2")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.17.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.check {
    dependsOn("installKotlinterPrePushHook")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    //localPath.set("/Applications/IntelliJ IDEA.app")
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))

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
    // Set the compatibility versions to 11
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }

        withType<Detekt> {
            jvmTarget = it
        }
    }

    changelog {
        version.set(properties("pluginVersion"))
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        changeNotes.set(provider {
            changelog.run {
                getOrNull(properties("pluginVersion")) ?: getLatest()
            }.toHTML()
        })
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        @Suppress("NonNullable")
        channels.set(listOf((System.getenv("GIT_RELEASE") ?: "").split('#').getOrElse(1) { "default" }))
    }

    test {
        useJUnitPlatform()
    }

    kotlinter {
        ignoreFailures = false
        indentSize = 4
        reporters = arrayOf("checkstyle", "plain")
        experimentalRules = false
    }

}
