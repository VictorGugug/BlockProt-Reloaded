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

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.events.BlockProtUnlockEvent;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.inventories.BlockLockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.nbt.LockReturnValue;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * BlockProt's class for external API methods.
 *
 * @author spnda
 * @since 0.4.7
 */
public final class BlockProtAPI {
    @Nullable
    static BlockProtAPI instance;

    private final BlockProt blockProt;

    BlockProtAPI(BlockProt blockProt) {
        this.blockProt = blockProt;
        instance = this;
    }

    @Nullable
    public static BlockProtAPI getInstance() {
        return instance;
    }

    public void registerIntegration(@NotNull final PluginIntegration integration) {
        this.blockProt.registerIntegration(integration);
    }

    @NotNull
    public List<PluginIntegration> getIntegrations() {
        return this.blockProt.getIntegrations();
    }

    @NotNull
    public BlockNBTHandler getBlockHandler(@NotNull final Block block) {
        return new BlockNBTHandler(block);
    }

    @NotNull
    public PlayerSettingsHandler getPlayerSettings(@NotNull final Player player) {
        return new PlayerSettingsHandler(player);
    }

    @NotNull
    public LockReturnValue lockBlock(@NotNull final Block block, @NotNull final Player player) {
        return new BlockNBTHandler(block).lockBlock(player, BlockProtLockEvent.Cause.API);
    }

    public boolean unlockBlock(@NotNull final Block block, @NotNull final Player player) {
        BlockNBTHandler handler = new BlockNBTHandler(block);
        if (handler.isNotProtected()) return true;

        BlockProtUnlockEvent event = new BlockProtUnlockEvent(block, player, BlockProtUnlockEvent.Cause.API);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        StatHandler.removeContainer(player, block);
        handler.clear();
        handler.applyToOtherContainer();
        return true;
    }

    @Nullable
    public Inventory getLockInventoryForBlock(@NotNull final Block block, @NotNull final Player player) {
        final BlockAccessMenuEvent event = new BlockAccessMenuEvent(block, player);
        final String playerUuid = player.getUniqueId().toString();

        final BlockNBTHandler handler = new BlockNBTHandler(block);
        if (player.isOp() || player.hasPermission(Permissions.USER_ADMIN.key())) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO);
        }

        Optional<FriendHandler> friend;
        if (handler.isOwner(playerUuid)) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO,
                    BlockAccessMenuEvent.MenuPermission.MANAGER);
        } else if (handler.isNotProtected()) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.LOCK);
        } else if ((friend = handler.getFriend(playerUuid)).isPresent() && friend.get().isManager()) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.MANAGER);
        }

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled() || event.getPermissions().isEmpty()) {
            return null;
        }

        InventoryState state = new InventoryState(block);
        state.menuPermissions = event.getPermissions();
        state.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
        InventoryState.set(player.getUniqueId(), state);

        return new BlockLockInventory().fill(player, block.getType(), handler);
    }
}