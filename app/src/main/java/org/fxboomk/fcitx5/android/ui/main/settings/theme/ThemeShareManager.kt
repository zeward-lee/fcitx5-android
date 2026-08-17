/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.theme

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fxboomk.fcitx5.android.BuildConfig
import org.fxboomk.fcitx5.android.R
import org.fxboomk.fcitx5.android.data.theme.Theme
import org.fxboomk.fcitx5.android.data.theme.ThemeFilesManager
import org.fxboomk.fcitx5.android.ui.main.settings.behavior.share.JsonFileQrShareManager
import org.fxboomk.fcitx5.android.ui.main.settings.behavior.share.LayoutQrBitmapUtil
import org.fxboomk.fcitx5.android.utils.toast
import splitties.resources.styledDrawable
import java.io.File

class ThemeShareManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val previewViewProvider: () -> View?,
    private val startActivity: (Intent) -> Unit
) {
    fun share(theme: Theme.Custom, displayName: String = theme.name) {
        if (theme.backgroundImage != null) {
            MaterialAlertDialogBuilder(context)
                .setIcon(context.styledDrawable(android.R.attr.alertDialogIcon))
                .setTitle(R.string.share)
                .setMessage(R.string.theme_share_has_background_zip_tip)
                .setPositiveButton(android.R.string.ok) { _, _ -> shareThemeAsZip(theme) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }
        shareThemeAsQr(theme, displayName)
    }

    private fun shareThemeAsZip(theme: Theme.Custom) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val cacheDir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val zipFile = File(cacheDir, "theme-${System.currentTimeMillis()}.zip")
                    zipFile.outputStream().use { output ->
                        ThemeFilesManager.exportTheme(theme, output).getOrThrow()
                    }
                    zipFile
                }
            }
            result.onSuccess { zipFile ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${BuildConfig.APPLICATION_ID}.share.fileprovider",
                    zipFile
                )
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, context.getString(R.string.theme_share_zip_title)))
            }.onFailure(context::toast)
        }
    }

    private fun shareThemeAsQr(theme: Theme.Custom, displayName: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val bundle = ThemeQrTransferCodec.encodeThemeToChunks(theme)
                    val labels = JsonFileQrShareManager.buildChunkLabels(
                        bundle = bundle,
                        typeLabel = context.getString(R.string.qr_payload_type_theme),
                        nameLabel = displayName
                    )
                    val previewBitmap = withContext(Dispatchers.Main) { renderThemePreviewBitmap() }
                    val image = try {
                        LayoutQrBitmapUtil.composeLongImageStreamingWithPreview(
                            bundle.chunks.map { it.encode() },
                            labels,
                            previewBitmap
                        )
                    } finally {
                        if (previewBitmap != null && !previewBitmap.isRecycled) previewBitmap.recycle()
                    }
                    JsonFileQrShareManager.saveLongImageToShareCache(context, image, "theme-qr").also {
                        if (!image.isRecycled) image.recycle()
                    }
                }
            }
            result.onSuccess { uri ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, context.getString(R.string.theme_share_qr_title)))
                context.toast(R.string.text_keyboard_layout_qr_exported)
            }.onFailure {
                context.toast(
                    context.getString(R.string.text_keyboard_layout_qr_export_failed, it.localizedMessage ?: "")
                )
            }
        }
    }

    private suspend fun renderThemePreviewBitmap(): Bitmap? = withContext(Dispatchers.Main) {
        val root = previewViewProvider() ?: return@withContext null
        if (root.width <= 0 || root.height <= 0) return@withContext null
        delay(16)
        Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            root.draw(Canvas(bitmap))
        }
    }
}
