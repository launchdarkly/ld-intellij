package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.FeatureFlag
import com.launchdarkly.api.model.Target
import com.launchdarkly.api.model.Variation
import org.jdesktop.swingx.action.Targetable
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class FlagNodeTargetsTest {
    private var flag: FeatureFlag = createFlag()
    private val targets = createTargets()
    private val testTargets: FlagNodeTargets = FlagNodeTargets(flag = flag, targets)

    @Test
    fun getChildren() {
        assertEquals(2, testTargets.childCount)
    }

    fun createFlag(flagKey: String ="test"): FeatureFlag {
        val flag = FeatureFlag().apply {
            key = flagKey
            name = flagKey
        }

        val variation1 = Variation().apply {
            value = false
        }

        val variation2 = Variation().apply {
            value = true
        }


        flag.variations = listOf(variation1, variation2)
        return flag
    }

    fun createTargets(): List<Target> {
        val target1 = Target().apply {
            variation = 0
            values = listOf("false")
        }

        val target2 = Target().apply {
            variation = 1
            values = listOf("true")
        }

        return listOf(target1, target2)
    }
}