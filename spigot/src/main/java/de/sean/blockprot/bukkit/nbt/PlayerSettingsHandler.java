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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages player settings, default friends, history, and administrative roles.
 */
public final class PlayerSettingsHandler extends FriendSupportingHandler<NBTCompound> {
    private static final String ROOT_KEY = "blockprot";
    private static final String PREFERENCES_KEY = "preferences";
    private static final String FRIENDS_KEY = "friends";
    private static final String HISTORY_KEY = "history";
    private static final String ADMIN_KEY = "admin";
    private static final String MIGRATION_DONE_FLAG = "v3_migrated";

    private static final String LEGACY_LOCK_ON_PLACE = "splugin_lock_on_place";
    private static final String LEGACY_DEFAULT_FRIENDS = "blockprot_default_friends";
    private static final String LEGACY_SEARCH_HISTORY = "blockprot_player_search_history";
    private static final String LEGACY_MENU_INTERACTED = "blockprot_player_has_interacted_with_menu";
    private static final String LEGACY_PREFER_DIALOGS = "blockprot_prefer_dialogs";
    private static final String LEGACY_PREFER_BEDROCK_FORMS = "blockprot_prefer_bedrock_forms";
    private static final String LEGACY_NOTIFICATIONS_ENABLED = "blockprot_notifications_enabled";
    private static final String LEGACY_COLORBLIND_MODE = "blockprot_colorblind_mode";
    private static final String LEGACY_V2_MIGRATED = "blockprot_v2_migrated";

    private static final int MAX_HISTORY_SIZE = InventoryConstants.tripleLine - 2;

    public final Player player;
    public final NBTCompound rootContainer;

    @SuppressWarnings("deprecation")
    public PlayerSettingsHandler(@NotNull final Player player) {
        super(FRIENDS_KEY);
        this.player = player;
        this.rootContainer = new NBTEntity((org.bukkit.entity.Entity) player).getPersistentDataContainer();
        this.container = this.rootContainer.getOrCreateCompound(ROOT_KEY);
        migrateIfNeeded();
    }

    private void migrateIfNeeded() {
        if (this.container.hasTag(MIGRATION_DONE_FLAG)) return;

        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        if (rootContainer.hasTag(LEGACY_LOCK_ON_PLACE)) {
            prefs.setBoolean("lock_on_place", rootContainer.getBoolean(LEGACY_LOCK_ON_PLACE));
            rootContainer.removeKey(LEGACY_LOCK_ON_PLACE);
        }
        if (rootContainer.hasTag(LEGACY_PREFER_DIALOGS)) {
            prefs.setBoolean("prefer_dialogs", rootContainer.getBoolean(LEGACY_PREFER_DIALOGS));
            rootContainer.removeKey(LEGACY_PREFER_DIALOGS);
        }
        if (rootContainer.hasTag(LEGACY_PREFER_BEDROCK_FORMS)) {
            prefs.setBoolean("prefer_bedrock_forms", rootContainer.getBoolean(LEGACY_PREFER_BEDROCK_FORMS));
            rootContainer.removeKey(LEGACY_PREFER_BEDROCK_FORMS);
        }
        if (rootContainer.hasTag(LEGACY_NOTIFICATIONS_ENABLED)) {
            prefs.setBoolean("notifications_enabled", rootContainer.getBoolean(LEGACY_NOTIFICATIONS_ENABLED));
            rootContainer.removeKey(LEGACY_NOTIFICATIONS_ENABLED);
        }
        if (rootContainer.hasTag(LEGACY_COLORBLIND_MODE)) {
            prefs.setBoolean("colorblind_mode", rootContainer.getBoolean(LEGACY_COLORBLIND_MODE));
            rootContainer.removeKey(LEGACY_COLORBLIND_MODE);
        }
        if (rootContainer.hasTag(LEGACY_MENU_INTERACTED)) {
            prefs.setBoolean("menu_interacted", rootContainer.getBoolean(LEGACY_MENU_INTERACTED));
            rootContainer.removeKey(LEGACY_MENU_INTERACTED);
        }

        if (rootContainer.hasTag(LEGACY_SEARCH_HISTORY)) {
            NBTCompound history = this.container.getOrCreateCompound(HISTORY_KEY);
            history.setString("search", rootContainer.getString(LEGACY_SEARCH_HISTORY));
            rootContainer.removeKey(LEGACY_SEARCH_HISTORY);
        }

        if (rootContainer.hasTag(LEGACY_DEFAULT_FRIENDS)) {
            NBTCompound friendsComp = this.container.getOrCreateCompound(FRIENDS_KEY);
            if (rootContainer.getType(LEGACY_DEFAULT_FRIENDS) == NBTType.NBTTagString) {
                final List<String> originalList = BlockProtUtil.parseStringList(rootContainer.getString(LEGACY_DEFAULT_FRIENDS));
                originalList.forEach(this::addFriend);
            } else if (rootContainer.getType(LEGACY_DEFAULT_FRIENDS) == NBTType.NBTTagCompound) {
                NBTCompound oldFriends = rootContainer.getCompound(LEGACY_DEFAULT_FRIENDS);
                if (oldFriends != null) {
                    friendsComp.mergeCompound(oldFriends);
                }
            }
            rootContainer.removeKey(LEGACY_DEFAULT_FRIENDS);
        }

        if (rootContainer.hasTag(LEGACY_V2_MIGRATED)) {
            rootContainer.removeKey(LEGACY_V2_MIGRATED);
        }

        this.container.setBoolean(MIGRATION_DONE_FLAG, true);
    }

    public boolean getLockOnPlace() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        if (!prefs.hasTag("lock_on_place"))
            return BlockProt.getDefaultConfig().lockOnPlaceByDefault();
        return prefs.getBoolean("lock_on_place");
    }

    public void setLockOnPlace(final boolean lockOnPlace) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("lock_on_place", lockOnPlace);
    }

    @Override
    protected void preFriendReadCallback() {
        migrateIfNeeded();
    }

    public List<String> getSearchHistory() {
        NBTCompound history = this.container.getOrCreateCompound(HISTORY_KEY);
        if (!history.hasTag("search")) return new ArrayList<>();
        return BlockProtUtil.parseStringList(history.getString("search"));
    }

    public void clearSearchHistory() {
        NBTCompound history = this.container.getOrCreateCompound(HISTORY_KEY);
        if (history.hasTag("search")) {
            history.removeKey("search");
        }
    }

    public void addPlayerToSearchHistory(@NotNull final UUID player) {
        List<String> history = getSearchHistory();
        if (!history.contains(player.toString())) {
            if (history.size() == MAX_HISTORY_SIZE) {
                history.remove(0);
            }
            history.add(player.toString());
            this.container.getOrCreateCompound(HISTORY_KEY).setString("search", history.toString());
        }
    }

    public boolean hasPlayerInteractedWithMenu() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        return prefs.hasTag("menu_interacted") && prefs.getBoolean("menu_interacted");
    }

    public void setHasPlayerInteractedWithMenu(boolean bool) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("menu_interacted", bool);
    }

    @Override
    public void addFriend(@NotNull final String friend) {
        super.addFriend(friend);
        try {
            HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
            if (hybridDatabase != null) {
                hybridDatabase.addGlobalTrust(player.getUniqueId(), UUID.fromString(friend));
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public void removeFriend(@NotNull final String friend) {
        super.removeFriend(friend);
        try {
            HybridDatabase hybridDatabase = BlockProt.getHybridDatabase();
            if (hybridDatabase != null) {
                hybridDatabase.removeGlobalTrust(player.getUniqueId(), UUID.fromString(friend));
            }
        } catch (IllegalArgumentException ignored) {}
    }

    public boolean getNotificationsEnabled() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        if (!prefs.hasTag("notifications_enabled"))
            return BlockProt.getDefaultConfig().isOwnerNotificationsEnabled();
        return prefs.getBoolean("notifications_enabled");
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("notifications_enabled", enabled);
    }

    public boolean getPreferDialogs() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        if (!prefs.hasTag("prefer_dialogs"))
            return BlockProt.getDefaultConfig().isDialogsEnabled();
        return prefs.getBoolean("prefer_dialogs");
    }

    public void setPreferDialogs(boolean preferDialogs) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("prefer_dialogs", preferDialogs);
    }

    public boolean getPreferBedrockForms() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        if (!prefs.hasTag("prefer_bedrock_forms"))
            return true;
        return prefs.getBoolean("prefer_bedrock_forms");
    }

    public void setPreferBedrockForms(boolean preferBedrockForms) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("prefer_bedrock_forms", preferBedrockForms);
    }

    public boolean getColorblindMode() {
        NBTCompound prefs = this.container.getOrCreateCompound(PREFERENCES_KEY);
        return prefs.hasTag("colorblind_mode") && prefs.getBoolean("colorblind_mode");
    }

    public void setColorblindMode(boolean colorblindMode) {
        this.container.getOrCreateCompound(PREFERENCES_KEY).setBoolean("colorblind_mode", colorblindMode);
    }

    @NotNull
    public String getAdminTier() {
        NBTCompound admin = this.container.getOrCreateCompound(ADMIN_KEY);
        return admin.hasTag("tier") ? admin.getString("tier") : "";
    }

    public void setAdminTier(@NotNull String tier) {
        this.container.getOrCreateCompound(ADMIN_KEY).setString("tier", tier);
    }

    public void clearAdminTier() {
        NBTCompound admin = this.container.getOrCreateCompound(ADMIN_KEY);
        admin.removeKey("tier");
        admin.removeKey("custom_flags");
    }

    @NotNull
    public String getAdminCustomFlags() {
        NBTCompound admin = this.container.getOrCreateCompound(ADMIN_KEY);
        return admin.hasTag("custom_flags") ? admin.getString("custom_flags") : "";
    }

    public void setAdminCustomFlags(@NotNull String flags) {
        this.container.getOrCreateCompound(ADMIN_KEY).setString("custom_flags", flags);
    }

    @Override
    public void mergeHandler(@NotNull NBTHandler<?> handler) {
        if (!(handler instanceof final PlayerSettingsHandler playerSettingsHandler)) return;
        this.setLockOnPlace(playerSettingsHandler.getLockOnPlace());
        playerSettingsHandler.getFriends().forEach(this::addFriend);
    }
}