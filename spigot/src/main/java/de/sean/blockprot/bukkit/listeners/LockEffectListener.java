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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.events.BlockProtLockEvent;
import de.sean.blockprot.bukkit.events.BlockProtUnlockEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a particle ring and sound around a block when it is locked or unlocked,
 * or when redstone/hopper/piston settings are toggled.
 *
 * <p>Lock    → green dust particles
 * <p>Unlock  → red dust particles
 * <p>Redstone toggle → red ↔ white transition particles
 * <p>Hopper toggle   → gray ↔ dark-gray transition particles
 * <p>Piston toggle   → brown ↔ gray transition particles
 *
 * <p>For double chests the ring spans both halves.
 *
 * Enabled via {@code block_lock_effects: true} in config.yml (default: true).
 */
public final class LockEffectListener implements Listener {

    private static final int PARTICLE_COUNT = 24;
    private static final double RADIUS = 0.65;

    public enum Setting { REDSTONE, HOPPER, PISTON }

    @EventHandler
    public void onLock(@NotNull BlockProtLockEvent event) {
        if (!BlockProt.getDefaultConfig().isLockEffectEnabled()) return;
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(0, 220, 80), 1.2f);
        spawnRingsForBlock(event.getBlock(), green);
        playChestSound(centroid(event.getBlock()), true);
    }

    @EventHandler
    public void onUnlock(@NotNull BlockProtUnlockEvent event) {
        if (!BlockProt.getDefaultConfig().isLockEffectEnabled()) return;
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(220, 50, 50), 1.2f);
        spawnRingsForBlock(event.getBlock(), red);
        playChestSound(centroid(event.getBlock()), false);
    }

    public static void playSettingEffect(@NotNull Block block, @NotNull Setting setting, boolean enabled) {
        if (!BlockProt.getDefaultConfig().isLockEffectEnabled()) return;

        Particle.DustTransition transition = switch (setting) {
            case REDSTONE -> enabled
                ? new Particle.DustTransition(Color.fromRGB(220, 40, 40), Color.fromRGB(255, 255, 255), 1.1f)
                : new Particle.DustTransition(Color.fromRGB(255, 255, 255), Color.fromRGB(180, 30, 30), 1.1f);
            case HOPPER -> enabled
                ? new Particle.DustTransition(Color.fromRGB(200, 200, 200), Color.fromRGB(80, 80, 80), 1.1f)
                : new Particle.DustTransition(Color.fromRGB(80, 80, 80), Color.fromRGB(200, 200, 200), 1.1f);
            case PISTON -> enabled
                ? new Particle.DustTransition(Color.fromRGB(139, 90, 43), Color.fromRGB(150, 150, 150), 1.1f)
                : new Particle.DustTransition(Color.fromRGB(150, 150, 150), Color.fromRGB(100, 60, 20), 1.1f);
        };

        spawnTransitionRingsForBlock(block, transition);
        if (BlockProt.getDefaultConfig().isLockSoundEnabled()) {
            Location center = centroid(block);
            center.getWorld().playSound(center, Sound.BLOCK_LEVER_CLICK, 0.35f, enabled ? 1.4f : 0.9f);
        }
    }

    private static void spawnRingsForBlock(@NotNull Block block, @NotNull Particle.DustOptions dust) {
        for (Location center : getCenters(block)) {
            spawnDustRing(center, dust);
        }
    }

    private static void spawnTransitionRingsForBlock(
            @NotNull Block block,
            @NotNull Particle.DustTransition transition) {
        for (Location center : getCenters(block)) {
            spawnTransitionRing(center, transition);
        }
    }

    private static List<Location> getCenters(@NotNull Block block) {
        List<Location> centers = new ArrayList<>();
        centers.add(centroid(block));

        BlockState state = block.getState();
        if (state instanceof Chest) {
            var inv = ((Chest) state).getInventory();
            if (inv instanceof DoubleChestInventory dci) {
                var dc = dci.getHolder();
                if (dc != null) {
                    Location mid = dc.getLocation();
                    if (mid != null && mid.getWorld() != null) {
                        double bx = block.getX();
                        double bz = block.getZ();
                        double mx = mid.getX();
                        double mz = mid.getZ();
                        double ox = 2 * mx - bx;
                        double oz = 2 * mz - bz;
                        Location otherCenter = new Location(
                            block.getWorld(),
                            Math.floor(ox) + 0.5,
                            block.getY() + 0.5,
                            Math.floor(oz) + 0.5
                        );
                        if ((int) Math.floor(ox) != block.getX() || (int) Math.floor(oz) != block.getZ()) {
                            centers.add(otherCenter);
                        }
                    }
                }
            }
        }
        return centers;
    }

    private static void spawnDustRing(@NotNull Location center, @NotNull Particle.DustOptions dust) {
        double step = (2 * Math.PI) / PARTICLE_COUNT;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = step * i;
            center.getWorld().spawnParticle(
                BukkitCompat.PARTICLE_DUST,
                center.getX() + Math.cos(angle) * RADIUS,
                center.getY(),
                center.getZ() + Math.sin(angle) * RADIUS,
                1, 0, 0, 0, 0,
                dust
            );
        }
    }

    private static void spawnTransitionRing(
            @NotNull Location center,
            @NotNull Particle.DustTransition transition) {
        double step = (2 * Math.PI) / PARTICLE_COUNT;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = step * i;
            center.getWorld().spawnParticle(
                BukkitCompat.PARTICLE_DUST_COLOR_TRANSITION,
                center.getX() + Math.cos(angle) * RADIUS,
                center.getY(),
                center.getZ() + Math.sin(angle) * RADIUS,
                1, 0, 0, 0, 0,
                transition
            );
        }
    }

    private static Location centroid(@NotNull Block block) {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }

    private static void playChestSound(@NotNull Location loc, boolean locked) {
        if (!BlockProt.getDefaultConfig().isLockSoundEnabled()) return;
        Block block = loc.getWorld().getBlockAt(loc.clone().subtract(0.5, 0.5, 0.5));
        Sound sound;
        if (block.getState() instanceof org.bukkit.block.ShulkerBox) {
            sound = locked ? Sound.ENTITY_SHULKER_CLOSE : Sound.ENTITY_SHULKER_OPEN;
        } else {
            sound = locked ? Sound.BLOCK_CHEST_CLOSE : Sound.BLOCK_CHEST_OPEN;
        }
        loc.getWorld().playSound(loc, sound, 0.35f, 1.0f);
    }
}
