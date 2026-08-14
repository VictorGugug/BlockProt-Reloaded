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

import java.util.Locale;

/**
 * Entity protection category of the inventory-based admin config editor.
 */
public final class AdminConfigEntityInventory extends BlockProtInventory {

    private static final int SLOT_PROTECTION_ENABLED = 11;
    private static final int SLOT_AUTO_PROTECT_TAME = 12;
    private static final int SLOT_WORKSTATION_ENABLED = 13;
    private static final int SLOT_WORKSTATION_RADIUS = 14;
    private static final int SLOT_WORKSTATION_VERTICAL_RADIUS = 15;
    private static final int SLOT_VILLAGER_LOCATE_SECONDS = 21;
    private static final int SLOT_MENU_ITEM = 23;
    private static final int SLOT_BACK = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9,10, 16,17,
        18,19,20, 22, 24,25,26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigEntityInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(SLOT_PROTECTION_ENABLED, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED_TITLE),
            "entity_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED),
            cfg.isEntityProtectionEnabled()));
        inventory.setItem(SLOT_AUTO_PROTECT_TAME, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME_TITLE),
            "entity_protection.auto_protect_on_tame",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME),
            cfg.isEntityProtectionAutoProtectOnTame()));
        inventory.setItem(SLOT_WORKSTATION_ENABLED, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED_TITLE),
            "villager_workstation_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED),
            cfg.isVillagerWorkstationProtectionEnabled()));

        int workstationRadius = cfg.getBukkitConfig().getInt("villager_workstation_protection.radius", 2);
        inventory.setItem(SLOT_WORKSTATION_RADIUS, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS_TITLE),
            String.valueOf(workstationRadius),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS),
            "villager_workstation_protection.radius"));

        int workstationVerticalRadius = cfg.getBukkitConfig().getInt("villager_workstation_protection.vertical_radius", 1);
        inventory.setItem(SLOT_WORKSTATION_VERTICAL_RADIUS, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS_TITLE),
            String.valueOf(workstationVerticalRadius),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS),
            "villager_workstation_protection.vertical_radius"));

        int villagerLocateSeconds = cfg.getBukkitConfig().getInt("entity_protection.villager_locate_seconds", 6);
        inventory.setItem(SLOT_VILLAGER_LOCATE_SECONDS, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS_TITLE),
            String.valueOf(villagerLocateSeconds),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS),
            "entity_protection.villager_locate_seconds"));

        String menuItem = cfg.getBukkitConfig().getString("entity_protection.menu_item", "STICK");
        inventory.setItem(SLOT_MENU_ITEM, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_TITLE),
            menuItem != null ? menuItem : "STICK",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM),
            "entity_protection.menu_item"));

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
        } else if (slot == SLOT_PROTECTION_ENABLED) {
            cfg.setEntityProtectionEnabled(!cfg.isEntityProtectionEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_AUTO_PROTECT_TAME) {
            cfg.setAndSave("entity_protection.auto_protect_on_tame", !cfg.isEntityProtectionAutoProtectOnTame());
            player.openInventory(fill(player));
        } else if (slot == SLOT_WORKSTATION_ENABLED) {
            cfg.setAndSave("villager_workstation_protection.enabled", !cfg.isVillagerWorkstationProtectionEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_WORKSTATION_RADIUS) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setAndSave("villager_workstation_protection.radius", Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_WORKSTATION_VERTICAL_RADIUS) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setAndSave("villager_workstation_protection.vertical_radius", Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_VILLAGER_LOCATE_SECONDS) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setAndSave("entity_protection.villager_locate_seconds", Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_MENU_ITEM) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    if (Material.matchMaterial(input.trim()) == null) {
                        player.sendMessage(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_INVALID)
                            .replace("{input}", input));
                        return;
                    }
                    cfg.setAndSave("entity_protection.menu_item", input.trim().toUpperCase(Locale.ROOT));
                    player.openInventory(fill(player));
                });
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

    private void sendInvalidNumber(@NotNull Player player, @NotNull String input) {
        player.sendMessage(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER)
            .replace("{input}", input));
    }
}