package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeParentTest {
    private fun createModel(): FlagNodeViewModel {
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

    @Test
    fun testCreateNode() {
        var node = FlagNodeParent(createModel())
        assertEquals(node.childCount, 3)
    }
}
