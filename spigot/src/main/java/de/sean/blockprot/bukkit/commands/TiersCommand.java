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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Manages administrative tiers and roles via /bp tiers.
 */
public final class TiersCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || AdminTierManager.hasPermission(sender, AdminAction.SETROLE)
            || sender.hasPermission(Permissions.ADMIN_OWNER.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }

        if (!BlockProt.getDefaultConfig().isAdminTiersEnabled()) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_TIERS_DISABLED));
            return true;
        }

        String targetName = null;
        String roleName = null;

        if (args.length >= 4 && args[1].equalsIgnoreCase("setrole")) {
            targetName = args[2];
            roleName = args[3];
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("setrole")) {
            targetName = args[1];
            roleName = args[2];
        } else {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_USAGE));
            return true;
        }

        AdminTier tier = AdminTier.fromString(roleName);
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = target != null ? target.getUniqueId() : null;
        if (targetUuid == null) {
            try {
                targetUuid = UUID.fromString(targetName);
            } catch (IllegalArgumentException ignored) {}
        }
        if (targetUuid == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            if (op.hasPlayedBefore() || op.isOnline()) {
                targetUuid = op.getUniqueId();
            }
        }
        if (targetUuid == null) {
            ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                .replace("{player}", targetName));
            return true;
        }

        AdminTierManager.setPlayerRole(targetUuid, tier, null);
        ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
            .replace("{player}", targetName)
            .replace("{role}", tier.getIdentifier()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();

        if (args.length == 2) {
            return List.of("setrole");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("setrole")) {
            return null;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("setrole")) {
            return List.of("t1", "t2", "t3", "owner", "none");
        }
        return Collections.emptyList();
    }
}
