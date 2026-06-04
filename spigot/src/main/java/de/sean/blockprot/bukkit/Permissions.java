/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

/**
 * Permission nodes for BlockProt.
 *
 * <p>Three node model:
 * <ul>
 *   <li>{@link #USER}       — all standard player actions (lock, friends, settings, stats, etc.)  default: true</li>
 *   <li>{@link #USER_ADMIN} — all admin actions (reload, info, debug, etc.)                       default: op</li>
 *   <li>{@link #BYPASS}     — bypass all block protections                                        default: false</li>
 * </ul>
 *
 * <p>Legacy nodes are kept as aliases so existing code that references them continues to compile;
 * internally they resolve to one of the three real nodes above.
 *
 * @since 1.1.7
 */
public enum Permissions {

    // ── Real permission nodes (declared in plugin.yml) ──────────────────────
    USER("blockprot.user"),
    USER_ADMIN("blockprot.user.admin"),
    BYPASS("blockprot.bypass"),
    BLOCKS_TP("blockprot.blocks.tp"),

    // ── Legacy aliases (not in plugin.yml; used internally for back-compat) ─
    /** @deprecated Use {@link #USER} */
    @Deprecated LOCK("blockprot.user"),
    /** @deprecated Use {@link #USER_ADMIN} */
    @Deprecated INFO("blockprot.user.admin"),
    /** @deprecated Use {@link #USER_ADMIN} */
    @Deprecated ADMIN("blockprot.user.admin");

    private final String text;

    Permissions(final String text) {
        this.text = text;
    }

    @Override
    public final String toString() {
        return text;
    }

    public final String key() {
        return text;
    }
}
