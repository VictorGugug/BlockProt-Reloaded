/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.nbt.stats.ListStatisticItem;
import org.bukkit.Location;
import org.bukkit.Material;
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

public final class StatisticListInventory extends BlockProtInventory {
    public StatisticListInventory() { super(true); }
    private BukkitListStatistic<ListStatisticItem<?, Material>, ?> statistic;

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__STATISTICS__STATISTICS);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    closeAndOpen(player, this.fill(player, null));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                state.currentPageIndex++;
                closeAndOpen(player, fill(player, null));
            }
            case BARRIER -> goBack(player, state);
            case BLACK_STAINED_GLASS_PANE -> goBack(player, state);
            default -> handleBlockItemClick(event, player, state);
        }
    }

    private void handleBlockItemClick(@NotNull InventoryClickEvent event,
                                      @NotNull Player player,
                                      @NotNull InventoryState state) {
        List<ListStatisticItem<?, Material>> fullList = getFilteredList();
        int offset = (this.getSize() - 3) * state.currentPageIndex;
        int idx = offset + event.getSlot();
        if (idx < 0 || idx >= fullList.size()) return;

        ListStatisticItem<?, Material> entry = fullList.get(idx);
        if (!(entry instanceof LocationListEntry locEntry)) return;
        Location loc = locEntry.get();
        if (loc.getWorld() == null) return;

        // Only TP is available (left OR right click)
        if (!player.hasPermission(Permissions.BLOCKS_TP.key())) {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION_TP)));
            return;
        }
        player.closeInventory();
        player.teleport(loc.clone().add(0.5, 1.0, 0.5));
        InventoryState.remove(player.getUniqueId());
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(@NotNull final Player player,
                          @Nullable final BukkitListStatistic<ListStatisticItem<?, Material>, ?> stat) {
        if (stat != null) this.statistic = stat;
        if (this.statistic == null) throw new RuntimeException("No cached statistic available.");

        List<ListStatisticItem<?, Material>> list = getFilteredList();
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        final int max = this.getSize() - 3;
        int offset = max * state.currentPageIndex;

        boolean canTp = player.hasPermission(Permissions.BLOCKS_TP.key());
        String loreTP = Translator.get(canTp ? TranslationKey.INVENTORIES__STATS__LORE_TP
                                             : TranslationKey.INVENTORIES__STATS__LORE_NO_TP);

        for (int i = 0; i < Math.min(list.size() - offset, max); ++i) {
            final ListStatisticItem<?, Material> entry = list.get(offset + i);
            String lockedAgo = (entry instanceof LocationListEntry loc) ? loc.getLockedAgoText() : "";
            List<String> contents = (entry instanceof LocationListEntry loc) ? loc.getContentsLore() : List.of();
            setItemStackWithLore(i, resolveDisplayMaterial(entry.getItemType()), entry.getTitle(), loreTP, lockedAgo, contents);
        }

        if (list.size() - offset > max) {
            setItemStack(max,     Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
            setItemStack(max + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        }
        setBackButton();
        return inventory;
    }

    private List<ListStatisticItem<?, Material>> getFilteredList() {
        return statistic.get()
            .stream()
            // Skip stale entries where the block no longer exists
            .filter(e -> {
                if (e instanceof LocationListEntry loc) {
                    try { return loc.get().getBlock().getType() != Material.AIR; }
                    catch (Exception ignored) { return false; }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private Material resolveDisplayMaterial(Material raw) {
        if (raw == null || raw == Material.AIR) return Material.CHEST;
        return raw;
    }

    private void setItemStackWithLore(int index, Material material, String name, String loreTp, String lockedAgo, List<String> contents) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            inventory.setItem(index, stack);
            return;
        }
        meta.displayName(net.kyori.adventure.text.Component.text(
            name.replaceAll("[§&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize(loreTp));
        if (!lockedAgo.isEmpty()) lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize(lockedAgo));
        if (!contents.isEmpty()) {
            lore.add(net.kyori.adventure.text.Component.empty());
            for (String line : contents) {
                lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(line));
            }
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        inventory.setItem(index, stack);
    }
}
