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

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

/**
 * Represents the default string value and a translated value
 * of it, as loaded through {@link Translator#loadFromConfigs(YamlConfiguration, YamlConfiguration)}.
 *
 * @since 0.4.6
 */
public final class TranslationValue {
    @NotNull
    public static final String UNKNOWN_TRANSLATION = "Unknown Translation";

    @NotNull
    public static final TranslationValue UNKNOWN_TRANSLATION_VALUE = new TranslationValue(UNKNOWN_TRANSLATION);

    @NotNull
    private final String defaultValue;

    @NotNull
    private String translatedValue;

    TranslationValue(@NotNull final String defaultValue) {
        this.defaultValue = defaultValue;
        this.translatedValue = UNKNOWN_TRANSLATION;
    }

    TranslationValue(@NotNull final String defaultValue, @Nullable final String translatedValue) {
        this.defaultValue = defaultValue;
        this.translatedValue = (translatedValue == null)
            ? UNKNOWN_TRANSLATION
            : translatedValue;
    }

    @NotNull
    public String getDefaultValue() {
        return defaultValue;
    }

    @NotNull
    public String getTranslatedValue() {
        return translatedValue;
    }

    void setTranslatedValue(@NotNull final String value) {
        this.translatedValue = value;
    }

    @NotNull
    public String getValue() {
        return (translatedValue.equals(UNKNOWN_TRANSLATION))
            ? getDefaultValue()
            : translatedValue;
    }

    @Override
    @NotNull
    public String toString() {
        return new StringJoiner(
            " | ",
            TranslationValue.class.getSimpleName() + "[",
            "]"
        )
            .add("defaultValue=" + defaultValue)
            .add("translatedValue=" + translatedValue)
            .toString();
    }
}