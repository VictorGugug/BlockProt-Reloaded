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

package de.sean.blockprot.bukkit;

import com.google.common.collect.Sets;
import de.sean.blockprot.nbt.LockReturnValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Helper to quickly obtain translations from a config by an enum key.
 *
 * <h3>Color and formatting support</h3>
 * <p>Translation values support two formats:
 * <ul>
 *   <li>Legacy {@code &} color codes (e.g. {@code &6}, {@code &a}, {@code &l})</li>
 *   <li>Adventure <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a>
 *       (e.g. {@code <gold>}, {@code <bold>}, {@code <gradient:red:blue>})</li>
 * </ul>
 *
 * <p>Detection uses a whitelist of known MiniMessage tag names. Placeholder tokens
 * like {@code {player}} or {@code <player>} that appear in usage strings are NOT
 * MiniMessage tags and are never passed to the MiniMessage parser.
 *
 * <p>If the raw string already contains section-symbol ({@code §}) color codes it
 * is returned as-is — no further processing is applied to avoid double-escaping or
 * passing pre-formatted strings to MiniMessage.
 *
 * <h3>BPR self-repair</h3>
 * <p>When a key is missing from the active language file but exists in the bundled
 * English file, the English value is used silently.</p>
 *
 * @since 0.1.10
 */
public final class Translator {

    public static final HashSet<String> DEFAULT_TRANSLATION_FILES = Sets.newHashSet(
        "translations_cs.yml", "translations_de.yml", "translations_en.yml",
        "translations_es.yml", "translations_fr.yml", "translations_it.yml",
        "translations_ja.yml", "translations_ko.yml", "translations_pl.yml",
        "translations_pt-br.yml", "translations_ru.yml", "translations_sk.yml",
        "translations_tr.yml", "translations_zh-CN.yml", "translations_zh-TW.yml"
    );

    @NotNull
    public static final Locale defaultLocale = Locale.UK;

    @NotNull
    private static final HashMap<TranslationKey, TranslationValue> values = new HashMap<>();

    @NotNull
    private static Locale locale = defaultLocale;

    static String DEFAULT_FALLBACK = "";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.legacySection();

    /**
     * Whitelist of known MiniMessage tag names (lowercase).
     * Tokens not in this set are treated as plain text / placeholders, never passed to MM parser.
     */
    private static final Set<String> MM_TAGS = Set.of(
        // Color names
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white",
        // Formatting
        "bold", "italic", "underlined", "strikethrough", "obfuscated", "reset",
        // Special
        "gradient", "rainbow", "hover", "click", "insertion", "font",
        "color", "colour", "transition", "shadow_color",
        // Decorations
        "newline", "lang", "selector", "score", "nbt", "key"
    );

    /**
     * Regex to extract the tag name from a {@code <tagname>} or {@code </tagname>} token.
     * Group 1 is the name, stripped of leading slash and trailing content after colon/space.
     */
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("</?([a-zA-Z_][a-zA-Z0-9_]*)");

    private Translator() {}

    public static void loadFromConfigs(@NotNull final YamlConfiguration defaultConfig,
                                       @NotNull final YamlConfiguration config) {
        String localeStr = config.getString("locale");
        Translator.locale = (localeStr == null || localeStr.isBlank())
            ? Locale.ROOT
            : Locale.forLanguageTag(localeStr.replace('_', '-'));

        final String langFile = BlockProt.getDefaultConfig().getLanguageFile();
        TranslationKey[] translations = TranslationKey.values();

        List<String> missingInBoth   = new ArrayList<>();
        List<String> missingInActive = new ArrayList<>();

        for (TranslationKey translation : translations) {
            String translationKey = translation.toString();

            if (!defaultConfig.contains(translationKey, true)
                    && !config.contains(translationKey, true)) {
                values.put(translation, new TranslationValue(translationKey));
                missingInBoth.add(translationKey);
                continue;
            }

            Object defaultValue = defaultConfig.get(translationKey);
            TranslationValue translatedValue =
                (defaultValue instanceof String)
                    ? new TranslationValue((String) defaultValue)
                    : TranslationValue.UNKNOWN_TRANSLATION_VALUE;

            Object activeValue = config.get(translationKey);
            if (activeValue instanceof String) {
                translatedValue.setTranslatedValue((String) activeValue);
            } else {
                missingInActive.add(translationKey);
            }

            values.put(translation, translatedValue);
        }

        if (!missingInBoth.isEmpty()) {
            BlockProtLogger.log("Translator",
                "Keys missing in EN + " + langFile + " (" + missingInBoth.size() + "): "
                + String.join(", ", missingInBoth));
        }
        if (!missingInActive.isEmpty()) {
            BlockProtLogger.log("Translator",
                "English fallback keys used in " + langFile
                + " (" + missingInActive.size() + "): "
                + String.join(", ", missingInActive));
        }
    }

    private static boolean containsMiniMessage(@NotNull String text) {
        if (text.indexOf('\u00A7') >= 0) return false;

        var matcher = TAG_NAME_PATTERN.matcher(text);
        while (matcher.find()) {
            String tagName = matcher.group(1).toLowerCase(Locale.ROOT);
            if (MM_TAGS.contains(tagName)) return true;
        }
        return false;
    }

    @NotNull
    private static String process(@NotNull String raw) {
        if (raw.indexOf('\u00A7') >= 0) return raw;

        if (containsMiniMessage(raw)) {
            Component component = MINI_MESSAGE.deserialize(raw);
            return LEGACY_SERIALIZER.serialize(component);
        }

        return raw.replace('&', '\u00a7');
    }

    @NotNull
    public static String get(@NotNull final TranslationKey key) {
        TranslationValue value = values.get(key);
        String raw = value == null ? key.toString() : value.getValue();
        return process(raw);
    }

    @NotNull
    public static String get(@NotNull final LockReturnValue.Reason reason) {
        return switch (reason) {
            case NO_PERMISSION -> get(TranslationKey.MESSAGES__NO_PERMISSION);
            case EXCEEDED_MAX_BLOCK_COUNT -> get(TranslationKey.MESSAGES__EXCEEDED_MAX_BLOCK_COUNT);
        };
    }

    @NotNull
    public static Locale getLocale() {
        return locale;
    }

    public static void resetTranslations() {
        values.clear();
    }
}