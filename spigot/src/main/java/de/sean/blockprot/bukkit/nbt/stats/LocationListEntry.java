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

import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.nbt.stats.ListStatisticItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.StringJoiner;

/**
 * Resolves the correct display material for a block location.
 * Distinguishes shulker boxes, copper chests, barrels, trapped chests, etc.
 * so the stats list shows the right icon instead of always CHEST.
 */

public class LocationListEntry extends ListStatisticItem<Location, Material> {
    public LocationListEntry(@NotNull Location value) {
        super(value);
    }

    public @NotNull Block getBlock() {
        Location loc = this.get();
        if (loc.getWorld() == null) throw new IllegalStateException("World is not loaded");
        return loc.getBlock();
    }

    @Override
    public @NotNull Material getItemType() {
        try {
            Location loc = this.get();
            if (loc.getWorld() == null) return Material.CHEST;
            Material type = loc.getBlock().getType();
            return resolveDisplayMaterial(type);
        } catch (Exception e) {
            return Material.CHEST;
        }
    }

    /**
     * Maps the real block material to the best representative item material.
     * For wall variants and similar, falls back to the placeable item form.
     */
    private static @NotNull Material resolveDisplayMaterial(@NotNull Material type) {
        if (type == Material.AIR) return Material.CHEST;
        String name = type.name();
        // Shulker boxes — keep their colour; all coloured variants are valid ItemStack materials.
        // The generic SHULKER_BOX (no colour prefix) may not be a real block in all MC versions;
        // map it to the purple one which is always safe.
        if (name.equals("SHULKER_BOX")) return Material.PURPLE_SHULKER_BOX;
        if (name.endsWith("_SHULKER_BOX")) return type;
        // Copper chests and shelves (1.21.9+) — show their own variant
        if (name.contains("COPPER_CHEST") || name.contains("COPPER_TRAPPED_CHEST")) return type;
        if (name.endsWith("_SHELF")) return type;
        // Wall signs → sign item (wall variants aren't placeable as items)
        if (name.endsWith("_WALL_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            return m != null ? m : type;
        }
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            return m != null ? m : type;
        }
        return type;
    }

    @Override
    public String getTitle() {
        var coordinates = new StringJoiner(", ", "[", "]")
            .add(String.valueOf(this.value.getBlockX()))
            .add(String.valueOf(this.value.getBlockY()))
            .add(String.valueOf(this.value.getBlockZ()))
            .toString();
        try {
            // Use the live block type as the display name so it always reflects
            // the actual block (shulker, chest, barrel, etc.) regardless of
            // what name was stored in NBT when the block was first protected.
            Location loc = this.get();
            if (loc.getWorld() != null) {
                Material liveMat = loc.getBlock().getType();
                Material displayMat = resolveDisplayMaterial(liveMat);
                String typeName = toHumanReadable(displayMat);
                // Append the custom NBT name only if the owner set one
                try {
                    String nbtName = new BlockNBTHandler(loc.getBlock()).getName();
                    String defaultName = toHumanReadable(liveMat);
                    // Only append the custom name when it differs from the default
                    if (nbtName != null && !nbtName.isEmpty() && !nbtName.equals(defaultName)) {
                        return Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__LOCATION_FORMAT)
                            .replace("{type}", typeName)
                            .replace("{nbtName}", nbtName)
                            .replace("{coords}", coordinates);
                    }
                } catch (RuntimeException ignored) {}
                return typeName + " " + coordinates;
            }
        } catch (Exception ignored) {}
        return coordinates;
    }

    /** Converts a Material name like WHITE_SHULKER_BOX to §7White Shulker Box */
    private static String toHumanReadable(@NotNull Material mat) {
        String raw = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__LORE_PREFIX));
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') { sb.append(c); cap = true; }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }

    /**
     * Returns a human-readable string describing how long ago this block was locked,
     * e.g. "§8Locked 3 days ago" or "§8Locked just now".
     * Returns an empty string if no timestamp is recorded.
     */
    public @NotNull String getLockedAgoText() {
        try {
            Location loc = this.get();
            if (loc.getWorld() == null) return "";
            long lockedAt = new BlockNBTHandler(loc.getBlock()).getLockedAt();
            if (lockedAt <= 0) return "";
            long elapsedMs = System.currentTimeMillis() - lockedAt;
            if (elapsedMs < 0) return "";
            return Translator.get(TranslationKey.MESSAGES__LOCATION__LOCKED_AGO).replace("{time}", formatElapsed(elapsedMs));
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String formatElapsed(long ms) {
        long secs    = ms / 1000L;
        long minutes = secs / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;
        if (days   >= 1) return days + " " + (days   == 1 ? Translator.get(TranslationKey.MESSAGES__TIME__DAY)    : Translator.get(TranslationKey.MESSAGES__TIME__DAYS));
        if (hours  >= 1) return hours + " " + (hours  == 1 ? Translator.get(TranslationKey.MESSAGES__TIME__HOUR)   : Translator.get(TranslationKey.MESSAGES__TIME__HOURS));
        if (minutes >= 1) return minutes + " " + (minutes == 1 ? Translator.get(TranslationKey.MESSAGES__TIME__MINUTE) : Translator.get(TranslationKey.MESSAGES__TIME__MINUTES));
        return Translator.get(TranslationKey.MESSAGES__LOCATION__JUST_NOW);
    }

    /**
     * Reads the contents of the container block at this location and returns
     * a formatted list of lore lines detailing its contents.
     */
    public @NotNull List<String> getContentsLore() {
        try {
            Location loc = this.get();
            if (loc.getWorld() == null) return List.of();
            org.bukkit.block.BlockState state = loc.getBlock().getState();

            if (state instanceof org.bukkit.block.Jukebox jukebox) {
                org.bukkit.inventory.ItemStack record = jukebox.getRecord();
                if (record != null && !record.getType().isAir()) {
                    String typeName = toHumanReadable(record.getType()).substring(2);
                    return List.of(Translator.get(TranslationKey.INVENTORIES__STATS__CONTENTS_ITEM_FORMAT)
                        .replace("{count}", "1")
                        .replace("{item}", typeName));
                } else {
                    return List.of(Translator.get(TranslationKey.MESSAGES__LOCATION__EMPTY_CONTENTS));
                }
            }

            if (state instanceof org.bukkit.inventory.InventoryHolder holder) {
                org.bukkit.inventory.Inventory inv = holder.getInventory();
                org.bukkit.inventory.ItemStack[] contents = inv.getContents();
                Map<Material, Integer> itemCounts = new LinkedHashMap<>();
                for (org.bukkit.inventory.ItemStack stack : contents) {
                    if (stack != null && !stack.getType().isAir()) {
                        itemCounts.put(stack.getType(), itemCounts.getOrDefault(stack.getType(), 0) + stack.getAmount());
                    }
                }
                if (itemCounts.isEmpty()) {
                    return List.of(Translator.get(TranslationKey.MESSAGES__LOCATION__EMPTY_CONTENTS));
                }
                List<String> list = new ArrayList<>();
                int count = 0;
                for (Map.Entry<Material, Integer> e : itemCounts.entrySet()) {
                    if (count >= 5) {
                        list.add(Translator.get(TranslationKey.INVENTORIES__STATS__CONTENTS_MORE_FORMAT)
                            .replace("{count}", String.valueOf(itemCounts.size() - count)));
                        break;
                    }
                    String typeName = toHumanReadable(e.getKey()).substring(2);
                    list.add(Translator.get(TranslationKey.INVENTORIES__STATS__CONTENTS_ITEM_FORMAT)
                        .replace("{count}", String.valueOf(e.getValue()))
                        .replace("{item}", typeName));
                    count++;
                }
                return list;
            }
        } catch (Exception ignored) {}
        return List.of();
    }
}