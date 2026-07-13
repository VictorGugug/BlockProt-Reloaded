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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.inventories.*;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.tasks.VillagerLocateTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BlockLockDialog {

    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private BlockLockDialog() {}

    public static void showBlock(@NotNull Player player, @NotNull Block block) {
        showBlock(player, block, DialogOrigin.NONE);
    }

    public static void showBlock(@NotNull Player player, @NotNull Block block, @NotNull DialogOrigin backOrigin) {
        BlockNBTHandler handler;
        try {
            handler = new BlockNBTHandler(block);
        } catch (RuntimeException e) {
            return;
        }
        show(player, block, handler, backOrigin);
    }

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        show(player, block, handler, DialogOrigin.NONE);
    }

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        boolean isAdmin = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isNotProtected = handler.isNotProtected();
        boolean isOwnerOrAdmin = handler.isOwner(player.getUniqueId()) || isAdmin;
        boolean isStorageBlock = isStorageType(block.getType());
        boolean isTraversalBlock = isTraversalType(block.getType());
        boolean canManage = !isNotProtected && handler.isOwner(player.getUniqueId());
        boolean showInspect = !isNotProtected && isStorageBlock
            && block.getState() instanceof InventoryHolder && isOwnerOrAdmin;
        boolean hasAudit = !isNotProtected && isOwnerOrAdmin && BlockProt.getAuditLogger() != null;
        boolean hasClipboard = PlayerInventoryClipboard.contains(player.getUniqueId().toString());

        String materialName = formatMaterialName(block.getType().name());
        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(materialName, SOFT_GRAY)));
        if (!isNotProtected) {
            String ownerName = getOwnerName(handler);
            if (ownerName != null) {
                body.add(DialogBodyEntry.text(Component.text()
                    .append(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)), SOFT_GRAY))
                    .append(Component.text(" " + ownerName, NamedTextColor.WHITE))
                    .build()));
            }
        }

        InventoryState existing = InventoryState.get(player.getUniqueId());
        InventoryState state;
        if (existing != null && existing.getBlock() == block) {
            state = existing;
        } else {
            state = new InventoryState(block);
            state.menuPermissions.add(BlockAccessMenuEvent.MenuPermission.LOCK);
            if (isOwnerOrAdmin) state.menuPermissions.add(BlockAccessMenuEvent.MenuPermission.MANAGER);
            InventoryState.set(player.getUniqueId(), state);
        }
        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;

        List<DialogButton> actions = new ArrayList<>();

        actions.add(actionBtn(
            isNotProtected
                ? stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK))
                : stripColor(Translator.get(TranslationKey.INVENTORIES__UNLOCK)),
            NamedTextColor.WHITE,
            p -> {
                if (isNotProtected) {
                    handler.setOwner(p.getUniqueId().toString());
                    handler.applyToOtherContainer();
                } else {
                    handler.clear();
                    handler.applyToOtherContainer();
                }
                show(p, block, handler);
            }
        ));

        if (canManage) {
            if (isStorageBlock || isTraversalBlock) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE)),
                    SOFT_BLUE,
                    p -> BlockSettingsDialog.show(p, block, handler)
                ));
            }

            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)),
                    PASTEL_PURPLE,
                    p -> FriendManageDialog.showForBlock(p, block, handler)
                ));
            }

            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME)),
                PASTEL_MINT,
                p -> {
                    p.closeInventory();
                    final Block nameBlock = block;
                    var currentName = new BlockNBTHandler(nameBlock).getName();
                    java.util.function.Consumer<String> handleName = text -> {
                        new BlockNBTHandler(nameBlock).setName(text);
                        BlockLockDialog.show(p, nameBlock, new BlockNBTHandler(nameBlock));
                    };
                    AnvilInput.open(p, BlockProt.getInstance(), currentName,
                        Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME), handleName);
                }
            ));

            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__TRANSFER__BUTTON)),
                PASTEL_CORAL,
                p -> {
                    p.closeInventory();
                    TransferSearchInventory.openSearch(p, block);
                }
            ));

            if (isWorkstation(block.getType())) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__LOCATE_VILLAGER)),
                    PASTEL_GOLD,
                    p -> {
                        p.closeInventory();
                        int seconds = BlockProt.getDefaultConfig().getVillagerLocateSeconds();
                        boolean found = VillagerLocateTask.startIfLinked(p, block, seconds);
                        if (!found) {
                            p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
                        }
                    }
                ));
            }

            if (hasClipboard) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__PASTE_CONFIGURATION)),
                    NamedTextColor.WHITE,
                    p -> {
                        var container = PlayerInventoryClipboard.get(p.getUniqueId().toString());
                        if (handler != null && container != null) {
                            handler.pasteNbt(container);
                            PlayerInventoryClipboard.remove(p.getUniqueId().toString());
                            p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                Translator.get(TranslationKey.MESSAGES__PASTE_DONE)));
                        }
                        show(p, block, handler);
                    }
                ));
            }

            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__COPY_CONFIGURATION)),
                NamedTextColor.WHITE,
                p -> {
                    PlayerInventoryClipboard.set(p.getUniqueId().toString(), handler.getNbtCopy());
                    p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__COPY_DONE)));
                    show(p, block, handler);
                }
            ));
        }

        if (showInspect) {
            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__INSPECT_CONTENTS)),
                SOFT_BLUE,
                p -> {
                    InventoryHolder holder = (InventoryHolder) block.getState();
                    org.bukkit.inventory.Inventory inv = holder.getInventory();
                    List<DialogBodyEntry> inspectBody = new ArrayList<>();
                    for (int i = 0; i < Math.min(inv.getSize(), 54); i++) {
                        org.bukkit.inventory.ItemStack item = inv.getItem(i);
                        if (item != null && !item.getType().isAir()) {
                            String itemName = formatMaterialName(item.getType().name());
                            int amount = item.getAmount();
                            Component line = Component.text()
                                .append(Component.text(itemName, NamedTextColor.WHITE))
                                .append(Component.text(" x" + amount, SOFT_GRAY))
                                .build();
                            inspectBody.add(DialogBodyEntry.text(line));
                        }
                    }
                    if (inspectBody.isEmpty()) {
                        inspectBody.add(DialogBodyEntry.text(Component.text(
                            stripColor(Translator.get(TranslationKey.INVENTORIES__INSPECT_CONTENTS_EMPTY)), SOFT_GRAY)));
                    }
                    bridge.showNotice(p,
                        Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__INSPECT_CONTENTS)),
                            SOFT_BLUE, TextDecoration.BOLD),
                        inspectBody.stream().map(e -> e.text() != null ? e.text() : Component.text("")).toList(),
                        new DialogButton("exit",
                            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                            pl -> show(pl, block, handler, backOrigin)));
                }
            ));
        }

        if (hasAudit) {
            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__OPEN)),
                TextColor.color(0xE8A0A0),
                    p -> AuditDialog.show(p, block)
            ));
        }

        if (canManage || (!isNotProtected && isAdmin)) {
            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE)),
                SOFT_BLUE,
                p -> BlockInfoDialog.show(p, block, handler)
            ));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? p -> {} : (backOrigin == DialogOrigin.ADMIN_MENU ? p -> AdminMenuDialog.show(p) : p -> {})
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn, 3);
    }

    public static void showForEntity(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler) {
        showForEntity(player, entity, handler, DialogOrigin.NONE);
    }

    public static void showForEntity(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        boolean isAdmin = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isProtected = handler.isProtected();
        boolean canManage = handler.isManager(player.getUniqueId().toString()) || isAdmin;
        boolean hasInv = entity instanceof InventoryHolder;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK)),
            PASTEL_GOLD, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(formatMaterialName(entity.getType().name()), SOFT_GRAY)));

        InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
        state.entityUUID = entity.getUniqueId();
        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;

        List<DialogButton> actions = new ArrayList<>();

        actions.add(actionBtn(
            isProtected
                ? stripColor(Translator.get(TranslationKey.INVENTORIES__UNLOCK))
                : stripColor(Translator.get(TranslationKey.INVENTORIES__LOCK)),
            NamedTextColor.WHITE,
            p -> {
                if (!isProtected) {
                    if (p.hasPermission(Permissions.USER.key())) {
                        handler.setOwner(p.getUniqueId().toString());
                    }
                } else {
                    if (canManage) {
                        handler.clearOwner();
                    } else {
                        p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
                    }
                }
                showForEntity(p, entity, handler);
            }
        ));

        if (isProtected && canManage) {
            if (hasInv) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE)),
                    SOFT_BLUE,
                    p -> EntityBlockSettingsDialog.show(p, entity, handler)
                ));
            }

            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)),
                    PASTEL_PURPLE,
                    p -> EntityFriendManageDialog.show(p, entity, handler)
                ));
            }

            if (hasInv) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__TRANSFER__BUTTON)),
                    PASTEL_CORAL,
                    p -> p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.INVENTORIES__ENTITY__TRANSFER_NOT_AVAILABLE)))
                ));

                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__ENTITY__INSPECT_CONTENTS)),
                    SOFT_BLUE,
                    p -> {
                        InventoryHolder holder = (InventoryHolder) entity;
                        org.bukkit.inventory.Inventory inv = holder.getInventory();
                        List<Component> inspectBody = new ArrayList<>();
                        for (int i = 0; i < Math.min(inv.getSize(), 54); i++) {
                            org.bukkit.inventory.ItemStack item = inv.getItem(i);
                            if (item != null && !item.getType().isAir()) {
                                String itemName = formatMaterialName(item.getType().name());
                                inspectBody.add(Component.text()
                                    .append(Component.text(itemName, NamedTextColor.WHITE))
                                    .append(Component.text(" x" + item.getAmount(), SOFT_GRAY))
                                    .build());
                            }
                        }
                        if (inspectBody.isEmpty()) {
                            inspectBody.add(Component.text(
                                stripColor(Translator.get(TranslationKey.INVENTORIES__INSPECT_CONTENTS_EMPTY)), SOFT_GRAY));
                        }
                        bridge.showNotice(p,
                            Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__ENTITY__INSPECT_CONTENTS)),
                                SOFT_BLUE, TextDecoration.BOLD),
                            inspectBody,
                            new DialogButton("exit",
                                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
                                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
                                pl -> showForEntity(pl, entity, handler, backOrigin)));
                    }
                ));
            }

            if (BlockProt.getAuditLogger() != null) {
                actions.add(actionBtn(
                    stripColor(Translator.get(TranslationKey.INVENTORIES__AUDIT__OPEN)),
                    TextColor.color(0xE8A0A0),
                    p -> AuditDialog.show(p, entity.getLocation())
                ));
            }

            actions.add(actionBtn(
                stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE)),
                SOFT_BLUE,
                    p -> EntityInfoDialog.show(p, entity, handler)
            ));
        }

        DialogOrigin exitOrigin2 = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton exitBtn2 = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin2 == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin2 == DialogOrigin.NONE ? TranslationKey.DIALOGS__RETURN_PREVIOUS : TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin2 == DialogOrigin.NONE ? p -> {} : p -> {}
        );

        bridge.showMultiAction(player, title, body, actions, exitBtn2, 3);
    }

    private static DialogButton actionBtn(String label, TextColor color, DialogButton.DialogClickHandler handler) {
        return new DialogButton("action_" + label.hashCode(),
            Component.text(label, color),
            Component.text(label, TextColor.color(0x888888)),
            handler);
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }

    private static String formatMaterialName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') { sb.append(' '); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }

    private static String getOwnerName(BlockNBTHandler handler) {
        String uuidStr = handler.getOwner();
        if (uuidStr == null || uuidStr.isEmpty()) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isStorageType(Material m) {
        String n = m.name();
        return n.contains("CHEST") || n.equals("BARREL") || n.contains("SHULKER_BOX")
            || n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")
            || n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE")
            || n.equals("BREWING_STAND") || n.equals("JUKEBOX")
            || n.equals("CHISELED_BOOKSHELF") || n.equals("DECORATED_POT") || n.equals("CRAFTER")
            || n.endsWith("_SHELF");
    }

    private static boolean isTraversalType(Material m) {
        String n = m.name();
        return (n.endsWith("_DOOR") && !n.contains("TRAP")) || n.contains("TRAPDOOR") || n.contains("FENCE_GATE");
    }

    private static boolean isWorkstation(Material material) {
        String name = material.name();
        return name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")
            || name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE")
            || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")
            || name.equals("LECTERN") || name.equals("COMPOSTER") || name.equals("BREWING_STAND")
            || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("BARREL")
            || name.equals("CAULDRON");
    }
}
