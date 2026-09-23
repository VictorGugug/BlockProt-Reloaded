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

package de.sean.blockprot.bukkit.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminTierTest {

    @Test
    void testTierFromString() {
        assertEquals(AdminTier.T1, AdminTier.fromString("t1"));
        assertEquals(AdminTier.T1, AdminTier.fromString("T1"));
        assertEquals(AdminTier.T1, AdminTier.fromString("mod"));
        assertEquals(AdminTier.T1, AdminTier.fromString("1"));
        assertEquals(AdminTier.T2, AdminTier.fromString("t2"));
        assertEquals(AdminTier.T2, AdminTier.fromString("helper"));
        assertEquals(AdminTier.T2, AdminTier.fromString("2"));
        assertEquals(AdminTier.T3, AdminTier.fromString("t3"));
        assertEquals(AdminTier.T3, AdminTier.fromString("admin"));
        assertEquals(AdminTier.T3, AdminTier.fromString("3"));
        assertEquals(AdminTier.OWNER, AdminTier.fromString("owner"));
        assertEquals(AdminTier.OWNER, AdminTier.fromString("OWNER"));
        assertEquals(AdminTier.OWNER, AdminTier.fromString("t4"));
        assertEquals(AdminTier.CUSTOM, AdminTier.fromString("custom"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("user"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("USER"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("normal"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("0"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("none"));
        assertEquals(AdminTier.NONE, AdminTier.fromString("invalid_input"));
    }

    @Test
    void testTierIdentifiersAndLevels() {
        assertEquals("owner", AdminTier.OWNER.getIdentifier());
        assertEquals(4, AdminTier.OWNER.getLevel());
        assertEquals("t3", AdminTier.T3.getIdentifier());
        assertEquals(3, AdminTier.T3.getLevel());
        assertEquals("t2", AdminTier.T2.getIdentifier());
        assertEquals(2, AdminTier.T2.getLevel());
        assertEquals("t1", AdminTier.T1.getIdentifier());
        assertEquals(1, AdminTier.T1.getLevel());
        assertEquals("none", AdminTier.NONE.getIdentifier());
        assertEquals(0, AdminTier.NONE.getLevel());
        assertEquals("custom", AdminTier.CUSTOM.getIdentifier());
        assertEquals(-1, AdminTier.CUSTOM.getLevel());
    }

    @Test
    void testTierHierarchyInclusions() {
        assertTrue(AdminTier.OWNER.includes(AdminTier.T3));
        assertTrue(AdminTier.OWNER.includes(AdminTier.T2));
        assertTrue(AdminTier.OWNER.includes(AdminTier.T1));
        assertTrue(AdminTier.OWNER.includes(AdminTier.NONE));

        assertTrue(AdminTier.T3.includes(AdminTier.T2));
        assertTrue(AdminTier.T3.includes(AdminTier.T1));
        assertTrue(AdminTier.T3.includes(AdminTier.NONE));
        assertFalse(AdminTier.T3.includes(AdminTier.OWNER));

        assertTrue(AdminTier.T2.includes(AdminTier.T1));
        assertFalse(AdminTier.T2.includes(AdminTier.T3));

        assertTrue(AdminTier.T1.includes(AdminTier.T1));
        assertFalse(AdminTier.T1.includes(AdminTier.T2));

        assertFalse(AdminTier.NONE.includes(AdminTier.T1));
    }

    @Test
    void testAdminActionMinimumTiers() {
        assertEquals(AdminTier.T1, AdminAction.INFO.getMinimumTier());
        assertEquals(AdminTier.T1, AdminAction.TELEPORT.getMinimumTier());
        assertEquals(AdminTier.T1, AdminAction.LOGS.getMinimumTier());

        assertEquals(AdminTier.T2, AdminAction.BREAK.getMinimumTier());
        assertEquals(AdminTier.T2, AdminAction.UNLOCK.getMinimumTier());
        assertEquals(AdminTier.T2, AdminAction.LOCKABLES.getMinimumTier());
        assertEquals(AdminTier.T2, AdminAction.CONTAINER_BYPASS.getMinimumTier());

        assertEquals(AdminTier.T3, AdminAction.PROTDEL.getMinimumTier());
        assertEquals(AdminTier.T3, AdminAction.CONFIG.getMinimumTier());
        assertEquals(AdminTier.T3, AdminAction.DEBUG.getMinimumTier());

        assertEquals(AdminTier.OWNER, AdminAction.RELOAD.getMinimumTier());
        assertEquals(AdminTier.OWNER, AdminAction.UPDATE.getMinimumTier());
        assertEquals(AdminTier.OWNER, AdminAction.INTEGRATIONS.getMinimumTier());
        assertEquals(AdminTier.OWNER, AdminAction.RECOMMENDED.getMinimumTier());
        assertEquals(AdminTier.OWNER, AdminAction.SETROLE.getMinimumTier());
    }
}
