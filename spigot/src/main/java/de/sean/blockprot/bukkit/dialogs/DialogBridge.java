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

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DialogBridge {

    void closeDialog(@NotNull Player player);

    void showNotice(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @Nullable DialogButton ok
    );

    void showConfirmation(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull DialogButton yes,
        @NotNull DialogButton no
    );

    void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull List<DialogButton> actions
    );

    void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull List<DialogButton> actions,
        @Nullable DialogButton exit,
        int columns
    );

    /**
     * Shows a dialog with a single native text input field plus a confirm
     * button and an optional back/exit button. The input's current value is
     * read back and passed to onSubmit in the same click that triggers the
     * confirm button, without closing or re-showing the dialog first, so the
     * client-side cursor-recenter behavior tied to opening a new Screen does
     * not occur. Implementations that lack a native text-input dialog type
     * should fall back to a chat/anvil/sign prompt instead of silently
     * dropping the field.
     */
    void showValueInput(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull DialogTextField field,
        @NotNull Consumer<String> onSubmit,
        @Nullable DialogButton back
    );
}
