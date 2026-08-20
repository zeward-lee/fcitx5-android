/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fxboomk.fcitx5.android.ui.main.settings.behavior

import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import org.fxboomk.fcitx5.android.R
import org.fxboomk.fcitx5.android.data.prefs.AppPrefs
import org.fxboomk.fcitx5.android.data.prefs.ManagedPreferenceFragment
import org.fxboomk.fcitx5.android.input.candidates.floating.FloatingCandidatesMode
import org.fxboomk.fcitx5.android.ui.main.settings.SettingsRoute
import org.fxboomk.fcitx5.android.utils.addPreference
import org.fxboomk.fcitx5.android.utils.navigateWithAnim

class KeyboardSettingsHomeFragment : ManagedPreferenceFragment(AppPrefs.getInstance().keyboard) {

    private var candidatesPreference: Preference? = null

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        screen.removeAll()
        buildScreen(screen)
    }

    override fun onResume() {
        super.onResume()
        candidatesPreference?.summary = buildCandidatesSummary()
    }

    private fun buildScreen(screen: PreferenceScreen) {
        val keyboardPrefs = AppPrefs.getInstance().keyboard
        screen.addPreference(
            R.string.keyboard_settings_basic_behavior,
            buildBasicBehaviorSummary(keyboardPrefs)
        ) {
            navigateWithAnim(SettingsRoute.VirtualKeyboardBasic)
        }
        KeyboardSettingsSupport.run {
            screen.addDestinationPreference(
                this@KeyboardSettingsHomeFragment,
                R.string.keyboard_settings_touch_and_sound,
                buildTouchAndSoundSummary(keyboardPrefs),
                SettingsRoute.VirtualKeyboardTouchAndSound
            )
            screen.addDestinationPreference(
                this@KeyboardSettingsHomeFragment,
                R.string.keyboard_settings_toolbar_and_voice,
                buildToolbarSummary(keyboardPrefs),
                SettingsRoute.VirtualKeyboardToolbarAndInput
            )
            screen.addDestinationPreference(
                this@KeyboardSettingsHomeFragment,
                R.string.keyboard_settings_key_and_gesture,
                buildKeyAndGestureSummary(keyboardPrefs),
                SettingsRoute.VirtualKeyboardKeyAndGesture
            )
            screen.addDestinationPreference(
                this@KeyboardSettingsHomeFragment,
                R.string.keyboard_settings_layout_and_split,
                buildLayoutSummary(keyboardPrefs),
                SettingsRoute.VirtualKeyboardLayoutAndSplit
            )
            screen.addPreference(
                Preference(requireContext()).apply {
                    setTitle(R.string.keyboard_settings_candidates)
                    summary = buildCandidatesSummary()
                    isIconSpaceReserved = false
                    isSingleLineTitle = false
                    setOnPreferenceClickListener {
                        navigateWithAnim(SettingsRoute.VirtualKeyboardCandidates)
                        true
                    }
                    candidatesPreference = this
                }
            )
            screen.addDestinationPreference(
                this@KeyboardSettingsHomeFragment,
                R.string.keyboard_settings_advanced_customization,
                getString(R.string.keyboard_advanced_customization_summary),
                SettingsRoute.VirtualKeyboardAdvancedCustomization
            )
        }
    }

    private fun buildBasicBehaviorSummary(keyboardPrefs: AppPrefs.Keyboard): String {
        val focusSummary = if (keyboardPrefs.focusChangeResetKeyboard.getValue()) {
            getString(R.string.focus_change_reset_enabled_summary)
        } else {
            getString(R.string.focus_change_reset_disabled_summary)
        }
        val inlineSummary = if (keyboardPrefs.inlineSuggestions.getValue()) {
            getString(R.string.inline_suggestions_enabled_summary)
        } else {
            getString(R.string.inline_suggestions_disabled_summary)
        }
        return "$focusSummary，$inlineSummary"
    }

    private fun buildTouchAndSoundSummary(keyboardPrefs: AppPrefs.Keyboard): String {
        val haptic = getString(keyboardPrefs.hapticOnKeyPress.getValue().stringRes)
        val sound = getString(keyboardPrefs.soundOnKeyPress.getValue().stringRes)
        return "$haptic，$sound"
    }

    private fun buildToolbarSummary(keyboardPrefs: AppPrefs.Keyboard): String {
        val toolbarSummary = if (keyboardPrefs.expandToolbarByDefault.getValue()) {
            getString(R.string.expand_toolbar_by_default)
        } else {
            getString(R.string.toolbar_collapsed_by_default)
        }
        val voiceSummary = if (keyboardPrefs.showVoiceInputButton.getValue()) {
            getString(R.string.voice_input_button_visible_summary)
        } else {
            getString(R.string.voice_input_hidden_summary)
        }
        return "$toolbarSummary，$voiceSummary"
    }

    private fun buildKeyAndGestureSummary(keyboardPrefs: AppPrefs.Keyboard): String {
        val popupSummary = if (keyboardPrefs.popupOnKeyPress.getValue()) {
            getString(R.string.popup_preview_visible_summary)
        } else {
            getString(R.string.popup_preview_hidden_summary)
        }
        val langSwitchSummary = getString(keyboardPrefs.langSwitchKeyBehavior.getValue().stringRes)
        return "$popupSummary，$langSwitchSummary"
    }

    private fun buildLayoutSummary(keyboardPrefs: AppPrefs.Keyboard): String {
        val height = getString(
            R.string.keyboard_height_summary_compact,
            keyboardPrefs.keyboardHeightPercent.getValue(),
            keyboardPrefs.keyboardHeightPercentLandscape.getValue()
        )
        val split = if (keyboardPrefs.splitKeyboardEnabled.getValue()) {
            getString(R.string.split_keyboard_enabled_summary_short)
        } else {
            getString(R.string.split_keyboard_disabled_summary)
        }
        return "$height，$split"
    }

    private fun buildCandidatesSummary(): String {
        val prefs = AppPrefs.getInstance()
        val keyboardPrefs = prefs.keyboard
        val candidatesPrefs = prefs.candidates
        when (candidatesPrefs.mode.getValue()) {
            FloatingCandidatesMode.Always -> {
                val mode = getString(R.string.candidate_display_mode_floating)
                val orientation = getString(candidatesPrefs.orientation.getValue().stringRes)
                val position = getString(candidatesPrefs.virtualKeyboardPosition.getValue().stringRes)
                return "$mode，$orientation，$position"
            }
            FloatingCandidatesMode.SystemDefault -> {
                return getString(R.string.system_default)
            }
            FloatingCandidatesMode.Disabled -> {
                return getString(R.string.disabled)
            }
            FloatingCandidatesMode.InputDevice -> Unit
        }

        val mode = getString(R.string.candidate_display_mode_horizontal)
        val composition = getString(keyboardPrefs.compositionAreaStyle.getValue().stringRes)
        val horizontal = getString(keyboardPrefs.horizontalCandidateStyle.getValue().stringRes)
        val expanded = getString(keyboardPrefs.expandedCandidateStyle.getValue().stringRes)
        return "$mode，$composition，$horizontal，$expanded"
    }
}
