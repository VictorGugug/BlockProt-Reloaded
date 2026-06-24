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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Friend management menu for protected entities.
 *
 * <p>Mirrors {@link FriendManageInventory} but operates against
 * {@link EntityNBTHandler} instead of block NBT. Supports adding,
 * removing, and toggling the manager flag for each friend.
 */
public final class EntityFriendManageInventory extends BlockProtInventory {

    private Entity entity;
    private EntityNBTHandler handler;

    private final int maxSkulls = getSize() - InventoryConstants.lineLength;

    public EntityFriendManageInventory() { super(true); }

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> {
                state.currentPageIndex = 0;
                if (entity != null && handler != null) {
                    closeAndOpen(player, new BlockLockInventory().fillForEntity(player, entity, handler));
                } else {
                    closeAndOpen(player, null);
                }
            }
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    closeAndOpen(player, fill(player, entity, handler));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                ItemStack last = event.getInventory().getItem(maxSkulls - 1);
                if (last != null && last.getAmount() != 0) {
                    state.currentPageIndex++;
                    closeAndOpen(player, fill(player, entity, handler));
                }
            }
            case MAP -> openAddFriendInput(player);
            case SKELETON_SKULL, PLAYER_HEAD -> {
                int index = findItemIndex(item);
                if (index >= 0 && index < state.friendResultCache.size()) {
                    UUID friendUuid = state.friendResultCache.get(index);
                    EntityNBTHandler.FriendEntry entry = handler.getFriendEntry(friendUuid.toString());
                    if (entry != null) {
                        handler.setFriendManager(friendUuid.toString(), !entry.manager());
                    } else {
                        handler.removeFriend(friendUuid.toString());
                    }
                    closeAndOpen(player, fill(player, entity, handler));
                }
            }
            default -> {}
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @Nullable
    public Inventory fill(@NotNull Player player, @Nullable Entity entity, @Nullable EntityNBTHandler handler) {
        this.entity  = entity;
        this.handler = handler;

        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null || handler == null) return inventory;

        inventory.clear();
        state.friendResultCache.clear();

        List<String> friendUuids = handler.getFriendUuids();
        int pageOffset = maxSkulls * state.currentPageIndex;

        for (int i = 0; i < Math.min(friendUuids.size() - pageOffset, maxSkulls); i++) {
            String uuid = friendUuids.get(pageOffset + i);
            EntityNBTHandler.FriendEntry entry = handler.getFriendEntry(uuid);
            boolean isManager = entry != null && entry.manager();

            if (isManager) {
                setEnchantedItemStack(i, Material.PLAYER_HEAD,
                    TranslationKey.INVENTORIES__FRIENDS__PERMISSIONS, true);
            } else {
                setItemStack(i, Material.SKELETON_SKULL, uuid);
            }
            state.friendResultCache.add(UUID.fromString(uuid));
        }

        setItemStack(maxSkulls,     Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
        setItemStack(maxSkulls + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        setItemStack(getSize() - 2, Material.MAP,  TranslationKey.INVENTORIES__FRIENDS__SEARCH);
        setBackButton();

        final List<UUID> uuidSnapshot = new ArrayList<>(state.friendResultCache);
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            try {
                var profiles = BlockProt.getProfileService().findAllByUuid(uuidSnapshot);
                for (var profile : profiles) {
                    int idx = uuidSnapshot.indexOf(profile.getUniqueId());
                    if (idx < 0) continue;
                    String name = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();
                    setPlayerSkull(idx, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), name));
                }
            } catch (Exception ignored) {}
        });

        return inventory;
    }

    private void openAddFriendInput(@NotNull Player player) {
        player.closeInventory();
        ChatInput.open(player, BlockProt.getInstance(), text -> {
            if (text == null || text.isBlank()) return;
            Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                try {
                    var profile = BlockProt.getProfileService().findByName(text);
                    if (profile == null) {
                        sendActionBar(player, Translator.get(TranslationKey.MESSAGES__FRIEND_CANT_BE_REMOVED));
                        return;
                    }
                    Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                        handler.addFriend(profile.getUniqueId().toString());
                        Inventory inv = fill(player, entity, handler);
                        if (inv != null) player.openInventory(inv);
                    });
                } catch (Exception ignored) {}
            });
        });
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        player.sendActionBar(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(text));
    }
}
