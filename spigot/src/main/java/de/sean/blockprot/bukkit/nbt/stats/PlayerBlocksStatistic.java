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

package de.sean.blockprot.bukkit.nbt.stats;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.nbt.stats.StatisticType;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerBlocksStatistic extends LocationListStatistic {
    @Override
    public @NotNull String getKey() {
        return "containers";
    }

    @Override
    public @NotNull StatisticType getType() {
        return StatisticType.PLAYER;
    }

    @Override
    public @NotNull Material getItemType() {
        return Material.CHEST;
    }

    @Override
    public String getStatisticName() {
        return Translator.get(TranslationKey.INVENTORIES__STATISTICS__YOUR_BLOCKS);
    }

    @Override
    public @NotNull String getTitle() {
        return getStatisticName() + ": " + get().size();
    }

    /**
     * Returns a lore list with the count of protected blocks broken down by
     * type. Only types actually present are included. At most 10 lines to
     * avoid an oversized tooltip.
     */
    @NotNull
    public List<String> getBreakdownLore() {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (LocationListEntry entry : get()) {
            try {
                Material mat = entry.getItemType();
                counts.merge(mat, 1, Integer::sum);
            } catch (Exception ignored) {}
        }

        if (counts.isEmpty()) return List.of();

        List<String> lore = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<Material, Integer> e : counts.entrySet()) {
            if (shown >= 10) {
                lore.add("§8+ " + (counts.size() - shown) + " more...");
                break;
            }
            String name = toHumanReadable(e.getKey());
            lore.add("§7" + name + "§8: §f" + e.getValue());
            shown++;
        }
        return lore;
    }

    private static String toHumanReadable(@NotNull Material mat) {
        String raw = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') { sb.append(c); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }
}
