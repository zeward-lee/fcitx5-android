/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeColorEditorAlphaStateTest {

    @Test
    fun makesInitiallyTransparentColorVisibleForHsvEdit() {
        val state = ThemeColorEditorAlphaState()

        assertEquals(0xFF, state.alphaForHsvEdit(0x00000000))
    }

    @Test
    fun preservesInitialNonTransparentAlphaForHsvEdit() {
        val state = ThemeColorEditorAlphaState()

        assertEquals(0x1F, state.alphaForHsvEdit(0x1F000000))
    }

    @Test
    fun preservesExplicitlyTransparentAlphaForHsvEdit() {
        val state = ThemeColorEditorAlphaState()
        state.recordAlphaEdit()

        assertEquals(0, state.alphaForHsvEdit(0x00000000))
    }

    @Test
    fun preservesExplicitlyTransparentArgbForHsvEdit() {
        val state = ThemeColorEditorAlphaState()
        state.recordArgbEdit()

        assertEquals(0, state.alphaForHsvEdit(0x00112233))
    }
}
