/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.behavior.dialog

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSelectionHistoryTest {

    @Test
    fun recordMovesDuplicateToFrontAndKeepsThreeRecentColors() {
        val first = ColorSelection.Argb(0xFF112233.toInt())
        val second = ColorSelection.Argb(0xFF445566.toInt())
        val third = ColorSelection.Monet("system_primary_40")
        val fourth = ColorSelection.Argb(0xFF778899.toInt())

        val state = ColorSelectionHistoryState()
            .record("key:textColor", first)
            .record("key:backgroundColor", second)
            .record("row:backgroundColor", third)
            .record("key:shadowColor", fourth)
            .record("key:textColor", second)

        assertEquals(listOf(second, fourth, third), state.recent)
    }

    @Test
    fun lastColorIsIndependentPerAttributeWhileRecentColorsAreShared() {
        val keyText = ColorSelection.Argb(0xFF112233.toInt())
        val rowBackground = ColorSelection.Argb(0xFF445566.toInt())

        val state = ColorSelectionHistoryState()
            .record("key:textColor", keyText)
            .record("row:backgroundColor", rowBackground)

        assertEquals(keyText, state.lastByAttribute["key:textColor"])
        assertEquals(rowBackground, state.lastByAttribute["row:backgroundColor"])
        assertEquals(listOf(rowBackground, keyText), state.recent)
    }

    @Test
    fun themeReferencesAreSharedAndRemainDistinctFromMonetColors() {
        val themeReference = ColorSelection.ThemeReference("theme:primary")
        val monetColor = ColorSelection.Monet("system_primary_40")

        val state = ColorSelectionHistoryState()
            .record("key:textColor", themeReference)
            .record("row:backgroundColor", monetColor)

        assertEquals(themeReference, state.lastByAttribute["key:textColor"])
        assertEquals(monetColor, state.lastByAttribute["row:backgroundColor"])
        assertEquals(listOf(monetColor, themeReference), state.recent)
    }
}
