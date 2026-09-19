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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WorldExpiryDialog {

    private static final int PER_PAGE = 6;

    private WorldExpiryDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        List<World> worlds = Bukkit.getWorlds().stream()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .toList();

        int totalPages = Math.max(1, (int) Math.ceil(worlds.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, worlds.size());
        List<World> pageWorlds = worlds.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__WORLD_EXPIRY__HEADER))
                + " | " + stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
            SOFT_GRAY
        )));

        Map<String, String> worldDurations = cfg.getWorldExpiryDurations();
        List<DialogButton> buttons = new ArrayList<>();

        for (World world : pageWorlds) {
            String duration = worldDurations.getOrDefault(world.getName(), "0");
            boolean active = !"0".equals(duration) && !"-1".equals(duration);
            TextColor statusColor = active ? PASTEL_MINT : PASTEL_CORAL;
            String statusText = active
                ? stripColor(Translator.get(TranslationKey.DIALOGS__WORLD_EXPIRY__ACTIVE_LABEL)).replace("{duration}", duration)
                : stripColor(Translator.get(TranslationKey.DIALOGS__WORLD_EXPIRY__DISABLED_LABEL));

            buttons.add(new DialogButton("world_" + world.getName(),
                Component.text()
                    .append(Component.text(active ? "[+]" : "[-]", statusColor))
                    .append(Component.text(" " + world.getName(), NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(world.getEnvironment().name(), SOFT_BLUE),
                    Component.text(statusText, statusColor),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__WORLD_EXPIRY__CLICK_HINT)), TextColor.color(0x888888))
                ),
                p -> AdminConfigValueDialog.openText(
                    p,
                    "world_expiry.worlds." + world.getName(),
                    stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__USAGE)),
                    active ? duration : "0",
                    input -> {
                        if (input == null || input.isBlank()) return null;
                        String trimmed = input.trim();
                        if (trimmed.equals("0") || trimmed.equals("-1")) return null;
                        if (trimmed.matches("\\d+[smhd]")) return null;
                        return stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__ERROR)).replace("{input}", trimmed);
                    },
                    validInput -> {
                        String trimmed = validInput.trim();
                        if (trimmed.equals("0") || trimmed.equals("-1")) {
                            cfg.setWorldExpiryDuration(world.getName(), "0");
                            p.sendMessage(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__CLEARED).replace("{world}", world.getName()));
                        } else {
                            cfg.setWorldExpiryDuration(world.getName(), trimmed);
                            p.sendMessage(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__SET).replace("{world}", world.getName()).replace("{duration}", trimmed));
                        }
                        show(p, backOrigin, safePage);
                    },
                    () -> show(p, backOrigin, safePage)
                )
            ));
        }

        BpDialogStyles.padToGrid(buttons, 6);

        if (totalPages > 1) {
            DialogButton prevBtn = safePage > 0
                ? new DialogButton("prev",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, safePage - 1))
                : new DialogButton("prev_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            DialogButton refreshBtn = new DialogButton("refresh",
                Component.text(stripColor(Translator.get(TranslationKey.ICON__RELOAD)), SOFT_GRAY),
                Component.text(""),
                p -> show(p, backOrigin, safePage)
            );

            DialogButton nextBtn = safePage + 1 < totalPages
                ? new DialogButton("next",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                    p -> show(p, backOrigin, safePage + 1))
                : new DialogButton("next_disabled",
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), TextColor.color(0x555555)),
                    Component.text(""),
                    p -> {});

            buttons.add(prevBtn);
            buttons.add(refreshBtn);
            buttons.add(nextBtn);
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : p -> {
                if (backOrigin == DialogOrigin.ADMIN_CONFIG_EXPIRY) {
                    AdminConfigExpiryDialog.show(p, DialogOrigin.ADMIN_MENU);
                } else {
                    AdminMenuDialog.show(p);
                }
            }
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }
}
