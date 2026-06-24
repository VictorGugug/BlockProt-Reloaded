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

import de.sean.blockprot.bukkit.util.DurationParser;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StatHandlerDirtyFlagTest {

    @Test
    void durationParserZeroIsNull() {
        assertNull(DurationParser.parse("0d0h0m0s"));
    }

    @Test
    void durationParserNegativeStringIsNull() {
        assertNull(DurationParser.parse("abc"));
    }

    @Test
    void durationParserOnlySeconds() {
        Duration d = DurationParser.parse("45s");
        assertNotNull(d);
        assertEquals(45L, d.getSeconds());
    }

    @Test
    void durationParserLargeValue() {
        Duration d = DurationParser.parse("365d");
        assertNotNull(d);
        assertEquals(365L, d.toDays());
    }

    @Test
    void formatThenParse() {
        Duration original = Duration.ofHours(3).plusMinutes(15);
        String formatted = DurationParser.format(original);
        Duration reparsed = DurationParser.parse(formatted);
        assertNotNull(reparsed);
        assertEquals(original.getSeconds(), reparsed.getSeconds());
    }
}
