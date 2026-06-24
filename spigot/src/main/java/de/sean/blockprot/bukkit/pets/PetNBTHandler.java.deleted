package de.sean.blockprot.bukkit.pets;

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
 * Handles reading and writing pet protection data stored in an entity's
 * {@link PersistentDataContainer}.
 *
 * <p>Keys stored (all under namespace {@code blockprot}):
 * <ul>
 *   <li>{@code pet_owner}     — UUID string of the BlockProt owner</li>
 *   <li>{@code pet_protected} — byte: 1 = protection active</li>
 *   <li>{@code no_damage}     — byte: 1 = block damage from other players/mobs</li>
 *   <li>{@code no_interact}   — byte: 1 = block right-click from others (feed, name…)</li>
 *   <li>{@code no_leash}      — byte: 1 = block leash/unleash from others</li>
 *   <li>{@code no_pickup}     — byte: 1 = block parrot-shoulder pickup from others</li>
 * </ul>
 *
 * @since SP26-ZV
 */
public final class PetNBTHandler {

    private final PersistentDataContainer pdc;
    private final Entity entity;

    private final NamespacedKey keyOwner;
    private final NamespacedKey keyProtected;
    private final NamespacedKey keyNoDamage;
    private final NamespacedKey keyNoInteract;
    private final NamespacedKey keyNoLeash;
    private final NamespacedKey keyNoPickup;

    public PetNBTHandler(@NotNull Entity entity) {
        this.entity = entity;
        this.pdc    = entity.getPersistentDataContainer();
        BlockProt p = BlockProt.getInstance();
        this.keyOwner      = new NamespacedKey(p, "pet_owner");
        this.keyProtected  = new NamespacedKey(p, "pet_protected");
        this.keyNoDamage   = new NamespacedKey(p, "no_damage");
        this.keyNoInteract = new NamespacedKey(p, "no_interact");
        this.keyNoLeash    = new NamespacedKey(p, "no_leash");
        this.keyNoPickup   = new NamespacedKey(p, "no_pickup");
    }

    // ── Owner ──────────────────────────────────────────────────────────────────

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

    // ── Protection state ───────────────────────────────────────────────────────

    public boolean isProtected() {
        Byte b = pdc.get(keyProtected, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public void setProtected(boolean v) {
        pdc.set(keyProtected, PersistentDataType.BYTE, v ? (byte) 1 : (byte) 0);
    }

    // ── Individual settings ────────────────────────────────────────────────────

    public boolean isNoDamage()   { return getBool(keyNoDamage,   true);  }
    public boolean isNoInteract() { return getBool(keyNoInteract, false); }
    public boolean isNoLeash()    { return getBool(keyNoLeash,    true);  }
    public boolean isNoPickup()   { return getBool(keyNoPickup,   false); }

    public void setNoDamage(boolean v)   { setBool(keyNoDamage,   v); }
    public void setNoInteract(boolean v) { setBool(keyNoInteract, v); }
    public void setNoLeash(boolean v)    { setBool(keyNoLeash,    v); }
    public void setNoPickup(boolean v)   { setBool(keyNoPickup,   v); }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean getBool(@NotNull NamespacedKey key, boolean def) {
        Byte b = pdc.get(key, PersistentDataType.BYTE);
        return b == null ? def : b == 1;
    }

    private void setBool(@NotNull NamespacedKey key, boolean v) {
        pdc.set(key, PersistentDataType.BYTE, v ? (byte) 1 : (byte) 0);
    }

    /** Returns true when the entity type can be protected (any tamed animal). */
    public static boolean isSupportedPet(@NotNull Entity entity) {
        return entity instanceof Tameable;
    }

    /** Returns a new handler only if the entity is a tamed animal, otherwise null. */
    @Nullable
    public static PetNBTHandler forEntityOrNull(@NotNull Entity entity) {
        return isSupportedPet(entity) ? new PetNBTHandler(entity) : null;
    }
}
