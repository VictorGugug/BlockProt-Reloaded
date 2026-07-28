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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.dialogs.DialogOrigin;
import de.sean.blockprot.bukkit.dialogs.ProtdelDialog;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.inventories.WorldProtDeleteConfirmInventory;
import de.sean.blockprot.bukkit.inventories.WorldProtDeleteInventory;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /bp protdel [world]: world protection deletion tool.
 */
public final class WorldProtDeleteCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS)));
            return true;
        }

        if (!player.isOp() && !player.hasPermission(Permissions.USER_ADMIN.key())) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
            return true;
        }

        if (args.length >= 2) {
            String worldName = args[1];
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND)
                        .replace("{world}", worldName)));
                return true;
            }
            if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
                ProtdelDialog.show(player, world.getName(), DialogOrigin.NONE);
                return true;
            }
            InventoryState.remove(player.getUniqueId());
            WorldProtDeleteConfirmInventory confirm = new WorldProtDeleteConfirmInventory();
            player.openInventory(confirm.fill(player, world.getName()));
            return true;
        }

        if (BlockProt.getDefaultConfig().shouldUseDialogs(player)) {
            ProtdelDialog.show(player, null, DialogOrigin.NONE);
            return true;
        }
        InventoryState.remove(player.getUniqueId());
        WorldProtDeleteInventory selector = new WorldProtDeleteInventory();
        player.openInventory(selector.fill(player, null));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(partial))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}