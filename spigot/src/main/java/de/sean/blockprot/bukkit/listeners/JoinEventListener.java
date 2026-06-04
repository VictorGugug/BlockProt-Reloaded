/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.sean.blockprot.bukkit.util.SkinCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;

public class JoinEventListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Pre-fetch the player's own skin so their head is ready on first GUI open.
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () ->
            SkinCache.getOrFetch(player.getName(), player.getUniqueId()));

        if (BlockProt.getDefaultConfig().shouldNotifyOpOfUpdates() && player.isOp()) {
            Bukkit.getScheduler().runTaskAsynchronously(
                BlockProt.getInstance(),
                new UpdateChecker(
                    BlockProt.getPluginVersion(),
                    Collections.singletonList(player)
                )
            );
        }
        if (BlockProt.getDefaultConfig().publicIsFriendByDefault() && !player.hasPlayedBefore()) {
            new PlayerSettingsHandler(player).addEveryoneAsFriend();
        }
    }
}
