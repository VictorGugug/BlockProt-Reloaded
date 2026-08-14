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

package de.sean.blockprot.bukkit.nbt;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.jetbrains.annotations.NotNull;

/**
 * The friend handler used by {@link BlockNBTHandler} to handle
 * each of the {@link NBTCompound} used in the "friends" sub-tag of each block.
 * A single {@link FriendHandler} itself only handles a *single friend*
 * in the list of friends.
 *
 * @since 0.3.0
 */
public final class FriendHandler extends NBTHandler<NBTCompound> {
    static final String ACCESS_FLAGS_ATTRIBUTE = "blockprot_access_flags";

    /**
     * @param compound The NBT compound used.
     * @since 0.3.0
     */
    public FriendHandler(@NotNull final NBTCompound compound) {
        super();
        this.container = compound;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.3.0
     */
    @NotNull
    public String getName() {
        String name = container.getName();
        return name == null ? "" : name;
    }

    /**
     * A single friend handler can represent the whole player-base, giving access
     * to anyone on the server with specific access flags. We represent everyone by
     * using an invalid UUID.
     */
    public boolean doesRepresentPublic() {
        return getName().equals(FriendSupportingHandler.publicUuid.toString());
    }

    /**
     * Checks if this player can read the contents of the parent block.
     * Access control has been removed.
     *
     * @return Always true - access flags have been removed
     * @since 0.3.0
     */
    public boolean canRead() {
        return true;
    }

    /**
     * Checks if this player can write to the parent block.
     * Access control has been removed.
     *
     * @return Always true - access flags have been removed
     * @since 0.3.0
     */
    public boolean canWrite() {
        return true;
    }

    /**
     * Checks if this player is a manager of the parent block.
     * Access control has been removed.
     *
     * @return Always false - access flags have been removed
     * @since 1.0.0
     */
    public boolean isManager() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This only merges values if {@code handler} is an instance of {@link FriendHandler},
     * and only merges the access flags.
     *
     * @since 0.4.7
     */
    @Override
    public void mergeHandler(@NotNull NBTHandler<?> handler) {
        // Access flags feature removed
    }
}