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
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Confirmation GUI before wiping all block protections in a world.
 *
 * <p>Layout (27 slots):
 * <ul>
 *   <li>Slot 11 — Confirm (TNT): executes deletion</li>
 *   <li>Slot 13 — Emerald: undo last deletion</li>
 *   <li>Slot 15 — Barrier: cancel / back to selector</li>
 * </ul>
 *
 * <p>All NBT reads and writes happen on the main thread.
 * For the chunk-scan fallback, work is batched per tick (20 blocks/tick)
 * to avoid server stalls on large worlds.
 */
public final class WorldProtDeleteConfirmInventory extends BlockProtInventory {

    private static final int BATCH_PER_TICK = 20;

    static final Map<UUID, List<ProtectionSnapshot>> UNDO_SNAPSHOTS = new HashMap<>();

    public record ProtectionSnapshot(
        @NotNull Location location,
        @NotNull String ownerUuid,
        @NotNull List<String> friendUuids
    ) {}

    private @NotNull String worldName = "";

    public WorldProtDeleteConfirmInventory() {
        super(false);
    }

    @Override
    int getSize() { return 27; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        String t = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_TITLE);
        if (t == null || t.isBlank()) t = "{world}";
        return t.replace("{world}", worldName);
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
        InventoryState state = InventoryState.builder().build();
        InventoryState.set(player.getUniqueId(), state);
        inventory = createInventory();
        renderSlots(player);
        return inventory;
    }

    private void renderSlots(@NotNull Player player) {
        inventory.clear();

        String confirmText = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_BUTTON);
        if (confirmText == null || confirmText.isBlank()) confirmText = "§cConfirm Delete";
        String confirmLore = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_LORE);
        if (confirmLore == null) confirmLore = "§7Removes ALL protections in §e{world}";
        setItemStackWithLore(11, Material.TNT,
            confirmText.replace("{world}", worldName),
            confirmLore.replace("{world}", worldName));

        boolean hasSnapshot = UNDO_SNAPSHOTS.containsKey(player.getUniqueId())
            && !UNDO_SNAPSHOTS.get(player.getUniqueId()).isEmpty();
        if (hasSnapshot) {
            String undoText = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__UNDO_BUTTON);
            if (undoText == null || undoText.isBlank()) undoText = "§aUndo Last Deletion";
            String undoLore = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__UNDO_LORE);
            if (undoLore == null) undoLore = "§7Restores the protections removed in the last operation.";
            setItemStackWithLore(13, Material.EMERALD, undoText, undoLore);
        } else {
            setItemStack(13, Material.GRAY_STAINED_GLASS_PANE, "§7No undo available");
        }

        String cancelText = Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CANCEL_BUTTON);
        if (cancelText == null || cancelText.isBlank()) cancelText = "§fCancel";
        setItemStack(15, Material.BARRIER, cancelText);
    }

    private void executeDelete(@NotNull Player player) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND)
                    .replace("{world}", worldName)));
            player.closeInventory();
            InventoryState.remove(player.getUniqueId());
            return;
        }

        player.closeInventory();
        InventoryState.remove(player.getUniqueId());

        HybridDatabase db = BlockProt.getHybridDatabase();
        if (db != null && db.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                List<Location> locations = db.getBlockIndexByWorld(world.getName());
                Bukkit.getScheduler().runTask(BlockProt.getInstance(),
                    () -> clearLocationsBatched(player, locations));
            });
        } else {
            Chunk[] chunks = world.getLoadedChunks();
            List<Location> locations = new ArrayList<>();
            for (Chunk chunk : chunks) {
                for (BlockState state : chunk.getTileEntities()) {
                    locations.add(state.getLocation());
                }
            }
            clearLocationsBatched(player, locations);
        }
    }

    private void clearLocationsBatched(@NotNull Player player, @NotNull List<Location> locations) {
        List<ProtectionSnapshot> snapshots = new ArrayList<>();
        int[] counter = {0};
        int[] index   = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (index[0] < locations.size() && processed < BATCH_PER_TICK) {
                    Location loc = locations.get(index[0]++);
                    processed++;
                    if (loc.getWorld() == null) continue;
                    Block block = loc.getBlock();
                    try {
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        if (!handler.isProtected()) continue;
                        snapshots.add(snapshotOf(loc, handler));
                        handler.clear();
                        HopperEventListener.invalidate(block);
                        ProtectedBlockCache.unmark(block);
                        counter[0]++;
                    } catch (RuntimeException ignored) {}
                }

                if (index[0] >= locations.size()) {
                    cancel();
                    UNDO_SNAPSHOTS.put(player.getUniqueId(), snapshots);
                    int count = counter[0];
                    String msg = count == 0
                        ? Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_NONE)
                              .replace("{world}", worldName)
                        : Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_DONE)
                              .replace("{world}", worldName)
                              .replace("{count}", String.valueOf(count));
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
                }
            }
        }.runTaskTimer(BlockProt.getInstance(), 0L, 1L);
    }

    private void executeUndo(@NotNull Player player) {
        List<ProtectionSnapshot> snapshots = UNDO_SNAPSHOTS.remove(player.getUniqueId());
        if (snapshots == null || snapshots.isEmpty()) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_NOTHING)));
            player.closeInventory();
            InventoryState.remove(player.getUniqueId());
            return;
        }

        player.closeInventory();
        InventoryState.remove(player.getUniqueId());

        int[] restored = {0};
        int[] index    = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (index[0] < snapshots.size() && processed < BATCH_PER_TICK) {
                    ProtectionSnapshot snap = snapshots.get(index[0]++);
                    processed++;
                    if (snap.location().getWorld() == null) continue;
                    Block block = snap.location().getBlock();
                    try {
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        handler.setOwner(snap.ownerUuid());
                        for (String friend : snap.friendUuids()) {
                            handler.addFriend(friend);
                        }
                        handler.applyToOtherContainer();
                        ProtectedBlockCache.mark(block);
                        restored[0]++;
                    } catch (RuntimeException ignored) {}
                }

                if (index[0] >= snapshots.size()) {
                    cancel();
                    String msg = Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_DONE)
                        .replace("{count}", String.valueOf(restored[0]));
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
                }
            }
        }.runTaskTimer(BlockProt.getInstance(), 0L, 1L);
    }

    private void goBackToSelector(@NotNull Player player) {
        InventoryState.remove(player.getUniqueId());
        WorldProtDeleteInventory selector = new WorldProtDeleteInventory();
        player.openInventory(selector.fill(player, null));
    }

    @NotNull
    private ProtectionSnapshot snapshotOf(@NotNull Location loc, @NotNull BlockNBTHandler handler) {
        List<String> friends = handler.getFriends().stream()
            .map(f -> f.getName())
            .collect(Collectors.toList());
        return new ProtectionSnapshot(loc.clone(), handler.getOwner(), friends);
    }

    private void setItemStackWithLore(int slot, @NotNull Material material,
                                      @NotNull String name, @NotNull String loreLine) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name.replaceAll("[§&][0-9a-fk-orx]", "")));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize(loreLine));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }
}
