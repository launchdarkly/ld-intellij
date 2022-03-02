package com.launchdarkly.intellij.toolwindow

import com.launchdarkly.api.model.Variation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FlagNodeVariationTest {
    @Test
    fun hasCorrectLabelWhenDescriptionUsed() {
        val variation = Variation().apply {
            name = "TestVariation"
            value = 1
            description = "Example Description"
        }
        val testTarget = FlagNodeVariation(variation)
        val expectedNameValue = "Value: ${variation.value}"
        val expectedDescriptionValue = "Description: ${variation.description}"

        val children = testTarget.children
        assertEquals(expectedNameValue, children[0].label)
        assertEquals(expectedDescriptionValue, children[1].label)
        assertEquals(2, children.size)
    }

    @Test
    fun hasCorrectLabelsWithNoDescription() {
        val variation = Variation().apply {
            name = ""
            value = 1
        }
        val testTarget = FlagNodeVariation(variation)
        val expectedNameValue = "Value: ${variation.value}"

        val children = testTarget.children
        assertEquals(expectedNameValue, children[0].label)
        assertEquals(1, children.size)
    }
}