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
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Admin GUI listing every player who has played on the server.
 * Opened via /bp info (no argument). Supports paging and two sort modes:
 *   - Name A-Z / Z-A (toggle)
 *   - Block count descending / ascending (toggle)
 *
 * Clicking a player skull opens AdminBlockListInventory for that player.
 */
public final class PlayerListInventory extends BlockProtInventory {

    /** Sort modes cycled on sort-button click. */
    public enum SortMode {
        NAME_ASC, NAME_DESC, BLOCKS_DESC, BLOCKS_ASC;

        public SortMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final int PLAYER_SLOTS = 45;
    private static final int SLOT_PREV    = 45;
    private static final int SLOT_NEXT    = 46;
    private static final int SLOT_SORT    = 49;
    private static final int SLOT_BACK    = 53;

    private SortMode sortMode = SortMode.NAME_ASC;
    private List<PlayerEntry> entries = null;

    @Nullable private BukkitTask loadTask = null;

    private record PlayerEntry(OfflinePlayer player, String name, int blockCount) {}

    public PlayerListInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__TITLE);
    }

    /**
     * Begins async loading of all players, then opens the GUI.
     * Call from the main thread.
     */
    @NotNull
    public Inventory open(@NotNull Player admin) {
        InventoryState state = InventoryState.get(admin.getUniqueId());
        if (state == null) {
            state = new InventoryState(null);
            InventoryState.set(admin.getUniqueId(), state);
        }
        state.currentPageIndex = 0;

        inventory = createInventory();
        fillLoading();

        final InventoryState finalState = state;
        loadTask = Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            List<PlayerEntry> loaded = loadEntries();
            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                entries = loaded;
                renderPage(admin, finalState);
            });
        });

        admin.openInventory(inventory);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        int slot = event.getRawSlot();

        if (slot < PLAYER_SLOTS) {
            if (entries == null) return;
            int idx = state.currentPageIndex * PLAYER_SLOTS + slot;
            if (idx >= entries.size()) return;
            PlayerEntry entry = entries.get(idx);

            String displayName = entry.name();
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatisticByUuid(stat, entry.player().getUniqueId());

            InventoryState ns = new InventoryState(null);
            ns.currentPageIndex = 0;
            ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            InventoryState.set(admin.getUniqueId(), ns);
            admin.openInventory(new AdminBlockListInventory().fill(admin, displayName, stat));
            return;
        }

        switch (slot) {
            case SLOT_PREV -> {
                if (state.currentPageIndex > 0) {
                    state.currentPageIndex--;
                    renderPage(admin, state);
                }
            }
            case SLOT_NEXT -> {
                if (entries != null && (state.currentPageIndex + 1) * PLAYER_SLOTS < entries.size()) {
                    state.currentPageIndex++;
                    renderPage(admin, state);
                }
            }
            case SLOT_SORT -> {
                sortMode = sortMode.next();
                if (entries != null) sortEntries();
                state.currentPageIndex = 0;
                renderPage(admin, state);
            }
            case SLOT_BACK -> {
                cancelLoad();
                closeAndOpen(admin, new de.sean.blockprot.bukkit.inventories.AdminMenuInventory().fill(admin));
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
        cancelLoad();
    }

    private void fillLoading() {
        inventory.clear();
        for (int i = 0; i < PLAYER_SLOTS; i++) {
            setItemStack(i, Material.SKELETON_SKULL, TranslationKey.INVENTORIES__LOADING);
        }
        renderControls(null);
    }

    private void renderPage(@NotNull Player admin, @NotNull InventoryState state) {
        inventory.clear();
        if (entries == null || entries.isEmpty()) {
            setItemStack(22, Material.PAPER,
                Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__EMPTY));
            renderControls(state);
            return;
        }

        int offset = state.currentPageIndex * PLAYER_SLOTS;
        int max    = Math.min(entries.size() - offset, PLAYER_SLOTS);

        for (int i = 0; i < max; i++) {
            PlayerEntry e = entries.get(offset + i);
            renderPlayerSlot(i, e, admin);
        }

        renderControls(state);

        List<PlayerEntry> page = entries.subList(offset, offset + max);
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            for (int i = 0; i < page.size(); i++) {
                PlayerEntry e = page.get(i);
                final int slot = i;
                var profile = createPlayerProfile(e.player().getUniqueId(), e.name());
                Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                    if (inventory.getViewers().isEmpty()) return;
                    ItemStack skull = inventory.getItem(slot);
                    if (skull == null || skull.getType() != Material.PLAYER_HEAD) return;
                    if (!(skull.getItemMeta() instanceof SkullMeta meta)) return;
                    //noinspection deprecation
                    meta.setOwnerProfile(profile);
                    skull.setItemMeta(meta);
                });
            }
        });
    }

    private void renderPlayerSlot(int slot, @NotNull PlayerEntry e, @NotNull Player viewer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(e.name()));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__BLOCK_COUNT)
                    .replace("{count}", String.valueOf(e.blockCount()))));

            boolean online = Bukkit.getPlayer(e.player().getUniqueId()) != null;
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                online
                    ? Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__ONLINE)
                    : Translator.get(TranslationKey.INVENTORIES__PLAYER_LIST__OFFLINE)));
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        inventory.setItem(slot, skull);
    }

    private void renderControls(@Nullable InventoryState state) {
        if (state != null && state.currentPageIndex > 0) {
            setItemStack(SLOT_PREV, Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
        } else {
            inventory.setItem(SLOT_PREV, null);
        }

        boolean hasNext = state != null && entries != null
            && (state.currentPageIndex + 1) * PLAYER_SLOTS < entries.size();
        if (hasNext) {
            setItemStack(SLOT_NEXT, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        } else {
            inventory.setItem(SLOT_NEXT, null);
        }

        String sortLabel = Translator.get(sortModeKey());
        setItemStack(SLOT_SORT, Material.COMPARATOR, sortLabel);

        setItemStack(SLOT_BACK, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
    }

    private TranslationKey sortModeKey() {
        return switch (sortMode) {
            case NAME_ASC   -> TranslationKey.INVENTORIES__PLAYER_LIST__SORT_NAME_ASC;
            case NAME_DESC  -> TranslationKey.INVENTORIES__PLAYER_LIST__SORT_NAME_DESC;
            case BLOCKS_DESC -> TranslationKey.INVENTORIES__PLAYER_LIST__SORT_BLOCKS_DESC;
            case BLOCKS_ASC  -> TranslationKey.INVENTORIES__PLAYER_LIST__SORT_BLOCKS_ASC;
        };
    }

    private List<PlayerEntry> loadEntries() {
        OfflinePlayer[] all = Bukkit.getOfflinePlayers();
        List<PlayerEntry> result = new ArrayList<>(all.length);

        for (OfflinePlayer op : all) {
            if (op.getName() == null) continue;
            java.util.UUID uuid = op.getUniqueId();
            if (uuid == null) continue;
            // Skip fake/legacy UUIDs from cracked servers that indicate non-real players
            int v = uuid.version();
            if (v != 0 && v != 3 && v != 4) continue;

            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatisticByUuid(stat, uuid);
            int count = stat.get().size();
            result.add(new PlayerEntry(op, op.getName(), count));
        }

        sortEntries(result, sortMode);
        return result;
    }

    private void sortEntries() {
        if (entries == null) return;
        sortEntries(entries, sortMode);
    }

    private static void sortEntries(@NotNull List<PlayerEntry> list, @NotNull SortMode mode) {
        Comparator<PlayerEntry> cmp = switch (mode) {
            case NAME_ASC    -> Comparator.comparing(e -> e.name().toLowerCase());
            case NAME_DESC   -> Comparator.comparing((PlayerEntry e) -> e.name().toLowerCase()).reversed();
            case BLOCKS_DESC -> Comparator.comparingInt(PlayerEntry::blockCount).reversed();
            case BLOCKS_ASC  -> Comparator.comparingInt(PlayerEntry::blockCount);
        };
        list.sort(cmp);
    }

    private void cancelLoad() {
        if (loadTask != null && !loadTask.isCancelled()) loadTask.cancel();
    }
}
