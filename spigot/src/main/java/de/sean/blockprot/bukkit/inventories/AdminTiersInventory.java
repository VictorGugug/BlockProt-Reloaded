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
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminTiersInventory extends BlockProtInventory {

    private static final int SLOT_SET_OFFLINE = 4;
    private static final int SLOT_PREV        = 45;
    private static final int SLOT_BACK        = 49;
    private static final int SLOT_NEXT        = 53;

    private static final int[] PLAYER_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] SEPARATOR_SLOTS = {
        0, 1, 2, 3, 5, 6, 7, 8,
        9, 17,
        18, 26,
        27, 35,
        36, 44,
        46, 47, 48, 50, 51, 52
    };

    private record PlayerEntry(@NotNull String name, @NotNull UUID uuid, @NotNull AdminTier tier, boolean isOp) {}

    private int currentPage = 0;
    private final List<PlayerEntry> entries = new ArrayList<>();

    public AdminTiersInventory() {
        super(false);
    }

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        return fill(player, 0);
    }

    @NotNull
    public Inventory fill(@NotNull Player player, int page) {
        inventory = createInventory();
        fillSeparators();

        entries.clear();
        for (Player online : Bukkit.getOnlinePlayers()) {
            AdminTier tier = AdminTierManager.getPlayerTier(online);
            entries.add(new PlayerEntry(online.getName(), online.getUniqueId(), tier, online.isOp()));
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PLAYER_SLOTS.length));
        currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int from = currentPage * PLAYER_SLOTS.length;
        int to = Math.min(from + PLAYER_SLOTS.length, entries.size());

        inventory.setItem(SLOT_SET_OFFLINE, item(Material.NAME_TAG,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__SET_OFFLINE),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__SET_OFFLINE_LORE)));

        for (int i = 0; i < (to - from); i++) {
            PlayerEntry entry = entries.get(from + i);
            int slot = PLAYER_SLOTS[i];
            String opTag = entry.isOp ? " §c[OP]" : "";
            List<String> lore = new ArrayList<>();
            if (entry.isOp) {
                lore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_OP));
            } else {
                lore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__STATUS_NOT_OP));
            }
            lore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CURRENT_ROLE)
                .replace("{role}", entry.tier.getIdentifier()));
            lore.add(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CLICK_TO_ASSIGN));

            setPlayerSkullAsync(slot, player, entry.uuid, entry.name,
                "§e" + entry.name + opTag + " §7[" + entry.tier.getIdentifier() + "]",
                lore
            );
        }

        if (currentPage > 0) {
            inventory.setItem(SLOT_PREV, item(Material.ARROW,
                Translator.get(TranslationKey.INVENTORIES__LAST_PAGE), ""));
        }

        if (currentPage < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, item(Material.ARROW,
                Translator.get(TranslationKey.INVENTORIES__NEXT_PAGE), ""));
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
            InventoryState backState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            InventoryState.set(player.getUniqueId(), backState);
            player.openInventory(new AdminMenuInventory().fill(player));
            return;
        }

        if (slot == SLOT_PREV && currentPage > 0) {
            player.openInventory(fill(player, currentPage - 1));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PLAYER_SLOTS.length));
        if (slot == SLOT_NEXT && currentPage < totalPages - 1) {
            player.openInventory(fill(player, currentPage + 1));
            return;
        }

        if (slot == SLOT_SET_OFFLINE) {
            state.suppressCloseReopen = true;
            TextInput.open(player, BlockProt.getInstance(),
                stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__SET_OFFLINE)),
                inputName -> {
                    if (inputName == null || inputName.isBlank()) return;
                    String clean = inputName.trim();
                    Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                        UUID targetUuid = null;
                        Player online = Bukkit.getPlayer(clean);
                        if (online != null) targetUuid = online.getUniqueId();
                        if (targetUuid == null) {
                            try {
                                targetUuid = UUID.fromString(clean);
                            } catch (IllegalArgumentException ignored) {}
                        }
                        if (targetUuid == null) {
                            @SuppressWarnings("deprecation")
                            OfflinePlayer op = Bukkit.getOfflinePlayer(clean);
                            if (op.hasPlayedBefore() || op.isOnline()) {
                                targetUuid = op.getUniqueId();
                            }
                        }
                        if (targetUuid == null) {
                            targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + clean).getBytes(StandardCharsets.UTF_8));
                        }
                        final UUID finalUuid = targetUuid;
                        Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                            InventoryState selectState = InventoryState.builder()
                                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                                .build();
                            InventoryState.set(player.getUniqueId(), selectState);
                            player.openInventory(new AdminTierSelectInventory(clean, finalUuid).fill(player));
                        });
                    });
                }
            );
            return;
        }

        for (int i = 0; i < PLAYER_SLOTS.length; i++) {
            if (slot == PLAYER_SLOTS[i]) {
                int index = currentPage * PLAYER_SLOTS.length + i;
                if (index < entries.size()) {
                    PlayerEntry entry = entries.get(index);
                    InventoryState selectState = InventoryState.builder()
                        .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                        .build();
                    InventoryState.set(player.getUniqueId(), selectState);
                    player.openInventory(new AdminTierSelectInventory(entry.name, entry.uuid).fill(player));
                }
                return;
            }
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

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        ComponentMessages.displayName(meta, Component.text(stripColor(name)));
        List<Component> loreList = new ArrayList<>(lore.length);
        for (String s : lore) {
            if (!s.isEmpty()) loreList.add(LegacyComponentSerializer.legacySection().deserialize(s));
        }
        ComponentMessages.lore(meta, loreList);
        item.setItemMeta(meta);
        return item;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }
}
