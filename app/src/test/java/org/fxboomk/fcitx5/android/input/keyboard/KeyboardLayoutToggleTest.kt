/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.input.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutToggleTest {

    @Test
    fun normalKeyboardTogglesToNumberKeyboard() {
        assertEquals(
            NumberKeyboard.Name,
            toggledNumberKeyboardLayout(TextKeyboard.Name),
        )
    }

    @Test
    fun numberKeyboardTogglesToNormalKeyboard() {
        assertEquals(
            TextKeyboard.Name,
            toggledNumberKeyboardLayout(NumberKeyboard.Name),
        )
    }
}
