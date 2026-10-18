package com.pixelgift.app

import com.pixelgift.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test
    fun bottomNavItems_containsFiveScreensInExpectedOrder() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        assertEquals(Screen.Studio, items[0])
        assertEquals(Screen.Effects, items[1])
        assertEquals(Screen.Magic, items[2])
        assertEquals(Screen.Time, items[3])
        assertEquals(Screen.Device, items[4])
    }
}
