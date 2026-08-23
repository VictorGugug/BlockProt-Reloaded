/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorldLockableSelectionInventory extends BlockProtInventory {

    private static final int SLOT_BACK = 53;

    private static final int CONTENT_START = 0;
    private static final int CONTENT_END   = 45;
    private static final int MAX_WORLDS    = CONTENT_END - CONTENT_START;

    public WorldLockableSelectionInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return "§6" + Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE) + " §7- §e" + Translator.get(TranslationKey.WORLDS__WORLDS);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory(getTranslatedInventoryName());

        List<World> worlds = Bukkit.getWorlds();
        WorldsConfig wc = BlockProt.getWorldsConfig();

        Map<String, Integer> counts = wc != null ? wc.getAllWorldLockedCounts() : Map.of();

        int slot = CONTENT_START;
        for (int i = 0; i < worlds.size() && slot < CONTENT_END; i++) {
            World w = worlds.get(i);
            String wName = w.getName();
            boolean enabled = wc == null || wc.hasWorldConfig(w) || !wc.isWorldDisabled(w);
            int count = counts.getOrDefault(wName, 0);

            ItemStack item = new ItemStack(enabled ? Material.GRASS_BLOCK : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                ComponentMessages.displayName(meta, Component.text("§e" + wName));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7" + Translator.get(TranslationKey.WORLDS__STATUS)
                    + ": " + (enabled ? "§a" + Translator.get(TranslationKey.ENABLED)
                                      : "§c" + Translator.get(TranslationKey.DISABLED))));
                lore.add(Component.text("§7" + Translator.get(TranslationKey.WORLDS__PROTECTED_COUNT)
                    + ": §f" + (count >= 0 ? String.valueOf(count) : "?") + Translator.get(TranslationKey.INVENTORIES__LOCKABLE_SELECTION__BLOCKS_SUFFIX)));
                if (wc != null && wc.hasWorldConfig(w)) {
                    lore.add(Component.text("§8" + Translator.get(TranslationKey.WORLDS__WORLD_CONFIG_HINT)));
                }
                ComponentMessages.lore(meta, lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
        }

        if (worlds.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta em = empty.getItemMeta();
            if (em != null) {
                em.displayName(Component.text("§7" + Translator.get(TranslationKey.WORLDS__NO_WORLDS)));
                empty.setItemMeta(em);
            }
            inventory.setItem(22, empty);
        }

        InventoryState state = InventoryState.get(player.getUniqueId());
        boolean hasParent = state != null && (state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty());
        if (hasParent) {
            setBackButton(SLOT_BACK);
        } else {
            setItemStack(SLOT_BACK, Material.BARRIER, TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        }
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) {
            boolean hasParent = state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty();
            if (hasParent) {
                goBack(player, state);
            } else {
                closeAndOpen(player, null);
            }
            return;
        }

        if (slot >= CONTENT_START && slot < CONTENT_END) {
            int idx = slot - CONTENT_START;
            List<World> worlds = Bukkit.getWorlds();
            if (idx < worlds.size()) {
                World w = worlds.get(idx);
                state.originStack.push(InventoryState.MenuOrigin.WORLD_LOCKABLE_SELECTION);
                InventoryState.set(player.getUniqueId(), state);
                player.openInventory(new WorldLockableDetailInventory(w).fill(player));
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}
}