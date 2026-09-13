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

import org.jetbrains.annotations.NotNull;

/**
 * Granular administrative actions mapped to minimum tiers.
 */
public enum AdminAction {
    INFO(AdminTier.T1),
    TELEPORT(AdminTier.T1),
    LOGS(AdminTier.T1),
    BREAK(AdminTier.T2),
    UNLOCK(AdminTier.T2),
    LOCKABLES(AdminTier.T2),
    CONTAINER_BYPASS(AdminTier.T2),
    PROTDEL(AdminTier.T3),
    CONFIG(AdminTier.T3),
    DEBUG(AdminTier.T3),
    RELOAD(AdminTier.OWNER),
    UPDATE(AdminTier.OWNER),
    INTEGRATIONS(AdminTier.OWNER),
    RECOMMENDED(AdminTier.OWNER),
    SETROLE(AdminTier.OWNER);

    private final AdminTier minimumTier;

    AdminAction(@NotNull AdminTier minimumTier) {
        this.minimumTier = minimumTier;
    }

    @NotNull
    public AdminTier getMinimumTier() {
        return minimumTier;
    }
}
