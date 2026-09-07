/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.behavior.dialog

import android.content.Context
import android.content.SharedPreferences

internal sealed class ColorSelection {
    data class Argb(val color: Int) : ColorSelection()
    data class Monet(val resourceId: String) : ColorSelection()
    data class ThemeReference(val reference: String) : ColorSelection()
}

internal data class ColorSelectionHistoryState(
    val recent: List<ColorSelection> = emptyList(),
    val lastByAttribute: Map<String, ColorSelection> = emptyMap()
) {
    fun record(attribute: String, selection: ColorSelection): ColorSelectionHistoryState {
        val updatedRecent = buildList {
            add(selection)
            recent.forEach { existing ->
                if (existing != selection) add(existing)
            }
        }.take(MAX_RECENT_COLORS)
        return copy(
            recent = updatedRecent,
            lastByAttribute = lastByAttribute + (attribute to selection)
        )
    }

    companion object {
        const val MAX_RECENT_COLORS = 3
    }
}

internal class ColorSelectionHistory(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun recent(): List<ColorSelection> = readState().recent

    fun last(attribute: String): ColorSelection? = readState().lastByAttribute[attribute]

    fun record(attribute: String, selection: ColorSelection) {
        val state = readState().record(attribute, selection)
        preferences.edit()
            .putString(KEY_RECENT, state.recent.joinToString(ENTRY_SEPARATOR, transform = ::encode))
            .putString(
                KEY_LAST,
                state.lastByAttribute.entries.joinToString(ATTRIBUTE_SEPARATOR) { (key, value) ->
                    "${encodePart(key)}$VALUE_SEPARATOR${encode(value)}"
                }
            )
            .apply()
    }

    private fun readState(): ColorSelectionHistoryState {
        val recent = preferences.getString(KEY_RECENT, null)
            ?.split(ENTRY_SEPARATOR)
            ?.mapNotNull(::decode)
            ?.take(ColorSelectionHistoryState.MAX_RECENT_COLORS)
            .orEmpty()
        val lastByAttribute = preferences.getString(KEY_LAST, null)
            ?.split(ATTRIBUTE_SEPARATOR)
            ?.mapNotNull { entry ->
                val separator = entry.indexOf(VALUE_SEPARATOR)
                if (separator <= 0) return@mapNotNull null
                val key = decodePart(entry.substring(0, separator))
                val selection = decode(entry.substring(separator + VALUE_SEPARATOR.length))
                key to selection
            }
            ?.mapNotNull { (key, selection) ->
                if (selection == null) null else key to selection
            }
            ?.toMap()
            .orEmpty()
        return ColorSelectionHistoryState(recent, lastByAttribute)
    }

    private fun encode(selection: ColorSelection): String = when (selection) {
        is ColorSelection.Argb -> "argb:${selection.color.toUInt().toString(16).padStart(8, '0')}"
        is ColorSelection.Monet -> "monet:${encodePart(selection.resourceId)}"
        is ColorSelection.ThemeReference -> "theme:${encodePart(selection.reference)}"
    }

    private fun decode(value: String): ColorSelection? {
        return when {
            value.startsWith("argb:") -> value.removePrefix("argb:")
                .toLongOrNull(16)
                ?.toInt()
                ?.let(ColorSelection::Argb)
            value.startsWith("monet:") -> value.removePrefix("monet:")
                .let(::decodePart)
                .takeUnless { it.isEmpty() }
                ?.let(ColorSelection::Monet)
            value.startsWith("theme:") -> value.removePrefix("theme:")
                .let(::decodePart)
                .takeUnless { it.isEmpty() }
                ?.let(ColorSelection::ThemeReference)
            else -> null
        }
    }

    private fun encodePart(value: String): String = value
        .replace("%", "%25")
        .replace("|", "%7C")
        .replace(";", "%3B")

    private fun decodePart(value: String): String = value
        .replace("%3B", ";")
        .replace("%7C", "|")
        .replace("%25", "%")

    companion object {
        private const val PREFERENCES_NAME = "keyboard_color_selection_history"
        private const val KEY_RECENT = "recent"
        private const val KEY_LAST = "last"
        private const val ENTRY_SEPARATOR = "|"
        private const val ATTRIBUTE_SEPARATOR = ";"
        private const val VALUE_SEPARATOR = "="
    }
}
