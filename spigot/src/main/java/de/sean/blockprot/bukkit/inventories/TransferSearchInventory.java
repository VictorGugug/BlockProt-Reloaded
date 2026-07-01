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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.enginehub.squirrelid.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GUI for searching a player to transfer block ownership to.
 * Opens a chat/anvil input for the query, then shows matching players as skulls.
 * Clicking a skull executes the transfer immediately.
 */
public final class TransferSearchInventory extends BlockProtInventory {

    public TransferSearchInventory() { super(true); }

    private final ConcurrentLinkedQueue<Profile> resultQueue = new ConcurrentLinkedQueue<>();
    private final int maxResults = getSize() - 1;

    @Nullable private BukkitTask loadTask = null;
    @Nullable private BukkitTask updateTask = null;

    @Override
    int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__TRANSFER__TITLE);
    }

    public static void openSearch(@NotNull Player player, @NotNull org.bukkit.block.Block block) {
        InventoryState preserved = new InventoryState(block);
        preserved.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
        InventoryState existing = InventoryState.get(player.getUniqueId());
        if (existing != null) preserved.menuPermissions = existing.menuPermissions;
        InventoryState.set(player.getUniqueId(), preserved);

        String prompt = Translator.get(TranslationKey.INVENTORIES__TRANSFER__SEARCH_PROMPT);
        if (VersionCompat.isPaper()) {
            ChatInput.open(player, BlockProt.getInstance(),
                text -> openResult(player, text));
        } else {
            AnvilInput.open(player, BlockProt.getInstance(),
                prompt, prompt,
                text -> openResult(player, text));
        }
    }

    /** @deprecated Use {@link #openSearch(Player, org.bukkit.block.Block)} instead. */
    @Deprecated
    public static void openSearch(@NotNull Player player) {
        InventoryState state = InventoryState.get(player.getUniqueId());
        org.bukkit.block.Block block = state != null ? state.getBlock() : null;
        if (block == null) return;
        openSearch(player, block);
    }

    private static void openResult(@NotNull Player player, @Nullable String text) {
        if (text == null || text.isBlank()) return;
        Inventory inv = new TransferSearchInventory().fill(player, text);
        if (inv != null) player.openInventory(inv);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        event.setCancelled(true);

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> closeAndOpen(player,
                new BlockLockInventory().fill(player,
                    state.getBlock() != null ? state.getBlock().getType() : Material.CHEST,
                    state.getBlock() != null ? new BlockNBTHandler(state.getBlock()) : null));
            case PLAYER_HEAD, SKELETON_SKULL -> {
                if (!(item.getItemMeta() instanceof SkullMeta meta)) return;
                if (meta.getOwningPlayer() == null) return;
                OfflinePlayer target = meta.getOwningPlayer();
                Block block = state.getBlock();
                if (block == null) { player.closeInventory(); return; }

                doTransfer(player, block, target);
            }
            default -> closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
        if (loadTask != null) loadTask.cancel();
        if (updateTask != null) updateTask.cancel();
    }

    @Nullable
    public Inventory fill(@NotNull Player player, @NotNull String searchQuery) {
        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return null;

        for (int i = 0; i < maxResults; i++) {
            setItemStack(i, Material.SKELETON_SKULL, TranslationKey.INVENTORIES__SEARCHING);
        }
        setBackButton();

        updateTask = Bukkit.getScheduler().runTaskTimer(BlockProt.getInstance(),
            new UpdateTask(state), 0L, 1L);
        loadTask = Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(),
            new LoadTask(player, searchQuery,
                BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage()));

        return inventory;
    }

    private void doTransfer(@NotNull Player player, @NotNull Block block, @NotNull OfflinePlayer target) {
        if (target.getUniqueId() == null) { player.closeInventory(); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF_GUI)));
            closeAndOpen(player, new BlockLockInventory().fill(player, block.getType(), new BlockNBTHandler(block)));
            return;
        }

        BlockNBTHandler handler;
        try { handler = new BlockNBTHandler(block); }
        catch (RuntimeException e) { player.closeInventory(); return; }

        if (!handler.isOwner(player.getUniqueId())) {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_NOT_OWNER_GUI)));
            player.closeInventory();
            return;
        }

        var result = handler.transferOwner(
            player.getUniqueId().toString(),
            target.getUniqueId().toString()
        );

        if (result.success) {
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                StatHandler.addBlock(onlineTarget, block.getLocation());
            } else {
                StatHandler.addBlockByUuid(target.getUniqueId(), block.getLocation());
            }
            String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_SUCCESS).replace("{player}", name)));
            player.closeInventory();
        } else {
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_FAILED)));
            closeAndOpen(player, new BlockLockInventory().fill(player, block.getType(), new BlockNBTHandler(block)));
        }
    }

    private class UpdateTask implements Runnable {
        private final InventoryState state;
        private int idx = 0;

        UpdateTask(@NotNull InventoryState state) { this.state = state; }

        @Override
        public void run() {
            var scheduler = Bukkit.getScheduler();
            if (loadTask != null
                && !scheduler.isQueued(loadTask.getTaskId())
                && !scheduler.isCurrentlyRunning(loadTask.getTaskId())
                && resultQueue.isEmpty()) {
                if (idx == 0) for (int i = 0; i < maxResults; i++) inventory.clear(i);
                if (updateTask != null) updateTask.cancel();
                return;
            }

            Profile profile;
            while ((profile = resultQueue.poll()) != null && idx < maxResults) {
                if (idx == 0) for (int i = 0; i < maxResults; i++) inventory.clear(i);
                final String name = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();
                setPlayerSkull(idx, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), name));
                idx++;
            }
            if (idx == maxResults) {
                if (loadTask != null) loadTask.cancel();
                if (updateTask != null) updateTask.cancel();
            }
        }
    }

    private class LoadTask implements Runnable {
        private final Player player;
        private final String query;
        private final double minSimilarity;

        LoadTask(@NotNull Player player, @NotNull String query, double minSimilarity) {
            this.player = player;
            this.query = query;
            this.minSimilarity = minSimilarity;
        }

        @Override
        public void run() {
            try {
                final var offlinePlayers = Bukkit.getOfflinePlayers();

                Arrays.stream(offlinePlayers)
                    .filter(op -> op.getName() != null && !op.getUniqueId().equals(player.getUniqueId()))
                    .filter(op -> {
                        java.util.UUID uuid = op.getUniqueId();
                        return uuid != null && (uuid.version() == 3 || uuid.version() == 4 || uuid.version() == 0);
                    })
                    .map(op -> new org.enginehub.squirrelid.Profile(op.getUniqueId(), op.getName()))
                    .map(p -> new org.apache.commons.lang3.tuple.ImmutablePair<>(p, similarity(p.getName(), query)))
                    .filter(pair -> pair.right >= minSimilarity)
                    .sorted((a, b) -> b.right.compareTo(a.right))
                    .limit(maxResults)
                    .map(pair -> pair.left)
                    .forEach(resultQueue::add);
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("TransferSearchInventory load failed: " + e.getMessage());
            }
        }

        private double similarity(String a, String b) {
            return StringUtil.similarity(a, b);
        }
    }
}