package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.*
import com.launchdarkly.intellij.LDIcons
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeViewModelTest {
    private fun createFlagViewModel(flagKey: String, flagName: String?, flagDescription: String?, flagConfig: FlagConfiguration?): FlagNodeViewModel {
        val variationA = Variation().apply {
            name = "variation a"
        }

        val variationB = Variation().apply {
            name = "variation b"
        }

        val flag = FeatureFlag().apply {
            key = flagKey
            name = flagName
            description = flagDescription
            variations = listOf(variationA, variationB)
            tags = listOf()
        }

        val flags = FeatureFlags().apply {
            items = listOf(flag)
        }
        return FlagNodeViewModel(flag, flags, flagConfig)
    }

    @Test
    fun testFlagLabels() {
        assertEquals("flag name", createFlagViewModel("flag key", "flag name", "flag description", null).flagLabel)
        assertEquals("flag key", createFlagViewModel("flag key", null, "flag description", null).flagLabel)
    }

    @Test
    fun testIcons() {
        assertEquals(
                LDIcons.TOGGLE_DISCONNECTED,
                createFlagViewModel("flag key", null, null, null).icon)
        assertEquals(LDIcons.TOGGLE_ON, createFlagViewModel("flag key", null, null, FlagConfiguration("flag key", null, null, listOf(), listOf(), arrayOf(), true, 1)).icon)
        assertEquals(LDIcons.TOGGLE_OFF, createFlagViewModel("flag key", null, null, FlagConfiguration("flag key", null, null, listOf(), listOf(), arrayOf(), false, 1)).icon)
    }
}
