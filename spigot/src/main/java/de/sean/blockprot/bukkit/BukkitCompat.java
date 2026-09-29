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

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Central references to particle and enchantment constants used for lock effects.
 * Constants renamed between Minecraft releases are resolved by name at class load,
 * so the class compiles and runs against every supported Paper API.
 */
public final class BukkitCompat {

    public static final Particle PARTICLE_DUST = particle(null, "DUST", "REDSTONE");
    public static final Particle PARTICLE_DUST_COLOR_TRANSITION =
        particle(PARTICLE_DUST, "DUST_COLOR_TRANSITION", "REDSTONE_TRANSITION");
    public static final Enchantment GLOW_ENCHANT =
        enchantment("INFINITY", "ARROW_INFINITE", "UNBREAKING", "DURABILITY");

    private BukkitCompat() {}

    public static Material material(String name, Material fallback) {
        Material found = Material.matchMaterial(name);
        return found != null ? found : fallback;
    }

    private static Particle particle(Particle fallback, String... names) {
        for (String name : names) {
            for (Particle particle : Particle.values()) {
                if (particle.name().equals(name)) return particle;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("No particle constant found among " + Arrays.toString(names));
        }
        return fallback;
    }

    private static Enchantment enchantment(String... names) {
        for (String name : names) {
            for (Field field : Enchantment.class.getFields()) {
                if (!field.getName().equals(name)) continue;
                try {
                    return (Enchantment) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Enchantment constant not readable: " + name, e);
                }
            }
        }
        throw new IllegalStateException("No enchantment constant found among " + Arrays.toString(names));
    }

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
