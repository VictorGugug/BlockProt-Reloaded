package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.pets.PetNBTHandler;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles all protection logic for tamed pets.
 *
 * <p>Protected events:
 * <ul>
 *   <li>Damage — from players or their projectiles (when no_damage is ON)</li>
 *   <li>Interact — right-click, feeding, naming (when no_interact is ON)</li>
 *   <li>Leash / Unleash — attach or remove leads (when no_leash is ON)</li>
 *   <li>Pickup — parrot shoulder pickup (when no_pickup is ON)</li>
 *   <li>Death message — notifies owner when a protected pet dies</li>
 *   <li>Auto-protect on tame — when global setting is enabled in config</li>
 * </ul>
 *
 * <p>Players with {@code blockprot.admin} or {@code blockprot.bypass} always bypass protection.
 *
 * @since SP26-ZV
 */
public final class PetProtectionListener implements Listener {

    // ── Tame: auto-protect ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(@NotNull EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (!BlockProt.getDefaultConfig().isPetAutoProtectOnTame()) return;

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(event.getEntity());
        if (handler == null) return;
        handler.enable(player.getUniqueId());
    }

    // ── Damage ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;

        Entity victim = event.getEntity();
        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(victim);
        if (handler == null || !handler.isProtected() || !handler.isNoDamage()) return;

        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return; // mob/environment damage — allow
        if (isBypassing(attacker, handler)) return;

        event.setCancelled(true);
        attacker.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
    }

    // ── Interact (feed, name tag, right-click) ─────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // fire once

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(event.getRightClicked());
        if (handler == null || !handler.isProtected() || !handler.isNoInteract()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
    }

    // ── Leash ──────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeash(@NotNull PlayerLeashEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(event.getEntity());
        if (handler == null || !handler.isProtected() || !handler.isNoLeash()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnleash(@NotNull PlayerUnleashEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(event.getEntity());
        if (handler == null || !handler.isProtected() || !handler.isNoLeash()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
    }

    // ── Parrot shoulder pickup ─────────────────────────────────────────────────
    // Parrot-on-shoulder is triggered by a PlayerInteractEntityEvent on the parrot,
    // so the no_interact flag already covers that case. The no_pickup flag acts as
    // a standalone guard in case no_interact is OFF but the owner still wants to
    // prevent others from wearing their parrot.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onParrotPickup(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (!(event.getRightClicked() instanceof Parrot)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(event.getRightClicked());
        if (handler == null || !handler.isProtected() || !handler.isNoPickup()) return;

        Player player = event.getPlayer();
        if (isBypassing(player, handler)) return;

        event.setCancelled(true);
        player.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
    }

    // ── Death notification ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPetDeath(@NotNull EntityDeathEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;

        LivingEntity dead = event.getEntity();
        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(dead);
        if (handler == null || !handler.isProtected()) return;

        UUID ownerUuid = handler.getOwner();
        if (ownerUuid == null) return;

        Player owner = dead.getServer().getPlayer(ownerUuid);
        if (owner == null || !owner.isOnline()) return;

        String petName = dead.customName() != null
            ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(dead.customName())
            : dead.getType().name();
        String message = Translator.get(TranslationKey.MESSAGES__PET_DEATH_NOTIFICATION).replace("{pet_name}", petName);
        owner.sendMessage(message);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves a damaging entity to a {@link Player}. Handles direct hits and
     * projectiles (arrows, tridents, snowballs, etc.) shot by a player.
     */
    @Nullable
    private Player resolvePlayer(@NotNull Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p) return p;
        }
        // Wolf or other pet attacking on behalf of a player — intentionally ignored;
        // pet-on-pet PvP is considered vanilla and not blocked by default.
        return null;
    }

    /**
     * Returns true when the player should bypass protection:
     * they are the owner, have admin/bypass permission, or the global
     * pet-protection feature is disabled.
     */
    private boolean isBypassing(@NotNull Player player, @NotNull PetNBTHandler handler) {
        return handler.isOwner(player.getUniqueId())
            || player.hasPermission(Permissions.USER_ADMIN.key())
            || player.hasPermission(Permissions.BYPASS.key());
    }
}
