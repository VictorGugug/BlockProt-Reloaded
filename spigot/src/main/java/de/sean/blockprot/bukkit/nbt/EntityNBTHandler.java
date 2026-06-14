/*
 * Copyright (C) 2025 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
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

import de.tr7zw.changeme.nbtapi.NBTEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NBT handler for entities that can be protected (item frames, chest boats,
 * minecarts with storage). Protection data is stored in the entity's
 * persistent NBT compound under the {@code blockprot} namespace so it
 * survives chunk reloads and server restarts.
 *
 * <p>The data layout inside the entity's NBT is:
 * <pre>
 *   blockprot_owner   — String UUID of the owner
 *   blockprot_friends — Compound of {uuid: {flags}} entries (same schema as blocks)
 * </pre>
 *
 * <p>Because entities do not expose a {@code PersistentDataContainer} in all
 * supported server versions through the NBT-API entity wrapper, we use the
 * {@link NBTEntity} accessor provided by NBT-API directly.
 */
public final class EntityNBTHandler {

    private static final String OWNER_KEY   = "blockprot_owner";
    private static final String FRIENDS_KEY = "blockprot_friends";
    /** Permission flags stored per friend entry. Mirrors FriendHandler flags. */
    private static final String FLAG_READ    = "read";
    private static final String FLAG_WRITE   = "write";
    private static final String FLAG_MANAGER = "manager";

    private final NBTEntity nbt;

    public EntityNBTHandler(@NotNull Entity entity) {
        this.nbt = new NBTEntity(entity);
    }

    // ── Owner ─────────────────────────────────────────────────────────────────

    @NotNull
    public String getOwner() {
        return nbt.hasTag(OWNER_KEY) ? nbt.getString(OWNER_KEY) : "";
    }

    public void setOwner(@NotNull String ownerUuid) {
        nbt.setString(OWNER_KEY, ownerUuid);
    }

    public boolean isOwner(@NotNull String playerUuid) {
        return getOwner().equals(playerUuid);
    }

    public boolean isProtected() {
        return !getOwner().isEmpty();
    }

    public void clearOwner() {
        nbt.removeKey(OWNER_KEY);
        nbt.removeKey(FRIENDS_KEY);
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code playerUuid} is the owner or a friend with at least READ access.
     */
    public boolean canAccess(@NotNull String playerUuid) {
        if (!isProtected()) return true;
        if (isOwner(playerUuid)) return true;
        var compound = nbt.getCompound(FRIENDS_KEY);
        if (compound == null || !compound.hasTag(playerUuid)) return false;
        var entry = compound.getCompound(playerUuid);
        return entry != null && entry.getBoolean(FLAG_READ);
    }

    /** Returns all friend UUID strings registered on this entity. */
    @NotNull
    public List<String> getFriendUuids() {
        var compound = nbt.getCompound(FRIENDS_KEY);
        if (compound == null) return List.of();
        return new ArrayList<>(compound.getKeys());
    }

    public void addFriend(@NotNull String friendUuid) {
        var compound = nbt.getOrCreateCompound(FRIENDS_KEY);
        var entry    = compound.getOrCreateCompound(friendUuid);
        entry.setBoolean(FLAG_READ,    true);
        entry.setBoolean(FLAG_WRITE,   false);
        entry.setBoolean(FLAG_MANAGER, false);
    }

    public void removeFriend(@NotNull String friendUuid) {
        var compound = nbt.getCompound(FRIENDS_KEY);
        if (compound != null) compound.removeKey(friendUuid);
    }

    public boolean hasFriend(@NotNull String friendUuid) {
        var compound = nbt.getCompound(FRIENDS_KEY);
        return compound != null && compound.hasTag(friendUuid);
    }

    // ── Friend permission helpers ─────────────────────────────────────────────

    @Nullable
    public Boolean getFriendRead(@NotNull String friendUuid) {
        var compound = nbt.getCompound(FRIENDS_KEY);
        if (compound == null || !compound.hasTag(friendUuid)) return null;
        var entry = compound.getCompound(friendUuid);
        return entry != null ? entry.getBoolean(FLAG_READ) : null;
    }

    public void setFriendPermissions(@NotNull String friendUuid, boolean read, boolean write, boolean manager) {
        var compound = nbt.getOrCreateCompound(FRIENDS_KEY);
        var entry    = compound.getOrCreateCompound(friendUuid);
        entry.setBoolean(FLAG_READ,    read);
        entry.setBoolean(FLAG_WRITE,   write);
        entry.setBoolean(FLAG_MANAGER, manager);
    }
}
