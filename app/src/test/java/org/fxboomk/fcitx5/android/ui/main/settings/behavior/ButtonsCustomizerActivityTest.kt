/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */

package org.fxboomk.fcitx5.android.ui.main.settings.behavior

import org.fxboomk.fcitx5.android.input.config.ConfigurableButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonsCustomizerActivityTest {

    @Test
    fun findCurrentButtonPositionIgnoresStaleIndexAndFindsCurrentSectionItem() {
        val items = listOf(
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("undo"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("clipboard"),
                ButtonsCustomizerActivity.Section.StatusArea,
            ),
        )

        assertEquals(
            1,
            items.findCurrentButtonPosition(
                buttonId = "clipboard",
                section = ButtonsCustomizerActivity.Section.StatusArea,
            ),
        )
    }

    @Test
    fun findCurrentButtonPositionReturnsMissingForAbsentButton() {
        val items = listOf(
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("undo"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
        )

        assertEquals(
            -1,
            items.findCurrentButtonPosition(
                buttonId = "clipboard",
                section = ButtonsCustomizerActivity.Section.StatusArea,
            ),
        )
    }

    @Test
    fun moveButtonAcrossSectionsKeepsHeadersAndUpdatesSection() {
        val items = mutableListOf<ButtonsCustomizerActivity.ListItem>(
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.KawaiiBar
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("undo"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.StatusArea
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("clipboard"),
                ButtonsCustomizerActivity.Section.StatusArea,
            ),
        )

        val destination = items.moveButton(
            fromPosition = 1,
            insertPosition = 4,
            targetSection = ButtonsCustomizerActivity.Section.StatusArea,
        )

        assertEquals(3, destination)
        assertEquals(
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.KawaiiBar
            ),
            items[0],
        )
        assertEquals(
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.StatusArea
            ),
            items[1],
        )
        val moved = items[3] as ButtonsCustomizerActivity.ListItem.ButtonItem
        assertEquals("undo", moved.button.id)
        assertEquals(ButtonsCustomizerActivity.Section.StatusArea, moved.section)
        assertTrue(items[2] is ButtonsCustomizerActivity.ListItem.ButtonItem)
    }

    @Test
    fun moveButtonToSectionHeaderPlacesItAfterTheHeader() {
        val items = mutableListOf<ButtonsCustomizerActivity.ListItem>(
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.KawaiiBar
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("undo"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.StatusArea
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("clipboard"),
                ButtonsCustomizerActivity.Section.StatusArea,
            ),
        )

        items.moveButton(
            fromPosition = 1,
            insertPosition = 4,
            targetSection = ButtonsCustomizerActivity.Section.StatusArea,
        )

        assertTrue(items[3] is ButtonsCustomizerActivity.ListItem.ButtonItem)
        assertEquals(
            "undo",
            (items[3] as ButtonsCustomizerActivity.ListItem.ButtonItem).button.id,
        )
        assertEquals(
            ButtonsCustomizerActivity.Section.StatusArea,
            (items[3] as ButtonsCustomizerActivity.ListItem.ButtonItem).section,
        )
    }

    @Test
    fun fixedMoreButtonStaysFirstInKawaiiBar() {
        val items = mutableListOf<ButtonsCustomizerActivity.ListItem>(
            ButtonsCustomizerActivity.ListItem.SectionHeader(
                ButtonsCustomizerActivity.Section.KawaiiBar
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("more"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
            ButtonsCustomizerActivity.ListItem.ButtonItem(
                ConfigurableButton("undo"),
                ButtonsCustomizerActivity.Section.KawaiiBar,
            ),
        )

        val destination = items.moveButton(
            fromPosition = 2,
            insertPosition = 2,
            targetSection = ButtonsCustomizerActivity.Section.KawaiiBar,
        )

        assertEquals(2, destination)
        assertEquals(
            "more",
            (items[1] as ButtonsCustomizerActivity.ListItem.ButtonItem).button.id,
        )
    }
}
