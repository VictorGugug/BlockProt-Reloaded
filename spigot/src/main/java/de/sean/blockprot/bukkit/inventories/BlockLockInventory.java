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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.tasks.VillagerLocateTask;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.function.Consumer;


public class BlockLockInventory extends BlockProtInventory {
    public BlockLockInventory() { super(true); }

    @Override int getSize() { return InventoryConstants.doubleLine; }

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
                    final Block nameBlock = block;
                    var currentName = new BlockNBTHandler(nameBlock).getName();
                    Consumer<String> handleName = text -> {
                        new BlockNBTHandler(nameBlock).setName(text);
                        Inventory inventory = new BlockLockInventory().fill(player, nameBlock.getType(), new BlockNBTHandler(nameBlock));
                        if (inventory != null) player.openInventory(inventory);
                    };
                    AnvilInput.open(player, BlockProt.getInstance(), currentName,
                        Translator.get(TranslationKey.INVENTORIES__SET_BLOCK_NAME), handleName);
                }
                case ENDER_PEARL -> {
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
                case EMERALD -> {
                    // Locate the villager linked to this workstation via particles.
                    player.closeInventory();
                    int seconds = BlockProt.getDefaultConfig().getVillagerLocateSeconds();
                    boolean found = VillagerLocateTask.startIfLinked(player, block, seconds);
                    if (!found) {
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.MESSAGES__NO_PERMISSION)));
                    }
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
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(@NotNull Player player, Material material, BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        var isNotProtected = handler.isNotProtected();
        if (isNotProtected && state.menuPermissions.size() == 1
                && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.INFO)) {
            return null;
        }

        boolean isAdmin        = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isStorageBlock = isStorageType(material);
        boolean isDisplayBlock = isDisplayType(material);
        boolean isTraversalBlock = isTraversalType(material);

        // Slot 0: lock / unlock
        if (state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.LOCK)) {
            setItemStack(0, getProperMaterial(material),
                isNotProtected
                    ? Translator.get(TranslationKey.INVENTORIES__LOCK)
                    : Translator.get(TranslationKey.INVENTORIES__UNLOCK),
                Collections.emptyList());
        }

        // Row 1 manager items
        if (!isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER)) {
            fillManagerItems(state, player, handler, isStorageBlock, isDisplayBlock, isTraversalBlock);
        }

        // Row 2
        boolean isOwnerOrAdmin = handler.isOwner(player.getUniqueId()) || isAdmin;
        boolean showInspect = !isNotProtected && isStorageBlock
            && (state.getBlock() != null && state.getBlock().getState() instanceof InventoryHolder)
            && isOwnerOrAdmin;
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
            setItemStack(16, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
        } else if (!isNotProtected && isAdmin) {
            setItemStack(16, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
        }
        setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
        return inventory;
    }

    /**
     * Fills a simplified protection menu for an entity (item frame, chest boat, minecart).
     * Entities do not have hopper/piston/redstone protection options, and there is no
     * inspect-contents button for item frames.
     */
    @Nullable
    public Inventory fillForEntity(@NotNull Player player, @NotNull EntityNBTHandler handler) {
        if (!player.hasPermission(Permissions.USER.key())) return null;

        boolean isAdmin     = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isProtected = handler.isProtected();
        boolean isOwner     = handler.isOwner(player.getUniqueId().toString());
        boolean canManage   = isOwner || isAdmin;

        setItemStack(0, Material.NAME_TAG,
            isProtected
                ? Translator.get(TranslationKey.INVENTORIES__UNLOCK)
                : Translator.get(TranslationKey.INVENTORIES__LOCK),
            Collections.emptyList());

        if (isProtected && canManage) {
            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                setItemStack(1, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__MANAGE);
            }
        }

        setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
        return inventory;
    }

    // ── Context helpers ───────────────────────────────────────────────────────

    private static boolean isStorageType(@NotNull Material m) {
        String n = m.name();
        return n.contains("CHEST") || n.equals("BARREL") || n.contains("SHULKER_BOX")
            || n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")
            || n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE")
            || n.equals("BREWING_STAND") || n.equals("JUKEBOX")
            || n.equals("CHISELED_BOOKSHELF") || n.equals("DECORATED_POT") || n.equals("CRAFTER")
            || n.endsWith("_SHELF");
    }

    private static boolean isDisplayType(@NotNull Material m) {
        String n = m.name();
        return n.equals("LECTERN") || n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN")
            || n.endsWith("_HANGING_SIGN") || n.endsWith("_WALL_HANGING_SIGN")
            || n.equals("BEEHIVE") || n.equals("BEE_NEST");
    }

    private static boolean isTraversalType(@NotNull Material m) {
        String n = m.name();
        return (n.endsWith("_DOOR") && !n.contains("TRAP")) || n.contains("TRAPDOOR") || n.contains("FENCE_GATE");
    }

    private void fillManagerItems(
            @NotNull InventoryState state,
            @NotNull Player player,
            @NotNull BlockNBTHandler handler,
            boolean isStorage,
            boolean isDisplay,
            boolean isTraversal) {
        int offset = 1;

        if (isStorage || isTraversal) {
            setItemStack(offset++, Material.REDSTONE, TranslationKey.INVENTORIES__REDSTONE__SETTINGS);
        }

        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setItemStack(offset++, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__MANAGE);
        }

        setItemStack(offset++, Material.NAME_TAG, TranslationKey.INVENTORIES__SET_BLOCK_NAME);
        setItemStack(offset++, Material.ENDER_PEARL, TranslationKey.INVENTORIES__TRANSFER__BUTTON);

        // Locate Villager button: appears when the block is a workstation that may have a linked villager.
        if (state.getBlock() != null && isWorkstation(state.getBlock().getType())) {
            setItemStack(offset++, Material.EMERALD, TranslationKey.INVENTORIES__LOCATE_VILLAGER);
        }

        if (isStorage && offset < InventoryConstants.doubleLine / 2
                && BlockProt.getDefaultConfig().isProtectionExpiryEnabled()) {
            long exp = handler.getExpiresAt();
            setItemStack(offset, exp > 0 ? Material.LIME_DYE : Material.HOPPER,
                exp > 0
                    ? TranslationKey.INVENTORIES__BLOCK_LOCK_EXPIRY_SET
                    : TranslationKey.INVENTORIES__BLOCK_LOCK_EXPIRY_UNSET);
        }
    }

    private static boolean isWorkstation(@NotNull Material material) {
        String name = material.name();
        return name.equals("GRINDSTONE") || name.equals("STONECUTTER") || name.equals("LOOM")
            || name.equals("CARTOGRAPHY_TABLE") || name.equals("SMITHING_TABLE")
            || name.equals("ENCHANTING_TABLE") || name.equals("FLETCHING_TABLE")
            || name.equals("LECTERN") || name.equals("COMPOSTER") || name.equals("BREWING_STAND")
            || name.equals("BLAST_FURNACE") || name.equals("SMOKER") || name.equals("BARREL")
            || name.equals("CAULDRON");
    }
}
