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
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.StringUtil;
import de.sean.blockprot.nbt.FriendModifyAction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Inventory showing friend search results as player skulls.
 */
public class FriendSearchResultInventory extends BlockProtInventory {
    public FriendSearchResultInventory() { super(true); }
    final ConcurrentLinkedQueue<Profile> resultQueue = new ConcurrentLinkedQueue<>();

    private final int maxResults = getSize() - 1;

    BukkitTask loadTask = null;
    BukkitTask updateTask = null;

    @Override
    int getSize() {
        return InventoryConstants.tripleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__RESULT);
    }



    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE ->
                closeAndOpen(
                    player,
                    new FriendManageInventory().fill(player)
                );
            case PLAYER_HEAD, SKELETON_SKULL -> {
                final var meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    final var id = meta.getOwningPlayer().getUniqueId();
                    modifyFriendsForAction(player, id, FriendModifyAction.ADD_FRIEND);
                    closeAndOpen(player, new FriendManageInventory().fill(player));

                    PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
                    settingsHandler.addPlayerToSearchHistory(id);
                }
            }
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
        if (loadTask != null)
            loadTask.cancel();
        if (updateTask != null)
            updateTask.cancel();
    }

    private double compareStrings(String str1, String str2) {
        return StringUtil.similarity(str1, str2);
    }

    @Nullable
    public Inventory fill(@NotNull Player player, String searchQuery) {
        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        updateTask = Bukkit.getScheduler().runTaskTimer(BlockProt.getInstance(), new ResultUpdateTask(state), 0, 1);
        loadTask = Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), new AsyncResultLoadTask(state, player, searchQuery));

        for (int i = 0; i < maxResults; i++) {
            this.setItemStack(i, Material.SKELETON_SKULL, TranslationKey.INVENTORIES__LOADING);
        }
        setBackButton();
        return inventory;
    }

    private class ResultUpdateTask implements Runnable {
        InventoryState state;
        int playersIndex = 0;

        ResultUpdateTask(@NotNull InventoryState state) {
            this.state = state;
        }

        @Override
        public void run() {
            final var scheduler = Bukkit.getScheduler();
            if (!scheduler.isQueued(loadTask.getTaskId()) && !scheduler.isCurrentlyRunning(loadTask.getTaskId()) && resultQueue.isEmpty()) {
                if (playersIndex == 0) {
                    for (int i = 0; i < maxResults; i++) {
                        inventory.clear(i);
                    }
                }
                loadTask.cancel();
                updateTask.cancel();
            }

            Profile profile;
            while ((profile = resultQueue.poll()) != null && playersIndex < maxResults) {
                if (playersIndex == 0) {
                    for (int i = 0; i < maxResults; i++) {
                        inventory.clear(i);
                    }
                }

                state.friendResultCache.add(profile.getUniqueId());

                final String pName = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();
                setPlayerSkull(playersIndex, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), pName));
                ++playersIndex;
            }

            if (playersIndex == maxResults) {
                loadTask.cancel();
                updateTask.cancel();
            }
        }
    }

    private class AsyncResultLoadTask implements Runnable {
        InventoryState state;
        Player player;
        String searchQuery;

        AsyncResultLoadTask(@NotNull InventoryState state, @NotNull Player player, @NotNull String searchQuery) {
            this.state = state;
            this.player = player;
            this.searchQuery = searchQuery;
        }

        @Override
        public void run() {
            double minimumSimilarity = BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage();
            final var offlinePlayers = Bukkit.getOfflinePlayers();

            try {
                var filterStream = Arrays.stream(offlinePlayers)
                    .filter(op -> op.getName() != null && !op.getUniqueId().equals(player.getUniqueId()))
                    .filter(op -> {
                        UUID uuid = op.getUniqueId();
                        return uuid != null && (uuid.version() == 3 || uuid.version() == 4 || uuid.version() == 0);
                    })
                    .map(op -> new org.enginehub.squirrelid.Profile(op.getUniqueId(), op.getName()))
                    .map(p -> new ImmutablePair<>(p, compareStrings(p.getName(), searchQuery)))
                    .filter(pair -> pair.right >= minimumSimilarity)
                    .sorted((a, b) -> b.right.compareTo(a.right))
                    .map(pair -> pair.left);

                if (state.friendSearchState == InventoryState.FriendSearchState.FRIEND_SEARCH && state.getBlock() != null) {
                    filterStream = filterStream
                            .filter(f -> PluginIntegration.filterFriendByUuidForAll(f.getUniqueId(), player, state.getBlock()));
                }

                filterStream.limit(maxResults).forEach(resultQueue::add);
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("Failed to search and filter players during friend search: " + e.getMessage());
            }
        }
    }
}