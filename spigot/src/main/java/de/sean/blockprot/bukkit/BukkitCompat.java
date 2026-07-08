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

package de.sean.blockprot.bukkit;

import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime compatibility shims for Bukkit API fields that were renamed
 * between MC 1.20 and 26.x.
 *
 * Particle.DUST : added in 1.20.6; older servers use Particle.REDSTONE.
 * Enchantment.INFINITY: added/renamed in 1.20.6; older servers use ARROW_INFINITE.
 *
 * All fields are resolved once at class-load via Enum.valueOf so there is
 * zero overhead after the first access.
 */
public final class BukkitCompat {

    public static final Particle PARTICLE_DUST;
    public static final Particle PARTICLE_DUST_COLOR_TRANSITION;
    public static final Enchantment GLOW_ENCHANT;

    static {
        // 1.20.6+ -> "DUST"   |   1.20.x -> "REDSTONE"
        Particle dust;
        try {
            dust = Particle.valueOf("DUST");
        } catch (IllegalArgumentException e) {
            dust = Particle.valueOf("REDSTONE");
        }
        PARTICLE_DUST = dust;

        // 1.20.6+ -> "DUST_COLOR_TRANSITION"  |  1.20.x -> "REDSTONE_TRANSITION" (some builds)
        Particle dustTransition;
        try {
            dustTransition = Particle.valueOf("DUST_COLOR_TRANSITION");
        } catch (IllegalArgumentException e) {
            try {
                dustTransition = Particle.valueOf("REDSTONE_TRANSITION");
            } catch (IllegalArgumentException e2) {
                dustTransition = PARTICLE_DUST; // safe fallback
            }
        }
        PARTICLE_DUST_COLOR_TRANSITION = dustTransition;

        // 1.20.6+ -> "INFINITY"   |   1.20.x -> "ARROW_INFINITE"
        Enchantment glow;
        try {
            // Prefer field access (fastest, avoids keyed lookup issues across versions)
            glow = (Enchantment) Enchantment.class.getField("INFINITY").get(null);
        } catch (Exception e1) {
            try {
                glow = (Enchantment) Enchantment.class.getField("ARROW_INFINITE").get(null);
            } catch (Exception e2) {
                // Ultimate fallback: any common enchant works for the glow effect
                glow = Enchantment.UNBREAKING;
            }
        }
        GLOW_ENCHANT = glow;
    }

    private BukkitCompat() {}

    public static boolean hasNewParticleNames() {
        return PARTICLE_DUST.name().equals("DUST");
    }

    public static boolean hasNewEnchantmentNames() {
        return GLOW_ENCHANT.getKey().getKey().equals("infinity");
    }

    @NotNull
    public static String getDiagnosticString() {
        return "BukkitCompat[particle=" + PARTICLE_DUST.name()
            + " enchant=" + GLOW_ENCHANT.getKey().getKey()
            + " newParticle=" + hasNewParticleNames()
            + " newEnchant=" + hasNewEnchantmentNames() + "]";
    }
}