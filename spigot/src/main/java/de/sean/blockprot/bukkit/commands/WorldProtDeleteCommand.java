/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
 * This file is part of BlockProt Reloaded.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
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
 * /bp protdel [world]
 *
 * <p>Without argument: opens the world-selector GUI.
 * <p>With argument: skips the selector and goes directly to the confirmation GUI for that world.
 *
 * <p>Requires {@code blockprot.user.admin}.
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

        // /bp protdell <world> — direct confirmation
        if (args.length >= 2) {
            String worldName = args[1];
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND)
                        .replace("{world}", worldName)));
                return true;
            }
            InventoryState.remove(player.getUniqueId());
            WorldProtDeleteConfirmInventory confirm = new WorldProtDeleteConfirmInventory();
            player.openInventory(confirm.fill(player, world.getName()));
            return true;
        }

        // /bp protdell — open world selector GUI
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
