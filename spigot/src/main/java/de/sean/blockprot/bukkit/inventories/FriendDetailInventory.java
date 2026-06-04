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
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.nbt.FriendModifyAction;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The detail inventory for managing a single friend and their permissions.
 *
 * <p>Layout (single row = 9 slots):
 * <ul>
 *   <li>Slot 0: Player skull</li>
 *   <li>Slot 1: Remove friend (red glass)</li>
 *   <li>Slot 2: Timed access — CLOCK (only when viewing a block friend)</li>
 *   <li>Slot 8: Back button</li>
 * </ul>
 */
public final class FriendDetailInventory extends BlockProtInventory {
    public FriendDetailInventory() { super(true); }
    @Nullable
    private FriendHandler playerHandler = null;

    @Override
    public int getSize() {
        return InventoryConstants.lineLength;
    }

    @NotNull
    @Override
    public String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__EDIT);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> goBack(player, state);
            case RED_STAINED_GLASS_PANE -> {
                final var friend = state.currentFriend;
                assert friend != null;
                modifyFriendsForAction(player, friend, FriendModifyAction.REMOVE_FRIEND);
                this.playerHandler = null;
                closeAndOpen(player, new FriendManageInventory().fill(player));
            }
            case CLOCK -> { /* timed access removed */ }
            case ENDER_EYE -> { /* Feature removed */ }
            case PLAYER_HEAD -> { /* Don't do anything */ }
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @Nullable
    public Inventory fill(@NotNull Player player) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        final var uuid = state.currentFriend;
        if (uuid == null) return inventory;

        // Slot 0: player skull
        if (!uuid.equals(FriendSupportingHandler.publicUuid)) {
            try {
                final var profile = BlockProt.getProfileService().findByUuid(uuid);
                assert profile != null;
                final String pName = profile.getName() != null ? profile.getName() : uuid.toString();
                setPlayerSkull(0, BlockProtInventory.createPlayerProfile(profile.getUniqueId(), pName));
            } catch (Exception e) {
                BlockProt.getInstance().getLogger().warning("Failed to find PlayerProfile: " + uuid);
            }
        } else {
            setItemStack(0, Material.PLAYER_HEAD,
                TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC,
                List.of(Translator.get(TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC_DESC)));
        }

        // Slot 1: remove button
        setItemStack(1, Material.RED_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__FRIENDS__REMOVE);


        // Resolve friend handler
        final @Nullable FriendSupportingHandler<NBTCompound> handler =
            getFriendSupportingHandler(state.friendSearchState, player, state.getBlock());
        if (handler == null) return null;

        final Optional<FriendHandler> friendHandler = handler.getFriend(uuid.toString());
        if (friendHandler.isEmpty()) {
            BlockProt.getInstance().getLogger().warning(
                "Tried to open a " + this.getClass().getSimpleName() + " with an unknown player.");
            return null;
        }
        playerHandler = friendHandler.get();

        setBackButton();
        return inventory;
    }
}
