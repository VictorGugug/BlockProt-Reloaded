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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AdminCustomFlagsInventory extends BlockProtInventory {

    private static final int SLOT_PLAYER = 4;
    private static final int SLOT_BACK   = 36;

    private static final int[] ACTION_SLOTS = {
        11, 12, 13, 14, 15,
        20, 21, 22, 23, 24,
        29, 30, 31, 32, 33
    };

    private static final AdminAction[] ORDERED_ACTIONS = {
        AdminAction.INFO,
        AdminAction.TELEPORT,
        AdminAction.LOGS,
        AdminAction.BREAK,
        AdminAction.UNLOCK,
        AdminAction.LOCKABLES,
        AdminAction.CONTAINER_BYPASS,
        AdminAction.PROTDEL,
        AdminAction.CONFIG,
        AdminAction.DEBUG,
        AdminAction.RELOAD,
        AdminAction.UPDATE,
        AdminAction.INTEGRATIONS,
        AdminAction.RECOMMENDED,
        AdminAction.SETROLE
    };

    private static final int[] SEPARATOR_SLOTS = {
        0, 1, 2, 3, 5, 6, 7, 8,
        9, 10, 16, 17,
        18, 19, 25, 26,
        27, 28, 34, 35,
        37, 38, 39, 40, 41, 42, 43, 44
    };

    private final String targetName;
    private final UUID targetUuid;
    private final Map<Integer, AdminAction> slotToAction = new HashMap<>();

    public AdminCustomFlagsInventory(@NotNull String targetName, @NotNull UUID targetUuid) {
        super(false);
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        for (int i = 0; i < ORDERED_ACTIONS.length && i < ACTION_SLOTS.length; i++) {
            slotToAction.put(ACTION_SLOTS[i], ORDERED_ACTIONS[i]);
        }
    }

    @Override
    int getSize() {
        return InventoryConstants.quintupleLine;
    }

    @Override
    String getTranslatedInventoryName() {
        return stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__TITLE))
            .replace("{player}", targetName);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        Set<AdminAction> activeFlags = AdminTierManager.getCustomFlags(targetUuid);
        Player onlineTarget = Bukkit.getPlayer(targetUuid);
        boolean isOp = onlineTarget != null ? onlineTarget.isOp() : Bukkit.getOfflinePlayer(targetUuid).isOp();

        List<String> skullLore = new ArrayList<>();
        if (isOp) {
            skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_OP));
        } else {
            skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_NOT_OP));
        }
        skullLore.add("§dCustom: §f" + activeFlags.size() + "/" + ORDERED_ACTIONS.length + " active");
        skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__CLICK_TO_TOGGLE));

        setPlayerSkullAsync(SLOT_PLAYER, player, targetUuid, targetName,
            "§e" + targetName + (isOp ? " §c[OP]" : ""),
            skullLore
        );

        for (int i = 0; i < ORDERED_ACTIONS.length; i++) {
            AdminAction action = ORDERED_ACTIONS[i];
            int slot = ACTION_SLOTS[i];
            inventory.setItem(slot, createActionItem(action, activeFlags.contains(action)));
        }

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
            player.openInventory(new AdminTierSelectInventory(targetName, targetUuid).fill(player));
            return;
        }

        AdminAction action = slotToAction.get(slot);
        if (action != null) {
            boolean nowActive = AdminTierManager.toggleCustomFlag(targetUuid, action);
            inventory.setItem(slot, createActionItem(action, nowActive));
            Set<AdminAction> activeFlags = AdminTierManager.getCustomFlags(targetUuid);
            Player onlineTarget = Bukkit.getPlayer(targetUuid);
            boolean isOp = onlineTarget != null ? onlineTarget.isOp() : Bukkit.getOfflinePlayer(targetUuid).isOp();
            List<String> skullLore = new ArrayList<>();
            if (isOp) {
                skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_OP));
            } else {
                skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_NOT_OP));
            }
            skullLore.add("§dCustom: §f" + activeFlags.size() + "/" + ORDERED_ACTIONS.length + " active");
            skullLore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__CLICK_TO_TOGGLE));
            setPlayerSkullAsync(SLOT_PLAYER, player, targetUuid, targetName,
                "§e" + targetName + (isOp ? " §c[OP]" : ""),
                skullLore
            );
            player.updateInventory();
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

    private ItemStack createActionItem(@NotNull AdminAction action, boolean enabled) {
        Material mat = getActionMaterial(action);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = stripColor(Translator.get(getActionNameKey(action)));
        String prefix = enabled ? "§a[✓] " : "§7[○] ";
        ComponentMessages.displayName(meta, Component.text(prefix + name));

        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize(Translator.get(getActionDescKey(action))));
        lore.add(Component.text(""));
        String statusStr = enabled
            ? Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__ENABLED)
            : Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__DISABLED);
        lore.add(LegacyComponentSerializer.legacySection().deserialize(statusStr));
        String minTierStr = Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__MIN_TIER)
            .replace("{tier}", action.getMinimumTier().getIdentifier().toUpperCase(Locale.ROOT));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(minTierStr));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(
            Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__CLICK_TO_TOGGLE)));

        ComponentMessages.lore(meta, lore);

        if (enabled) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static Material getActionMaterial(@NotNull AdminAction action) {
        return switch (action) {
            case INFO -> Material.BOOK;
            case TELEPORT -> Material.ENDER_PEARL;
            case LOGS -> Material.WRITABLE_BOOK;
            case BREAK -> Material.DIAMOND_PICKAXE;
            case UNLOCK -> Material.TRIPWIRE_HOOK;
            case LOCKABLES -> Material.CHEST;
            case CONTAINER_BYPASS -> Material.HOPPER;
            case PROTDEL -> Material.TNT;
            case CONFIG -> Material.COMPARATOR;
            case DEBUG -> Material.REDSTONE_TORCH;
            case RELOAD -> Material.CLOCK;
            case UPDATE -> Material.NETHER_STAR;
            case INTEGRATIONS -> Material.CHAIN;
            case RECOMMENDED -> Material.BEACON;
            case SETROLE -> Material.GOLDEN_HELMET;
        };
    }

    private static TranslationKey getActionNameKey(@NotNull AdminAction action) {
        return switch (action) {
            case INFO -> TranslationKey.ADMIN_ACTION__INFO__NAME;
            case TELEPORT -> TranslationKey.ADMIN_ACTION__TELEPORT__NAME;
            case LOGS -> TranslationKey.ADMIN_ACTION__LOGS__NAME;
            case BREAK -> TranslationKey.ADMIN_ACTION__BREAK__NAME;
            case UNLOCK -> TranslationKey.ADMIN_ACTION__UNLOCK__NAME;
            case LOCKABLES -> TranslationKey.ADMIN_ACTION__LOCKABLES__NAME;
            case CONTAINER_BYPASS -> TranslationKey.ADMIN_ACTION__CONTAINER_BYPASS__NAME;
            case PROTDEL -> TranslationKey.ADMIN_ACTION__PROTDEL__NAME;
            case CONFIG -> TranslationKey.ADMIN_ACTION__CONFIG__NAME;
            case DEBUG -> TranslationKey.ADMIN_ACTION__DEBUG__NAME;
            case RELOAD -> TranslationKey.ADMIN_ACTION__RELOAD__NAME;
            case UPDATE -> TranslationKey.ADMIN_ACTION__UPDATE__NAME;
            case INTEGRATIONS -> TranslationKey.ADMIN_ACTION__INTEGRATIONS__NAME;
            case RECOMMENDED -> TranslationKey.ADMIN_ACTION__RECOMMENDED__NAME;
            case SETROLE -> TranslationKey.ADMIN_ACTION__SETROLE__NAME;
        };
    }

    private static TranslationKey getActionDescKey(@NotNull AdminAction action) {
        return switch (action) {
            case INFO -> TranslationKey.ADMIN_ACTION__INFO__DESC;
            case TELEPORT -> TranslationKey.ADMIN_ACTION__TELEPORT__DESC;
            case LOGS -> TranslationKey.ADMIN_ACTION__LOGS__DESC;
            case BREAK -> TranslationKey.ADMIN_ACTION__BREAK__DESC;
            case UNLOCK -> TranslationKey.ADMIN_ACTION__UNLOCK__DESC;
            case LOCKABLES -> TranslationKey.ADMIN_ACTION__LOCKABLES__DESC;
            case CONTAINER_BYPASS -> TranslationKey.ADMIN_ACTION__CONTAINER_BYPASS__DESC;
            case PROTDEL -> TranslationKey.ADMIN_ACTION__PROTDEL__DESC;
            case CONFIG -> TranslationKey.ADMIN_ACTION__CONFIG__DESC;
            case DEBUG -> TranslationKey.ADMIN_ACTION__DEBUG__DESC;
            case RELOAD -> TranslationKey.ADMIN_ACTION__RELOAD__DESC;
            case UPDATE -> TranslationKey.ADMIN_ACTION__UPDATE__DESC;
            case INTEGRATIONS -> TranslationKey.ADMIN_ACTION__INTEGRATIONS__DESC;
            case RECOMMENDED -> TranslationKey.ADMIN_ACTION__RECOMMENDED__DESC;
            case SETROLE -> TranslationKey.ADMIN_ACTION__SETROLE__DESC;
        };
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }
}
