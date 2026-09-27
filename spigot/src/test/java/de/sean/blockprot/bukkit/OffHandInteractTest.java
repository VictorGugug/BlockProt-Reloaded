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

import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.listeners.InteractEventListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffHandInteractTest {

    private static ServerMock server;
    private PlayerMock player;
    private Block chest;

    @BeforeAll
    static void initServer() throws Exception {
        server = MockBukkit.mock();

        YamlConfiguration blocksYaml = new YamlConfiguration();
        blocksYaml.set("lockable_tile_entities", List.of("CHEST"));

        File tempDir = File.createTempFile("bp_offhand_test", "");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        blocksYaml.save(new File(tempDir, "blocks.yml"));

        DefaultConfig config = new DefaultConfig(new YamlConfiguration(), tempDir);
        var field = BlockProt.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        field.set(null, config);
    }

    @AfterAll
    static void tearDown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        var world = server.addSimpleWorld("offhand_world_" + System.nanoTime());
        player = server.addPlayer();
        chest = world.getBlockAt(0, 64, 0);
        chest.setType(Material.CHEST);
        assertTrue(BlockProt.getDefaultConfig().isLockable(Material.CHEST, world));
    }

    private PlayerInteractEvent offHandClick(Material offHand) {
        ItemStack item = new ItemStack(offHand);
        player.getInventory().setItemInOffHand(item);
        player.getInventory().setItemInMainHand(null);
        PlayerInteractEvent event = new PlayerInteractEvent(
            player, Action.RIGHT_CLICK_BLOCK, item, chest, BlockFace.UP, EquipmentSlot.OFF_HAND);
        new InteractEventListener().playerInteract(event);
        return event;
    }

    @Test
    void sneakingWithShieldInOffHandDeniesContainerOpen() {
        player.setSneaking(true);
        assertEquals(Event.Result.DENY, offHandClick(Material.SHIELD).useInteractedBlock());
    }

    @Test
    void sneakingWithBlockInOffHandStillAllowsPlacement() {
        player.setSneaking(true);
        assertNotEquals(Event.Result.DENY, offHandClick(Material.COBBLESTONE).useInteractedBlock());
    }

    @Test
    void standingWithShieldInOffHandLeavesInteractionUntouched() {
        player.setSneaking(false);
        assertNotEquals(Event.Result.DENY, offHandClick(Material.SHIELD).useInteractedBlock());
    }
}
