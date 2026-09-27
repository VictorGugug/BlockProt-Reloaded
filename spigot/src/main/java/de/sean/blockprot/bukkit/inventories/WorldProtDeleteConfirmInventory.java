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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.tasks.WorldProtectionEraser;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI before wiping all block protections in a world.
 *
 * <p>Layout (27 slots):
 * <ul>
 *   <li>Slot 11: Confirm (TNT): executes deletion</li>
 *   <li>Slot 13: Emerald: undo last deletion</li>
 *   <li>Slot 15: Barrier: cancel / back to selector</li>
 * </ul>
 *
 * <p>Deletion and undo are delegated to {@link WorldProtectionEraser}.
 */
public final class WorldProtDeleteConfirmInventory extends BlockProtInventory {

    private @NotNull String worldName = "";

    public WorldProtDeleteConfirmInventory() {
        super(false);
    }

    @Override
    int getSize() { return 27; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_TITLE)
            .replace("{world}", worldName);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        switch (item.getType()) {
            case TNT     -> executeDelete(player);
            case EMERALD -> executeUndo(player);
            case BARRIER -> goBackToSelector(player);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(@NotNull Player player, @NotNull String worldName) {
        this.worldName = worldName;
        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) {
            state = InventoryState.builder().build();
            InventoryState.set(player.getUniqueId(), state);
        }
        inventory = createInventory();
        renderSlots(player);
        return inventory;
    }

    private void renderSlots(@NotNull Player player) {
        inventory.clear();

        setItemStackWithLore(11, Material.TNT,
            Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_BUTTON)
                .replace("{world}", worldName),
            Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_LORE)
                .replace("{world}", worldName));

        boolean hasSnapshot = WorldProtectionEraser.latestUndo(player.getUniqueId()) != null;
        if (hasSnapshot) {
            setItemStackWithLore(13, Material.EMERALD,
                Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__UNDO_BUTTON),
                Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__UNDO_LORE));
        } else {
            setItemStack(13, Material.GRAY_STAINED_GLASS_PANE, Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__NO_UNDO_AVAILABLE));
        }

        setItemStack(15, Material.BARRIER,
            Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CANCEL_BUTTON));
    }

    private void executeDelete(@NotNull Player player) {
        World world = Bukkit.getWorld(worldName);
        player.closeInventory();
        InventoryState.remove(player.getUniqueId());
        if (world == null) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND)
                .replace("{world}", worldName));
            return;
        }
        WorldProtectionEraser.erase(world, player.getUniqueId(), count -> ComponentMessages.sendLegacy(player, count == 0
            ? Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_NONE).replace("{world}", worldName)
            : Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_DONE).replace("{world}", worldName)
                .replace("{count}", String.valueOf(count))));
    }

    private void executeUndo(@NotNull Player player) {
        player.closeInventory();
        InventoryState.remove(player.getUniqueId());
        WorldProtectionEraser.UndoBatch batch = WorldProtectionEraser.latestUndo(player.getUniqueId());
        if (batch == null) {
            ComponentMessages.sendLegacy(player, Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_NOTHING));
            return;
        }
        WorldProtectionEraser.undo(player.getUniqueId(), batch, restored -> ComponentMessages.sendLegacy(player,
            Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_DONE).replace("{count}", String.valueOf(restored))));
    }

    private void goBackToSelector(@NotNull Player player) {
        WorldProtDeleteInventory selector = new WorldProtDeleteInventory();
        player.openInventory(selector.fill(player, null));
    }

    private void setItemStackWithLore(int slot, @NotNull Material material,
                                      @NotNull String name, @NotNull String loreLine) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(name.replaceAll("[§&][0-9a-fk-orx]", "")));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(loreLine));
            ComponentMessages.lore(meta, lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }
}