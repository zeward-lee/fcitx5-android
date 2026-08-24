/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: 2021-2023 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.input.config

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.PathParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a configurable button on Kawaii Bar or Status Area.
 */
@Serializable
data class ConfigurableButton(
    /**
     * Unique identifier for the button action.
     * Examples: "undo", "redo", "cursor_move", "floating_toggle", "clipboard", "more",
     *           "theme_toggle", "language_switch", "theme", "input_method_options", "reload_config",
     *           "virtual_keyboard", "one_handed_keyboard"
     */
    @SerialName("id")
    val id: String,

    /**
     * Optional: Drawable resource name or iconfont Unicode code point.
     * If null, uses default icon for the action.
     * Examples: "ic_baseline_undo_24", "ic_clipboard", "font:E141", "svg:<svg ...>"
     */
    @SerialName("icon")
    val icon: String? = null,

    /**
     * Optional: Custom label for accessibility/content description.
     * If null, uses default label for the action.
     */
    @SerialName("label")
    val label: String? = null,

    /**
     * Optional: Long press action, if different from short press.
     * For buttons that support different long-press behavior.
     * Long-press behavior is currently controlled by the button action itself.
     */
    @SerialName("longPressAction")
    val longPressAction: String? = null
)

/** Shared parsing rules for drawable names and iconfont code points. */
object ButtonIconSpec {
    private val codePointPattern = Regex("(?:font:)?([0-9A-Fa-f]{4,6})")

    fun codePoint(value: String?): Int? {
        val match = value?.trim()?.let(codePointPattern::matchEntire) ?: return null
        return match.groupValues[1].toIntOrNull(16)?.takeIf {
            it in 0..0x10FFFF && it !in 0xD800..0xDFFF
        }
    }

    fun glyph(value: String?): String? = codePoint(value)?.let { String(Character.toChars(it)) }

    fun canonicalCodePoint(value: String): String? = codePoint(value)?.let { "font:%04X".format(it) }

    fun svg(value: String?): String? = value?.trim()?.takeIf { it.startsWith("svg:") }

    fun canonicalSvg(value: String): String? = svg(value)?.let {
        "svg:" + it.removePrefix("svg:").trim()
    }

    @DrawableRes
    fun drawableResource(context: Context, value: String?, @DrawableRes fallback: Int): Int {
        if (value.isNullOrBlank() || codePoint(value) != null || svg(value) != null) return fallback
        return context.resources.getIdentifier(value, "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: fallback
    }

    fun drawable(context: Context, value: String?, @DrawableRes fallback: Int): Drawable? {
        val svgValue = svg(value)
        if (svgValue != null) {
            return SvgPathDrawable.parse(svgValue.removePrefix("svg:"), context.resources.displayMetrics.density)
        }
        if (fallback == 0) return null
        return AppCompatResources.getDrawable(context, drawableResource(context, value, fallback))
    }
}

/** Renders the path-based SVG subset used by toolbar icons without adding another dependency. */
private class SvgPathDrawable(
    private val path: Path,
    private val fillColor: Int,
    private val viewBoxWidth: Float,
    private val viewBoxHeight: Float,
    private val density: Float
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var tintList: ColorStateList? = null
    private var tintColor: Int? = null

    override fun draw(canvas: Canvas) {
        paint.color = tintColor ?: fillColor
        val scale = minOf(bounds.width() / viewBoxWidth, bounds.height() / viewBoxHeight)
        canvas.save()
        canvas.translate(
            bounds.left + (bounds.width() - viewBoxWidth * scale) / 2f,
            bounds.top + (bounds.height() - viewBoxHeight * scale) / 2f
        )
        canvas.scale(scale, scale)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun setTint(tintColor: Int) {
        tintList = null
        this.tintColor = tintColor
        invalidateSelf()
    }

    override fun setTintList(tint: ColorStateList?) {
        tintList = tint
        tintColor = tint?.getColorForState(state, tint.defaultColor)
        invalidateSelf()
    }

    override fun isStateful(): Boolean = tintList?.isStateful == true

    override fun onStateChange(state: IntArray): Boolean {
        val tint = tintList ?: return false
        val newColor = tint.getColorForState(state, tint.defaultColor)
        if (newColor == tintColor) return false
        tintColor = newColor
        invalidateSelf()
        return true
    }
    override fun getIntrinsicWidth(): Int = (24f * density + 0.5f).toInt()
    override fun getIntrinsicHeight(): Int = getIntrinsicWidth()
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    companion object {
        fun parse(xml: String, density: Float): Drawable? = runCatching {
            val parser = android.util.Xml.newPullParser().apply { setInput(xml.reader()) }
            val combinedPath = Path()
            var hasPath = false
            var fillColor = Color.BLACK
            var width = 24f
            var height = 24f
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) continue
                if (parser.name == "svg") {
                    parser.getAttributeValue(null, "viewBox")?.trim()?.split(Regex("[ ,]+"))
                        ?.mapNotNull(String::toFloatOrNull)
                        ?.takeIf { it.size == 4 }
                        ?.let { width = it[2]; height = it[3] }
                } else if (parser.name == "path") {
                    parser.getAttributeValue(null, "d")?.let { data ->
                        combinedPath.addPath(PathParser.createPathFromPathData(data))
                        hasPath = true
                    }
                    parser.getAttributeValue(null, "fill")
                        ?.takeUnless { it == "currentColor" || it == "none" }
                        ?.let { fillColor = Color.parseColor(it) }
                }
            }
            SvgPathDrawable(combinedPath, fillColor, width, height, density).takeIf { hasPath }
        }.getOrNull()
    }
}

/**
 * Unified configuration for both Kawaii Bar and Status Area buttons layout.
 * Stored in a single JSON file for easier management.
 */
@Serializable
data class ButtonsLayoutConfig(
    /**
     * List of buttons to display on Kawaii Bar, in order.
     * Maximum 6 buttons recommended for visual balance.
     * The fixed 'more' button is stored first and cannot be moved.
     */
    @SerialName("kawaiiBarButtons")
    val kawaiiBarButtons: List<ConfigurableButton>,

    /**
     * List of buttons to display in Status Area, in order.
     * Displayed in a 4-column grid layout.
     * Note: 'input_method_options' button is always added automatically at the end and should not be in this list.
     */
    @SerialName("statusAreaButtons")
    val statusAreaButtons: List<ConfigurableButton>,

    /** Buttons hidden from both visible toolbar sections. */
    @SerialName("optionalButtons")
    val optionalButtons: List<ConfigurableButton> = emptyList()
) {
    companion object {
        private const val DEFAULT_MORE_ICON = "font:E141"

        fun defaultMoreButton(): ConfigurableButton = ConfigurableButton(
            id = "more",
            icon = DEFAULT_MORE_ICON
        )

        fun moreButtonOrDefault(buttons: List<ConfigurableButton>): ConfigurableButton {
            val button = buttons.firstOrNull { it.id == "more" } ?: return defaultMoreButton()
            return if (button.icon.isNullOrBlank()) button.copy(icon = DEFAULT_MORE_ICON) else button
        }

        /**
         * Default unified button configuration.
         */
        fun default(): ButtonsLayoutConfig = ButtonsLayoutConfig(
            kawaiiBarButtons = listOf(
                defaultMoreButton(),
                ConfigurableButton("undo"),
                ConfigurableButton("redo"),
                ConfigurableButton("cursor_move"),
                ConfigurableButton("floating_toggle"),
                ConfigurableButton("clipboard"),
                ConfigurableButton("theme_toggle")
            ),
            // Note: input_method_options is always added automatically at the end of Status Area
            statusAreaButtons = listOf(
                ConfigurableButton("theme"),
                ConfigurableButton("reload_config"),
                ConfigurableButton("virtual_keyboard"),
                ConfigurableButton("one_handed_keyboard")
            ),
            optionalButtons = listOf(ConfigurableButton("search"))
        )
    }
}

/**
 * Configuration for Kawaii Bar buttons layout.
 * @deprecated Use [ButtonsLayoutConfig] instead
 */
@Deprecated("Use ButtonsLayoutConfig instead", ReplaceWith("ButtonsLayoutConfig"))
@Serializable
data class KawaiiBarButtonsConfig(
    /**
     * List of buttons to display on Kawaii Bar, in order.
     * Maximum 6 buttons recommended for visual balance.
     */
    @SerialName("buttons")
    val buttons: List<ConfigurableButton>
) {
    companion object {
        /**
         * Default Kawaii Bar button configuration.
         * Note: 'more' button is always added automatically and is not part of this default config.
         */
        @Deprecated("Use ButtonsLayoutConfig.default() instead")
        @Suppress("DEPRECATION")
        fun default(): KawaiiBarButtonsConfig = KawaiiBarButtonsConfig(
            buttons = listOf(
                ConfigurableButton("undo"),
                ConfigurableButton("redo"),
                ConfigurableButton("cursor_move"),
                ConfigurableButton("floating_toggle"),
                ConfigurableButton("clipboard")
            )
        )
    }
}

/**
 * Configuration for Status Area buttons layout.
 * @deprecated Use [ButtonsLayoutConfig] instead
 */
@Deprecated("Use ButtonsLayoutConfig instead", ReplaceWith("ButtonsLayoutConfig"))
@Serializable
data class StatusAreaButtonsConfig(
    /**
     * List of buttons to display in Status Area, in order.
     * Displayed in a 4-column grid layout.
     */
    @SerialName("buttons")
    val buttons: List<ConfigurableButton>
) {
    companion object {
        /**
         * Default Status Area button configuration.
         */
        @Deprecated("Use ButtonsLayoutConfig.default() instead")
        @Suppress("DEPRECATION")
        fun default(): StatusAreaButtonsConfig = StatusAreaButtonsConfig(
            // Note: input_method_options is always added automatically at the end of Status Area
            buttons = listOf(
                ConfigurableButton("theme"),
                ConfigurableButton("reload_config"),
                ConfigurableButton("virtual_keyboard"),
                ConfigurableButton("one_handed_keyboard")
            )
        )
    }
}
