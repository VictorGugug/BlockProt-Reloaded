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

import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.events.BlockProtUnlockEvent;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.inventories.BlockLockInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.nbt.LockReturnValue;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BlockProt's class for external API methods.
 *
 * <p>Obtain it with {@link #getInstance()} once BlockProt is enabled. Methods that read or write
 * a block or an entity must run on the thread that owns it: the main thread on Paper, or the owning
 * region thread on Folia.
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

    /**
     * @return true if the block has an owner.
     * @since 1.3.8
     */
    public boolean isProtected(@NotNull final Block block) {
        return new BlockNBTHandler(block).isProtected();
    }

    /**
     * @return the owner of the block, or null if the block is not protected.
     * @since 1.3.8
     */
    @Nullable
    public UUID getOwner(@NotNull final Block block) {
        return parseUuid(new BlockNBTHandler(block).getOwner());
    }

    /**
     * @return true if the player may open or use the block: it is unprotected, owned by the player,
     *     shared with the player as a friend, or made public.
     * @since 1.3.8
     */
    public boolean canAccess(@NotNull final Block block, @NotNull final UUID player) {
        BlockNBTHandler handler = new BlockNBTHandler(block);
        return handler.isNotProtected() || handler.canAccess(player.toString());
    }

    /**
     * @return the players added as friends of the block, excluding the public entry.
     * @since 1.3.8
     */
    @NotNull
    public List<UUID> getFriends(@NotNull final Block block) {
        return new BlockNBTHandler(block).getFriends().stream()
            .filter(friend -> !friend.doesRepresentPublic())
            .map(friend -> parseUuid(friend.getName()))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    /**
     * @return true if the block has been made public, so every player can access it.
     * @since 1.3.8
     */
    public boolean isPublic(@NotNull final Block block) {
        return new BlockNBTHandler(block).getFriends().stream().anyMatch(FriendHandler::doesRepresentPublic);
    }

    /**
     * @return true if the block type can be locked in its world with the current configuration.
     * @since 1.3.8
     */
    public boolean isLockable(@NotNull final Block block) {
        return BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld());
    }

    /**
     * @return true if the entity is protected by BlockProt's entity protection.
     * @since 1.3.8
     */
    public boolean isEntityProtected(@NotNull final Entity entity) {
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(entity);
        return handler != null && handler.isProtected();
    }

    /**
     * @return the owner of a protected entity, or null if the entity is not protected.
     * @since 1.3.8
     */
    @Nullable
    public UUID getEntityOwner(@NotNull final Entity entity) {
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(entity);
        return handler != null && handler.isProtected() ? handler.getOwner() : null;
    }

    @Nullable
    private static UUID parseUuid(@Nullable final String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        if (AdminTierManager.hasPermission(player, AdminAction.UNLOCK)) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO);
        } else if (!handler.isNotProtected() && AdminTierManager.hasPermission(player, AdminAction.INFO)) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.INFO);
        }

        Optional<FriendHandler> friend;
        if (handler.isOwner(playerUuid)) {
            event.addPermissions(
                    BlockAccessMenuEvent.MenuPermission.LOCK,
                    BlockAccessMenuEvent.MenuPermission.INFO,
                    BlockAccessMenuEvent.MenuPermission.MANAGER);
        } else if (handler.isNotProtected()) {
            event.addPermission(BlockAccessMenuEvent.MenuPermission.LOCK);
        } else if ((friend = handler.getFriend(playerUuid)).isPresent() && !friend.get().doesRepresentPublic()) {
            if (friend.get().canOpenMenu()) {
                event.addPermission(BlockAccessMenuEvent.MenuPermission.INFO);
            }
            if (friend.get().isManager()) {
                event.addPermission(BlockAccessMenuEvent.MenuPermission.MANAGER);
            }
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