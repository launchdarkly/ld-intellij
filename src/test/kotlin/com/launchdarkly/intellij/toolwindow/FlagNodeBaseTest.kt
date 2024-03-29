package com.launchdarkly.intellij.toolwindow

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeBaseTest {
    private val testBase: FlagNodeBase = FlagNodeBase(label = "Test")

    @Test
    fun getChildren() {
        assertEquals(0, testBase.childCount)
    }

    @Test
    fun getLabel() {
        val expected = "Test"
        assertEquals(expected, testBase.label)
    }
}
