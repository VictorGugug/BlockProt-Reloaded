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

import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.listeners.EntityProtectionListener;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.BlockUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSimulationTest {

    private static ServerMock server;
    private static DefaultConfig config;
    private WorldMock world;
    private PlayerMock owner;
    private PlayerMock stranger;

    @BeforeAll
    static void initServer() throws Exception {
        server = MockBukkit.mock();

        YamlConfiguration mainYaml = new YamlConfiguration();
        mainYaml.set("entity_protection.enabled", true);
        mainYaml.set("entity_protection.menu_item", "STICK");

        YamlConfiguration blocksYaml = new YamlConfiguration();
        blocksYaml.set("protectable_entities", List.of("VILLAGER"));

        File tempDir = File.createTempFile("bp_player_sim_test", "");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        File blocksFile = new File(tempDir, "blocks.yml");
        blocksYaml.save(blocksFile);

        config = new DefaultConfig(mainYaml, tempDir);
        var field = BlockProt.class.getDeclaredField("defaultConfig");
        field.setAccessible(true);
        field.set(null, config);
    }

    @AfterAll
    static void shutdownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        world = server.addSimpleWorld("simulation_world");
        owner = server.addPlayer("OwnerSim");
        stranger = server.addPlayer("StrangerSim");
    }

    @Test
    void testBlockLockCacheLifecycleSimulation() {
        Block chestBlock = world.getBlockAt(10, 64, 10);
        chestBlock.setType(Material.CHEST);

        assertFalse(ProtectedBlockCache.isProtected(chestBlock));
        ProtectedBlockCache.mark(chestBlock);
        assertTrue(ProtectedBlockCache.isProtected(chestBlock));

        ProtectedBlockCache.unmark(chestBlock);
        assertFalse(ProtectedBlockCache.isProtected(chestBlock));
    }

    @Test
    void testMultiBlockStructureParsing() {
        assertTrue(BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.DOORS).contains(Material.OAK_DOOR));
        assertTrue(BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.TRAPDOOR).contains(Material.OAK_TRAPDOOR));
        assertTrue(BlockFamilyParser.getSubFamilyMembers(BlockFamilyParser.SubFamily.FENCE_GATE).contains(Material.OAK_FENCE_GATE));
        assertNotNull(BlockUtil.getHumanReadableBlockName(Material.CHEST));
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void testEntityLockAndStrangerDenialSimulation() {
        Villager villager = world.spawn(world.getBlockAt(0, 64, 0).getLocation(), Villager.class);
        EntityProtectionHandler handler = new EntityProtectionHandler(villager);
        handler.setOwner(owner.getUniqueId());
        handler.setProtected(true);

        assertTrue(handler.isProtected());
        assertEquals(owner.getUniqueId(), handler.getOwner());

        EntityProtectionListener listener = new EntityProtectionListener();
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
            stranger, villager, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 4.0);
        listener.onEntityDamage(damageEvent);
        assertTrue(damageEvent.isCancelled());

        handler.setProtected(false);
        assertFalse(handler.isProtected());
    }
}
