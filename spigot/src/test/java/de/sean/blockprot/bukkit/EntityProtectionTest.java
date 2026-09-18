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
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.inventories.EntitySettingsInventory;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.listeners.EntityMenuOpenListener;
import de.sean.blockprot.bukkit.listeners.EntityProtectionListener;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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

class EntityProtectionTest {

    private static ServerMock server;
    private static DefaultConfig config;
    private static YamlConfiguration mainYaml;
    private static YamlConfiguration blocksYaml;

    private WorldMock world;
    private PlayerMock owner;
    private PlayerMock stranger;

    @BeforeAll
    static void initServer() throws Exception {
        server = MockBukkit.mock();

        mainYaml = new YamlConfiguration();
        mainYaml.set("entity_protection.enabled", true);
        mainYaml.set("entity_protection.menu_item", "STICK");

        blocksYaml = new YamlConfiguration();
        blocksYaml.set("protectable_entities", List.of("VILLAGER"));

        File tempDir = File.createTempFile("bp_test_data", "");
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
        world = server.addSimpleWorld("test_world");
        owner = server.addPlayer("OwnerPlayer");
        stranger = server.addPlayer("StrangerPlayer");
    }

    @Test
    void testIsSupportedEntity() {
        Villager villager = world.spawn(owner.getLocation(), Villager.class);
        Wolf wolf = world.spawn(owner.getLocation(), Wolf.class);

        assertTrue(EntityProtectionHandler.isSupportedEntity(villager));
        assertTrue(EntityProtectionHandler.isSupportedEntity(wolf));
    }

    @Test
    @SuppressWarnings("removal")
    void testVillagerLockingAndDenial() {
        Villager villager = world.spawn(owner.getLocation(), Villager.class);
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(villager);
        assertNotNull(handler);
        assertFalse(handler.isProtected());
        assertNull(handler.getOwner());

        owner.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        var openEvent = new PlayerInteractEntityEvent(owner, villager, EquipmentSlot.HAND);
        new EntityMenuOpenListener().onInteract(openEvent);

        assertTrue(openEvent.isCancelled());

        InventoryState state = InventoryState.getOrCreate(owner.getUniqueId());
        state.setEntityProtectionId(villager.getUniqueId());

        EntitySettingsInventory inv = new EntitySettingsInventory();
        assertNotNull(inv.fill(owner, villager));

        inv.onClick(new InventoryClickEvent(owner.getOpenInventory(), org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 0, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL), state);
        inv.onClick(new InventoryClickEvent(owner.getOpenInventory(), org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 2, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL), state);

        assertTrue(handler.isProtected());
        assertTrue(handler.isNoDamage());
        assertTrue(handler.isNoInteract());
        assertEquals(owner.getUniqueId(), handler.getOwner());

        stranger.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        var strangerStickEvent = new PlayerInteractEntityEvent(stranger, villager, EquipmentSlot.HAND);
        new EntityMenuOpenListener().onInteract(strangerStickEvent);
        assertTrue(strangerStickEvent.isCancelled());

        stranger.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        var strangerTradeEvent = new PlayerInteractEntityEvent(stranger, villager, EquipmentSlot.HAND);
        new EntityProtectionListener().onInteract(strangerTradeEvent);
        assertTrue(strangerTradeEvent.isCancelled());

        var damageEvent = new EntityDamageByEntityEvent(stranger, villager, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        new EntityProtectionListener().onEntityDamage(damageEvent);
        assertTrue(damageEvent.isCancelled());

        var ownerTradeEvent = new PlayerInteractEntityEvent(owner, villager, EquipmentSlot.HAND);
        new EntityProtectionListener().onInteract(ownerTradeEvent);
        assertFalse(ownerTradeEvent.isCancelled());
    }
}
