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
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminTiersDialog {

    private static final int PER_PAGE = 6;

    private record PlayerEntry(@NotNull String name, @NotNull UUID uuid, @NotNull AdminTier tier, boolean isOp) {}

    private AdminTiersDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        show(player, backOrigin, 0);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin, int page) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        List<PlayerEntry> entries = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            AdminTier tier = AdminTierManager.getPlayerTier(online);
            entries.add(new PlayerEntry(online.getName(), online.getUniqueId(), tier, online.isOp()));
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, entries.size());
        List<PlayerEntry> pageEntries = entries.subList(from, to);

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__SUBTITLE)), TextColor.color(0x888888))));

        if (totalPages > 1) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__PAGE))
                    .replace("{current}", String.valueOf(safePage + 1))
                    .replace("{total}", String.valueOf(totalPages)),
                TextColor.color(0x888888))));
        }

        List<DialogButton> actions = new ArrayList<>();

        DialogButton setOfflineBtn = new DialogButton("set_offline",
            Component.text(stripColor(Translator.get(TranslationKey.ICON__SEARCH))
                + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__SET_OFFLINE_BTN)), NamedTextColor.WHITE),
            Component.join(
                JoinConfiguration.newlines(),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__SET_OFFLINE_TOOLTIP)), PASTEL_MINT),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
            ),
            p -> AdminConfigValueDialog.openText(
                p,
                "tier_player_name",
                stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__SET_OFFLINE_TOOLTIP)),
                "",
                input -> (input == null || input.isBlank()) ? "Player name required" : null,
                inputName -> {
                    String clean = inputName.trim();
                    UUID targetUuid = null;
                    Player online = Bukkit.getPlayer(clean);
                    if (online != null) {
                        targetUuid = online.getUniqueId();
                    }
                    if (targetUuid == null) {
                        try {
                            targetUuid = UUID.fromString(clean);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (targetUuid == null) {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer op = Bukkit.getOfflinePlayer(clean);
                        if (op.hasPlayedBefore() || op.isOnline()) {
                            targetUuid = op.getUniqueId();
                        }
                    }
                    if (targetUuid == null) {
                        targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + clean).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    AdminTierSelectDialog.show(p, clean, targetUuid, backOrigin);
                },
                () -> show(p, backOrigin, safePage)
            )
        );
        actions.add(setOfflineBtn);

        int idx = 0;
        for (PlayerEntry entry : pageEntries) {
            String roleStr = entry.tier.getIdentifier().toUpperCase(Locale.ROOT);
            TextColor accent = getTierColor(entry.tier);
            String btnId = "player_" + safePage + "_" + (idx++);
            String label = entry.name + (entry.isOp ? " [OP]" : "") + " [" + roleStr + "]";
            Component tooltip = Component.join(
                JoinConfiguration.newlines(),
                entry.isOp
                    ? Component.text("● " + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__STATUS_OP)), PASTEL_CORAL)
                    : Component.text("○ " + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__STATUS_NOT_OP)), TextColor.color(0x888888)),
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CURRENT_ROLE))
                    .replace("{role}", entry.tier.getIdentifier()), accent),
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__CLICK_TO_ASSIGN)), TextColor.color(0x888888))
            );
            actions.add(new DialogButton(btnId,
                Component.text(label, NamedTextColor.WHITE),
                tooltip,
                p -> AdminTierSelectDialog.show(p, entry.name, entry.uuid, backOrigin)
            ));
        }

        if (safePage > 0) {
            actions.add(new DialogButton("prev",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV)), NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PREV_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, safePage - 1)
            ));
        }

        if (safePage < totalPages - 1) {
            actions.add(new DialogButton("next",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT)), NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__NEXT_HINT)), TextColor.color(0x888888)),
                p -> show(p, backOrigin, safePage + 1)
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            AdminMenuDialog.originHint(exitOrigin),
            AdminMenuDialog.originBack(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn, 2);
    }

    private static TextColor getTierColor(@NotNull AdminTier tier) {
        return switch (tier) {
            case OWNER -> PASTEL_CORAL;
            case T3 -> PASTEL_GOLD;
            case T2 -> PASTEL_MINT;
            case T1 -> SOFT_BLUE;
            case CUSTOM -> PASTEL_PURPLE;
            default -> SOFT_GRAY;
        };
    }
}
