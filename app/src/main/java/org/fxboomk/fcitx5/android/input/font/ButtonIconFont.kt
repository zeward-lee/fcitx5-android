/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.input.font

import android.content.Context
import android.graphics.Typeface

object ButtonIconFont {
    private const val ASSET_PATH = "iconfont.ttf"

    @Volatile
    private var cachedTypeface: Typeface? = null

    fun typeface(context: Context): Typeface {
        if (FontProviders.hasFont("button_icon_font")) {
            return FontProviders.resolveTypeface("button_icon_font", Typeface.DEFAULT)
        }
        cachedTypeface?.let { return it }
        return synchronized(this) {
            cachedTypeface ?: runCatching {
                Typeface.createFromAsset(context.assets, ASSET_PATH)
            }.getOrElse {
                FontProviders.resolveTypeface("button_icon_font", Typeface.DEFAULT)
            }.also { cachedTypeface = it }
        }
    }
}
