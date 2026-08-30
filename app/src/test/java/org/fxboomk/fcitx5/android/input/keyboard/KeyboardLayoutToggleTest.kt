/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.input.keyboard

import android.text.InputType
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

    @Test
    fun newTextInputUsesNormalKeyboard() {
        assertEquals(
            TextKeyboard.Name,
            keyboardLayoutOnStartInput(
                inputType = InputType.TYPE_CLASS_TEXT,
                currentLayout = NumberKeyboard.Name,
                restarting = false,
                manuallySelected = true,
            ),
        )
    }

    @Test
    fun newNumberAndPhoneInputsUseNumberKeyboard() {
        listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE).forEach { inputType ->
            assertEquals(
                NumberKeyboard.Name,
                keyboardLayoutOnStartInput(
                    inputType = inputType,
                    currentLayout = TextKeyboard.Name,
                    restarting = false,
                    manuallySelected = false,
                ),
            )
        }
    }

    @Test
    fun restartingTextInputKeepsManuallySelectedNumberKeyboard() {
        assertEquals(
            NumberKeyboard.Name,
            keyboardLayoutOnStartInput(
                inputType = InputType.TYPE_CLASS_TEXT,
                currentLayout = NumberKeyboard.Name,
                restarting = true,
                manuallySelected = true,
            ),
        )
    }

    @Test
    fun restartingInputWithoutManualSelectionFollowsInputType() {
        assertEquals(
            NumberKeyboard.Name,
            keyboardLayoutOnStartInput(
                inputType = InputType.TYPE_CLASS_NUMBER,
                currentLayout = TextKeyboard.Name,
                restarting = true,
                manuallySelected = false,
            ),
        )
        assertEquals(
            TextKeyboard.Name,
            keyboardLayoutOnStartInput(
                inputType = InputType.TYPE_CLASS_TEXT,
                currentLayout = NumberKeyboard.Name,
                restarting = true,
                manuallySelected = false,
            ),
        )
    }
}
