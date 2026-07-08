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

package de.sean.blockprot.bukkit.entities;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles reading and writing tamed-entity protection data stored in an entity's
 * {@link PersistentDataContainer}.
 *
 * <p>Keys stored (all under namespace {@code blockprot}):
 * <ul>
 *   <li>{@code entity_owner}    : UUID string of the BlockProt owner</li>
 *   <li>{@code entity_protected}: byte: 1 = protection active</li>
 *   <li>{@code no_damage}       : byte: 1 = block damage from other players/mobs</li>
 *   <li>{@code no_interact}     : byte: 1 = block right-click from others (feed, name…)</li>
 *   <li>{@code no_leash}        : byte: 1 = block leash/unleash from others</li>
 *   <li>{@code no_pickup}       : byte: 1 = block parrot-shoulder pickup from others</li>
 * </ul>
 *
 * <p>Renamed from the original {@code pet_owner}/{@code pet_protected} keys used by
 * the earlier "Pet Protection" feature. On first read of any entity, if the legacy
 * keys are present and the modern keys are not, the data is transparently migrated
 * in place (read old, write new, remove old) so no existing protection is lost.
 */
public final class EntityProtectionHandler {

    private final PersistentDataContainer pdc;
    private final Entity entity;

    private final NamespacedKey keyOwner;
    private final NamespacedKey keyProtected;
    private final NamespacedKey keyNoDamage;
    private final NamespacedKey keyNoInteract;
    private final NamespacedKey keyNoLeash;
    private final NamespacedKey keyNoPickup;

    private final NamespacedKey legacyKeyOwner;
    private final NamespacedKey legacyKeyProtected;

    public EntityProtectionHandler(@NotNull Entity entity) {
        this.entity = entity;
        this.pdc    = entity.getPersistentDataContainer();
        BlockProt p = BlockProt.getInstance();
        this.keyOwner      = new NamespacedKey(p, "entity_owner");
        this.keyProtected  = new NamespacedKey(p, "entity_protected");
        this.keyNoDamage   = new NamespacedKey(p, "no_damage");
        this.keyNoInteract = new NamespacedKey(p, "no_interact");
        this.keyNoLeash    = new NamespacedKey(p, "no_leash");
        this.keyNoPickup   = new NamespacedKey(p, "no_pickup");

        this.legacyKeyOwner     = new NamespacedKey(p, "pet_owner");
        this.legacyKeyProtected = new NamespacedKey(p, "pet_protected");

        migrateLegacyKeysIfNeeded();
    }

    /**
     * One-time migration from the old "pet_*" PDC keys to the new "entity_*" keys.
     * Runs on every construction but is a no-op once migrated (modern key present).
     * {@code no_damage}/{@code no_interact}/{@code no_leash}/{@code no_pickup} keep
     * their names unchanged: only the owner/protected flags were ever prefixed
     * with {@code pet_}.
     */
    private void migrateLegacyKeysIfNeeded() {
        if (pdc.has(keyOwner, PersistentDataType.STRING) || pdc.has(keyProtected, PersistentDataType.BYTE)) {
            return; // already on modern keys, nothing to migrate
        }
        String legacyOwner = pdc.get(legacyKeyOwner, PersistentDataType.STRING);
        Byte legacyProtected = pdc.get(legacyKeyProtected, PersistentDataType.BYTE);
        if (legacyOwner == null && legacyProtected == null) return; // never protected under old system

        if (legacyOwner != null) {
            pdc.set(keyOwner, PersistentDataType.STRING, legacyOwner);
            pdc.remove(legacyKeyOwner);
        }
        if (legacyProtected != null) {
            pdc.set(keyProtected, PersistentDataType.BYTE, legacyProtected);
            pdc.remove(legacyKeyProtected);
        }
    }

    @Nullable
    public UUID getOwner() {
        String raw = pdc.get(keyOwner, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            // Fallback: use Tameable owner when no explicit PDC owner is set.
            if (entity instanceof Tameable t && t.getOwnerUniqueId() != null) {
                return t.getOwnerUniqueId();
            }
            return null;
        }
        try { return UUID.fromString(raw); }
        catch (IllegalArgumentException e) { return null; }
    }

    public void setOwner(@NotNull UUID owner) {
        pdc.set(keyOwner, PersistentDataType.STRING, owner.toString());
    }

    public boolean isOwner(@NotNull UUID uuid) {
        UUID owner = getOwner();
        return owner != null && owner.equals(uuid);
    }

    public boolean isProtected() {
        Byte b = pdc.get(keyProtected, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public void setProtected(boolean v) {
        pdc.set(keyProtected, PersistentDataType.BYTE, v ? (byte) 1 : (byte) 0);
    }

    public boolean isNoDamage()   { return getBool(keyNoDamage,   true);  }
    public boolean isNoInteract() { return getBool(keyNoInteract, false); }
    public boolean isNoLeash()    { return getBool(keyNoLeash,    true);  }
    public boolean isNoPickup()   { return getBool(keyNoPickup,   false); }

    public void setNoDamage(boolean v)   { setBool(keyNoDamage,   v); }
    public void setNoInteract(boolean v) { setBool(keyNoInteract, v); }
    public void setNoLeash(boolean v)    { setBool(keyNoLeash,    v); }
    public void setNoPickup(boolean v)   { setBool(keyNoPickup,   v); }

    /** Enables protection with sensible defaults (damage + leash ON, interact + pickup OFF). */
    public void enable(@NotNull UUID owner) {
        setOwner(owner);
        setProtected(true);
        setNoDamage(true);
        setNoInteract(false);
        setNoLeash(true);
        setNoPickup(false);
    }

    /** Removes all BlockProt data from this entity's PDC. */
    public void clear() {
        pdc.remove(keyOwner);
        pdc.remove(keyProtected);
        pdc.remove(keyNoDamage);
        pdc.remove(keyNoInteract);
        pdc.remove(keyNoLeash);
        pdc.remove(keyNoPickup);
        pdc.remove(legacyKeyOwner);
        pdc.remove(legacyKeyProtected);
    }

    private boolean getBool(@NotNull NamespacedKey key, boolean def) {
        Byte b = pdc.get(key, PersistentDataType.BYTE);
        return b == null ? def : b == 1;
    }

    private void setBool(@NotNull NamespacedKey key, boolean v) {
        pdc.set(key, PersistentDataType.BYTE, v ? (byte) 1 : (byte) 0);
    }

    /** Returns true when the entity type can be protected (any tamed animal). */
    public static boolean isSupportedEntity(@NotNull Entity entity) {
        return entity instanceof Tameable;
    }

    /** Returns a new handler only if the entity is a tamed animal, otherwise null. */
    @Nullable
    public static EntityProtectionHandler forEntityOrNull(@NotNull Entity entity) {
        return isSupportedEntity(entity) ? new EntityProtectionHandler(entity) : null;
    }
}