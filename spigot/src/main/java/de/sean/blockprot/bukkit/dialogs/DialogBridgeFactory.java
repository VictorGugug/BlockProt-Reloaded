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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.VersionCompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DialogBridgeFactory {

    private static @Nullable DialogBridge bridge;
    private static @Nullable DialogBridge testBridge;
    private static boolean loggedNoApi = false;

    private DialogBridgeFactory() {}

    /**
     * Overrides the active bridge for diagnostic runs. Pass null to restore.
     */
    public static void setTestBridge(@Nullable DialogBridge test) {
        testBridge = test;
    }

    @Nullable
    public static DialogBridge getBridge() {
        if (testBridge != null) return testBridge;
        if (bridge != null) return bridge;
        if (!VersionCompat.hasDialogApi()) {
            if (!loggedNoApi) {
                BlockProt.getInstance().getLogger().warning(
                    "Dialog API not found (requires Paper 1.21.7+). Falling back to inventories. Set use_dialogs: false in config.yml to suppress this warning, or the plugin will auto-disable it on next reload.");
                loggedNoApi = true;
            }
            return null;
        }
        try {
            Class<?> clazz = Class.forName("de.sean.blockprot.bukkit.dialogs.impl.PaperDialogBridge");
            bridge = (DialogBridge) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            BlockProt.getInstance().getLogger().severe(
                "Failed to load PaperDialogBridge: " + e.getMessage());
            bridge = null;
        }
        return bridge;
    }

    /**
     * Returns the origin a dialog's Back button should point to.
     *
     * <p>This used to force {@link DialogOrigin#NONE} (Close-only, no Back) whenever
     * {@code use_menus} was disabled ({@code areExtraCommandsEnabled()}). That was wrong:
     * with Paper Dialogs active, {@code /bp admin} always opens through the full
     * {@code AdminMenuDialog} navigation chain regardless of {@code use_menus} (see
     * {@code AdminMenuCommand}, which bypasses that flag entirely once
     * {@code shouldUseDialogs()} is true). Gating on the global flag here broke Back
     * navigation for every dialog in that already-valid chain the instant {@code use_menus}
     * was turned off - including from inside the very screen used to toggle it back on,
     * leaving no way back except closing and reopening with {@code /bp admin}.
     *
     * <p>{@code backOrigin} is only ever set by the call site that actually opened the
     * dialog (its real parent), so it is always safe to honor directly.
     */
    public static DialogOrigin resolveOrigin(@NotNull DialogOrigin backOrigin) {
        return backOrigin;
    }
}
