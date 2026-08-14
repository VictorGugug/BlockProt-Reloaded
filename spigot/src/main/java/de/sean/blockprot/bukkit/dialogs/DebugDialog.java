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
import de.sean.blockprot.bukkit.commands.DebugCommand;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DebugDialog {

    private DebugDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        String version = BlockProt.getPluginVersion();
        String serverVer = Bukkit.getVersion();
        String bukkitVer = Bukkit.getBukkitVersion();
        String javaVer = System.getProperty("java.version");

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__LABEL_PLUGIN)), SOFT_GRAY))
            .append(Component.text(version, NamedTextColor.WHITE))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__LABEL_SERVER)), SOFT_GRAY))
            .append(Component.text(serverVer, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__LABEL_API)), SOFT_GRAY))
            .append(Component.text(bukkitVer, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__LABEL_JAVA)), SOFT_GRAY))
            .append(Component.text(javaVer, TextColor.color(0x888888)))
            .build()));
        body.add(DialogBodyEntry.text(Component.empty()));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.MESSAGES__DEBUG__RESULTS_GO_TO_LOG)),
            TextColor.color(0x888888))));

        DialogButton runBtn = new DialogButton("run_diagnostics",
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG)),
                NamedTextColor.WHITE),
            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG_LORE)),
                TextColor.color(0x888888)),
            p -> {
                p.sendMessage(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__DEBUG__RUN_SEPARATOR)), PASTEL_GOLD));
                Bukkit.getScheduler().runTaskAsynchronously(
                    BlockProt.getInstance(),
                    () -> new DebugCommand().onCommand(
                        p, null, "blockprot", new String[]{"debug", "run"}
                    )
                );
            }
        );

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : (backOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : null)
        );

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(runBtn);
        bridge.showMultiAction(player, title, body, buttons, exitBtn, 1);
    }
}
