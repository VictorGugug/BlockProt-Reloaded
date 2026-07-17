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
    private static boolean loggedNoApi = false;

    private DialogBridgeFactory() {}

    @Nullable
    public static DialogBridge getBridge() {
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

    public static DialogOrigin resolveOrigin(@NotNull DialogOrigin backOrigin) {
        if (BlockProt.getDefaultConfig().areExtraCommandsEnabled()) {
            return DialogOrigin.NONE;
        }
        return backOrigin;
    }
}
