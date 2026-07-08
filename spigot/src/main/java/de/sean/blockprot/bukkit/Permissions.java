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

/**
 * Permission nodes for BlockProt Reloaded.
 *
 * <p>Four active nodes (declared in plugin.yml):
 * <ul>
 *   <li>{@link #USER}      : all standard player actions. Default: true (everyone)</li>
 *   <li>{@link #USER_ADMIN}: all admin actions plus the ability to break any protected
 *                             block or shulker, clearing the protection on break.
 *                             Implicitly grants USER. Default: op</li>
 *   <li>{@link #MAX_BLOCKS}: exempts the player from the player_max_locked_block_count
 *                             cap in config.yml. Default: false</li>
 *   <li>{@link #BLOCKS_TP} : teleport to a block from the statistics inventory.
 *                             Default: op</li>
 * </ul>
 *
 * <p>{@link #BYPASS} is kept as a deprecated alias of {@link #USER_ADMIN} so that any
 * external code or permission plugin entries that still reference {@code blockprot.bypass}
 * continue to compile and behave correctly without modification.
 *
 * @since 1.1.7
 */
public enum Permissions {

    /** Standard player features: locking, friends, settings, statistics, transfer. */
    USER("blockprot.user"),

    /**
     * Admin features and the ability to break any protected block (incl. shulkers
     * owned by other players). Protection data is cleared automatically on break.
     * Implicitly grants {@link #USER} via plugin.yml children.
     */
    USER_ADMIN("blockprot.user.admin"),

    /**
     * Exempts the holder from the {@code player_max_locked_block_count} limit.
     * Assign to VIPs, donors, or trusted players.
     */
    MAX_BLOCKS("blockprot.max_blocks"),

    /** Teleport to a protected block from the statistics inventory. */
    BLOCKS_TP("blockprot.blocks.tp"),

    /** Run diagnostic checks and view debug output. */
    DEBUG("blockprot.debug"),

    /**
     * @deprecated Use {@link #USER_ADMIN}. Kept as an alias so existing permission
     *             plugin entries and compiled code referencing {@code blockprot.bypass}
     *             continue to work without changes. The node is no longer declared in
     *             plugin.yml; {@code blockprot.user.admin} now covers its purpose.
     */
    @Deprecated
    BYPASS("blockprot.user.admin"),

    /** @deprecated Use {@link #USER} */
    @Deprecated
    LOCK("blockprot.user"),

    /** @deprecated Use {@link #USER_ADMIN} */
    @Deprecated
    INFO("blockprot.user.admin"),

    /** @deprecated Use {@link #USER_ADMIN} */
    @Deprecated
    ADMIN("blockprot.user.admin");

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