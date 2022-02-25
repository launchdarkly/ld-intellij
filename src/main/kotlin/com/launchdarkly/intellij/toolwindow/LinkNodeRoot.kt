package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.intellij.settings.LDSettings

class LinkNodeRoot(private val settings: LDSettings) :
    SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (settings.project != "" && settings.environment != "") {
            myChildren.add(
                LinkNodeBase(
                    "Open Flags",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/features"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Segments",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/segments"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Users",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/users"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Debugger",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/debugger"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Experiments",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/experiments"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Audit Log",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/audit"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open Flag Comparison",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/features/compare"
                )
            )
            // TODO: Fix selector so we can uncomment this
//            myChildren.add(
//                LinkNodeBase(
//                    "Open Flag Environment Overview",
//                    "${settings.baseUri}/${settings.project}/${settings.environment}/features/"
//                )
//            )
            myChildren.add(
                LinkNodeBase(
                    "Open Documentation",
                    "https://docs.launchdarkly.com"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open API Documentation",
                    "https://apidocs.launchdarkly.com"
                )
            )
        }

        return myChildren.toTypedArray()
    }

    override fun update(data: PresentationData) {
        super.update(data)
        data.presentableText = "root"
    }

}