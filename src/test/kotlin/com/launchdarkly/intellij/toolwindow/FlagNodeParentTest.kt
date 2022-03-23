package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.*
import com.launchdarkly.intellij.featurestore.FlagConfiguration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeParentTest {
    private fun createFlagViewModel(withTags: Boolean, flagConfig: FlagConfiguration? = null): FlagNodeViewModel {
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
            tags = when {
                withTags -> listOf("tag1", "tag2")
                else -> listOf()
            }
        }

        val flags = FeatureFlags().apply {
            items = listOf(flag)
        }
        return FlagNodeViewModel(flag, flags, flagConfig)
    }

    @Test
    fun testCreateNodeBasic() {
        var node = FlagNodeParent(createFlagViewModel(false))
        assertEquals(3, node.childCount)
    }

    @Test
    fun testCreateNodeWithTags() {
        var node = FlagNodeParent(createFlagViewModel(true))
        assertEquals(4, node.childCount)
    }

    @Test
    fun testCreateNodeWithOffVariation() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", 1, null, listOf(), listOf(), arrayOf(), false, 1)))
        assertEquals(4, node.childCount)
    }

    @Test
    fun testCreateNodeWithFallthrough() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", null, Fallthrough(), listOf(), listOf(), arrayOf(), false, 1)))
        assertEquals(4, node.childCount)
    }

    @Test
    fun testCreateNodeWithOffVariationAndFallthrough() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", 0, Fallthrough(), listOf(), listOf(), arrayOf(), false, 1)))
        assertEquals(5, node.childCount)
    }

    @Test
    fun testCreateNodeWithPrerequisites() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", null, null, listOf(Prerequisite()), listOf(), arrayOf(), false, 1)))
        assertEquals(4, node.childCount)
    }

    @Test
    fun testCreateNodeWithTarget() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", null, null, listOf(), listOf(com.launchdarkly.api.model.Target()), arrayOf(), false, 1)))
        assertEquals(4, node.childCount)
    }

    @Test
    fun testCreateNodeWithRule() {
        var node = FlagNodeParent(createFlagViewModel(false, FlagConfiguration("key", null, null, listOf(), listOf(), arrayOf(Rule()), false, 1)))
        assertEquals(4, node.childCount)
    }
}
