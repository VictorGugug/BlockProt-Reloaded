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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

/**
 * Admin GUI: lists every block owned by a specific player.
 * Similar to {@link StatisticListInventory} but works for offline players and
 * requires {@code blockprot.user.admin}.
 *
 * <p>Opened via {@code /bp info <player>} or the INFO button in the admin menu.
 */
public final class AdminBlockListInventory extends BlockProtInventory {

    private @Nullable PlayerBlocksStatistic statistic;
    private @NotNull  String targetName = "?";

    public AdminBlockListInventory() {
        super(false);
    }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__ADMIN_BLOCK_LIST__TITLE)
            .replace("{player}", targetName);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    closeAndOpen(admin, fill(admin, null, null));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                state.currentPageIndex++;
                closeAndOpen(admin, fill(admin, null, null));
            }
            case BARRIER -> {
                if (state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty()) {
                    goBack(admin, state);
                } else {
                    closeAndOpen(admin, null);
                }
            }
            default -> teleportToBlock(event, admin, state);
        }
    }

    private void teleportToBlock(@NotNull InventoryClickEvent event,
                                 @NotNull Player admin,
                                 @NotNull InventoryState state) {
        List<LocationListEntry> list = filteredList();
        int max    = getSize() - 3;
        int offset = max * state.currentPageIndex;
        int idx    = offset + event.getSlot();
        if (idx < 0 || idx >= list.size()) return;

        Location loc = list.get(idx).get();
        if (loc.getWorld() == null) return;

        if (!admin.hasPermission(Permissions.BLOCKS_TP.key())) {
            ComponentMessages.sendLegacyActionBar(admin, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION_TP));
            return;
        }
        admin.closeInventory();
        admin.teleport(loc.clone().add(0.5, 1.0, 0.5));
        InventoryState.remove(admin.getUniqueId());
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(@NotNull Player admin,
                          @Nullable String targetName,
                          @Nullable PlayerBlocksStatistic stat) {
        if (targetName != null) this.targetName = targetName;
        if (stat       != null) this.statistic  = stat;
        if (this.statistic == null)
            throw new IllegalStateException("No statistic loaded for AdminBlockListInventory");

        inventory = createInventory();

        final InventoryState state = InventoryState.get(admin.getUniqueId());
        if (state == null) return inventory;

        List<LocationListEntry> list   = filteredList();
        final int               max    = getSize() - 3;
        int                     offset = max * state.currentPageIndex;

        boolean canTp  = admin.hasPermission(Permissions.BLOCKS_TP.key());
        String  loreTp = Translator.get(canTp
                ? TranslationKey.INVENTORIES__STATS__LORE_TP
                : TranslationKey.INVENTORIES__STATS__LORE_NO_TP);

        if (list.isEmpty()) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta  m     = paper.getItemMeta();
            if (m != null) {
                ComponentMessages.displayName(m, net.kyori.adventure.text.Component.text(
                    Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_NO_BLOCKS)
                        .replace("{player}", this.targetName)
                        .replaceAll("[§&][0-9a-fk-orx]", "")));
                paper.setItemMeta(m);
            }
            inventory.setItem(22, paper);
        } else {
            for (int i = 0; i < Math.min(list.size() - offset, max); i++) {
                renderEntry(i, list.get(offset + i), loreTp);
            }
        }

        if (state.currentPageIndex > 0) {
            setItemStack(max,     Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
        }
        if (list.size() - offset > max) {
            setItemStack(max + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        }

        boolean hasParent = state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty();
        setItemStack(max + 2, Material.BARRIER, hasParent ? TranslationKey.INVENTORIES__BACK : TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        return inventory;
    }

    private List<LocationListEntry> filteredList() {
        if (statistic == null) return List.of();
        return statistic.get().stream()
            .filter(e -> {
                try {
                    Location loc = e.get();
                    if (loc.getWorld() == null) return false;
                    Material mat = loc.getBlock().getType();
                    if (mat.isAir()) return false;
                    return BlockProt.getDefaultConfig().isLockable(mat, loc.getWorld());
                } catch (Exception ignored) {
                    return false;
                }
            })
            .toList();
    }

    private void renderEntry(int slot, @NotNull LocationListEntry entry, @NotNull String loreTp) {
        Material mat = entry.getItemType();
        if (mat == Material.AIR) return;

        ItemStack stack = new ItemStack(mat, 1);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) { inventory.setItem(slot, stack); return; }

        ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(
            entry.getTitle().replaceAll("[§&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize(loreTp));
        String ago = entry.getLockedAgoText();
        if (!ago.isEmpty()) lore.add(LegacyComponentSerializer.legacySection().deserialize(ago));
        ComponentMessages.lore(meta, lore);
        stack.setItemMeta(meta);
        inventory.setItem(slot, stack);
    }
}