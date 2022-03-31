package com.launchdarkly.intellij.toolwindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.treeStructure.SimpleNode
import com.launchdarkly.intellij.Utils
import com.launchdarkly.intellij.settings.LDSettings

class LinkNodeRoot(private val settings: LDSettings) :
    SimpleNode() {
    private var myChildren: MutableList<SimpleNode> = ArrayList()

    override fun getChildren(): Array<SimpleNode> {
        if (settings.project != "" && settings.environment != "") {
            myChildren.add(
                LinkNodeBase(
                    "Flags",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/features?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Segments",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/segments?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Users",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/users?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Debugger",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/debugger?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Experiments",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/experiments?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Audit Log",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/audit?${Utils.getAppQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Flag Comparison",
                    "${settings.baseUri}/${settings.project}/${settings.environment}/features/compare?${Utils.getAppQueryParams()}"
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
                    "https://docs.launchdarkly.com?${Utils.getDocsQueryParams()}"
                )
            )
            myChildren.add(
                LinkNodeBase(
                    "Open API Documentation",
                    "https://apidocs.launchdarkly.com?${Utils.getDocsQueryParams()}"
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
