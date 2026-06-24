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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.util.stream.Collectors;

/**
 * GUI: displays all loaded worlds as clickable items.
 * Supports search filter (anvil or chat input if available).
 * Clicking a world opens {@link WorldProtDeleteConfirmInventory}.
 *
 * <p>Slots layout (54):
 * <ul>
 *   <li>0–50: world items (max 51 per page)</li>
 *   <li>51: prev page</li>
 *   <li>52: next page</li>
 *   <li>53: close</li>
 * </ul>
 */
public final class WorldProtDeleteInventory extends BlockProtInventory {

    private static final int PAGE_SIZE = 51;

    private @Nullable String filter = null;

    public WorldProtDeleteInventory() {
        super(false);
    }

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        String title = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__TITLE);
        return (title == null || title.isBlank()) ? "Delete World Protections" : title;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        int slot = event.getSlot();

        switch (slot) {
            case PAGE_SIZE -> {
                if (state.currentPageIndex > 0) {
                    state.currentPageIndex--;
                    refill(player, state);
                }
                return;
            }
            case PAGE_SIZE + 1 -> {
                List<World> worlds = filteredWorlds();
                if ((long) (state.currentPageIndex + 1) * PAGE_SIZE < worlds.size()) {
                    state.currentPageIndex++;
                    refill(player, state);
                }
                return;
            }
            case PAGE_SIZE + 2 -> {
                player.closeInventory();
                InventoryState.remove(player.getUniqueId());
                return;
            }
        }

        List<World> worlds = filteredWorlds();
        int idx = state.currentPageIndex * PAGE_SIZE + slot;
        if (idx < 0 || idx >= worlds.size()) return;

        World selected = worlds.get(idx);
        InventoryState.remove(player.getUniqueId());

        WorldProtDeleteConfirmInventory confirm = new WorldProtDeleteConfirmInventory();
        Inventory inv = confirm.fill(player, selected.getName());
        player.openInventory(inv);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    /** Opens the world-selector for the given player. */
    public Inventory fill(@NotNull Player player, @Nullable String searchFilter) {
        this.filter = searchFilter;

        InventoryState state = InventoryState.builder().build();
        state.currentPageIndex = 0;
        InventoryState.set(player.getUniqueId(), state);

        inventory = createInventory();
        return populateInventory(player, state);
    }

    private void refill(@NotNull Player player, @NotNull InventoryState state) {
        inventory = createInventory();
        populateInventory(player, state);
        player.openInventory(inventory);
    }

    private Inventory populateInventory(@NotNull Player player, @NotNull InventoryState state) {
        inventory.clear();

        List<World> worlds = filteredWorlds();
        int offset = state.currentPageIndex * PAGE_SIZE;

        String hintKey = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__SELECT_HINT);

        for (int i = 0; i < PAGE_SIZE && (offset + i) < worlds.size(); i++) {
            World w = worlds.get(offset + i);
            ItemStack item = new ItemStack(worldMaterial(w), 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(w.getName()));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection()
                    .deserialize(hintKey != null ? hintKey : "§eClick to select"));
                lore.add(Component.text(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__ENVIRONMENT) + w.getEnvironment().name()));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        if (state.currentPageIndex > 0) {
            setItemStack(PAGE_SIZE, Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
        }
        if (worlds.size() - offset > PAGE_SIZE) {
            setItemStack(PAGE_SIZE + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        }
        setItemStack(PAGE_SIZE + 2, Material.BARRIER, TranslationKey.INVENTORIES__BACK);

        return inventory;
    }

    private List<World> filteredWorlds() {
        List<World> all = Bukkit.getWorlds();
        if (filter == null || filter.isBlank()) return all;
        String lf = filter.toLowerCase();
        return all.stream()
            .filter(w -> w.getName().toLowerCase().contains(lf))
            .collect(Collectors.toList());
    }

    private static Material worldMaterial(@NotNull World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }
}
