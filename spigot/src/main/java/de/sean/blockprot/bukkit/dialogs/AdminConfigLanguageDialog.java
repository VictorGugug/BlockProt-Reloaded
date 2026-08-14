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
import de.sean.blockprot.bukkit.config.LangConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Language category of the dialog-based admin config editor: language
 * toggles, active language selection, and the translation fallback string.
 */
public final class AdminConfigLanguageDialog {

    private AdminConfigLanguageDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE)),
            AdminConfigDialog.PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__STATUS_HEADER)), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(new DialogButton("toggle_languages",
            Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_CATEGORY)), NamedTextColor.WHITE),
            Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_HINT)), TextColor.color(0x888888)),
            p -> showToggle(p, backOrigin)));

        buttons.add(new DialogButton("select_language",
            Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_CATEGORY)), NamedTextColor.WHITE),
            Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_HINT)), TextColor.color(0x888888)),
            p -> showSelector(p, backOrigin)));

        buttons.add(AdminConfigDialog.toggleBtn("replace_translations", "replace_translations",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__REPLACE_TRANSLATIONS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__REPLACE_TRANSLATIONS),
            BlockProt.getDefaultConfig().shouldReplaceTranslations(),
            p -> { BlockProt.getDefaultConfig().setAndSave("replace_translations", !BlockProt.getDefaultConfig().shouldReplaceTranslations()); show(p, backOrigin); }));

        String fallback = BlockProt.getDefaultConfig().getTranslationFallbackString();
        buttons.add(AdminConfigDialog.valueBtn("fallback_string", "fallback_string",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING),
            fallback != null ? fallback : "",
            p -> AdminConfigValueDialog.openText(p, "fallback_string",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING_HINT)),
                fallback != null ? fallback : "",
                raw -> null,
                v -> { BlockProt.getDefaultConfig().setAndSave("fallback_string", v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    private static void showToggle(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_CATEGORY)),
            AdminConfigDialog.PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_HINT)), AdminConfigDialog.SOFT_GRAY)));

        String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

        List<DialogButton> buttons = new ArrayList<>();

        boolean anyDisabled = false;
        for (String lang : allLangs) {
            if (!LangConfig.isLanguageEnabled(lang)) {
                anyDisabled = true;
                break;
            }
        }
        boolean allEnabled = !anyDisabled;
        String toggleLabel = allEnabled
            ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_DISABLE))
            : AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_ENABLE));
        buttons.add(new DialogButton("toggle_all",
            Component.text()
                .append(Component.text(AdminConfigDialog.stripColor(Translator.get(allEnabled ? TranslationKey.ICON__TOGGLE_OFF : TranslationKey.ICON__TOGGLE_ON)), allEnabled ? AdminConfigDialog.PASTEL_CORAL : AdminConfigDialog.PASTEL_MINT))
                .append(Component.text(toggleLabel, NamedTextColor.WHITE))
                .build(),
            Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_HINT)), TextColor.color(0x888888)),
            p -> {
                for (String lang : allLangs) {
                    LangConfig.setLanguageEnabled(lang, !allEnabled);
                }
                showToggle(p, backOrigin);
            }));

        BlockProt plugin = BlockProt.getInstance();
        for (String lang : allLangs) {
            boolean isEnabled = LangConfig.isLanguageEnabled(lang);
            String label = getLanguageLabel(plugin, lang);

            TextColor c = isEnabled ? NamedTextColor.WHITE : AdminConfigDialog.SOFT_GRAY;
            String langStatus = isEnabled
                ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_ENABLED))
                : AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_DISABLED));
            String clickAction = isEnabled
                ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_DISABLE))
                : AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_ENABLE));
            buttons.add(new DialogButton("lang_" + lang,
                Component.text()
                    .append(Component.text(AdminConfigDialog.stripColor(Translator.get(isEnabled ? TranslationKey.ICON__TOGGLE_ON : TranslationKey.ICON__TOGGLE_OFF)), c))
                    .append(Component.text(label, c))
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(AdminConfigDialog.stripColor(Translator.get(
                        TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__LANG_STATUS))
                        .replace("{status}", langStatus), c),
                    Component.text(clickAction, TextColor.color(0x888888))),
                p -> {
                    LangConfig.setLanguageEnabled(lang, !isEnabled);
                    showToggle(p, backOrigin);
                }));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(AdminConfigDialog.stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), AdminConfigDialog.SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ?
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : p -> show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static void showSelector(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_CATEGORY)),
            AdminConfigDialog.SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_HINT)), AdminConfigDialog.SOFT_GRAY)));

        String currentLang = cfg.getLanguageFile();
        String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

        List<DialogButton> buttons = new ArrayList<>();

        BlockProt plugin = BlockProt.getInstance();
        for (String lang : allLangs) {
            boolean isConfigLang = lang.equals(currentLang);
            boolean isEnabled = LangConfig.isLanguageEnabled(lang);
            String label = getLanguageLabel(plugin, lang);

            TextColor c = isConfigLang ? AdminConfigDialog.PASTEL_GOLD : (isEnabled ? NamedTextColor.WHITE : AdminConfigDialog.SOFT_GRAY);
            Component labelComp = isConfigLang
                ? Component.text().append(Component.text(label, NamedTextColor.WHITE))
                    .append(Component.text(" "
                        + AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__ACTIVE_MARKER)),
                        AdminConfigDialog.PASTEL_GOLD)).build()
                : Component.text(label, isEnabled ? NamedTextColor.WHITE : AdminConfigDialog.SOFT_GRAY);
            String configStatus = isConfigLang
                ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_ACTIVE))
                : AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_INACTIVE));
            String clickAction = isConfigLang
                ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CURRENT))
                : (isEnabled ? AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_ENABLE))
                    : AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__LANG_STATUS)).replace("{status}",
                        AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__STATUS_DISABLED))));
            buttons.add(new DialogButton("lang_" + lang,
                labelComp,
                Component.join(JoinConfiguration.newlines(),
                    Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CONFIG_YML_PREFIX)) + configStatus, isConfigLang ? AdminConfigDialog.PASTEL_GOLD : AdminConfigDialog.SOFT_GRAY),
                    Component.text(clickAction, TextColor.color(0x888888))),
                p -> {
                    if (!isConfigLang && isEnabled) {
                        cfg.setLanguageFile(lang);
                    }
                    showSelector(p, backOrigin);
                }));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(AdminConfigDialog.stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), AdminConfigDialog.SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ?
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                Component.text(AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? null : p -> show(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static @Nullable YamlConfiguration loadLanguageFile(@NotNull BlockProt plugin, @NotNull String fileName) {
        File diskFile = new File(plugin.getDataFolder(), "lang/" + fileName);
        try {
            if (diskFile.exists()) {
                return YamlConfiguration.loadConfiguration(diskFile);
            }
            InputStream jarStream = plugin.getResource("lang/" + fileName);
            if (jarStream == null) return null;
            return YamlConfiguration.loadConfiguration(
                new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getLanguageName(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return fileName;
        String name = langFile.getString("language_name");
        if (name != null && !name.isEmpty()) return name;
        return langFile.getString("locale", fileName);
    }

    private static int computeCompletion(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return 0;
        return Translator.computeCompletionPercentage(langFile);
    }

    private static String getLanguageLabel(@NotNull BlockProt plugin, @NotNull String fileName) {
        return getLanguageName(plugin, fileName) + " (" + computeCompletion(plugin, fileName) + "%)";
    }
}