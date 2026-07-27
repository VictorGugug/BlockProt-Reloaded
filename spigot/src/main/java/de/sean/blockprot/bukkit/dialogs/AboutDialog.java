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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AboutDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private AboutDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.USER_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        String version = BlockProt.getPluginVersion();
        String serverVersion = Bukkit.getVersion();
        String bukkitVersion = Bukkit.getBukkitVersion();
        String javaVersion = System.getProperty("java.version");

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__TITLE)),
            PASTEL_MINT, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__DESCRIPTION)), SOFT_GRAY))
            .append(Component.text(" BlockProt", NamedTextColor.WHITE, TextDecoration.BOLD))
            .build()));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__MAINTAINER)),
            TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.empty()));

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__VERSION_LABEL)), SOFT_GRAY))
            .append(Component.text(version, PASTEL_GOLD, TextDecoration.BOLD))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__SERVER_LABEL)), SOFT_GRAY))
            .append(Component.text(serverVersion, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__API_LABEL)), SOFT_GRAY))
            .append(Component.text(bukkitVersion, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__JAVA_LABEL)), SOFT_GRAY))
            .append(Component.text(javaVersion, TextColor.color(0x888888)))
            .build()));

        boolean dbActive = BlockProt.getHybridDatabase() != null && BlockProt.getHybridDatabase().isEnabled();
        String dbMode = dbActive ? "MySQL" : "SQLite";
        int cacheCount = de.sean.blockprot.bukkit.storage.ProtectedBlockCache.size();
        List<String> activeInts = new ArrayList<>();
        List<de.sean.blockprot.bukkit.integrations.PluginIntegration> allInts = BlockProt.getInstance().getIntegrations();
        for (var pi : allInts) {
            if (pi.isEnabled()) activeInts.add(pi.name);
        }
        String intText = activeInts.isEmpty()
            ? "Disabled (0/" + allInts.size() + ")"
            : "Active (" + activeInts.size() + "/" + allInts.size() + ": " + String.join(", ", activeInts) + ")";

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text("Database Engine: ", SOFT_GRAY))
            .append(Component.text(dbMode, PASTEL_MINT))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text("Cache: ", SOFT_GRAY))
            .append(Component.text(cacheCount + " protected blocks", PASTEL_GOLD))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text("Integrations: ", SOFT_GRAY))
            .append(Component.text(intText, activeInts.isEmpty() ? PASTEL_CORAL : PASTEL_MINT))
            .build()));

        body.add(DialogBodyEntry.text(Component.empty()));

        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__REPORT_ISSUES)), SOFT_GRAY))
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ABOUT__ISSUES_URL)),
                SOFT_BLUE, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl("https://github.com/VictorGugug/BlockProt-Reloaded/issues"))
                .hoverEvent(HoverEvent.showText(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), SOFT_GRAY))))
            .build()));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(
                exitOrigin != DialogOrigin.NONE ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, List.of(backBtn), null, 1);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private static Component backHint(DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_USER_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler backAction(Player player, DialogOrigin origin) {
        switch (origin) {
            case USER_MENU: return p -> UserMenuDialog.show(p);
            default: return null;
        }
    }
}
