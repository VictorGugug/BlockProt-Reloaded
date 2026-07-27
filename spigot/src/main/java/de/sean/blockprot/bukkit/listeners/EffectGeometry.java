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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.util.BlockUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.Bed;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates union bounding geometry and perimeter particle sampling points for lockable blocks.
 */
public final class EffectGeometry {

    private final BoundingBox box;
    private final World world;

    public EffectGeometry(@NotNull World world, @NotNull BoundingBox box) {
        this.world = world;
        this.box = box;
    }

    @NotNull
    public static EffectGeometry createForBlock(@NotNull Block block) {
        World world = block.getWorld();
        BoundingBox unionBox = block.getBoundingBox();

        // 1. Bed check
        if (block.getBlockData() instanceof Bed) {
            Block otherHalf = BlockUtil.getOtherBedHalf(block.getState());
            if (otherHalf != null) {
                unionBox = unionBox.union(otherHalf.getBoundingBox());
            }
        }
        // 2. Double chest check
        else if (block.getState() instanceof Chest chest) {
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest doubleChest) {
                Chest left = (Chest) doubleChest.getLeftSide();
                Chest right = (Chest) doubleChest.getRightSide();
                if (left != null && right != null) {
                    unionBox = left.getBlock().getBoundingBox().union(right.getBlock().getBoundingBox());
                }
            }
        }

        return new EffectGeometry(world, unionBox);
    }

    @NotNull
    public BoundingBox getBoundingBox() {
        return box;
    }

    @NotNull
    public Location getUnionCenter() {
        return new Location(world, box.getCenterX(), box.getCenterY(), box.getCenterZ());
    }

    @NotNull
    public List<Location> getPerimeterPoints(double pointSpacing) {
        List<Location> points = new ArrayList<>();

        double minX = box.getMinX();
        double maxX = box.getMaxX();
        double minZ = box.getMinZ();
        double maxZ = box.getMaxZ();
        double midY = (box.getMinY() + box.getMaxY()) / 2.0;

        double widthX = maxX - minX;
        double widthZ = maxZ - minZ;

        int countX = Math.max(2, (int) Math.ceil(widthX / pointSpacing));
        int countZ = Math.max(2, (int) Math.ceil(widthZ / pointSpacing));

        // North edge (z = minZ, x from minX to maxX)
        for (int i = 0; i < countX; i++) {
            double x = minX + (i * widthX / countX);
            points.add(new Location(world, x, midY, minZ));
        }

        // East edge (x = maxX, z from minZ to maxZ)
        for (int i = 0; i < countZ; i++) {
            double z = minZ + (i * widthZ / countZ);
            points.add(new Location(world, maxX, midY, z));
        }

        // South edge (z = maxZ, x from maxX to minX)
        for (int i = 0; i < countX; i++) {
            double x = maxX - (i * widthX / countX);
            points.add(new Location(world, x, midY, maxZ));
        }

        // West edge (x = minX, z from maxZ to minZ)
        for (int i = 0; i < countZ; i++) {
            double z = maxZ - (i * widthZ / countZ);
            points.add(new Location(world, minX, midY, z));
        }

        return points;
    }
}
