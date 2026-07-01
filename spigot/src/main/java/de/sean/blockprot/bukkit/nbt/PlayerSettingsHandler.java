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

package de.sean.blockprot.bukkit.nbt;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.inventories.InventoryConstants;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.util.BlockProtUtil;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import de.tr7zw.changeme.nbtapi.NBTType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A simple handler to get a player's BlockProt settings.
 *
 * @since 0.2.3
 */
public final class PlayerSettingsHandler extends FriendSupportingHandler<NBTCompound> {
    static final String LOCK_ON_PLACE_ATTRIBUTE = "splugin_lock_on_place";

    static final String DEFAULT_FRIENDS_ATTRIBUTE = "blockprot_default_friends";

    static final String PLAYER_SEARCH_HISTORY = "blockprot_player_search_history";

    static final String PLAYER_HAS_INTERACTED_WITH_MENU = "blockprot_player_has_interacted_with_menu";

    /** Per-player toggle: receive access notifications. Defaults to server config value. */
    static final String NOTIFICATIONS_ENABLED_ATTRIBUTE = "blockprot_notifications_enabled";

    private static final int MAX_HISTORY_SIZE = InventoryConstants.tripleLine - 2;

    /**
     * The player that this settings handler is getting values
     * for.
     *
     * @since 0.2.3
     */
    public final Player player;

    /**
     * Create a new settings handler.
     *
     * @param player The player to get the settings for.
     * @since 0.2.3
     */
    @SuppressWarnings("deprecation")
    public PlayerSettingsHandler(@NotNull final Player player) {
        super(DEFAULT_FRIENDS_ATTRIBUTE);
        this.player = player;

        this.container = new NBTEntity((org.bukkit.entity.Entity) player).getPersistentDataContainer();
    }

    /**
     * Check if the player wants their blocks to be locked when
     * placed.
     *
     * @return Will return the default setting from the config, or the
     * value the player has set it to.
     * @since 0.2.3
     */
    public boolean getLockOnPlace() {
        // We will default to 'true'. The default value for a boolean is 'false',
        // which would also be the default value for NBTCompound#getBoolean
        if (!container.hasTag(LOCK_ON_PLACE_ATTRIBUTE))
            return BlockProt.getDefaultConfig().lockOnPlaceByDefault();
        return container.getBoolean(LOCK_ON_PLACE_ATTRIBUTE);
    }

    public void setLockOnPlace(final boolean lockOnPlace) {
        container.setBoolean(LOCK_ON_PLACE_ATTRIBUTE, lockOnPlace);
    }

    /**
     * We are switching to a similar system that {@link BlockNBTHandler}
     * uses. To retain compatibility and upgradability with older versions
     * we will try to remap the previous data to the new data structure.
     * 
     * <p>A migration flag is written to NBT after the first successful migration so that
     * subsequent reads skip the compound-format check entirely. This avoids an unnecessary
     * {@code hasTag + getType} call on every block interaction for already-migrated players.
     *
     * @since 1.0.0
     */
    private static final String MIGRATION_DONE_FLAG = "blockprot_v2_migrated";

    @Override
    protected void preFriendReadCallback() {
        // Fast-path: migration already done for this player — skip all checks.
        if (container.hasTag(MIGRATION_DONE_FLAG)) return;

        if (container.hasTag(DEFAULT_FRIENDS_ATTRIBUTE)
            && container.getType(DEFAULT_FRIENDS_ATTRIBUTE) == NBTType.NBTTagString) {
            final List<String> originalList = BlockProtUtil
                .parseStringList(container.getString(DEFAULT_FRIENDS_ATTRIBUTE));
            
            container.removeKey(DEFAULT_FRIENDS_ATTRIBUTE); // We have to remove the string to then add the compound.
            container.addCompound(DEFAULT_FRIENDS_ATTRIBUTE);
            originalList.forEach(this::addFriend);
        }

        // Mark this player as migrated so future reads skip the check entirely.
        container.setBoolean(MIGRATION_DONE_FLAG, true);
    }

    /**
     * Get the current search history for this player.
     * 
     * @return A list of UUIDs for each player this player has
     * searched for.
     */
    public List<String> getSearchHistory() {
        if (!container.hasTag(PLAYER_SEARCH_HISTORY)) return new ArrayList<>();
        else {
            return BlockProtUtil
                .parseStringList(container.getString(PLAYER_SEARCH_HISTORY));
        }
    }

    public void clearSearchHistory() {
        if (container.hasTag(PLAYER_SEARCH_HISTORY)) {
            container.removeKey(PLAYER_SEARCH_HISTORY);
        }
    }

    @Deprecated
    public void addPlayerToSearchHistory(@NotNull final OfflinePlayer player) {
        this.addPlayerToSearchHistory(player.getUniqueId());
    }

    @Deprecated
    public void addPlayerToSearchHistory(@NotNull final String playerUuid) {
        this.addPlayerToSearchHistory(UUID.fromString(playerUuid));
    }

    /**
     * Add a player to the search history.
     *
     * @param player The player to add.
     * @since 1.1.16
     */
    public void addPlayerToSearchHistory(@NotNull final UUID player) {
        List<String> history = getSearchHistory();
        if (!history.contains(player.toString())) {
            // We want the list to not be bigger than MAX_HISTORY_SIZE,
            // therefore we remove the first entry if we would exceed that size.
            if (history.size() == MAX_HISTORY_SIZE) {
                history.remove(0);
            }
            history.add(player.toString());
            container.setString(PLAYER_SEARCH_HISTORY, history.toString());
        }
    }

    public boolean hasPlayerInteractedWithMenu() {
        if (!container.hasTag(PLAYER_HAS_INTERACTED_WITH_MENU)) {
            return false;
        } else {
            return container.getBoolean(PLAYER_HAS_INTERACTED_WITH_MENU);
        }
    }

    public void setHasPlayerInteractedWithMenu(boolean bool) {
        container.setBoolean(PLAYER_HAS_INTERACTED_WITH_MENU, bool);
    }

    @Override
    public void addFriend(@NotNull final String friend) {
        super.addFriend(friend);
        try {
            HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
            if (hybridDatabase != null) {
                hybridDatabase.addGlobalTrust(player.getUniqueId(), UUID.fromString(friend));
            }
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    public void removeFriend(@NotNull final String friend) {
        super.removeFriend(friend);
        try {
            HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
            if (hybridDatabase != null) {
                hybridDatabase.removeGlobalTrust(player.getUniqueId(), UUID.fromString(friend));
            }
        } catch (IllegalArgumentException ignored) { }
    }

    /**
     * Whether this player wants to receive access notifications when someone
     * opens or interacts with their protected blocks.
     * Defaults to the server-wide config value when the player has no preference stored.
     */
    public boolean getNotificationsEnabled() {
        if (!container.hasTag(NOTIFICATIONS_ENABLED_ATTRIBUTE))
            return BlockProt.getDefaultConfig().isOwnerNotificationsEnabled();
        return container.getBoolean(NOTIFICATIONS_ENABLED_ATTRIBUTE);
    }

    /** Persists the player's notification preference. */
    public void setNotificationsEnabled(boolean enabled) {
        container.setBoolean(NOTIFICATIONS_ENABLED_ATTRIBUTE, enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mergeHandler(@NotNull NBTHandler<?> handler) {
        if (!(handler instanceof final PlayerSettingsHandler playerSettingsHandler)) return;
        this.setLockOnPlace(playerSettingsHandler.getLockOnPlace());
        this.container.setString(DEFAULT_FRIENDS_ATTRIBUTE,
            playerSettingsHandler.container.getString(DEFAULT_FRIENDS_ATTRIBUTE));
    }
}