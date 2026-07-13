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
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UserMenuInventory extends BlockProtInventory {

    private static final int SLOT_SETTINGS   = 10;
    private static final int SLOT_FRIENDS    = 11;
    private static final int SLOT_STATS      = 12;
    private static final int SLOT_TRANSFER   = 14;
    private static final int SLOT_ABOUT      = 16;
    private static final int SLOT_BACK       = 49;

    private static final int[] SEPARATOR_SLOTS = {0,1,2,3,4,5,6,7,8, 9,13,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48,50,51,52,53};

    public UserMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();

        inventory.setItem(SLOT_SETTINGS, item(Material.WRITABLE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS_LORE)));
        inventory.setItem(SLOT_FRIENDS, item(Material.PLAYER_HEAD,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS_LORE)));
        inventory.setItem(SLOT_TRANSFER, item(Material.HOPPER,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER_LORE)));
        inventory.setItem(SLOT_ABOUT, item(Material.NETHER_STAR,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT_LORE)));

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null && state.origin != InventoryState.MenuOrigin.NONE) {
            setBackButton(SLOT_BACK);
        } else {
            inventory.setItem(SLOT_BACK, item(Material.BARRIER,
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE),
                Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE_LORE)));
        }

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_SETTINGS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new UserSettingsInventory().fill(player));
            new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
        } else if (slot == SLOT_FRIENDS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            var inv = new FriendManageInventory().fill(player);
            if (inv != null) player.openInventory(inv);
        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));
        } else if (slot == SLOT_TRANSFER) {
            player.closeInventory();
            player.performCommand("blockprot transferall");
        } else if (slot == SLOT_ABOUT) {
            player.closeInventory();
            player.performCommand("blockprot about");
        } else if (slot == SLOT_BACK) {
            goBack(player, state);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators() {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : SEPARATOR_SLOTS) {
            inventory.setItem(s, sep);
        }
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(net.kyori.adventure.text.Component.text(
            name.replaceAll("[§&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> l = new ArrayList<>();
        for (String s : lore) l.add(LegacyComponentSerializer.legacySection().deserialize(s));
        meta.lore(l);
        item.setItemMeta(meta);
        return item;
    }
}
