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
import de.sean.blockprot.bukkit.util.ComponentMessages;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminTierSelectDialog {

    private AdminTierSelectDialog() {}

    public static void show(@NotNull Player player, @NotNull String targetName,
                            @NotNull UUID targetUuid, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        AdminTier currentTier = AdminTierManager.getRole(targetUuid);

        Player onlineTarget = org.bukkit.Bukkit.getPlayer(targetUuid);
        boolean isOp = onlineTarget != null ? onlineTarget.isOp() : org.bukkit.Bukkit.getOfflinePlayer(targetUuid).isOp();

        String titleText = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__ASSIGN_TITLE))
            .replace("{player}", targetName);
        Component title = Component.text(titleText, PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__ASSIGN_PROMPT))
                .replace("{player}", targetName), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CURRENT_TIER))
                .replace("{tier}", currentTier.getIdentifier()), TextColor.color(0x888888))));
        if (isOp) {
            body.add(DialogBodyEntry.text(Component.text(
                "● " + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__STATUS_OP)), PASTEL_CORAL, TextDecoration.BOLD)));
        } else {
            body.add(DialogBodyEntry.text(Component.text(
                "○ " + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__STATUS_NOT_OP)), TextColor.color(0x888888))));
        }

        List<DialogButton> actions = new ArrayList<>();

        actions.add(roleButton("owner", targetName, targetUuid, backOrigin, AdminTier.OWNER,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_OWNER,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_OWNER_LORE,
            PASTEL_CORAL, currentTier == AdminTier.OWNER));

        actions.add(roleButton("t3", targetName, targetUuid, backOrigin, AdminTier.T3,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T3,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T3_LORE,
            PASTEL_GOLD, currentTier == AdminTier.T3));

        actions.add(roleButton("t2", targetName, targetUuid, backOrigin, AdminTier.T2,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T2,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T2_LORE,
            PASTEL_MINT, currentTier == AdminTier.T2));

        actions.add(roleButton("t1", targetName, targetUuid, backOrigin, AdminTier.T1,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T1,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_T1_LORE,
            SOFT_BLUE, currentTier == AdminTier.T1));

        actions.add(new DialogButton("custom",
            Component.text((currentTier == AdminTier.CUSTOM ? "● " : "○ ")
                + stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_CUSTOM)),
                currentTier == AdminTier.CUSTOM ? PASTEL_PURPLE : NamedTextColor.WHITE),
            Component.join(
                JoinConfiguration.newlines(),
                Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_CUSTOM_LORE)), PASTEL_PURPLE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
            ),
            p -> {
                AdminTierManager.setPlayerRole(targetUuid, AdminTier.CUSTOM, AdminTierManager.getCustomFlags(targetUuid));
                ComponentMessages.sendLegacy(p, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
                    .replace("{player}", targetName)
                    .replace("{role}", AdminTier.CUSTOM.getIdentifier()));
                AdminCustomFlagsDialog.show(p, targetName, targetUuid, backOrigin);
            }
        ));

        actions.add(new DialogButton("custom_config",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CUSTOM_CONFIG_BTN)), PASTEL_PURPLE),
            Component.join(
                JoinConfiguration.newlines(),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_TIERS__CUSTOM_CONFIG_TOOLTIP))
                    .replace("{player}", targetName), PASTEL_PURPLE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
            ),
            p -> AdminCustomFlagsDialog.show(p, targetName, targetUuid, backOrigin)
        ));

        actions.add(roleButton("none", targetName, targetUuid, backOrigin, AdminTier.NONE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_NONE,
            TranslationKey.INVENTORIES__ADMIN_TIERS__ROLE_NONE_LORE,
            SOFT_GRAY, currentTier == AdminTier.NONE));

        DialogButton exitBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> AdminTiersDialog.show(p, backOrigin)
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn, 1);
    }

    private static DialogButton roleButton(@NotNull String id, @NotNull String targetName,
                                           @NotNull UUID targetUuid, @NotNull DialogOrigin backOrigin,
                                           @NotNull AdminTier tier,
                                           @NotNull TranslationKey labelKey,
                                           @NotNull TranslationKey loreKey,
                                           @NotNull TextColor color,
                                           boolean active) {
        String label = stripColor(Translator.get(labelKey));
        String marker = active ? "● " : "○ ";
        return new DialogButton(id,
            Component.text(marker + label, active ? color : NamedTextColor.WHITE),
            Component.join(
                JoinConfiguration.newlines(),
                Component.text(stripColor(Translator.get(loreKey)), color),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLICK_TO_OPEN)), TextColor.color(0x888888))
            ),
            p -> {
                AdminTierManager.setPlayerRole(targetUuid, tier, null);
                ComponentMessages.sendLegacy(p, Translator.get(TranslationKey.MESSAGES__ADMIN_SETROLE_SUCCESS)
                    .replace("{player}", targetName)
                    .replace("{role}", tier.getIdentifier()));
                AdminTiersDialog.show(p, backOrigin);
            }
        );
    }
}
