package com.launchdarkly.intellij

import com.intellij.openapi.util.IconLoader

class LDIcons {
    companion object {
        @JvmField
        val DEFAULT_VARIATION = IconLoader.getIcon("/icons/variationDefault.svg", LDIcons::class.java)

        @JvmField
        val DESCRIPTION = IconLoader.getIcon("/icons/description.svg", LDIcons::class.java)

        @JvmField
        val LOGO = IconLoader.getIcon("/icons/logo.svg", LDIcons::class.java)

        @JvmField
        val FLAG_KEY = IconLoader.getIcon("/icons/flagKey.svg", LDIcons::class.java)

        @JvmField
        val OFF_VARIATION = IconLoader.getIcon("/icons/offVariation.svg", LDIcons::class.java)

        @JvmField
        val PREREQUISITE = IconLoader.getIcon("/icons/prereq.svg", LDIcons::class.java)

        @JvmField
        val RULES = IconLoader.getIcon("/icons/rules.svg", LDIcons::class.java)

        @JvmField
        val TAGS = IconLoader.getIcon("/icons/tags.svg", LDIcons::class.java)

        @JvmField
        val TARGETS = IconLoader.getIcon("/icons/targets.svg", LDIcons::class.java)

        @JvmField
        val TOGGLE_OFF = IconLoader.getIcon("/icons/toggleoff.svg", LDIcons::class.java)

        @JvmField
        val TOGGLE_ON = IconLoader.getIcon("/icons/toggleon.svg", LDIcons::class.java)

        @JvmField
        val TOGGLE_DISCONNECTED = IconLoader.getIcon("/icons/toggledisc.svg", LDIcons::class.java)

        @JvmField
        val VARIATION = IconLoader.getIcon("/icons/variation.svg", LDIcons::class.java)
    }
}
