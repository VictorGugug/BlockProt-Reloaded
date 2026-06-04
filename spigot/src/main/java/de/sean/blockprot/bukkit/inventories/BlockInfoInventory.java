/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BlockInfoInventory extends BlockProtInventory {
    public BlockInfoInventory() { super(true); }
    private final int maxSkulls = getSize() - InventoryConstants.lineLength;

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE:
                if (state.getBlock() != null) {
                    state.currentPageIndex = 0;
                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : new BlockLockInventory().fill(player, state.getBlock().getType(), handler)
                    );
                }
                break;
            case CYAN_STAINED_GLASS_PANE:
                if (state.getBlock() == null) break;
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;

                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : this.fill(player, handler)
                    );
                }
                break;
            case BLUE_STAINED_GLASS_PANE:
                if (state.getBlock() == null) break;
                final ItemStack lastFriendInInventory = inventory.getItem(maxSkulls - 1);
                if (lastFriendInInventory != null && lastFriendInInventory.getAmount() != 0) {
                    // There's an item in the last slot => The page is fully filled up, meaning we should go to the next page.
                    state.currentPageIndex++;

                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : this.fill(player, handler)
                    );
                }
                break;
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {

    }

    @NotNull
    public Inventory fill(Player player, BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        String owner = handler.getOwner();
        var friends = handler.getFriends();

        state.friendResultCache.clear();
        this.inventory.clear();

        // Exclude the owner from the friends list — the owner is already displayed in slot 0.
        var filteredFriends = friends.stream()
            .filter(f -> !f.getName().equals(owner))
            .toList();

        var pageOffset = maxSkulls * state.currentPageIndex;
        for (int i = 0; i < Math.min(filteredFriends.size() - pageOffset, maxSkulls); i++) {
            final var uuid = filteredFriends.get(pageOffset + i).getName();

            if (filteredFriends.get(pageOffset + i).doesRepresentPublic()) {
                this.setItemStack(InventoryConstants.lineLength + i, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC);
            } else {
                this.setItemStack(InventoryConstants.lineLength + i, Material.SKELETON_SKULL, uuid);
            }
            state.friendResultCache.add(UUID.fromString(uuid));
        }

        if (!owner.isEmpty()) {
            try {
                final var profile = BlockProt.getProfileService().findByUuid(UUID.fromString(owner));
                assert profile != null;
                final String ownerName = profile.getName() != null ? profile.getName() : owner.substring(0, 8);
                setPlayerSkull(0, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), ownerName));
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("Failed to update PlayerProfile: " + e.getMessage());
            }
        }
        setItemStack(
            1,
            Material.OAK_SIGN,
            handler.getName().replaceAll("[§&][0-9a-fk-orx]", "")
        );

        setItemStack(
            InventoryConstants.lineLength - 3,
            Material.CYAN_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__LAST_PAGE
        );
        setItemStack(
            InventoryConstants.lineLength - 2,
            Material.BLUE_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__NEXT_PAGE
        );

        RedstoneSettingsHandler redstoneSettingsHandler = handler.getRedstoneHandler();
        setEnchantedOptionItemStack(
            2,
            Material.REDSTONE,
            TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION,
            redstoneSettingsHandler.getCurrentProtection()
        );
        setEnchantedOptionItemStack(
            3,
            Material.HOPPER,
            TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION,
            redstoneSettingsHandler.getHopperProtection()
        );
        setEnchantedOptionItemStack(
            4,
            Material.PISTON,
            TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION,
            redstoneSettingsHandler.getPistonProtection()
        );
        setBackButton(InventoryConstants.lineLength - 1);

        Bukkit.getScheduler().runTaskAsynchronously(
            BlockProt.getInstance(),
            () -> {
                try {
                    final var profiles = BlockProt.getProfileService().findAllByUuid(state.friendResultCache);

                    // Offset by 1 when the public UUID is in the list (it occupies a slot already).
                    var offset = state.friendResultCache.contains(FriendSupportingHandler.publicUuid) ? 1 : 0;
                    int i = 0;
                    for (var profile : profiles) {
                        // Skip the public-access placeholder — it is already rendered as a named PLAYER_HEAD.
                        if (profile.getUniqueId().equals(FriendSupportingHandler.publicUuid)) continue;
                        if (i >= maxSkulls) break;
                        setPlayerSkull(InventoryConstants.lineLength + offset + i,
                            BlockProtInventory.createPlayerProfile(profile.getUniqueId(),
                                profile.getName() != null ? profile.getName() : profile.getUniqueId().toString().substring(0, 8)));
                        i++;
                    }
                } catch (Exception e) {
                    BlockProt.getInstance().getLogger().warning("Failed to update PlayerProfile: " + e.getMessage());
                }
            }
        );

        return inventory;
    }
}
