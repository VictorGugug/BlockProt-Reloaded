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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Displays plugin version, maintainer info, and issue tracker link.
 */
public class AboutCommand implements CommandExecutor {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String version = BlockProt.getPluginVersion();

        Component title = LEGACY.deserialize(
            Translator.get(TranslationKey.ABOUT__TITLE).replace("{version}", "v" + version));
        Component maintainers = LEGACY.deserialize(
            Translator.get(TranslationKey.ABOUT__FORK_MAINTAINERS));

        String reportText = Translator.get(TranslationKey.ABOUT__REPORT_FORK);
        String reportHover = Translator.get(TranslationKey.ABOUT__REPORT_FORK_HOVER);
        Component reportLink = LEGACY.deserialize(reportText)
            .clickEvent(ClickEvent.openUrl("https://github.com/VictorGugug/BlockProt-Reloaded/issues"))
            .hoverEvent(HoverEvent.showText(LEGACY.deserialize(reportHover)));

        sender.sendMessage(title);
        sender.sendMessage(maintainers);
        sender.sendMessage(reportLink);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }
}