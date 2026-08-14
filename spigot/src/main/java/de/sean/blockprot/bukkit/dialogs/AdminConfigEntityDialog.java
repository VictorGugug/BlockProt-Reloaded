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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Entity protection category of the dialog-based admin config editor.
 */
public final class AdminConfigEntityDialog {

    private AdminConfigEntityDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY)),
            AdminConfigDialog.PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("entity_protection.enabled", "entity_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED),
            cfg.isEntityProtectionEnabled(),
            p -> { cfg.setEntityProtectionEnabled(!cfg.isEntityProtectionEnabled()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("entity_protection.auto_protect_on_tame", "entity_protection.auto_protect_on_tame",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME),
            cfg.isEntityProtectionAutoProtectOnTame(),
            p -> { cfg.setAndSave("entity_protection.auto_protect_on_tame", !cfg.isEntityProtectionAutoProtectOnTame()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("villager_workstation_protection.enabled", "villager_workstation_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED),
            cfg.isVillagerWorkstationProtectionEnabled(),
            p -> { cfg.setAndSave("villager_workstation_protection.enabled", !cfg.isVillagerWorkstationProtectionEnabled()); show(p, backOrigin); }));

        int workstationRadius = cfg.getBukkitConfig().getInt("villager_workstation_protection.radius", 2);
        buttons.add(AdminConfigDialog.valueBtn("villager_workstation_protection.radius", "villager_workstation_protection.radius",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS),
            String.valueOf(workstationRadius),
            p -> AdminConfigValueDialog.openInt(p, "villager_workstation_protection.radius",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_RADIUS_HINT)), workstationRadius,
                v -> { cfg.setAndSave("villager_workstation_protection.radius", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        int workstationVerticalRadius = cfg.getBukkitConfig().getInt("villager_workstation_protection.vertical_radius", 1);
        buttons.add(AdminConfigDialog.valueBtn("villager_workstation_protection.vertical_radius", "villager_workstation_protection.vertical_radius",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS),
            String.valueOf(workstationVerticalRadius),
            p -> AdminConfigValueDialog.openInt(p, "villager_workstation_protection.vertical_radius",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_VERTICAL_RADIUS_HINT)), workstationVerticalRadius,
                v -> { cfg.setAndSave("villager_workstation_protection.vertical_radius", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        int villagerLocateSeconds = cfg.getBukkitConfig().getInt("entity_protection.villager_locate_seconds", 6);
        buttons.add(AdminConfigDialog.valueBtn("entity_protection.villager_locate_seconds", "entity_protection.villager_locate_seconds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS),
            String.valueOf(villagerLocateSeconds),
            p -> AdminConfigValueDialog.openInt(p, "entity_protection.villager_locate_seconds",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__VILLAGER_LOCATE_SECONDS_HINT)), villagerLocateSeconds,
                v -> { cfg.setAndSave("entity_protection.villager_locate_seconds", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        String menuItem = cfg.getBukkitConfig().getString("entity_protection.menu_item", "STICK");
        buttons.add(AdminConfigDialog.valueBtn("entity_protection.menu_item", "entity_protection.menu_item",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM),
            menuItem != null ? menuItem : "STICK",
            p -> AdminConfigValueDialog.openText(p, "entity_protection.menu_item",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_HINT)),
                menuItem != null ? menuItem : "STICK",
                raw -> Material.matchMaterial(raw) == null
                    ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__MENU_ITEM_INVALID)).replace("{input}", raw)
                    : null,
                v -> { cfg.setAndSave("entity_protection.menu_item", v.toUpperCase(Locale.ROOT)); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}