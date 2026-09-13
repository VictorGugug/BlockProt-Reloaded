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
import de.sean.blockprot.bukkit.util.PlayerLookup;
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
import org.enginehub.squirrelid.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory showing friend search results as player skulls.
 */
public class FriendSearchResultInventory extends BlockProtInventory {
    public FriendSearchResultInventory() { super(true); }

    private final int maxResults = getSize() - 1;

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
                if (meta != null && meta.getOwningPlayer() != null) {
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
    }

    private double compareStrings(String str1, String str2) {
        return StringUtil.similarity(str1, str2);
    }

    @Nullable
    public Inventory fill(@NotNull Player player, String searchQuery) {
        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        for (int i = 0; i < maxResults; i++) {
            this.setItemStack(i, Material.SKELETON_SKULL, TranslationKey.INVENTORIES__LOADING);
        }
        setBackButton();

        BlockProt.getFoliaLib().getScheduler().runAsync(task -> {
            double minimumSimilarity = BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage();
            List<Profile> results = new ArrayList<>();
            try {
                var filterStream = PlayerLookup.candidates(player.getUniqueId()).entrySet().stream()
                    .map(e -> new Profile(e.getKey(), e.getValue()))
                    .map(p -> new ImmutablePair<>(p, compareStrings(p.getName(), searchQuery)))
                    .filter(pair -> pair.right >= minimumSimilarity)
                    .sorted((a, b) -> b.right.compareTo(a.right))
                    .map(pair -> pair.left);

                if (state.friendSearchState == InventoryState.FriendSearchState.FRIEND_SEARCH && state.getBlock() != null) {
                    filterStream = filterStream
                            .filter(f -> PluginIntegration.filterFriendByUuidForAll(f.getUniqueId(), player, state.getBlock()));
                }

                results = filterStream.limit(maxResults).toList();
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("Failed to search and filter players during friend search: " + e.getMessage());
            }

            final List<Profile> finalResults = results;
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
                for (int i = 0; i < maxResults; i++) {
                    inventory.clear(i);
                }
                int idx = 0;
                for (var profile : finalResults) {
                    if (idx >= maxResults) break;
                    state.friendResultCache.add(profile.getUniqueId());
                    final String pName = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();
                    setPlayerSkullAsync(idx, player, profile.getUniqueId(), pName);
                    idx++;
                }
            });
        });

        return inventory;
    }
}