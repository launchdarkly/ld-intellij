package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.LDIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.FeatureFlag
import java.util.*

class FlagNodeDefaults(flag: FeatureFlag) : SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val flag = flag

    override fun getChildren(): Array<SimpleNode> {
        myChildren.add(FlagNodeDefault("Off Variation", flag.variations[flag.defaults.offVariation]))
        myChildren.add(FlagNodeDefault("On Variation", flag.variations[flag.defaults.onVariation]))

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "Default Variations"
        data.setIcon(LDIcons.DEFAULT_VARIATION)
    }
}