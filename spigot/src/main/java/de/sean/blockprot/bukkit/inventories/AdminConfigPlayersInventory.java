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
 * Players and friends category of the inventory-based admin config editor.
 */
public final class AdminConfigPlayersInventory extends BlockProtInventory {

    private static final int SLOT_LOCK_ON_PLACE = 10;
    private static final int SLOT_PUBLIC_IS_FRIEND = 11;
    private static final int SLOT_MAX_BLOCKS = 12;
    private static final int SLOT_HINT_COOLDOWN = 14;
    private static final int SLOT_FRIEND_SEARCH = 15;
    private static final int SLOT_DISABLE_FRIENDS = 16;
    private static final int SLOT_BACK = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9, 13, 17,
        18,19,20,21,22,23,24,25,26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigPlayersInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(SLOT_LOCK_ON_PLACE, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE_TITLE),
            "lock_on_place_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE),
            cfg.lockOnPlaceByDefault()));
        inventory.setItem(SLOT_PUBLIC_IS_FRIEND, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND_TITLE),
            "public_is_friend_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND),
            cfg.publicIsFriendByDefault()));

        int maxBlocks = cfg.getMaxLockedBlockCount() != null ? cfg.getMaxLockedBlockCount() : -1;
        inventory.setItem(SLOT_MAX_BLOCKS, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS_TITLE),
            String.valueOf(maxBlocks),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS),
            "player_max_locked_block_count"));

        int cooldown = (int) cfg.getLockHintCooldown();
        inventory.setItem(SLOT_HINT_COOLDOWN, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN_TITLE),
            String.valueOf(cooldown),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN),
            "lock_hint_cooldown_in_seconds"));

        double similarity = cfg.getFriendSearchSimilarityPercentage();
        inventory.setItem(SLOT_FRIEND_SEARCH, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH_TITLE),
            String.valueOf(similarity),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH),
            "friend_search_similarity"));

        inventory.setItem(SLOT_DISABLE_FRIENDS, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS_TITLE),
            "disable_friend_functionality",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS),
            cfg.isFriendFunctionalityDisabled()));

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
        } else if (slot == SLOT_LOCK_ON_PLACE) {
            cfg.setLockOnPlaceByDefault(!cfg.lockOnPlaceByDefault());
            player.openInventory(fill(player));
        } else if (slot == SLOT_PUBLIC_IS_FRIEND) {
            cfg.setPublicIsFriendByDefault(!cfg.publicIsFriendByDefault());
            player.openInventory(fill(player));
        } else if (slot == SLOT_MAX_BLOCKS) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setPlayerMaxLockedBlockCount(Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_HINT_COOLDOWN) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setLockHintCooldown(Integer.parseInt(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_FRIEND_SEARCH) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    try {
                        cfg.setFriendSearchSimilarity(Double.parseDouble(input.trim()));
                    } catch (NumberFormatException e) {
                        sendInvalidNumber(player, input);
                        return;
                    }
                    player.openInventory(fill(player));
                });
        } else if (slot == SLOT_DISABLE_FRIENDS) {
            cfg.setAndSave("disable_friend_functionality", !cfg.isFriendFunctionalityDisabled());
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

    private void sendInvalidNumber(@NotNull Player player, @NotNull String input) {
        player.sendMessage(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER)
            .replace("{input}", input));
    }
}