/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.theme

internal class ThemeColorEditorAlphaState {
    private var explicitlyEdited = false

    fun recordAlphaEdit() {
        explicitlyEdited = true
    }

    fun recordArgbEdit() {
        explicitlyEdited = true
    }

    fun alphaForHsvEdit(color: Int): Int {
        val alpha = color ushr 24
        return if (!explicitlyEdited && alpha == 0) 0xFF else alpha
    }
}
