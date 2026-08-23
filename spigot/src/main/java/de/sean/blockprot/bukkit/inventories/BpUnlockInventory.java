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
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin GUI opened by {@code /bp unlock <player>}.
 *
 * <p>Lists every block owned by the target player.
 * <ul>
 *   <li><b>Left-click</b> a block: view its inventory contents (read-only; no taking or placing).</li>
 *   <li><b>Right-click</b> a block: remove its protection entirely; an action-bar message
 *       shows the block name and the owner it was removed from.</li>
 * </ul>
 *
 * Requires {@code blockprot.user.admin}.
 */
public final class BpUnlockInventory extends BlockProtInventory {

    private @NotNull  String                targetName = "?";
    private @Nullable PlayerBlocksStatistic statistic;

    public BpUnlockInventory() {
        super(false);
    }

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__TITLE)
            .replace("{player}", targetName);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        int max = getSize() - 3;

        switch (item.getType()) {
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    refill(player, state);
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                state.currentPageIndex++;
                refill(player, state);
            }
            case BARRIER -> {
                boolean hasParent = state.origin != InventoryState.MenuOrigin.NONE || !state.originStack.isEmpty();
                if (hasParent) {
                    goBack(player, state);
                } else {
                    closeAndOpen(player, null);
                }
            }
            default -> handleBlockClick(event, player, state, max);
        }
    }

    private void handleBlockClick(@NotNull InventoryClickEvent event,
                                   @NotNull Player player,
                                   @NotNull InventoryState state,
                                   int max) {
        List<LocationListEntry> list = filteredList();
        int offset = max * state.currentPageIndex;
        int idx    = offset + event.getSlot();
        if (idx < 0 || idx >= list.size()) return;

        Location loc = list.get(idx).get();
        if (loc == null || loc.getWorld() == null) return;
        Block block = loc.getBlock();

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
            if (!(block.getState() instanceof InventoryHolder)) return;
            try {
                InventoryState blockState = InventoryState.builder()
                    .block(block)
                    .origin(InventoryState.MenuOrigin.NONE)
                    .build();
                InventoryState.set(player.getUniqueId(), blockState);
                BlockInspectContentsInventory viewer = new BlockInspectContentsInventory(player);
                player.openInventory(viewer.fill());
            } catch (RuntimeException ignored) { }

        } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            BlockNBTHandler handler;
            try {
                handler = new BlockNBTHandler(block);
            } catch (RuntimeException ignored) {
                return;
            }
            if (!handler.isProtected()) return;

            String blockName = handler.getName();
            String ownerUuid = handler.getOwner();

            handler.clear();
            HopperEventListener.invalidate(block);
            if (ownerUuid != null && !ownerUuid.isEmpty()) {
                try {
                    StatHandler.removeContainerByUuid(
                        UUID.fromString(ownerUuid), loc.clone());
                } catch (IllegalArgumentException ignored) {}
            }

            String msg = Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_REMOVED)
                .replace("{block}",  blockName)
                .replace("{player}", targetName)
                .replace("{x}",      String.valueOf(block.getX()))
                .replace("{y}",      String.valueOf(block.getY()))
                .replace("{z}",      String.valueOf(block.getZ()));
            ComponentMessages.sendLegacyActionBar(player, msg);

            refill(player, state);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    /**
     * Populates the inventory.
     *
     * @param admin      the admin who opened this GUI
     * @param targetName the display name of the target player
     * @param stat       pre-loaded block statistics for the target player
     * @return the populated {@link Inventory}
     */
    public Inventory fill(@NotNull Player admin,
                          @NotNull String targetName,
                          @NotNull PlayerBlocksStatistic stat) {
        this.targetName = targetName;
        this.statistic  = stat;

        InventoryState state = InventoryState.get(admin.getUniqueId());
        if (state == null) {
            state = InventoryState.builder().build();
            InventoryState.set(admin.getUniqueId(), state);
        }
        state.currentPageIndex = 0;

        inventory = createInventory();
        return populateInventory(admin, state);
    }

    private void refill(@NotNull Player player, @NotNull InventoryState state) {
        inventory = createInventory();
        populateInventory(player, state);
        player.openInventory(inventory);
    }

    private Inventory populateInventory(@NotNull Player player, @NotNull InventoryState state) {
        inventory.clear();

        List<LocationListEntry> list = filteredList();
        final int max    = getSize() - 3;
        final int offset = max * state.currentPageIndex;

        String leftHint  = Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__LEFT_CLICK_HINT);
        String rightHint = Translator.get(TranslationKey.INVENTORIES__BP_UNLOCK__RIGHT_CLICK_HINT);

        if (list.isEmpty()) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta  m     = paper.getItemMeta();
            if (m != null) {
                ComponentMessages.displayName(m, net.kyori.adventure.text.Component.text(
                    Translator.get(TranslationKey.MESSAGES__BP_UNLOCK_NO_BLOCKS)
                        .replace("{player}", targetName)
                        .replaceAll("[§&][0-9a-fk-orx]", "")));
                paper.setItemMeta(m);
            }
            inventory.setItem(22, paper);
        } else {
            for (int i = 0; i < Math.min(list.size() - offset, max); i++) {
                renderEntry(i, list.get(offset + i), leftHint, rightHint);
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
                try   { return e.get().getBlock().getType() != Material.AIR; }
                catch (Exception ignored) { return false; }
            })
            .collect(Collectors.toList());
    }

    private void renderEntry(int slot,
                             @NotNull LocationListEntry entry,
                             @NotNull String leftHint,
                             @NotNull String rightHint) {
        Material mat = entry.getItemType();
        if (mat == Material.AIR) mat = Material.CHEST;

        ItemStack stack = new ItemStack(mat, 1);
        ItemMeta  meta  = stack.getItemMeta();
        if (meta == null) { inventory.setItem(slot, stack); return; }

        ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(
            entry.getTitle().replaceAll("[§&][0-9a-fk-orx]", "")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize(leftHint));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(rightHint));
        String ago = entry.getLockedAgoText();
        if (!ago.isEmpty()) lore.add(LegacyComponentSerializer.legacySection().deserialize(ago));

        ComponentMessages.lore(meta, lore);
        stack.setItemMeta(meta);
        inventory.setItem(slot, stack);
    }
}