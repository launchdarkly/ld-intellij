package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.LDIcons
import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.github.intheclouddan.intellijpluginld.settings.LDSettings
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.*
import java.util.*

class RootNode(private val flags: FeatureFlags, private val settings: LDSettings, private val intProject: Project) :
    SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (myChildren.isEmpty() && flags.items != null) {
            myChildren.add(FlagNodeBase("${settings.project} / ${settings.environment}", LDIcons.FLAG))
            for (flag in flags.items) {
                myChildren.add(FlagNodeParent(flag, flags, intProject))
            }
        } else if ((settings.project != "" && settings.environment != "") && flags.items == null) {
            myChildren.add(FlagNodeBase("Loading Flags..."))
        } else if (flags.items == null) {
            myChildren.add(FlagNodeBase("LaunchDarkly Plugin is not configured."))
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
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (variation.name != null) {
            myChildren.add(FlagNodeBase("Value: ${variation.value}", LDIcons.DESCRIPTION))
        }
        if (variation.description != null) {
            myChildren.add(FlagNodeBase("Description ${variation.description}", LDIcons.DESCRIPTION))
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
        if (flagConfig.fallthrough?.variation != null) {
            return NO_CHILDREN
        } else if (flagConfig.fallthrough!!.rollout != null) {
            myChildren.add(FlagNodeRollout(flagConfig.fallthrough!!.rollout, flag.variations))
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
            myChildren.add(FlagNodeBase("Flag Key: ${it.key}", LDIcons.FLAG))
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
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
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