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

package de.sean.blockprot.bukkit.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Lazy-loading WorldGuard logic for {@link WorldGuardIntegration}. This class is
 * only ever loaded and verified when WorldGuard is actually installed, so it is
 * free to reference WorldGuard types directly without breaking plugin startup on
 * servers that do not run WorldGuard.
 */
final class WorldGuardSupport {

    private static Object allowBlockprotFlag;

    private WorldGuardSupport() {}

    static void registerFlag() {
        try {
            var registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("allow-blockprot", true);
            registry.register(flag);
            allowBlockprotFlag = flag;
        } catch (FlagConflictException e) {
            var existing = WorldGuard.getInstance().getFlagRegistry().get("allow-blockprot");
            if (existing instanceof StateFlag sf) {
                allowBlockprotFlag = sf;
            }
        }
    }

    static boolean isAllowedByFlag(@NotNull Player who, @NotNull Block block) {
        if (allowBlockprotFlag == null) return true;
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        var localPlayer = WorldGuardPlugin.inst().wrapPlayer(who);
        return query.testState(BukkitAdapter.adapt(block.getLocation()), localPlayer, (StateFlag) allowBlockprotFlag);
    }
}