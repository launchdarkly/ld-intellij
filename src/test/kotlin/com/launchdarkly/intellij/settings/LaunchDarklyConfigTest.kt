package com.launchdarkly.intellij.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LaunchDarklyConfigTest : BasePlatformTestCase() {
    /// Test that the SDK is setup and testing framework is ok.

    @BeforeEach
    override fun setUp() {
        super.setUp()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun testNothing() {
        assertTrue(true)
    }
}