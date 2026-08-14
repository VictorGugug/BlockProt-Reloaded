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

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Maintenance category of the inventory-based admin config editor.
 */
public final class AdminConfigMaintenanceInventory extends BlockProtInventory {

    private static final int SLOT_INACTIVITY_CLEANUP = 11;
    private static final int SLOT_AUTO_RELOAD = 12;
    private static final int SLOT_AUTO_RELOAD_DELAY = 13;
    private static final int SLOT_SESSION_LOG = 14;
    private static final int SLOT_BACKUPS = 15;
    private static final int SLOT_BACK = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9,10, 16,17,
        18,19,20,21,22,23,24,25,26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigMaintenanceInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        int inactivityDays = cfg.getBukkitConfig().getInt("inactivity_cleanup_days", -1);
        inventory.setItem(SLOT_INACTIVITY_CLEANUP, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP_TITLE),
            String.valueOf(inactivityDays),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP),
            "inactivity_cleanup_days"));
        inventory.setItem(SLOT_AUTO_RELOAD, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_TITLE),
            "auto_reload_configs",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD),
            cfg.isAutoReloadEnabled()));
        inventory.setItem(SLOT_AUTO_RELOAD_DELAY, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY_TITLE),
            String.valueOf(cfg.getAutoReloadDelaySeconds()),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY),
            "auto_reload_delay_seconds"));
        inventory.setItem(SLOT_SESSION_LOG, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG_TITLE),
            "enable_session_log",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG),
            cfg.isSessionLogEnabled()));
        inventory.setItem(SLOT_BACKUPS, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS_TITLE),
            "enable_backups",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS),
            cfg.isBackupsEnabled()));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (slot == SLOT_BACK) {
            player.openInventory(new AdminConfigInventory().fill(player));
        } else if (slot == SLOT_INACTIVITY_CLEANUP) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__INACTIVITY_CLEANUP_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setAndSave("inactivity_cleanup_days", Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER).replace("{input}", input));
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_AUTO_RELOAD) {
            cfg.setAutoReloadConfigs(!cfg.isAutoReloadEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_AUTO_RELOAD_DELAY) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD_DELAY_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        int clamped = Math.max(0, Math.min(5, Integer.parseInt(input.trim())));
                        cfg.setAutoReloadDelaySeconds(clamped);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER).replace("{input}", input));
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_SESSION_LOG) {
            cfg.setSessionLogEnabled(!cfg.isSessionLogEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_BACKUPS) {
            cfg.setBackupsEnabled(!cfg.isBackupsEnabled());
            player.openInventory(fill(player));
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators() {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : SEPARATOR_SLOTS) {
            inventory.setItem(s, sep);
        }
    }
}