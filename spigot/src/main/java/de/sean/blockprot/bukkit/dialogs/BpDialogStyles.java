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

import net.kyori.adventure.text.format.TextColor;

/**
 * Shared pastel color palette and color-code stripper used by every dialog
 * screen, so the 26+ dialog classes do not each redeclare the same constants.
 * import static this class to use {@link #PASTEL_MINT}, {@link #PASTEL_CORAL},
 * {@link #PASTEL_GOLD}, {@link #SOFT_BLUE}, {@link #PASTEL_PURPLE},
 * {@link #SOFT_GRAY}, and {@link #stripColor(String)} without local copies.
 */
public final class BpDialogStyles {

    public static final TextColor SOFT_GRAY     = TextColor.color(0xAAAAAA);
    public static final TextColor PASTEL_MINT   = TextColor.color(0x8FE3B0);
    public static final TextColor PASTEL_CORAL  = TextColor.color(0xF0A0A0);
    public static final TextColor PASTEL_GOLD   = TextColor.color(0xD2B48C);
    public static final TextColor SOFT_BLUE     = TextColor.color(0xA0C4E8);
    public static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private BpDialogStyles() {}

    public static String stripColor(String s) {
        return s.replaceAll("[\u00a7&][0-9a-fk-orxA-F]", "");
    }

    public static TextColor stateColor(long active, long total) {
        if (active == 0) return PASTEL_CORAL;
        if (active == total) return PASTEL_MINT;
        double ratio = (double) active / total;
        double halfBand = total <= 5 ? 0.20 : 0.10;
        return Math.abs(ratio - 0.5) <= halfBand ? TextColor.color(0xF3C27C) : PASTEL_MINT;
    }
}
