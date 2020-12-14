package com.github.intheclouddan.intellijpluginld

import com.intellij.openapi.util.IconLoader

class LDIcons {
    companion object {
        @JvmField
        val DEFAULT_VARIATION = IconLoader.getIcon("/icons/variationDefault.svg", javaClass)

        @JvmField
        val DESCRIPTION = IconLoader.getIcon("/icons/description.svg", javaClass)

        @JvmField
        val FLAG = IconLoader.getIcon("/icons/flag.svg", javaClass)

        @JvmField
        val FLAG_KEY = IconLoader.getIcon("/icons/flagKey.svg", javaClass)

        @JvmField
        val OFF_VARIATION = IconLoader.getIcon("/icons/offVariation.svg", javaClass)

        @JvmField
        val PREREQUISITE = IconLoader.getIcon("/icons/prereq.svg", javaClass)

        @JvmField
        val RULES = IconLoader.getIcon("/icons/rules.svg", javaClass)

        @JvmField
        val TAGS = IconLoader.getIcon("/icons/tags.svg", javaClass)

        @JvmField
        val TARGETS = IconLoader.getIcon("/icons/targets.svg", javaClass)

        @JvmField
        val TOGGLE_OFF = IconLoader.getIcon("/icons/toggleoff.svg", javaClass)

        @JvmField
        val TOGGLE_ON = IconLoader.getIcon("/icons/toggleon.svg", javaClass)

        @JvmField
        val VARIATION = IconLoader.getIcon("/icons/variation.svg", javaClass)

    }
}