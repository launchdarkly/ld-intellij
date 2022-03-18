package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeViewModelTest {
    private fun createModelWithName(): FlagNodeViewModel {
        val variationA = Variation().apply {
            name = "variation a"
        }

        val variationB = Variation().apply {
            name = "variation b"
        }

        val flag = FeatureFlag().apply {
            key = "flag key"
            name = "flag name"
            description = "flag description"
            variations = listOf(variationA, variationB)
            tags = listOf()
        }

        val flags = FeatureFlags().apply {
            items = listOf(flag)
        }
        return FlagNodeViewModel(flag, flags, null)
    }

    private fun createModelWitoutName(): FlagNodeViewModel {
        val flag = FeatureFlag().apply {
            key = "flag key"
            description = "flag description"
        }
        val flags = FeatureFlags().apply {
            items = listOf(flag)
        }
        return FlagNodeViewModel(flag, flags, null)
    }

    @Test
    fun testNames() {
        assertEquals("flag name", createModelWithName().flagLabel)
        assertEquals("flag key", createModelWitoutName().flagLabel)
    }
}
