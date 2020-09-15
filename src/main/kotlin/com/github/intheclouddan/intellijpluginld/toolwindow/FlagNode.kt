package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.LDIcons
import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.github.intheclouddan.intellijpluginld.settings.LDSettings
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.*
import java.util.*
import javax.swing.Icon

class RootNode(flags: FeatureFlags, flagConfigs: Map<String, FlagConfiguration>, settings: LDSettings, project: Project) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    private val flags = flags
    private val settings = settings
    private val intProject = project

    override fun getChildren(): Array<SimpleNode> {
        println(settings.authorization)
        println(settings.baseUri)
        println(settings.environment)
        println(settings.project)

        if (myChildren.isEmpty() && flags.items != null) {
            myChildren.add(FlagNodeBase("${settings.project} / ${settings.environment}", LDIcons.FLAG))
            for (flag in flags.items) {
                myChildren.add(FlagNodeParent(flag, settings, flags, intProject /*flagConfigs*/))
            }
        } else if (flags.items == null) {
            myChildren.add(FlagNodeBase("LaunchDarkly Plugin is not configured."))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("root")
    }

}

class FlagNodeParent(FFlag: FeatureFlag, settings: LDSettings, flags: FeatureFlags, myProject: Project /* flagConfigs: Map<String, FlagConfiguration>*/) : SimpleNode() {
    private var children: MutableList<SimpleNode> = ArrayList()
    private val getFlags = myProject.service<FlagStore>()
    var flag: FeatureFlag = FFlag
    var flags = flags
    val settings = settings
    var env = getFlags.flagConfigs[flag.key]!!
    val key: String = flag.key


    override fun getChildren(): Array<SimpleNode> {
        if (children.isEmpty()) {
            buildChildren()
        } else {
            children = ArrayList()
            buildChildren()
        }
        return children.toTypedArray()
    }

    fun buildChildren() {
        children.add(FlagNodeBase("Key: ${flag.key}", LDIcons.FLAG_KEY))
        if (flag.description != "") {
            children.add(FlagNodeBase("Description: ${flag.description}", LDIcons.DESCRIPTION))
        }
        children.add(FlagNodeVariations(flag))

        if (env.prerequisites.size > 0) {
            children.add(FlagNodePrerequisites(flag, env.prerequisites, flags))
        }
        if (env.targets.size > 0) {
            children.add(FlagNodeTargets(flag, env.targets, flags))
        }
        if (env.rules.size > 0) {
            children.add(FlagNodeBase("Rules: ${env.rules.size}", LDIcons.RULES))
        }
        if (env.fallthrough != null) {
            children.add(FlagNodeFallthrough(flag, env))
        }
        if (env.offVariation != null) {
            children.add(FlagNodeBase("Off Variation: ${flag.variations[env.offVariation as Int].name ?: flag.variations[env.offVariation as Int].value}", LDIcons.OFF_VARIATION))
        }
        if (flag.tags.size > 0) {
            children.add(FlagNodeTags(flag.tags))
        }
        if (flag.defaults != null) {
            children.add(FlagNodeDefaults(flag))
        }
    }

    override fun update(data: PresentationData) {
        super.update(data)
        env = getFlags.flagConfigs[flag.key]!!
        flag = getFlags.flags.items.find { it.key == flag.key }!!
        var enabledIcon: Icon
        enabledIcon = if (env.on) LDIcons.TOGGLE_ON else LDIcons.TOGGLE_OFF
        val label = flag.name ?: flag.key
        data.setPresentableText(label)
        data.setIcon(enabledIcon)
    }
}

class FlagNodeBase(label: String, labelIcon: Icon? = null) : SimpleNode() {
    var label: String = label
    val labelIcon = labelIcon

    override fun getChildren(): Array<SimpleNode> {
        return SimpleNode.NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText(label)
        data.tooltip = label
        if (labelIcon != null) {
            data.setIcon(labelIcon)
        }
    }
}

class FlagNodeVariations(flag: FeatureFlag) : SimpleNode() {
    var flag: FeatureFlag = flag
    private var myChildren: MutableList<SimpleNode> = ArrayList()


    override fun getChildren(): Array<SimpleNode> {
        for (variation in flag.variations) {
            myChildren.add(FlagNodeVariation(variation))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("Variations")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeVariation(variation: Variation) : SimpleNode() {
    val variation = variation
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (variation.name != null) {
            myChildren.add(FlagNodeBase("Value: ${variation.value.toString()}", LDIcons.DESCRIPTION))
        }
        if (variation.description != null) {
            myChildren.add(FlagNodeBase("Description ${variation.description}", LDIcons.DESCRIPTION))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        var label: String = variation.name ?: variation.value.toString()
        data.setPresentableText(label)
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
        data.setPresentableText("Tags")
        data.setIcon(LDIcons.TAGS)
    }
}

class FlagNodeFallthrough(flag: FeatureFlag, flagConfig: FlagConfiguration) : SimpleNode() {
    val flag = flag
    val flagConfig = flagConfig
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val env = flag.environments.keys.first()

    override fun getChildren(): Array<SimpleNode> {
        if (flagConfig.fallthrough?.variation != null) {
            return SimpleNode.NO_CHILDREN
        }
        myChildren.add(FlagNodeRollout(flagConfig.fallthrough?.rollout, flag.variations))
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        var label: String
        if (flagConfig.fallthrough?.variation != null) {
            label = "Fallthrough: ${flag.variations[flagConfig.fallthrough?.variation as Int].name ?: flag.variations[flagConfig.fallthrough?.variation as Int].value}"
        } else {
            label = "Fallthrough"
        }
        data.setPresentableText(label)
        data.setIcon(LDIcons.DESCRIPTION)
    }
}

class FlagNodeRollout(rollout: Rollout?, variations: List<Variation>) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    var rollout = rollout as Rollout
    var variations = variations

    override fun getChildren(): Array<SimpleNode> {
        if (rollout?.bucketBy != null) {
            myChildren.add(FlagNodeBase("bucketBy ${rollout.bucketBy}", LDIcons.DESCRIPTION))
        }
        if (rollout != null) {
            for (variation in rollout.variations) {
                myChildren.add(FlagNodeBase("Variation: ${variations[variation.variation].name ?: variations[variation.variation].value}"))
                myChildren.add(FlagNodeBase("Weight: ${variation.weight / 1000.0}%"))

            }
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("Rollout")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodeDefaults(flag: FeatureFlag) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val flag = flag

    override fun getChildren(): Array<SimpleNode> {
        myChildren.add(FlagNodeBase("Off Variation: ${flag.variations[flag.defaults.offVariation]}", LDIcons.VARIATION))
        myChildren.add(FlagNodeBase("On Variation: ${flag.variations[flag.defaults.onVariation]}", LDIcons.VARIATION))

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("Default Variations")
        data.setIcon(LDIcons.VARIATION)
    }
}

class FlagNodePrerequisites(flag: FeatureFlag, prereqs: List<Prerequisite>, flags: FeatureFlags) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    var flag = flag
    var flags = flags
    var prereqs = prereqs

    override fun getChildren(): Array<SimpleNode> {
        prereqs.map {
            myChildren.add(FlagNodeBase("Flag Key: ${it.key}", LDIcons.FLAG))
            val flagKey = it.key
            var flagVariation = flags.items.find { findFlag -> findFlag.key == flagKey }
            myChildren.add(FlagNodeBase("Variation: ${flagVariation!!.variations[it.variation].name ?: flagVariation!!.variations[it.variation].value}", LDIcons.VARIATION))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("Prerequisites")
        data.setIcon(LDIcons.PREREQUISITE)
    }
}

class FlagNodeTargets(flag: FeatureFlag, targets: List<com.launchdarkly.api.model.Target>, flags: FeatureFlags) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    var flag = flag
    var flags = flags
    var targets = targets

    override fun getChildren(): Array<SimpleNode> {
        targets.map {
            val targetVariation = it.variation
            myChildren.add(FlagNodeBase("${flag!!.variations[it.variation].name ?: flag!!.variations[it.variation].value}: ${it.values.size}", LDIcons.VARIATION))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.setPresentableText("Targets")
        data.setIcon(LDIcons.TARGETS)
    }
}