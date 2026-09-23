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

import de.sean.blockprot.bukkit.listeners.InventoryEventListener;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class BlockProtInventoryClickTest {

    private static ServerMock server;
    private PlayerMock player;
    private InventoryEventListener listener;

    private static final class TestBpInventory extends BlockProtInventory {
        final AtomicBoolean clicked = new AtomicBoolean(false);
        final boolean throwOnClick;

        TestBpInventory(boolean throwOnClick) {
            super(false);
            this.throwOnClick = throwOnClick;
        }

        @Override
        int getSize() {
            return 9;
        }

        @Override
        String getTranslatedInventoryName() {
            return "Test Inventory";
        }

        Inventory fill() {
            inventory = createInventory();
            inventory.setItem(0, new ItemStack(Material.NAME_TAG));
            return inventory;
        }

        @Override
        public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
            clicked.set(true);
            if (throwOnClick) {
                throw new RuntimeException("Simulated error in onClick");
            }
        }

        @Override
        public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}
    }

    @BeforeAll
    static void initServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void shutdownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        player = server.addPlayer("ClickTester");
        listener = new InventoryEventListener();
    }

    @Test
    void testClickCancelledEvenWhenStateIsNull() {
        TestBpInventory customInv = new TestBpInventory(false);
        player.openInventory(customInv.fill());

        InventoryState.remove(player.getUniqueId());
        assertNull(InventoryState.get(player.getUniqueId()));

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertTrue(customInv.clicked.get());
        assertNotNull(InventoryState.get(player.getUniqueId()));
    }

    @Test
    void testClickWithExistingStateInvokesOnClick() {
        TestBpInventory customInv = new TestBpInventory(false);
        player.openInventory(customInv.fill());

        InventoryState state = InventoryState.builder().build();
        InventoryState.set(player.getUniqueId(), state);

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        listener.onInventoryClick(event);

        assertTrue(event.isCancelled());
        assertTrue(customInv.clicked.get());
        assertSame(state, InventoryState.get(player.getUniqueId()));
    }

    @Test
    void testClickExceptionDoesNotCrashAndLeavesEventCancelled() {
        TestBpInventory customInv = new TestBpInventory(true);
        player.openInventory(customInv.fill());

        InventoryClickEvent event = new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        assertDoesNotThrow(() -> listener.onInventoryClick(event));
        assertTrue(event.isCancelled());
        assertTrue(customInv.clicked.get());
    }

    @Test
    void testDragCancelledOnBlockProtInventory() {
        TestBpInventory customInv = new TestBpInventory(false);
        player.openInventory(customInv.fill());

        InventoryDragEvent dragEvent = new InventoryDragEvent(
            player.getOpenInventory(),
            new ItemStack(Material.STONE),
            new ItemStack(Material.STONE),
            false,
            Map.of(0, new ItemStack(Material.STONE))
        );

        listener.onInventoryDrag(dragEvent);
        assertTrue(dragEvent.isCancelled());
    }
}
