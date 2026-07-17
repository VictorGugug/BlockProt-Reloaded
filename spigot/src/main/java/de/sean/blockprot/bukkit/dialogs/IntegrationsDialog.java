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

import de.sean.blockprot.bukkit.BlockProtAPI;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class IntegrationsDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);

    private IntegrationsDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<PluginIntegration> allIntegrations = BlockProtAPI.getInstance().getIntegrations();
        int enabledCount = (int) allIntegrations.stream().filter(PluginIntegration::isEnabled).count();

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__INTEGRATIONS__COUNT))
                .replace("{enabled}", String.valueOf(enabledCount))
                .replace("{total}", String.valueOf(allIntegrations.size())),
            TextColor.color(0x888888))));
        body.add(DialogBodyEntry.text(Component.empty()));

        List<DialogButton> buttons = new ArrayList<>();
        for (PluginIntegration integration : allIntegrations) {
            boolean enabled = integration.isEnabled();
            TextColor c = enabled ? PASTEL_MINT : PASTEL_CORAL;
            buttons.add(new DialogButton("int_" + integration.name,
                Component.text()
                    .append(Component.text(enabled ? "● " : "○ ", c))
                    .append(Component.text(integration.name, NamedTextColor.WHITE))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__INTEGRATIONS__PLUGIN))
                        .replace("{name}", integration.name), SOFT_GRAY),
                    Component.text(stripColor(Translator.get(enabled
                        ? TranslationKey.DIALOGS__STATUS_ENABLED
                        : TranslationKey.DIALOGS__STATUS_DISABLED)), c),
                    Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__INTEGRATIONS__DEPENDENT))
                        .replace("{name}", integration.name), TextColor.color(0x888888))),
                null
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            backHint(exitOrigin),
            backAction(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }

    private static Component backHint(DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888));
            default: return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888));
        }
    }

    private static DialogButton.DialogClickHandler backAction(Player player, DialogOrigin origin) {
        switch (origin) {
            case ADMIN_MENU: return p -> AdminMenuDialog.show(p);
            default: return p -> {};
        }
    }
}
