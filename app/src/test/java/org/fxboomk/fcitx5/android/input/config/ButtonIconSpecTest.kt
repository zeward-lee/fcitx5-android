/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.input.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonIconSpecTest {

    @Test
    fun acceptsBareAndCanonicalHexCodePoints() {
        assertEquals(0xE141, ButtonIconSpec.codePoint("E141"))
        assertEquals(0xE141, ButtonIconSpec.codePoint("font:e141"))
        assertEquals("font:E141", ButtonIconSpec.canonicalCodePoint("e141"))
        assertEquals("\uE141", ButtonIconSpec.glyph("font:E141"))
    }

    @Test
    fun normalizesMissingAndBlankMoreIconsToTheDefaultGlyph() {
        assertEquals(
            ButtonsLayoutConfig.defaultMoreButton(),
            ButtonsLayoutConfig.moreButtonOrDefault(emptyList())
        )
        assertEquals(
            "font:E141",
            ButtonsLayoutConfig.moreButtonOrDefault(listOf(ConfigurableButton("more"))).icon
        )
        assertEquals(
            "font:E142",
            ButtonsLayoutConfig.moreButtonOrDefault(
                listOf(ConfigurableButton("more", icon = "font:E142"))
            ).icon
        )
    }

    @Test
    fun rejectsInvalidUnicodeCodePoints() {
        assertNull(ButtonIconSpec.codePoint("E04"))
        assertNull(ButtonIconSpec.codePoint("not-a-code-point"))
        assertNull(ButtonIconSpec.codePoint("D800"))
        assertNull(ButtonIconSpec.codePoint("110000"))
    }

    @Test
    fun recognizesAndCanonicalizesSvgIconCode() {
        val svg = "<svg viewBox=\"0 0 24 24\"><path d=\"M0 0h24v24H0z\"/></svg>"

        assertEquals("svg:$svg", ButtonIconSpec.canonicalSvg("svg:  $svg  "))
        assertNull(ButtonIconSpec.svg(svg))
    }

    @Test
    fun searchStartsInOptionalButtonsByDefault() {
        val config = ButtonsLayoutConfig.default()

        assertTrue(config.optionalButtons.any { it.id == "search" })
        assertTrue(config.kawaiiBarButtons.none { it.id == "search" })
        assertTrue(config.statusAreaButtons.none { it.id == "search" })
    }

}
