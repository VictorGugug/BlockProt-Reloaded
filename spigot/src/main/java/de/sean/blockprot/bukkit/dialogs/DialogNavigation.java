/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified back button for dialog screens, deriving the standard close or back
 * label and tooltip from the origin the screen was opened from.
 */
public final class DialogNavigation {

    private static final String BUTTON_ID = "back";
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor HINT_GRAY = TextColor.color(0x888888);

    private DialogNavigation() {}

    public static DialogButton backButton(@NotNull DialogOrigin origin, @Nullable DialogButton.DialogClickHandler onBack) {
        return backButton(origin, onBack, null);
    }

    public static DialogButton backButton(@NotNull DialogOrigin origin, @Nullable DialogButton.DialogClickHandler onBack,
                                          @Nullable TranslationKey tooltipOverride) {
        Component label = Component.text(
            stripColor(Translator.get(origin == DialogOrigin.NONE
                ? TranslationKey.DIALOGS__CLOSE
                : TranslationKey.DIALOGS__BACK)),
            SOFT_GRAY);
        Component tooltip = Component.text(
            stripColor(Translator.get(tooltipOverride != null ? tooltipOverride : tooltipKey(origin))),
            HINT_GRAY);
        return new DialogButton(BUTTON_ID, label, tooltip, onBack);
    }

    private static TranslationKey tooltipKey(@NotNull DialogOrigin origin) {
        return switch (origin) {
            case ADMIN_MENU -> TranslationKey.DIALOGS__RETURN_ADMIN_MENU;
            case USER_MENU -> TranslationKey.DIALOGS__RETURN_USER_MENU;
            case NONE -> TranslationKey.DIALOGS__CLOSE;
            default -> TranslationKey.DIALOGS__RETURN_PREVIOUS;
        };
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}