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
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
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
import java.util.Locale;
import java.util.Set;

/**
 * Applies the recommended starting configuration in two independent halves.
 *
 * <p>{@code /bp recommended blocks} only writes {@code blocks.yml} (lockable
 * lists and auto-drop) and remembers that with the {@code recommended_blocks_applied}
 * key stored inside {@code blocks.yml} itself. {@code /bp recommended config} only
 * writes {@code config.yml} (the recommended settings that are not enabled by
 * default: {@code modern_family_blocks}, {@code use_menus}, {@code use_dialogs})
 * and remembers that with {@code recommended_config_applied} in {@code config.yml}.
 * {@code /bp recommended all} applies both halves in sequence.
 *
 * <p>Each half can only be applied once; passing {@code force} as the third argument
 * bypasses that guard so the half can be re-applied.
 */
public class RecommendedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONSOLE_ONLY));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE));
            return true;
        }

        String target = args[1].toLowerCase(Locale.ROOT);
        if (!target.equals("blocks") && !target.equals("config") && !target.equals("all")) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE));
            return true;
        }

        boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
        if (args.length > 3 || (args.length == 3 && !force)) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_USAGE));
            return true;
        }

        if (target.equals("blocks")) {
            return applyBlocks(sender, force);
        }
        if (target.equals("config")) {
            return applyConfig(sender, force);
        }
        applyBlocks(sender, force);
        return applyConfig(sender, force);
    }

    private boolean applyBlocks(@NotNull CommandSender sender, boolean force) {
        BlockProt plugin = BlockProt.getInstance();
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");

        if (!blocksFile.exists()) {
            sender.sendMessage(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_MISSING));
            return true;
        }

        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(blocksFile);

            if (cfg.getBoolean("recommended_blocks_applied", false) && !force) {
                String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_ALREADY_APPLIED)
                    .replace("{target}", "blocks");
                sender.sendMessage(message);
                BlockProtLogger.log("recommended", message);
                return true;
            }

            cfg.set("lockable_tile_entities", List.of("[*-CHEST *-FURNACE *-TRANSPORT *-MISC *-SHELF *-SIGN]"));
            cfg.set("lockable_shulker_boxes", List.of("[*]"));
            cfg.set("lockable_blocks", List.of("[*-ANVIL *-CAULDRON *-WORKSTATION *-TRAPDOOR *-FENCE_GATE *-BED]"));
            cfg.set("lockable_doors", List.of("[*]"));
            cfg.set("lockable_entities", List.of("[*-ITEM_FRAMES]"));
            cfg.set("auto_drop_to_inventory.enabled", true);
            Set<org.bukkit.Material> allShulkers = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.SHULKER_BOXES);
            if (!allShulkers.isEmpty()) {
                cfg.set("auto_drop_to_inventory.blocks", List.of("[*-SHULKERS]"));
            } else {
                cfg.set("auto_drop_to_inventory.blocks", List.of());
            }
            cfg.set("recommended_blocks_applied", true);

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().suppressPath("blocks.yml");
            }
            cfg.save(blocksFile);
            DefaultConfig.prependBlocksHeader(blocksFile);

            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_DONE));
            BlockProtLogger.log("recommended", Translator.get(TranslationKey.CONSOLE__RECOMMENDED_BLOCKS_DONE));
            BlockProtLogger.log("recommended", Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));

            if (plugin.getFileWatcher() != null) {
                plugin.getFileWatcher().requestProgrammaticReload();
            }
        } catch (IOException e) {
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_FAILED)
                .replace("{file}", "blocks.yml"));
            BlockProtLogger.log("recommended", Translator.get(TranslationKey.CONSOLE__RECOMMENDED_FAILED)
                .replace("{file}", "blocks.yml")
                .replace("{error}", e.getMessage()));
        }

        return true;
    }

    private boolean applyConfig(@NotNull CommandSender sender, boolean force) {
        DefaultConfig defaultConfig = BlockProt.getDefaultConfig();

        if (defaultConfig.getBukkitConfig().getBoolean("recommended_config_applied", false) && !force) {
            String message = Translator.get(TranslationKey.CONSOLE__RECOMMENDED_ALREADY_APPLIED)
                .replace("{target}", "config");
            sender.sendMessage(message);
            BlockProtLogger.log("recommended", message);
            return true;
        }

        defaultConfig.setAndSave("modern_family_blocks", true);
        defaultConfig.setAndSave("use_menus", true);
        defaultConfig.setAndSave("use_dialogs", true);
        defaultConfig.setAndSave("recommended_config_applied", true);

        BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONFIG_DONE));
        BlockProtLogger.log("recommended", Translator.get(TranslationKey.CONSOLE__RECOMMENDED_CONFIG_DONE));
        BlockProtLogger.log("recommended", Translator.get(TranslationKey.CONSOLE__RECOMMENDED_RELOAD));

        return true;
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender instanceof ConsoleCommandSender;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 2) {
            return List.of("blocks", "config", "all");
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("blocks") || args[1].equalsIgnoreCase("config") || args[1].equalsIgnoreCase("all"))) {
            return List.of("force");
        }
        return Collections.emptyList();
    }
}
