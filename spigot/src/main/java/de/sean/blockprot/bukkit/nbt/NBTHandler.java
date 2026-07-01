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
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.NBT;
import org.jetbrains.annotations.NotNull;

/**
 * The base NBT Handler.
 *
 * @param <T> The type of the NBT container. {@link NBTCompound}
 *            is the base of all containers and works with all of them,
 *            however there are some special conditions and functions
 *            for specific containers.
 * @since 0.3.0
 */
public abstract class NBTHandler<T extends NBTCompound> {
    @Deprecated
    public static final String PERMISSION_LOCK = "blockprot.lock";

    @Deprecated
    public static final String PERMISSION_INFO = "blockprot.info";

    @Deprecated
    public static final String PERMISSION_ADMIN = "blockprot.admin";

    @Deprecated
    public static final String PERMISSION_BYPASS = "blockprot.bypass";

    /**
     * The NBT container for this handler.
     *
     * @since 0.3.0
     */
    protected T container;

    protected NBTHandler() {
    }

    public String getName() {
        String name = container.getName();
        return name == null ? "" : name;
    }

    public void mergeHandler(@NotNull final NBTHandler<?> handler) {}

    /**
     * Get's a copy of this NBT inside of a {@link NBTContainer}.
     * @since 1.0.0
     */
    @NotNull
    public NBTContainer getNbtCopy() {
        ReadWriteNBT raw = NBT.createNBTObject();
        raw.mergeCompound(this.container);
        // NBTContainer implements ReadWriteNBT; the factory always returns an NBTContainer instance.
        return (NBTContainer) raw;
    }

    /**
     * Pastes given NBT into this container, potentially
     * overriding everything.
     * 
     * @param container The NBT to paste.
     * @since 1.0.0
     */
    public void pasteNbt(@NotNull NBTContainer container) {
        this.container.mergeCompound(container);
    }
}