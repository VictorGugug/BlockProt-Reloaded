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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.inventories.WorldProtDeleteConfirmInventory;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.listeners.HopperEventListener;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProtdelDialog {

    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);

    private static final int BATCH_PER_TICK = 20;

    static final Map<UUID, List<UndoBatch>> UNDO_BATCHES = new HashMap<>();

    public record UndoBatch(long timestamp, List<WorldProtDeleteConfirmInventory.ProtectionSnapshot> snapshots) {}
    public record UndoEntry(int index, UndoBatch batch) {}

    private ProtdelDialog() {}

    public static void show(@NotNull Player player, @Nullable String worldName) {
        show(player, worldName, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @Nullable String worldName, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        if (worldName != null) {
            showConfirm(player, worldName, bridge, backOrigin);
            return;
        }

        showWorldSelector(player, bridge, backOrigin);
    }

    private static void showWorldSelector(@NotNull Player player, @NotNull DialogBridge bridge,
                                           @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        List<World> worlds = Bukkit.getWorlds();
        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__HEADER)), SOFT_GRAY)));
        body.add(DialogBodyEntry.text(Component.empty()));

        List<DialogButton> buttons = new ArrayList<>();
        for (World world : worlds) {
            String wName = world.getName();
            String hint = stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__WORLD_HINT))
                .replace("{world}", wName);
            buttons.add(new DialogButton("world_" + wName,
                Component.text(wName, NamedTextColor.WHITE),
                Component.text(hint, TextColor.color(0x888888)),
                p -> showConfirm(p, wName, bridge, backOrigin)));
        }

        List<DialogButton> undoNav = new ArrayList<>();
        if (UNDO_BATCHES.containsKey(player.getUniqueId()) && !UNDO_BATCHES.get(player.getUniqueId()).isEmpty()) {
            List<UndoBatch> batches = UNDO_BATCHES.get(player.getUniqueId());
            undoNav.add(new DialogButton("undo_all",
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__UNDO_LABEL))
                    .replace("{count}", String.valueOf(batches.size())), PASTEL_GOLD),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__RESTORE_HINT))
                    .replace("{count}", String.valueOf(
                        batches.stream().mapToInt(b -> b.snapshots().size()).sum())),
                    TextColor.color(0x888888)),
                p -> showUndoSelector(p, bridge, backOrigin)));
        }

        buttons.addAll(undoNav);

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            AdminMenuDialog.originHint(exitOrigin),
            AdminMenuDialog.originBack(player, exitOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 2);
    }

    private static void showUndoSelector(@NotNull Player player, @NotNull DialogBridge bridge,
                                          @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__UNDO_TITLE)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<UndoBatch> batches = UNDO_BATCHES.getOrDefault(player.getUniqueId(), List.of());
        List<DialogBodyEntry> body = new ArrayList<>();
        if (batches.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__NO_HISTORY)),
                TextColor.color(0x888888))));
        }

        List<DialogButton> buttons = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < batches.size(); i++) {
            UndoBatch ub = batches.get(i);
            String dateStr = sdf.format(new Date(ub.timestamp()));
            int count = ub.snapshots().size();
            int idx = i;
            buttons.add(new DialogButton("undo_" + i,
                Component.text("↩ " + dateStr + " (" + count + stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__BLOCKS_SUFFIX)) + ")", NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__RESTORE_HINT))
                    .replace("{count}", String.valueOf(count)), TextColor.color(0x888888)),
                p -> {
                    p.closeInventory();
                    executeUndo(player, ub);
                    showUndoSelector(player, bridge, backOrigin);
                }
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? p -> {} : p -> showWorldSelector(p, bridge, backOrigin)
        );

        bridge.showMultiAction(player, title, body, buttons, exitBtn, 1);
    }

    private static void showConfirm(@NotNull Player player, @NotNull String worldName,
                                     @NotNull DialogBridge bridge, @NotNull DialogOrigin backOrigin) {
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__WORLD_PROT_DEL__CONFIRM_TITLE)),
            PASTEL_CORAL, TextDecoration.BOLD
        );

        Component worldComp = Component.text(worldName, PASTEL_GOLD);

        DialogButton yesBtn = new DialogButton("confirm",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CONFIRM)),
                PASTEL_CORAL, TextDecoration.BOLD),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__WORLD_HINT))
                .replace("{world}", worldName), TextColor.color(0xF0A0A0)),
            p -> executeDelete(player, worldName, bridge, backOrigin)
        );

        DialogButton noBtn = new DialogButton("cancel",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CANCEL)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)),
                TextColor.color(0x888888)),
            p -> show(p, null, backOrigin)
        );

        bridge.showConfirmation(player, title,
            List.of(
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__HEADER))
                    + " - " + worldName + "?", NamedTextColor.WHITE),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__CANT_UNDO)), PASTEL_CORAL)),
            yesBtn, noBtn);
    }

    private static void executeDelete(@NotNull Player player, @NotNull String worldName,
                                       @NotNull DialogBridge bridge, @NotNull DialogOrigin backOrigin) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Component.text(
                stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_WORLD_NOT_FOUND))
                    .replace("{world}", worldName), PASTEL_CORAL));
            show(player, null, backOrigin);
            return;
        }

        player.sendMessage(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__PROTDEL__DELETING))
                .replace("{world}", worldName), SOFT_GRAY));

        HybridDatabase db = BlockProt.getHybridDatabase();
        final List<Location> locations;

        if (db != null && db.isEnabled()) {
            locations = db.getBlockIndexByWorld(world.getName());
        } else {
            locations = new ArrayList<>();
            Chunk[] chunks = world.getLoadedChunks();
            for (Chunk chunk : chunks) {
                for (BlockState state : chunk.getTileEntities()) {
                    locations.add(state.getLocation());
                }
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getState() instanceof TileState) continue;
                            Material t = block.getType();
                            if (t == Material.CHEST || t.name().contains("SHULKER") || t.name().endsWith("_DOOR")
                                || t.name().contains("FURNACE") || t.name().contains("HOPPER")
                                || t.name().contains("DISPENSER") || t.name().contains("DROPPER")
                                || t.name().contains("BARREL") || t.name().contains("SIGN")
                                || t.name().contains("ANVIL") || t.name().contains("CAULDRON")
                                || t.name().contains("GRINDSTONE") || t.name().contains("BELL")
                                || t.name().contains("CRAFTING") || t.name().contains("LOOM")
                                || t.name().contains("CARTOGRAPHY") || t.name().contains("SMITHING")
                                || t.name().contains("STONECUTTER") || t.name().contains("BREWING")
                                || t.name().contains("ENCHANTING") || t.name().contains("JUKEBOX")
                                || t.name().contains("BEE")) {
                                locations.add(block.getLocation());
                            }
                        }
                    }
                }
            }
        }

        final List<WorldProtDeleteConfirmInventory.ProtectionSnapshot> snapshots = new ArrayList<>();
        final int[] counter = {0};
        final int[] idx = {0};

        World w = world;
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (idx[0] < locations.size() && processed < BATCH_PER_TICK) {
                    Location loc = locations.get(idx[0]++);
                    processed++;
                    if (loc.getWorld() == null) continue;
                    Block block = loc.getBlock();
                    try {
                        BlockNBTHandler handler = new BlockNBTHandler(block);
                        if (!handler.isProtected()) continue;
                        String owner = handler.getOwner();
                        List<String> friends = handler.getFriends().stream()
                            .map(f -> f.getName()).collect(Collectors.toList());
                        snapshots.add(new WorldProtDeleteConfirmInventory.ProtectionSnapshot(
                            loc.clone(), owner, friends));
                        handler.clear();
                        counter[0]++;
                        try { handler.applyToOtherContainer(); } catch (RuntimeException ignored) {}
                        HopperEventListener.invalidate(block);
                        ProtectedBlockCache.unmark(block);
                        if (owner != null && !owner.isEmpty()) {
                            try { StatHandler.removeContainerByUuid(UUID.fromString(owner), loc.clone()); }
                            catch (IllegalArgumentException ignored) {}
                        }
                    } catch (RuntimeException ignored) {}
                }

                if (idx[0] >= locations.size()) {
                    cancel();
                    List<UndoBatch> batches = UNDO_BATCHES.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
                    batches.add(0, new UndoBatch(System.currentTimeMillis(), snapshots));
                    String msg = counter[0] == 0
                        ? stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_NONE))
                            .replace("{world}", worldName)
                        : stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_DONE))
                            .replace("{world}", worldName)
                            .replace("{count}", String.valueOf(counter[0]));
                    player.sendMessage(Component.text(msg, counter[0] == 0 ? SOFT_GRAY : PASTEL_MINT));
                    show(player, null, backOrigin);
                }
            }
        }.runTaskTimer(BlockProt.getInstance(), 0L, 1L);
    }

    private static void executeUndo(@NotNull Player player, @NotNull UndoBatch batch) {
        List<UndoBatch> playerBatches = UNDO_BATCHES.get(player.getUniqueId());
        if (playerBatches != null) {
            playerBatches.remove(batch);
        }

        List<WorldProtDeleteConfirmInventory.ProtectionSnapshot> snapshots = batch.snapshots();
        if (snapshots.isEmpty()) return;

        int[] restored = {0};
        int[] idx = {0};

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (idx[0] < snapshots.size() && processed < BATCH_PER_TICK) {
                    var snap = snapshots.get(idx[0]++);
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
                        try { StatHandler.addBlockByUuid(UUID.fromString(snap.ownerUuid()), snap.location().clone()); }
                        catch (IllegalArgumentException ignored) {}
                        restored[0]++;
                    } catch (RuntimeException ignored) {}
                }

                if (idx[0] >= snapshots.size()) {
                    cancel();
                    String msg = stripColor(Translator.get(TranslationKey.MESSAGES__WORLD_PROT_DEL_UNDO_DONE))
                        .replace("{count}", String.valueOf(restored[0]));
                    player.sendMessage(Component.text(msg, PASTEL_MINT));
                }
            }
        }.runTaskTimer(BlockProt.getInstance(), 0L, 1L);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
    }
}
