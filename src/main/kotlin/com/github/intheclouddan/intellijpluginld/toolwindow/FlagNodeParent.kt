package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.FlagStore
import com.github.intheclouddan.intellijpluginld.LDIcons
import com.github.intheclouddan.intellijpluginld.featurestore.FlagConfiguration
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import java.util.*

class FlagNodeParent(FFlag: FeatureFlag, private var flags: FeatureFlags, myProject: Project) : SimpleNode() {
    private var children: MutableList<SimpleNode> = ArrayList()
    private val getFlags = myProject.service<FlagStore>()
    var flag: FeatureFlag = FFlag
    var env = getFlags.flagConfigs[flag.key]
            ?: FlagConfiguration(flag.key, null, null, listOf(), listOf(), arrayOf(), false, -1)
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

    private fun buildChildren() {
        children.add(FlagNodeBase("Key: ${flag.key}", LDIcons.FLAG_KEY))
        if (flag.description != "") {
            children.add(FlagNodeBase("Description: ${flag.description}", LDIcons.DESCRIPTION))
        }
        children.add(FlagNodeVariations(flag))

        if (env.prerequisites.isNotEmpty()) {
            children.add(FlagNodePrerequisites(env.prerequisites, flags))
        }
        if (env.targets.isNotEmpty()) {
            children.add(FlagNodeTargets(flag, env.targets))
        }
        if (env.rules.isNotEmpty()) {
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
    }

    override fun update(data: PresentationData) {
        super.update(data)
        env = getFlags.flagConfigs[flag.key]
                ?: FlagConfiguration(flag.key, null, null, listOf(), listOf(), arrayOf(), false, -1)
        flag = getFlags.flags.items.find { it.key == flag.key }!!
        // Flag version should only be -1 if we manually created a FlagConfiguration so set icon to warning.
        val enabledIcon = if (env.version === -1) LDIcons.TOGGLE_DISCONNECTED else if (env.on) LDIcons.TOGGLE_ON else LDIcons.TOGGLE_OFF
        val label = flag.name ?: flag.key
        data.presentableText = label
        data.setIcon(enabledIcon)
    }
}