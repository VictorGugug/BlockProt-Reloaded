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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminTierSelectInventory extends BlockProtInventory {

    private static final int SLOT_PLAYER        = 4;
    private static final int SLOT_OWNER         = 11;
    private static final int SLOT_T3            = 12;
    private static final int SLOT_T2            = 13;
    private static final int SLOT_T1            = 14;
    private static final int SLOT_NONE          = 15;
    private static final int SLOT_BACK          = 18;
    private static final int SLOT_CUSTOM        = 22;

    private static final int[] SEPARATOR_SLOTS = {
        0, 1, 2, 3, 5, 6, 7, 8,
        9, 10, 16, 17,
        19, 20, 21, 23, 24, 25, 26
    };

    private final String targetName;
    private final UUID targetUuid;

    public AdminTierSelectInventory(@NotNull String targetName, @NotNull UUID targetUuid) {
        super(false);
        this.targetName = targetName;
        this.targetUuid = targetUuid;
    }

    @Override
    int getSize() {
        return InventoryConstants.tripleLine;
    }

    @Override
    String getTranslatedInventoryName() {
        return stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__SELECT_ROLE_TITLE))
            .replace("{player}", targetName);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        AdminTier currentTier = AdminTierManager.getRole(targetUuid);
        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        boolean isOp = onlineTarget != null ? onlineTarget.isOp() : Bukkit.getOfflinePlayer(targetUuid).isOp();

        List<String> skullLore = new ArrayList<>();
        if (isOp) {
            skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_OP));
        } else {
            skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_NOT_OP));
        }
        skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CURRENT_ROLE)
            .replace("{role}", currentTier.getIdentifier()));
        skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__SELECT_ROLE_BELOW));

        setPlayerSkullAsync(SLOT_PLAYER, player, targetUuid, targetName,
            "§e" + targetName + (isOp ? " §c[OP]" : ""),
            skullLore
        );

        inventory.setItem(SLOT_OWNER, roleItem(Material.NETHERITE_CHESTPLATE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_OWNER,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_OWNER_LORE,
            currentTier == AdminTier.OWNER));

        inventory.setItem(SLOT_T3, roleItem(Material.DIAMOND_CHESTPLATE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T3,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T3_LORE,
            currentTier == AdminTier.T3));

        inventory.setItem(SLOT_T2, roleItem(Material.GOLDEN_CHESTPLATE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T2,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T2_LORE,
            currentTier == AdminTier.T2));

        inventory.setItem(SLOT_T1, roleItem(Material.IRON_CHESTPLATE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T1,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T1_LORE,
            currentTier == AdminTier.T1));

        inventory.setItem(SLOT_NONE, roleItem(Material.BARRIER,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_NONE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_NONE_LORE,
            currentTier == AdminTier.NONE));

        inventory.setItem(SLOT_CUSTOM, roleItem(Material.LEATHER_CHESTPLATE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_CUSTOM,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_CUSTOM_LORE,
            currentTier == AdminTier.CUSTOM,
            Color.fromRGB(0x28, 0x56, 0xD0)));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) {
            InventoryState backState = InventoryState.builder()
                .origin(state.origin)
                .build();
            backState.originStack.addAll(state.originStack);
            InventoryState.set(player.getUniqueId(), backState);
            player.openInventory(new AdminTiersInventory().fill(player, 0));
            return;
        }

        if (slot == SLOT_CUSTOM) {
            if (event.isRightClick()) {
                player.openInventory(new AdminCustomFlagsInventory(targetName, targetUuid).fill(player));
                return;
            }
            AdminTierManager.setPlayerRole(targetUuid, AdminTier.CUSTOM, AdminTierManager.getCustomFlags(targetUuid));
            ComponentMessages.sendLegacyActionBar(player,
                Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
                    .replace("{player}", targetName)
                    .replace("{role}", AdminTier.CUSTOM.getIdentifier()));
            player.openInventory(new AdminCustomFlagsInventory(targetName, targetUuid).fill(player));
            return;
        }

        AdminTier selected = null;
        if (slot == SLOT_OWNER) selected = AdminTier.OWNER;
        else if (slot == SLOT_T3) selected = AdminTier.T3;
        else if (slot == SLOT_T2) selected = AdminTier.T2;
        else if (slot == SLOT_T1) selected = AdminTier.T1;
        else if (slot == SLOT_NONE) selected = AdminTier.NONE;

        if (selected != null) {
            AdminTierManager.setPlayerRole(targetUuid, selected, null);
            ComponentMessages.sendLegacyActionBar(player,
                Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
                    .replace("{player}", targetName)
                    .replace("{role}", selected.getIdentifier()));
            InventoryState backState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            InventoryState.set(player.getUniqueId(), backState);
            player.openInventory(new AdminTiersInventory().fill(player, 0));
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

    private ItemStack roleItem(@NotNull Material mat, @NotNull TranslationKey nameKey,
                               @NotNull TranslationKey loreKey, boolean active) {
        return roleItem(mat, nameKey, loreKey, active, null);
    }

    private ItemStack roleItem(@NotNull Material mat, @NotNull TranslationKey nameKey,
                               @NotNull TranslationKey loreKey, boolean active,
                               @Nullable Color armorColor) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (meta instanceof LeatherArmorMeta leatherMeta && armorColor != null) {
            leatherMeta.setColor(armorColor);
        }

        String prefix = active ? "§a● " : "§7○ ";
        ComponentMessages.displayName(meta, Component.text(prefix + stripColor(Translator.get(nameKey))));

        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize(Translator.get(loreKey)));
        if (nameKey == TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_CUSTOM) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CUSTOM_LEFT_CLICK)));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CUSTOM_RIGHT_CLICK)));
        } else {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CLICK_TO_ASSIGN)));
        }
        ComponentMessages.lore(meta, lore);

        item.setItemMeta(meta);
        return item;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }
}
