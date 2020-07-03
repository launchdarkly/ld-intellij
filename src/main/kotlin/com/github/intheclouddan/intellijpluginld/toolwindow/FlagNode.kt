package com.github.intheclouddan.intellijpluginld.toolwindow

import com.github.intheclouddan.intellijpluginld.LDIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.FeatureFlags
import com.launchdarkly.api.model.Variation
import java.util.*

class RootNode(flags: FeatureFlags): SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    private val flags = flags

    override fun getChildren(): Array<SimpleNode> {
        if (myChildren.isEmpty()) {
            for (flag in flags.items) {
                println(flag)
                myChildren.add(FlagNodeParent(flag))
            }
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
            data.setPresentableText("root")
        }

}

class FlagNodeParent(flag: FeatureFlag): SimpleNode() {
    var flag: FeatureFlag = flag
    private var myChildren: MutableList<SimpleNode> = ArrayList()


    override fun getChildren(): Array<SimpleNode> {
        if (myChildren.isEmpty()) {
            if (flag.description != "") {
                myChildren.add(FlagNodeBase("Description: ${flag.description}"))
            }
                myChildren.add(FlagNodeVariations(flag))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
            super.update(data)
            val label = flag.name ?: flag.key
            data.setPresentableText(label)
            data.setIcon(LDIcons.FLAG)
    }
}

class FlagNodeBase(label: String): SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()
    val label: String = label


    override fun getChildren(): Array<SimpleNode> {
        return SimpleNode.NO_CHILDREN
    }

    override fun update(data: PresentationData) {
        data.setPresentableText(label)
        data.tooltip = label
        data.setIcon(LDIcons.DESCRIPTION)
    }
}

class FlagNodeVariations(flag: FeatureFlag): SimpleNode() {
    var flag: FeatureFlag = flag
    private var myChildren: MutableList<SimpleNode> = ArrayList()


    override fun getChildren(): Array<SimpleNode> {
        for (variation in flag.variations) {
            myChildren.add(FlagNodeVariation(variation, false))
        }
        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        data.setPresentableText("Variations")
    }
}

class FlagNodeVariation(variation: Variation, child: Boolean): SimpleNode() {
    val variation = variation
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
            if (variation.name != null) {
                myChildren.add(FlagNodeBase("Value: ${variation.value.toString()}"))
            }
            if (variation.description != null) {
                myChildren.add(FlagNodeBase("Description ${variation.description}"))
            }
            return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        var label: String = variation.name ?: variation.value.toString()
        data.setPresentableText(label)
    }
}