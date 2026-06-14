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

import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Storage for the current state and location of each player's
 * interactions with this plugin's menus.
 *
 * @since 0.1.9
 */
public final class InventoryState {
    /**
     * HashMap containing the current InventoryState of each player.
     * The keys are the String representation of the player's UUID.
     *
     * @since 0.4.7
     */
    private static final HashMap<String, InventoryState> players = new HashMap<>();

    /**
     * A local cache of offline players for this state.
     *
     * @since 1.1.16
     */
    @NotNull
    public final ArrayList<UUID> friendResultCache = new ArrayList<>();

    @Nullable
    private final Block block;

    /**
     * UUID of the pet entity currently open in {@link de.sean.blockprot.bukkit.inventories.PetSettingsInventory}.
     * Null when the player is not in a pet-settings menu.
     *
     * @since SP26-ZV
     */
    @Nullable
    private UUID petEntityId = null;

    /**
     * UUID of the entity (item frame, chest boat, minecart) being edited through
     * an entity protection menu. Null when the current menu is for a block.
     *
     * @since SP26-ZV.BPR
     */
    @Nullable
    public UUID entityUUID = null;

    /**
     * The current state of the friend search mechanism.
     *
     * @since 0.4.7
     */
    @NotNull
    public FriendSearchState friendSearchState = FriendSearchState.FRIEND_SEARCH;

    /**
     * The current index of the page, if the inventory has any multi-page capabilities.
     *
     * @since 1.0.0
     */
    public int currentPageIndex = 0;

    /**
     * The current friend we currently want to modify with {@link FriendDetailInventory}.
     *
     * @since 1.1.16
     */
    @Nullable
    public UUID currentFriend = null;

    /**
     * The current cached menu permissions for this player.
     *
     * @since 1.0.0
     */
    @NotNull
    public Set<BlockAccessMenuEvent.MenuPermission> menuPermissions = new HashSet<>();

    public boolean remoteLockPendingConfirm = false;

    /**
     * Tracks which menu opened the current one, so the back button can return correctly.
     */
    @NotNull
    public MenuOrigin origin = MenuOrigin.NONE;

    public InventoryState(@Nullable Block block) {
        this.block = block;
    }

    // ── Static accessors ──────────────────────────────────────────────────────

    public static void set(String player, InventoryState state) {
        players.put(player, state);
    }

    public static void set(UUID player, InventoryState state) {
        players.put(player.toString(), state);
    }

    public static InventoryState get(String player) {
        return players.get(player);
    }

    public static InventoryState get(UUID player) {
        return players.get(player.toString());
    }

    /**
     * Returns the existing state for the player, or creates a new one (with no block)
     * if none exists. This is used by pet-menu open paths where there is no block.
     *
     * @since SP26-ZV
     */
    @NotNull
    public static InventoryState getOrCreate(@NotNull UUID player) {
        InventoryState existing = players.get(player.toString());
        if (existing != null) return existing;
        InventoryState fresh = new InventoryState(null);
        players.put(player.toString(), fresh);
        return fresh;
    }

    public static void remove(String player) {
        players.remove(player);
    }

    public static void remove(UUID player) {
        players.remove(player.toString());
    }

    // ── Instance accessors ────────────────────────────────────────────────────

    @Nullable
    public Block getBlock() {
        return this.block;
    }

    /**
     * Returns the UUID of the pet entity currently being configured,
     * or null when not in a pet settings menu.
     *
     * @since SP26-ZV
     */
    @Nullable
    public UUID getPetEntityId() {
        return petEntityId;
    }

    /**
     * Sets the pet entity UUID for this state. Used by
     * {@link de.sean.blockprot.bukkit.inventories.PetSettingsInventory} to track
     * which entity is being edited across click events.
     *
     * @since SP26-ZV
     */
    public void setPetEntityId(@Nullable UUID id) {
        this.petEntityId = id;
    }

    // ── Enum ──────────────────────────────────────────────────────────────────

    /**
     * The current search state of the friend menu.
     *
     * @since 0.1.9
     */
    public enum FriendSearchState {
        /** This search is currently for a single block. @since 0.1.9 */
        FRIEND_SEARCH,
        /** This search is currently for the default friends of a player. @since 0.1.9 */
        DEFAULT_FRIEND_SEARCH,
    }

    /** Identifies which menu opened the current one, driving back-button behaviour. */
    public enum MenuOrigin {
        NONE,
        BLOCK_LOCK,
        USER_MENU,
        ADMIN_MENU,
        FRIEND_MANAGE,
        STATISTICS,
        USER_SETTINGS,
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Block block = null;
        private MenuOrigin origin = MenuOrigin.NONE;
        private FriendSearchState friendSearchState = FriendSearchState.FRIEND_SEARCH;

        private Builder() {}

        public Builder block(@org.jetbrains.annotations.Nullable Block block) { this.block = block; return this; }
        public Builder origin(@NotNull MenuOrigin origin) { this.origin = origin; return this; }
        public Builder friendSearchState(@NotNull FriendSearchState state) { this.friendSearchState = state; return this; }

        @NotNull
        public InventoryState build() {
            InventoryState s = new InventoryState(block);
            s.origin = origin;
            s.friendSearchState = friendSearchState;
            return s;
        }
    }
}
