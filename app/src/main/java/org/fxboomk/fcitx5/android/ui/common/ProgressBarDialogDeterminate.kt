/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fxboomk.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.resolveThemeAttribute
import splitties.views.dsl.core.add
import splitties.views.dsl.core.endMargin
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.startMargin
import splitties.views.dsl.core.styles.AndroidStyles
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalMargin
import splitties.views.dsl.core.wrapContent
import splitties.views.textAppearance
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class DeterminateProgressBarDialog(
    context: Context,
    @StringRes title: Int,
    cancelable: Boolean = false,
    @StringRes negativeButton: Int? = null,
    onNegativeButtonClick: (() -> Unit)? = null,
) {
    private val progressBar: ProgressBar
    private val progressLabel: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dialogValue: AlertDialog? = null
    private val lastPercent = AtomicInteger(-1)

    val builder: AlertDialog.Builder
    val dialog: AlertDialog? get() = dialogValue

    init {
        val androidStyles = AndroidStyles(context)
        progressBar = androidStyles.progressBar.horizontal {
            isIndeterminate = false
            max = 100
            progress = 0
        }
        progressLabel = context.textView {
            textAppearance = context.resolveThemeAttribute(android.R.attr.textAppearanceListItem)
            text = context.getString(R.string.download_progress_percent, 0)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            minWidth = context.dp(48)
        }
        builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(
                context.horizontalLayout {
                    gravity = Gravity.CENTER_VERTICAL
                    add(progressBar, lParams {
                        width = 0
                        height = wrapContent
                        weight = 1f
                        verticalMargin = context.dp(20)
                        startMargin = context.dp(26)
                        endMargin = context.dp(12)
                    })
                    add(progressLabel, lParams {
                        width = wrapContent
                        height = wrapContent
                        endMargin = context.dp(26)
                    })
                }
            )
            .setCancelable(cancelable)
            .apply {
                if (negativeButton != null) {
                    setNegativeButton(negativeButton) { _, _ ->
                        onNegativeButtonClick?.invoke()
                    }
                }
            }
    }

    fun show(): AlertDialog {
        val existing = dialogValue
        if (existing != null) {
            existing.show()
            return existing
        }
        return builder.show().also { dialogValue = it }
    }

    fun dismiss() {
        dialogValue?.dismiss()
    }

    /**
     * Update the dialog with a 0-100 percentage. Safe to call from any thread;
     * duplicate percentages are ignored so download loops can call this freely.
     */
    fun setProgressPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        if (lastPercent.getAndSet(clamped) == clamped) return
        mainHandler.post {
            progressBar.progress = clamped
            progressLabel.text = progressBar.context.getString(
                R.string.download_progress_percent, clamped
            )
        }
    }
}

fun LifecycleCoroutineScope.withDeterminateProgressDialog(
    context: Context,
    @StringRes title: Int = R.string.loading,
    threshold: Long = 200L,
    cancellable: Boolean = false,
    @StringRes negativeButton: Int? = null,
    onCancel: (() -> Unit)? = null,
    action: suspend (setProgressPercent: (Int) -> Unit) -> Unit
): Job {
    val progressDialog = AtomicReference<DeterminateProgressBarDialog?>()
    val latestProgressPercent = AtomicInteger(0)
    var cancelHandled = false
    var actionJob: Job? = null
    fun handleCancel() {
        if (cancelHandled) return
        cancelHandled = true
        onCancel?.invoke()
        actionJob?.cancel()
    }
    val loadingJob = launch(Dispatchers.Main) {
        delay(threshold)
        if (actionJob?.isActive != true) return@launch
        val handle = DeterminateProgressBarDialog(
            context = context,
            title = title,
            cancelable = cancellable,
            negativeButton = negativeButton,
            onNegativeButtonClick = { handleCancel() },
        )
        progressDialog.set(handle)
        handle.setProgressPercent(latestProgressPercent.get())
        handle.show().apply {
            setCanceledOnTouchOutside(false)
            setOnCancelListener { handleCancel() }
        }
    }
    actionJob = launch {
        try {
            action { percent ->
                val clamped = percent.coerceIn(0, 100)
                latestProgressPercent.set(clamped)
                progressDialog.get()?.setProgressPercent(clamped)
            }
        } finally {
            loadingJob.cancelAndJoin()
            progressDialog.get()?.dismiss()
        }
    }
    return checkNotNull(actionJob)
}
