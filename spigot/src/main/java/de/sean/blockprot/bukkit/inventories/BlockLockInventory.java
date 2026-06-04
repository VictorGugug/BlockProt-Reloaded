/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.util.DurationParser;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;


public class BlockLockInventory extends BlockProtInventory {
    public BlockLockInventory() { super(true); }
    @Override
    int getSize() {
        return InventoryConstants.doubleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Block block = state.getBlock();
        if (block == null) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        Player player = (Player) event.getWhoClicked();
        if (BlockProt.getDefaultConfig().isLockable(block.getType()) && event.getSlot() == 0) {
            applyChanges(player, (h) -> h.lockBlock(player), null);
            closeAndOpen(player, null);
        } else {
            switch (item.getType()) {
                case PLAYER_HEAD -> {
                    state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                    closeAndOpen(player, new FriendManageInventory().fill(player));
                }
                case REDSTONE -> {
                    state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                    var handler = getNbtHandlerOrNull(block);
                    closeAndOpen(player, handler == null ? null : new RedstoneSettingsInventory().fill(player, state));
                }
                case COMPASS -> {
                    state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                    var handler = getNbtHandlerOrNull(block);
                    if (handler != null) closeAndOpen(player, new BlockInfoInventory().fill(player, handler));
                }
                case OAK_SIGN -> {}
                case KNOWLEDGE_BOOK -> {
                    var handler = getNbtHandlerOrNull(block);
                    var container = PlayerInventoryClipboard.get(player.getUniqueId().toString());
                    if (handler != null && container != null) {
                        handler.pasteNbt(container);
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__PASTE_DONE)));
                    }
                }
                case PAPER -> {
                    var handler = getNbtHandlerOrNull(block);
                    if (handler != null) {
                        PlayerInventoryClipboard.set(player.getUniqueId().toString(), handler.getNbtCopy());
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__COPY_DONE)));
                        closeAndOpen(player, null);
                    }
                }
                case NAME_TAG -> {
                    player.closeInventory();
                    // Capture block reference before close potentially clears the state
                    final Block nameBlock = block;
                    var currentName = new BlockNBTHandler(nameBlock).getName();
                    Consumer<String> handleName = text -> {
                        new BlockNBTHandler(nameBlock).setName(text);
                        Inventory inventory = new BlockLockInventory().fill(player, nameBlock.getType(), new BlockNBTHandler(nameBlock));
                        if (inventory != null) player.openInventory(inventory);
                    };
                    // AnvilInput works across all supported versions (1.20.6 through 26.x).
                    // SignInput is unreliable because SignChangeEvent does not fire consistently
                    // after the fake block is restored server-side.
                    AnvilInput.open(player, BlockProt.getInstance(), currentName,
                        Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME), handleName);
                }
                case ENDER_PEARL -> {
                    // Preserve the block reference before closing the inventory, because
                    // closeInventory() can trigger InventoryCloseEvent listeners that clear
                    // or replace InventoryState, making state.getBlock() null inside the
                    // ChatInput/AnvilInput callback.
                    final Block transferBlock = block;
                    player.closeInventory();
                    TransferSearchInventory.openSearch(player, transferBlock);
                }
                case SPYGLASS -> {
                    state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                    closeAndOpen(player, new BlockInspectContentsInventory(player).fill());
                }
                case CLOCK -> {
                    state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                    closeAndOpen(player, new AuditInventory().fill(player));
                }
                case LIME_DYE -> {
                    var handler = getNbtHandlerOrNull(block);
                    if (handler != null && BlockProt.getDefaultConfig().isProtectionExpiryEnabled()) {
                        handler.setExpiresAt(0L);
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__EXPIRY_CLEARED)));
                        closeAndOpen(player, fill(player, block.getType(), new BlockNBTHandler(block)));
                    }
                }
                case HOPPER -> {
                    if (!BlockProt.getDefaultConfig().isProtectionExpiryEnabled()) break;
                    final Block expiryBlock = block;
                    player.closeInventory();
                    Consumer<String> handleExpiry = text -> {
                        java.time.Duration dur = DurationParser.parse(text);
                        if (dur == null || dur.isZero() || dur.isNegative()) {
                            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                Translator.get(TranslationKey.MESSAGES__EXPIRY_INVALID_DURATION)));
                        } else {
                            long exp = System.currentTimeMillis() + dur.toMillis();
                            new BlockNBTHandler(expiryBlock).setExpiresAt(exp);
                            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                                Translator.get(TranslationKey.MESSAGES__EXPIRY_SET)
                                    .replace("{duration}", DurationParser.format(dur))));
                        }
                        Inventory inv = new BlockLockInventory().fill(player, expiryBlock.getType(), new BlockNBTHandler(expiryBlock));
                        if (inv != null) player.openInventory(inv);
                    };
                    AnvilInput.open(player, BlockProt.getInstance(), "",
                        Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK_EXPIRY_SLOT), handleExpiry);
                }
                case BARRIER -> closeAndOpen(player, null);
            }
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
    }

    public Inventory fill(@NotNull Player player, Material material, BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        var isNotProtected = handler.isNotProtected();
        if (isNotProtected && state.menuPermissions.size() == 1
            && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO)) {
            return null;
        }

        boolean isAdmin = player.hasPermission(Permissions.USER_ADMIN.key());

        // Slot 0: lock / unlock button — no lore, always clean
        if (state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.LOCK)) {
            setItemStack(0, getProperMaterial(material),
                isNotProtected
                    ? Translator.get(TranslationKey.INVENTORIES__LOCK)
                    : Translator.get(TranslationKey.INVENTORIES__UNLOCK),
                Collections.emptyList());
        }

        // ── Row 1: manager items slots 1-4 ───────────────────────────────────
        // Layout (photo): Lock(0) | Redstone(1) | Friends(2) | Name(3) | Transfer(4)
        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER)) {
            fillManagerItems(state, player, handler);
        }

        // ── Row 2 layout: Inspect(9) | ... | Log(13) | Paste(14) | Copy(15) | Info(16) | Back(17) ──
        boolean isOwnerOrAdmin = handler.isOwner(player.getUniqueId()) || isAdmin;
        boolean showInspect = !isNotProtected
            && (state.getBlock().getState() instanceof InventoryHolder)
            && (isAdmin || handler.isOwner(player.getUniqueId()));
        if (showInspect) {
            setItemStack(9, Material.SPYGLASS, TranslationKey.INVENTORIES__INSPECT_CONTENTS);
        }
        if (!isNotProtected && isOwnerOrAdmin && BlockProt.getAuditLogger() != null) {
            setItemStack(13, Material.CLOCK, TranslationKey.INVENTORIES__AUDIT__OPEN);
        }
        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER)) {
            if (PlayerInventoryClipboard.contains(player.getUniqueId().toString())) {
                setItemStack(14, Material.KNOWLEDGE_BOOK, TranslationKey.INVENTORIES__PASTE_CONFIGURATION);
            }
            setItemStack(15, Material.PAPER, TranslationKey.INVENTORIES__COPY_CONFIGURATION);
            setItemStack(16, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO);
        } else if (!isNotProtected && isAdmin) {
            // Admin viewing someone else's block: show block info without full manager permissions.
            setItemStack(16, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO);
        }
        // Slot 17: BARRIER as back button
        setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
        return inventory;
    }

    private void fillManagerItems(@NotNull InventoryState state, @NotNull Player player, @NotNull BlockNBTHandler handler) {
        int offset = 1;
        setItemStack(offset++, Material.REDSTONE, TranslationKey.INVENTORIES__REDSTONE__SETTINGS);
        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setItemStack(offset++, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__MANAGE);
        }
        setItemStack(offset++, Material.NAME_TAG, TranslationKey.INVENTORIES__SET_BLOCK_NAME);
        setItemStack(offset++, Material.ENDER_PEARL, TranslationKey.INVENTORIES__TRANSFER__BUTTON);
        if (offset < InventoryConstants.doubleLine / 2 && BlockProt.getDefaultConfig().isProtectionExpiryEnabled()) {
            long exp = handler.getExpiresAt();
            if (exp > 0) {
                setItemStack(offset, Material.LIME_DYE, TranslationKey.INVENTORIES__BLOCK_LOCK_EXPIRY_SET);
            } else {
                setItemStack(offset, Material.HOPPER, TranslationKey.INVENTORIES__BLOCK_LOCK_EXPIRY_UNSET);
            }
        }
    }
}
