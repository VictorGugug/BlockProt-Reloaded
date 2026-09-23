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
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminCustomFlagsDialog {

    private static final AdminAction[] ORDERED_ACTIONS = {
        AdminAction.INFO,
        AdminAction.TELEPORT,
        AdminAction.LOGS,
        AdminAction.BREAK,
        AdminAction.UNLOCK,
        AdminAction.LOCKABLES,
        AdminAction.CONTAINER_BYPASS,
        AdminAction.PROTDEL,
        AdminAction.CONFIG,
        AdminAction.DEBUG,
        AdminAction.RELOAD,
        AdminAction.UPDATE,
        AdminAction.INTEGRATIONS,
        AdminAction.RECOMMENDED,
        AdminAction.SETROLE
    };

    private AdminCustomFlagsDialog() {}

    public static void show(@NotNull Player player, @NotNull String targetName,
                            @NotNull UUID targetUuid, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Set<AdminAction> activeFlags = AdminTierManager.getCustomFlags(targetUuid);

        String titleText = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CUSTOM_FLAGS_TITLE))
            .replace("{player}", targetName);
        Component title = Component.text(titleText, PASTEL_PURPLE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CUSTOM_FLAGS_HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CUSTOM_FLAGS_SUBTITLE))
                .replace("{player}", targetName), TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.text(
            "● " + activeFlags.size() + "/" + ORDERED_ACTIONS.length + " active", PASTEL_MINT)));

        List<DialogButton> actions = new ArrayList<>();
        for (AdminAction action : ORDERED_ACTIONS) {
            boolean active = activeFlags.contains(action);
            String actionName = stripColor(Translator.get(getActionNameKey(action)));
            String marker = active ? "● " : "○ ";
            String minTier = action.getMinimumTier().getIdentifier().toUpperCase(Locale.ROOT);
            String status = active
                ? stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__ENABLED))
                : stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__DISABLED));

            actions.add(new DialogButton("flag_" + action.name().toLowerCase(Locale.ROOT),
                Component.text(marker + actionName, active ? PASTEL_MINT : NamedTextColor.WHITE),
                Component.join(
                    JoinConfiguration.newlines(),
                    Component.text(stripColor(Translator.get(getActionDescKey(action))), PASTEL_PURPLE),
                    Component.text(status + " [" + minTier + "]", active ? PASTEL_MINT : TextColor.color(0x888888)),
                    Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_CUSTOM_FLAGS__CLICK_TO_TOGGLE)), TextColor.color(0x888888))
                ),
                p -> {
                    AdminTierManager.toggleCustomFlag(targetUuid, action);
                    show(p, targetName, targetUuid, backOrigin);
                }
            ));
        }

        DialogButton exitBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> AdminTierSelectDialog.show(p, targetName, targetUuid, backOrigin)
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn, 1);
    }

    private static TranslationKey getActionNameKey(@NotNull AdminAction action) {
        return switch (action) {
            case INFO -> TranslationKey.ADMIN_ACTION__INFO__NAME;
            case TELEPORT -> TranslationKey.ADMIN_ACTION__TELEPORT__NAME;
            case LOGS -> TranslationKey.ADMIN_ACTION__LOGS__NAME;
            case BREAK -> TranslationKey.ADMIN_ACTION__BREAK__NAME;
            case UNLOCK -> TranslationKey.ADMIN_ACTION__UNLOCK__NAME;
            case LOCKABLES -> TranslationKey.ADMIN_ACTION__LOCKABLES__NAME;
            case CONTAINER_BYPASS -> TranslationKey.ADMIN_ACTION__CONTAINER_BYPASS__NAME;
            case PROTDEL -> TranslationKey.ADMIN_ACTION__PROTDEL__NAME;
            case CONFIG -> TranslationKey.ADMIN_ACTION__CONFIG__NAME;
            case DEBUG -> TranslationKey.ADMIN_ACTION__DEBUG__NAME;
            case RELOAD -> TranslationKey.ADMIN_ACTION__RELOAD__NAME;
            case UPDATE -> TranslationKey.ADMIN_ACTION__UPDATE__NAME;
            case INTEGRATIONS -> TranslationKey.ADMIN_ACTION__INTEGRATIONS__NAME;
            case RECOMMENDED -> TranslationKey.ADMIN_ACTION__RECOMMENDED__NAME;
            case SETROLE -> TranslationKey.ADMIN_ACTION__SETROLE__NAME;
        };
    }

    private static TranslationKey getActionDescKey(@NotNull AdminAction action) {
        return switch (action) {
            case INFO -> TranslationKey.ADMIN_ACTION__INFO__DESC;
            case TELEPORT -> TranslationKey.ADMIN_ACTION__TELEPORT__DESC;
            case LOGS -> TranslationKey.ADMIN_ACTION__LOGS__DESC;
            case BREAK -> TranslationKey.ADMIN_ACTION__BREAK__DESC;
            case UNLOCK -> TranslationKey.ADMIN_ACTION__UNLOCK__DESC;
            case LOCKABLES -> TranslationKey.ADMIN_ACTION__LOCKABLES__DESC;
            case CONTAINER_BYPASS -> TranslationKey.ADMIN_ACTION__CONTAINER_BYPASS__DESC;
            case PROTDEL -> TranslationKey.ADMIN_ACTION__PROTDEL__DESC;
            case CONFIG -> TranslationKey.ADMIN_ACTION__CONFIG__DESC;
            case DEBUG -> TranslationKey.ADMIN_ACTION__DEBUG__DESC;
            case RELOAD -> TranslationKey.ADMIN_ACTION__RELOAD__DESC;
            case UPDATE -> TranslationKey.ADMIN_ACTION__UPDATE__DESC;
            case INTEGRATIONS -> TranslationKey.ADMIN_ACTION__INTEGRATIONS__DESC;
            case RECOMMENDED -> TranslationKey.ADMIN_ACTION__RECOMMENDED__DESC;
            case SETROLE -> TranslationKey.ADMIN_ACTION__SETROLE__DESC;
        };
    }
}
