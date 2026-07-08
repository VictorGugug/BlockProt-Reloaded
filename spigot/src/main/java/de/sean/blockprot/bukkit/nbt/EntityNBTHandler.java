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
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistence handler for protectable entities (item frames, chest boats,
 * storage / hopper minecarts).
 *
 * <p>All data is stored in the entity's Bukkit {@link PersistentDataContainer},
 * which survives chunk unloads, server restarts and entity serialisation. This
 * replaces the earlier {@code NBTEntity}-based approach, which wrote to the raw
 * vanilla NBT compound (non-persistent for custom tags in modern Paper builds).
 *
 * <p>Data layout (NamespacedKey namespace = {@code "blockprot"}):
 * <pre>
 *   blockprot:owner             : String UUID of the owner
 *   blockprot:hopper_protection : byte  1 = blocked (default), 0 = allowed
 *   blockprot:friends           : String, semicolon-delimited friend list
 * </pre>
 *
 * <p>Friend list encoding (stored as a single String to avoid nested PDC complexity):
 * {@code "uuid1:0;uuid2:1"}: colon separates UUID from the manager flag (0 = regular
 * friend, 1 = manager). This mirrors the simplified access model used for blocks,
 * where every friend can read/write and only the manager flag is meaningful.
 */
public final class EntityNBTHandler {

    // NamespacedKey constants

    private static final String NS = "blockprot";

    private static NamespacedKey key(@NotNull String name) {
        // Reuse the plugin instance's namespace; safe to call after onEnable.
        return new NamespacedKey(BlockProt.getInstance(), name);
    }

    private static final String K_OWNER             = "owner";
    private static final String K_HOPPER_PROTECTION = "hopper_protection";
    private static final String K_FRIENDS           = "friends";
    private static final String K_LINKED_BLOCK      = "linked_block";

    // State

    private final PersistentDataContainer pdc;

    public EntityNBTHandler(@NotNull Entity entity) {
        this.pdc = entity.getPersistentDataContainer();
    }

    // Owner

    @NotNull
    public String getOwner() {
        String v = pdc.get(key(K_OWNER), PersistentDataType.STRING);
        return v != null ? v : "";
    }

    public void setOwner(@NotNull String ownerUuid) {
        pdc.set(key(K_OWNER), PersistentDataType.STRING, ownerUuid);
    }

    public boolean isOwner(@NotNull String playerUuid) {
        return getOwner().equals(playerUuid);
    }

    public boolean isProtected() {
        return !getOwner().isEmpty();
    }

    public void clearOwner() {
        pdc.remove(key(K_OWNER));
        pdc.remove(key(K_FRIENDS));
        pdc.remove(key(K_HOPPER_PROTECTION));
        pdc.remove(key(K_LINKED_BLOCK));
    }

    // Item frame <-> block link

    /**
     * Returns the world+coordinates of the block this item frame is linked to,
     * encoded as {@code "world,x,y,z"}, or an empty string if not linked.
     * Only meaningful for item frame entities: see
     * {@code BlockNBTHandler#getLinkedItemFrameUuid()} for the inverse link.
     */
    @NotNull
    public String getLinkedBlock() {
        String v = pdc.get(key(K_LINKED_BLOCK), PersistentDataType.STRING);
        return v != null ? v : "";
    }

    public void setLinkedBlock(@NotNull String worldName, int x, int y, int z) {
        pdc.set(key(K_LINKED_BLOCK), PersistentDataType.STRING, worldName + "," + x + "," + y + "," + z);
    }

    public void clearLinkedBlock() {
        pdc.remove(key(K_LINKED_BLOCK));
    }

    public boolean isLinkedToBlock() {
        return !getLinkedBlock().isEmpty();
    }

    // Friends

    /**
     * Returns true if {@code playerUuid} is the owner or a registered friend.
     * Mirrors block protection: every registered friend has read/write access;
     * only the manager flag is distinguished (see {@link #isManager(String)}).
     */
    public boolean canAccess(@NotNull String playerUuid) {
        if (!isProtected()) return true;
        if (isOwner(playerUuid)) return true;
        return hasFriend(playerUuid);
    }

    /**
     * Returns true if {@code playerUuid} is the owner or any registered friend.
     * Movement lock and inventory access use the same rule: friends always
     * have full read/write, matching block-friend semantics.
     */
    public boolean canWrite(@NotNull String playerUuid) {
        return canAccess(playerUuid);
    }

    /**
     * Returns true if {@code playerUuid} is the owner or a friend with the
     * manager flag, allowing them to edit settings/friends on this entity.
     */
    public boolean isManager(@NotNull String playerUuid) {
        if (isOwner(playerUuid)) return true;
        FriendEntry entry = getFriendEntry(playerUuid);
        return entry != null && entry.manager;
    }

    /** Returns all friend UUID strings registered on this entity. */
    @NotNull
    public List<String> getFriendUuids() {
        List<String> out = new ArrayList<>();
        for (FriendEntry e : parseFriends()) out.add(e.uuid);
        return out;
    }

    public void addFriend(@NotNull String friendUuid) {
        List<FriendEntry> list = parseFriends();
        list.removeIf(e -> e.uuid.equals(friendUuid));
        list.add(new FriendEntry(friendUuid, false));
        saveFriends(list);
    }

    public void removeFriend(@NotNull String friendUuid) {
        List<FriendEntry> list = parseFriends();
        list.removeIf(e -> e.uuid.equals(friendUuid));
        saveFriends(list);
    }

    public boolean hasFriend(@NotNull String friendUuid) {
        return parseFriends().stream().anyMatch(e -> e.uuid.equals(friendUuid));
    }

    public void setFriendManager(@NotNull String friendUuid, boolean manager) {
        List<FriendEntry> list = parseFriends();
        list.removeIf(e -> e.uuid.equals(friendUuid));
        list.add(new FriendEntry(friendUuid, manager));
        saveFriends(list);
    }

    @Nullable
    public FriendEntry getFriendEntry(@NotNull String friendUuid) {
        return parseFriends().stream()
            .filter(e -> e.uuid.equals(friendUuid))
            .findFirst().orElse(null);
    }

    // Redstone / hopper protection

    /**
     * Returns true if hopper pipelines should be blocked for this protected entity.
     * Defaults to {@code true} (protect) when the key is absent.
     */
    public boolean isHopperProtectionEnabled() {
        Byte v = pdc.get(key(K_HOPPER_PROTECTION), PersistentDataType.BYTE);
        return v == null || v != 0;
    }

    /**
     * Sets whether hopper pipelines are blocked for this entity.
     *
     * @param enabled {@code true} to block hoppers (default), {@code false} to allow.
     */
    public void setHopperProtectionEnabled(boolean enabled) {
        pdc.set(key(K_HOPPER_PROTECTION), PersistentDataType.BYTE, (byte)(enabled ? 1 : 0));
    }


    // Friend serialisation

    /**
     * Simple record holding one friend's manager flag. Mirrors the block-friend
     * model: every friend can read/write; only the manager flag is meaningful.
     */
    public record FriendEntry(
        @NotNull String uuid,
        boolean manager
    ) {}

    @NotNull
    private List<FriendEntry> parseFriends() {
        List<FriendEntry> out = new ArrayList<>();
        String raw = pdc.get(key(K_FRIENDS), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return out;
        for (String part : raw.split(";")) {
            if (part.isBlank()) continue;
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            String uuid = kv[0].trim();
            boolean manager = "1".equals(kv[1].trim());
            out.add(new FriendEntry(uuid, manager));
        }
        return out;
    }

    private void saveFriends(@NotNull List<FriendEntry> list) {
        if (list.isEmpty()) {
            pdc.remove(key(K_FRIENDS));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FriendEntry e : list) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.uuid).append(':').append(e.manager ? '1' : '0');
        }
        pdc.set(key(K_FRIENDS), PersistentDataType.STRING, sb.toString());
    }
}