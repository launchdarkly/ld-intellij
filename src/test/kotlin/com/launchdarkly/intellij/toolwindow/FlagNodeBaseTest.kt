package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.intellij.LDIcons
import junit.framework.TestCase
import org.junit.jupiter.api.Test

// Run Test
class FlagNodeBaseTest : TestCase() {

    @Test
    fun test() {
        val flagNode = FlagNodeBase("test label")

        assert(flagNode.label == "test label")
    }

    @Test
    fun testIcon() {
        val flagNode = FlagNodeBase("test icon", LDIcons.FLAG)

        assert(flagNode.labelIcon == LDIcons.FLAG)
    }
}