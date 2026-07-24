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

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a single native text input field to be shown inside a dialog,
 * independent of the underlying Paper Dialog API types. Used by
 * {@link DialogBridge#showValueInput} so callers never touch
 * {@code io.papermc.paper.registry.data.dialog.input.TextDialogInput} directly.
 *
 * <p>The field is read back, in the same click that submits the dialog,
 * through the response view keyed by {@link #key()}. No dialog reopen is
 * involved, which avoids the client cursor-recenter behavior from reopening
 * the dialog.
 */
public record DialogTextField(
    @NotNull String key,
    @NotNull Component label,
    @NotNull String initialValue,
    int maxLength,
    int width
) {
    /** Default widget width in GUI pixels, matching vanilla's text field default. */
    public static final int DEFAULT_WIDTH = 200;

    public DialogTextField {
        if (maxLength <= 0) maxLength = 32;
        if (width <= 0) width = DEFAULT_WIDTH;
    }

    public static @NotNull DialogTextField of(@NotNull String key, @NotNull Component label, @NotNull String initialValue) {
        return new DialogTextField(key, label, initialValue, 32, DEFAULT_WIDTH);
    }
}
