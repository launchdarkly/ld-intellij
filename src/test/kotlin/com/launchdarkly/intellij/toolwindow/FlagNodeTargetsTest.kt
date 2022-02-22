package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.Target
import com.launchdarkly.api.model.Variation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlagNodeTargetsTest {

    @Test
    fun hasCorrectLabelWhenVariationIsNamed() {
        val flag: FeatureFlag = createFlag()
        val target = Target().apply {
            variation = 0
            values = listOf("false")
        }
        val testTarget = FlagNodeTargets(flag = flag, listOf(target))
        val expected = "variation0: 1"
        val children = testTarget.children
        for (child in children) {
            assertEquals(expected, child.label)
        }
    }

    @Test
    fun hasCorrectLabelWhenVariationHasNoName() {
        val flag = FeatureFlag().apply {
            key = "test"
            name = "test"
            variations = listOf(
                Variation().apply {
                    value = false
                }
            )
        }
        val target = Target().apply {
            variation = 0
            values = listOf("user1", "user2")
        }
        val testTarget = FlagNodeTargets(flag = flag, listOf(target))
        val expected = "false: 2"
        val children = testTarget.children
        for (child in children) {
            assertEquals(expected, child.label)
        }
    }

    @Test
    fun returnsCorrectChildCount() {
        val flag: FeatureFlag = createFlag()
        val targets = createTargets()
        val testTargets = FlagNodeTargets(flag = flag, targets)
        assertEquals(targets.size, testTargets.childCount)
    }

    private fun createFlag(flagKey: String = "test"): FeatureFlag {
        val flag = FeatureFlag().apply {
            key = flagKey
            name = flagKey
        }

        val variation1 = Variation().apply {
            value = false
            name = "variation0"
        }

        val variation2 = Variation().apply {
            value = true
        }

        flag.variations = listOf(variation1, variation2)
        return flag
    }

    private fun createTargets(): List<Target> {
        val target1 = Target().apply {
            variation = 0
            values = listOf("user1")
        }

        val target2 = Target().apply {
            variation = 1
            values = listOf("user1")
        }

        return listOf(target1, target2)
    }
}
