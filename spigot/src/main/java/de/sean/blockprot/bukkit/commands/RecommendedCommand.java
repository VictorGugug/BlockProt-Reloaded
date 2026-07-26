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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RecommendedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONSOLE_ONLY));
            return true;
        }

        BlockProt plugin = BlockProt.getInstance();
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        if (!blocksFile.exists()) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_MISSING));
            return true;
        }

        if (!args[0].equals("recommended")) {
            return false;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(blocksFile);

            boolean modern = plugin.getConfig().getBoolean("modern_family_blocks", false);

            if (modern) {
                cfg.set("lockable_tile_entities", List.of("[*-CHEST *-FURNACE *-TRANSPORT *-MISC *-SHELF *-SIGN]"));
                cfg.set("lockable_shulker_boxes", List.of("[*]"));
                cfg.set("lockable_blocks", List.of("[*-ANVIL *-CAULDRON *-WORKSTATION *-TRAPDOOR *-FENCE_GATE]"));
                cfg.set("lockable_doors", List.of("[*]"));
                cfg.set("lockable_entities", List.of("[*-ITEM_FRAMES]"));
                cfg.set("auto_drop_to_inventory.enabled", true);
                Set<org.bukkit.Material> allShulkers = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.SHULKER_BOXES);
                if (!allShulkers.isEmpty()) {
                    cfg.set("auto_drop_to_inventory.blocks", List.of("[*-SHULKERS]"));
                } else {
                    cfg.set("auto_drop_to_inventory.blocks", List.of());
                }
            } else {
                cfg.set("lockable_tile_entities", List.of(
                    "CHEST", "TRAPPED_CHEST", "FURNACE", "SMOKER", "BLAST_FURNACE",
                    "HOPPER", "DISPENSER", "DROPPER", "BARREL", "BREWING_STAND",
                    "LECTERN", "DECORATED_POT", "CHISELED_BOOKSHELF", "CRAFTER",
                    "BEEHIVE", "BEE_NEST", "JUKEBOX", "BEACON"));
                cfg.set("lockable_shulker_boxes", List.of(
                    "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX",
                    "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX",
                    "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX",
                    "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX",
                    "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX",
                    "RED_SHULKER_BOX", "BLACK_SHULKER_BOX"));
                cfg.set("lockable_blocks", List.of(
                    "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL",
                    "CAULDRON", "WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON",
                    "GRINDSTONE", "STONECUTTER", "LOOM", "CARTOGRAPHY_TABLE",
                    "SMITHING_TABLE", "ENCHANTING_TABLE", "FLETCHING_TABLE",
                    "DRAGON_EGG", "COMPOSTER", "BELL", "NOTE_BLOCK",
                    "WHITE_BED", "ORANGE_BED", "MAGENTA_BED", "LIGHT_BLUE_BED",
                    "YELLOW_BED", "LIME_BED", "PINK_BED", "GRAY_BED",
                    "LIGHT_GRAY_BED", "CYAN_BED", "PURPLE_BED", "BLUE_BED",
                    "BROWN_BED", "GREEN_BED", "RED_BED", "BLACK_BED"));
                cfg.set("lockable_doors", List.of(
                    "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR",
                    "ACACIA_DOOR", "DARK_OAK_DOOR", "MANGROVE_DOOR", "CHERRY_DOOR",
                    "BAMBOO_DOOR", "CRIMSON_DOOR", "WARPED_DOOR", "IRON_DOOR"));
                cfg.set("lockable_entities", List.of("ITEM_FRAME", "GLOW_ITEM_FRAME"));
                cfg.set("auto_drop_to_inventory.enabled", true);
                cfg.set("auto_drop_to_inventory.blocks", List.of());
            }

            cfg.save(blocksFile);
            DefaultConfig.prependBlocksHeader(blocksFile);
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_DONE));
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));
        } catch (IOException e) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_FAILED)
                .replace("{error}", e.getMessage()));
        }

        return true;
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender instanceof ConsoleCommandSender;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("recommended");
        return Collections.emptyList();
    }
}