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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditionTest {

    @Test
    void recommendedForCoversEveryBoundary() {
        assertTrue(Edition.recommendedFor(1, 18, 1).isEmpty());
        assertEquals(Edition.BP_LEGACY, Edition.recommendedFor(1, 18, 2).orElseThrow());
        assertEquals(Edition.BP_LEGACY, Edition.recommendedFor(1, 20, 4).orElseThrow());
        assertEquals(Edition.BPR_LEGACY, Edition.recommendedFor(1, 20, 5).orElseThrow());
        assertEquals(Edition.BPR_LEGACY, Edition.recommendedFor(1, 21, 6).orElseThrow());
        assertEquals(Edition.BPR, Edition.recommendedFor(1, 21, 7).orElseThrow());
        assertEquals(Edition.BPR, Edition.recommendedFor(26, 3, 0).orElseThrow());
    }

    @Test
    void supportsIsInclusiveOnBothEnds() {
        assertTrue(Edition.BPR_LEGACY.supports(1, 20, 5));
        assertTrue(Edition.BPR_LEGACY.supports(1, 21, 6));
        assertFalse(Edition.BPR_LEGACY.supports(1, 20, 4));
        assertFalse(Edition.BPR_LEGACY.supports(1, 21, 7));
        assertTrue(Edition.BPR.supports(26, 3, 0));
        assertFalse(Edition.BPR.supports(1, 21, 6));
    }

    @Test
    void isNewerThanRangeNeverHoldsForOpenMaximum() {
        assertTrue(Edition.BPR_LEGACY.isNewerThanRange(1, 21, 7));
        assertTrue(Edition.BP_LEGACY.isNewerThanRange(1, 20, 5));
        assertFalse(Edition.BPR_LEGACY.isNewerThanRange(1, 21, 6));
        assertFalse(Edition.BPR.isNewerThanRange(99, 0, 0));
    }

    @Test
    void isEditionTagMatchesOnlyPrefixedTags() {
        assertTrue(Edition.isEditionTag("bprl-1.3.9.1"));
        assertTrue(Edition.isEditionTag("bpl-1.3.9.1"));
        assertFalse(Edition.isEditionTag("1.3.9"));
        assertFalse(Edition.isEditionTag("v1.3.9"));
        assertFalse(Edition.isEditionTag("1.3.8-BEDev.1"));
    }

    @Test
    void onlyReloadedSupportsDialogs() {
        assertTrue(Edition.BPR.supportsDialogs());
        assertFalse(Edition.BPR_LEGACY.supportsDialogs());
        assertFalse(Edition.BP_LEGACY.supportsDialogs());
    }

    @Test
    void rangeLabelFormatsClosedAndOpenRanges() {
        assertEquals("1.20.5 - 1.21.6", Edition.BPR_LEGACY.rangeLabel());
        assertEquals("1.18.2 - 1.20.4", Edition.BP_LEGACY.rangeLabel());
        assertEquals("1.21.7+", Edition.BPR.rangeLabel());
    }

    @Test
    void currentReadsTheBundledResource() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = EditionTest.class.getResourceAsStream("/edition.properties")) {
            properties.load(in);
        }
        assertEquals(properties.getProperty("id"), Edition.current().id());
    }
}
