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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.PendingConfigChanges;
import de.sean.blockprot.bukkit.config.ReloadCoordinator;
import de.sean.blockprot.bukkit.config.ReloadReport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Review and confirmation dialog for manual configuration reloads.
 */
public final class PendingReloadDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);

    private PendingReloadDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        PendingConfigChanges pending = PendingConfigChanges.getInstance();
        List<PendingConfigChanges.PendingEntry> snapshot = pending.getSortedSnapshot();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__HINT)), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();

        if (snapshot.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__EMPTY)), PASTEL_CORAL)));
        } else {
            String currentFile = "";
            for (PendingConfigChanges.PendingEntry entry : snapshot) {
                if (!entry.getFile().equals(currentFile)) {
                    currentFile = entry.getFile();
                    body.add(DialogBodyEntry.text(Component.text("[" + currentFile + "]", PASTEL_GOLD, TextDecoration.BOLD)));
                }

                Component changeLine = Component.text()
                    .append(Component.text(" • " + entry.getKey() + ": ", NamedTextColor.WHITE))
                    .append(Component.text(String.valueOf(entry.getOriginalValue()), PASTEL_CORAL))
                    .append(Component.text(" -> ", SOFT_GRAY))
                    .append(Component.text(String.valueOf(entry.getRequestedValue()), PASTEL_MINT))
                    .build();
                body.add(DialogBodyEntry.text(changeLine));
            }

            buttons.add(new DialogButton("confirm_reload",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__CONFIRM)), PASTEL_MINT, TextDecoration.BOLD),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__CONFIRM_HINT)), SOFT_GRAY),
                p -> {
                    ReloadReport report = ReloadCoordinator.commitManual(p.getUniqueId());
                    if (report.isSuccess()) {
                        p.sendMessage(Component.text(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__SUCCESS)
                            .replace("{count}", String.valueOf(report.getDiffs().size())), PASTEL_MINT));
                    } else {
                        p.sendMessage(Component.text(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__FAILURE)
                            .replace("{error}", String.valueOf(report.getErrorMessage())), PASTEL_CORAL));
                    }
                    AdminConfigDialog.show(p, backOrigin);
                }));

            buttons.add(new DialogButton("discard_changes",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD)), PASTEL_CORAL),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD_HINT)), SOFT_GRAY),
                p -> showDiscardConfirm(p, backOrigin)));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), SOFT_GRAY),
            p -> AdminConfigDialog.show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static void showDiscardConfirm(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD_CONFIRM_TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD_CONFIRM_HINT)), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(new DialogButton("confirm_discard",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD)), PASTEL_CORAL, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD_HINT)), SOFT_GRAY),
            p -> {
                PendingConfigChanges.getInstance().clear();
                p.sendMessage(Component.text(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__DISCARD), PASTEL_CORAL));
                AdminConfigDialog.show(p, backOrigin);
            }));

        DialogButton backBtn = new DialogButton("cancel_discard",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PENDING_RELOAD__CANCEL)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            p -> show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
