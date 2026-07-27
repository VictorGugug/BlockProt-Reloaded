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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Plays a perimeter particle effect and sound around a block when locked or unlocked,
 * respecting real block bounds, beds, and double chest footprints.
 */
public final class LockEffectListener implements Listener {

    private static final double POINT_SPACING = 0.25;

    public enum Setting { REDSTONE, HOPPER, PISTON }

    @EventHandler
    public void onLock(@NotNull BlockProtLockEvent event) {
        if (!BlockProt.getDefaultConfig().isLockEffectEnabled()) return;
        Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(0, 220, 80), 1.2f);
        spawnDustPerimeter(event.getBlock(), green);
        playChestSound(event.getBlock(), true);
    }

    @EventHandler
    public void onUnlock(@NotNull BlockProtUnlockEvent event) {
        if (!BlockProt.getDefaultConfig().isLockEffectEnabled()) return;
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(220, 50, 50), 1.2f);
        spawnDustPerimeter(event.getBlock(), red);
        playChestSound(event.getBlock(), false);
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

        spawnTransitionPerimeter(block, transition);
        if (BlockProt.getDefaultConfig().isLockSoundEnabled()) {
            EffectGeometry geom = EffectGeometry.createForBlock(block);
            Location center = geom.getUnionCenter();
            center.getWorld().playSound(center, Sound.BLOCK_LEVER_CLICK, 0.35f, enabled ? 1.4f : 0.9f);
        }
    }

    private static void spawnDustPerimeter(@NotNull Block block, @NotNull Particle.DustOptions dust) {
        EffectGeometry geom = EffectGeometry.createForBlock(block);
        List<Location> points = geom.getPerimeterPoints(POINT_SPACING);

        for (Location p : points) {
            p.getWorld().spawnParticle(
                BukkitCompat.PARTICLE_DUST,
                p.getX(), p.getY(), p.getZ(),
                1, 0, 0, 0, 0,
                dust
            );
        }
    }

    private static void spawnTransitionPerimeter(@NotNull Block block, @NotNull Particle.DustTransition transition) {
        EffectGeometry geom = EffectGeometry.createForBlock(block);
        List<Location> points = geom.getPerimeterPoints(POINT_SPACING);

        for (Location p : points) {
            p.getWorld().spawnParticle(
                BukkitCompat.PARTICLE_DUST_COLOR_TRANSITION,
                p.getX(), p.getY(), p.getZ(),
                1, 0, 0, 0, 0,
                transition
            );
        }
    }

    private static void playChestSound(@NotNull Block block, boolean locked) {
        if (!BlockProt.getDefaultConfig().isLockSoundEnabled()) return;
        EffectGeometry geom = EffectGeometry.createForBlock(block);
        Location center = geom.getUnionCenter();

        Sound sound;
        if (block.getState() instanceof org.bukkit.block.ShulkerBox) {
            sound = locked ? Sound.ENTITY_SHULKER_CLOSE : Sound.ENTITY_SHULKER_OPEN;
        } else {
            sound = locked ? Sound.BLOCK_CHEST_CLOSE : Sound.BLOCK_CHEST_OPEN;
        }
        center.getWorld().playSound(center, sound, 0.35f, 1.0f);
    }
}