package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Protects villagers that are linked to a protected workstation.
 */
public final class VillagerWorkstationProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.hasPermission(Permissions.USER_ADMIN.key())) return;

        Block workstation = getProtectedLinkedWorkstation(villager);
        if (workstation == null) return;
        if (canAccess(attacker, workstation)) return;

        event.setCancelled(true);
        attacker.sendActionBar(LegacyComponentSerializer.legacySection()
            .deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED villager damage: villager="
            + villager.getUniqueId() + " workstation=" + locString(workstation.getLocation())
            + " player=" + attacker.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.USER_ADMIN.key())) return;

        Block workstation = getProtectedLinkedWorkstation(villager);
        if (workstation == null) return;
        if (canAccess(player, workstation)) return;

        event.setCancelled(true);
        player.sendActionBar(LegacyComponentSerializer.legacySection()
            .deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
        BlockProtLogger.log("entity-protection", "ACCESS_DENIED villager interact: villager="
            + villager.getUniqueId() + " workstation=" + locString(workstation.getLocation())
            + " player=" + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakNearWorkstation(@NotNull BlockBreakEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.USER_ADMIN.key())) return;

        Block workstation = findNearbyProtectedWorkstation(event.getBlock());
        if (workstation == null) return;

        if (!canAccess(player, workstation)) {
            event.setCancelled(true);
            player.sendActionBar(LegacyComponentSerializer.legacySection()
                .deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED block break near workstation: block="
                + event.getBlock().getType().name() + " location=" + locString(event.getBlock().getLocation())
                + " workstation=" + locString(workstation.getLocation()) + " player=" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteractNearWorkstation(@NotNull PlayerInteractEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.USER_ADMIN.key())) return;

        Block workstation = findNearbyProtectedWorkstation(event.getClickedBlock());
        if (workstation == null) return;

        if (workstation.equals(event.getClickedBlock())) return;

        if (!canAccess(player, workstation)) {
            event.setCancelled(true);
            player.sendActionBar(LegacyComponentSerializer.legacySection()
                .deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
            BlockProtLogger.log("entity-protection", "ACCESS_DENIED block interact near workstation: block="
                + event.getClickedBlock().getType().name() + " location=" + locString(event.getClickedBlock().getLocation())
                + " workstation=" + locString(workstation.getLocation()) + " player=" + player.getName());
        }
    }

    @Nullable
    private static Block findNearbyProtectedWorkstation(@NotNull Block block) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block b = block.getRelative(dx, dy, dz);
                    if (isWorkstation(b.getType())) {
                        try {
                            BlockNBTHandler handler = new BlockNBTHandler(b);
                            if (handler.isProtected()) {
                                return b;
                            }
                        } catch (RuntimeException ignored) {}
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Villager findLinkedVillager(@NotNull Block workstation) {
        for (Entity entity : workstation.getWorld().getNearbyEntities(workstation.getLocation().add(0.5, 0.5, 0.5), 16, 8, 16)) {
            if (!(entity instanceof Villager villager)) continue;
            Location jobSite = getJobSite(villager);
            if (jobSite != null && sameBlock(jobSite, workstation.getLocation())) return villager;
        }
        return null;
    }

    @Nullable
    private static Block getProtectedLinkedWorkstation(@NotNull Villager villager) {
        Location jobSite = getJobSite(villager);
        if (jobSite == null || jobSite.getWorld() == null) return null;
        Block block = jobSite.getBlock();
        if (!isWorkstation(block.getType())) return null;
        if (!isWithinProtectedArea(villager.getLocation(), block.getLocation())) return null;
        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            return handler.isProtected() ? block : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Location getJobSite(@NotNull Villager villager) {
        try {
            Class<?> memoryKey = Class.forName("org.bukkit.entity.memory.MemoryKey");
            Object jobSiteKey = memoryKey.getField("JOB_SITE").get(null);
            Method getMemory = villager.getClass().getMethod("getMemory", memoryKey);
            Object value = getMemory.invoke(villager, jobSiteKey);
            return value instanceof Location loc ? loc : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean canAccess(@NotNull Player player, @NotNull Block workstation) {
        try {
            BlockNBTHandler handler = new BlockNBTHandler(workstation);
            return handler.canAccess(player.getUniqueId().toString());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isWithinProtectedArea(@NotNull Location villager, @NotNull Location workstation) {
        if (villager.getWorld() == null || workstation.getWorld() == null
                || !villager.getWorld().equals(workstation.getWorld())) return false;
        return Math.abs(villager.getBlockX() - workstation.getBlockX()) <= 2
            && Math.abs(villager.getBlockZ() - workstation.getBlockZ()) <= 2
            && Math.abs(villager.getBlockY() - workstation.getBlockY()) <= 1;
    }

    private static boolean isWorkstation(@NotNull Material material) {
        String name = material.name();
        return name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")
            || name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE")
            || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")
            || name.equals("LECTERN") || name.equals("COMPOSTER") || name.equals("BREWING_STAND")
            || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("BARREL")
            || name.equals("CAULDRON");
    }

    private static boolean sameBlock(@NotNull Location a, @NotNull Location b) {
        return a.getWorld() != null && b.getWorld() != null && a.getWorld().equals(b.getWorld())
            && a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    @Nullable
    private Player resolvePlayer(@NotNull Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }

    @NotNull
    private static String locString(@NotNull Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        return world + " [" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]";
    }
}
