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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.util.PlayerLookup;
import de.sean.blockprot.bukkit.util.StringUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
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
 * Search results for adding a friend to a protected entity (pet/villager).
 * Mirrors {@link FriendSearchResultInventory} and {@link TransferSearchInventory}:
 * shows real online+offline players as skulls via {@link PlayerLookup} so the
 * player picks who they meant instead of the plugin silently auto-adding
 * whatever name scored highest.
 */
public final class EntityFriendSearchResultInventory extends BlockProtInventory {

    private Entity entity;
    private EntityNBTHandler handler;

    private final int maxResults = getSize() - 1;

    public EntityFriendSearchResultInventory() { super(true); }

    @Override
    int getSize() { return InventoryConstants.tripleLine; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__RESULT);
    }

    @Nullable
    public Inventory fill(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler, @NotNull String searchQuery) {
        this.entity = entity;
        this.handler = handler;

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        for (int i = 0; i < maxResults; i++) {
            setItemStack(i, Material.SKELETON_SKULL, TranslationKey.INVENTORIES__SEARCHING);
        }
        setBackButton();

        BlockProt.getFoliaLib().getScheduler().runAsync(task -> {
            double minSimilarity = BlockProt.getDefaultConfig().getFriendSearchSimilarityPercentage();
            List<Profile> results = new ArrayList<>();
            try {
                results = PlayerLookup.candidates(player.getUniqueId()).entrySet().stream()
                    .map(e -> new Profile(e.getKey(), e.getValue()))
                    .map(p -> new ImmutablePair<>(p, StringUtil.similarity(p.getName(), searchQuery)))
                    .filter(pair -> pair.right >= minSimilarity)
                    .sorted((a, b) -> b.right.compareTo(a.right))
                    .limit(maxResults)
                    .map(pair -> pair.left)
                    .toList();
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("EntityFriendSearchResultInventory load failed: " + e.getMessage());
            }

            final List<Profile> finalResults = results;
            BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
                for (int i = 0; i < maxResults; i++) {
                    inventory.clear(i);
                }
                int idx = 0;
                for (var profile : finalResults) {
                    if (idx >= maxResults) break;
                    final String name = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();
                    setPlayerSkull(idx, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), name));
                    idx++;
                }
            });
        });

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        event.setCancelled(true);

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> closeAndOpen(player, new EntityFriendManageInventory().fill(player, entity, handler));
            case PLAYER_HEAD, SKELETON_SKULL -> {
                if (!(item.getItemMeta() instanceof SkullMeta meta)) return;
                if (meta.getOwningPlayer() == null) return;
                handler.addFriend(meta.getOwningPlayer().getUniqueId().toString());
                closeAndOpen(player, new EntityFriendManageInventory().fill(player, entity, handler));
            }
            default -> closeAndOpen(player, null);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
    }
}
