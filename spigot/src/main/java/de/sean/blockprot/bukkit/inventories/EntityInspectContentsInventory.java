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

package de.sean.blockprot.bukkit.inventories;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only inventory mirror for protectable entities that hold items
 * (chest boats, storage minecarts, hopper minecarts).
 *
 * <p>Mirrors {@link BlockInspectContentsInventory} but sources the inventory
 * from an {@link Entity} that implements {@link InventoryHolder} rather than a block.
 */
public final class EntityInspectContentsInventory extends BlockProtInventory {

    private final InventoryHolder source;

    public EntityInspectContentsInventory(@NotNull Entity entity) {
        super(false);
        if (!(entity instanceof InventoryHolder holder)) {
            throw new IllegalArgumentException("Entity does not implement InventoryHolder: " + entity.getType());
        }
        this.source = holder;
        InventoryType type = source.getInventory().getType();
        this.inventory = (type == InventoryType.CHEST)
            ? createInventory()
            : Bukkit.createInventory(this, type);
    }

    @Override
    int getSize() { return source.getInventory().getSize(); }

    @Nullable
    @Override
    String getTranslatedInventoryName() { return null; }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @NotNull
    public Inventory fill() {
        inventory.setContents(source.getInventory().getContents());
        return inventory;
    }
}