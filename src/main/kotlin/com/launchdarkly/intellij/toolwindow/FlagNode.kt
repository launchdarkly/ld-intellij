package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.*
import com.launchdarkly.intellij.FlagStore
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import com.launchdarkly.intellij.settings.LDSettings

class RootNode(private val flags: FeatureFlags, private val settings: LDSettings, private val intProject: Project) :
    SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {

        when {
            myChildren.isEmpty() && flags.items != null -> {
                myChildren.add(InfoNode("${settings.project} / ${settings.environment}"))
                for (flag in flags.items) {
                    val flagStore = intProject.service<FlagStore>()
                    val config = flagStore.flagConfigs[flag.key]!!
                    val flagModel = FlagNodeViewModel(flag, flags, config)
                    myChildren.add(FlagNodeParent(flagModel))
                }
            }
            (settings.project != "" && settings.environment != "") && flags.items == null -> myChildren.add(
                FlagNodeBase(
                    "Loading Flags..."
                )
            )
            flags.items == null -> myChildren.add(FlagNodeBase("LaunchDarkly is not configured."))
        }

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "root"
    }
}

class FlagNodeVariations(private var flag: FeatureFlag) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        for (variation in flag.variations) {
            myChildren.add(FlagNodeVariation(variation))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Variations"
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeVariation(private val variation: Variation) : SimpleNode() {
    private var myChildren: MutableList<FlagNodeBase> = ArrayList()

    override fun getChildren(): Array<FlagNodeBase> {
        if (variation.name != null) {
            myChildren.add(FlagNodeBase("Value: ${variation.value}", LDIcons.DESCRIPTION))
        }
        if (variation.description != null) {
            myChildren.add(FlagNodeBase("Description: ${variation.description}", LDIcons.DESCRIPTION))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        val label = variation.name ?: variation.value.toString()
        data.presentableText = label
    }
}

class FlagNodeTags(tags: List<String>) : SimpleNode() {
    var tags: List<String> = tags
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        for (tag in tags) {
            myChildren.add(FlagNodeBase(tag))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Tags"
        data.setIcon(LDIcons.TAGS)
    }
}

class FlagNodeFallthrough(var flag: FeatureFlag, private val flagConfig: FlagConfiguration) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        when {
            flagConfig.fallthrough?.variation != null -> return NO_CHILDREN
            flagConfig.fallthrough!!.rollout != null -> myChildren.add(
                FlagNodeRollout(
                    flagConfig.fallthrough!!.rollout,
                    flag.variations
                )
            )
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        var label = if (flagConfig.fallthrough?.variation != null) {
            "Fallthrough: ${flag.variations[flagConfig.fallthrough?.variation as Int].name ?: flag.variations[flagConfig.fallthrough?.variation as Int].value}"
        } else {
            "Fallthrough"
        }
        data.presentableText = label
        data.setIcon(LDIcons.DESCRIPTION)
    }
}

class FlagNodeRollout(private var rollout: Rollout, private var variations: List<Variation>) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (rollout.bucketBy != null) {
            myChildren.add(FlagNodeBase("bucketBy ${rollout.bucketBy}", LDIcons.DESCRIPTION))
        }
        for (variation in rollout.variations) {
            myChildren.add(FlagNodeBase("Variation: ${variations[variation.variation].name ?: variations[variation.variation].value}"))
            myChildren.add(FlagNodeBase("Weight: ${variation.weight / 1000.0}%"))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Rollout"
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeDefault(private val default: String, private var variation: Variation) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        myChildren.add(FlagNodeVariation(variation))

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = default
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodePrerequisites(private var prereqs: List<Prerequisite>, private var flags: FeatureFlags) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        prereqs.map {
            myChildren.add(FlagNodeBase("Flag Key: ${it.key}", LDIcons.LOGO))
            val flagKey = it.key
            var flagVariation = flags.items.find { findFlag -> findFlag.key == flagKey }
            myChildren.add(
                FlagNodeBase(
                    "Variation: ${flagVariation!!.variations[it.variation].name ?: flagVariation.variations[it.variation].value}",
                    LDIcons.VARIATION
                )
            )
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Prerequisites"
        data.setIcon(LDIcons.PREREQUISITE)
    }
}

class FlagNodeTargets(private var flag: FeatureFlag, private var targets: List<com.launchdarkly.api.model.Target>) :
    SimpleNode() {
    private var myChildren: MutableList<FlagNodeBase> = ArrayList()

    override fun getChildren(): Array<FlagNodeBase> {
        targets.map {
            myChildren.add(
                FlagNodeBase(
                    "${flag.variations[it.variation].name ?: flag.variations[it.variation].value}: ${it.values.size}",
                    LDIcons.VARIATION
                )
            )
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Targets"
        data.setIcon(LDIcons.TARGETS)
    }
}
