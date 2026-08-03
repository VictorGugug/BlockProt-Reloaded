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
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.nbt.LockReturnValue;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.tasks.VillagerLocateTask;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Main protection configuration inventory for blocks and entities.
 */
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
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        Player player = (Player) event.getWhoClicked();

        if (block == null && state.entityUUID != null) {
            UUID entityId = state.entityUUID;
            Entity entity = player.getServer().getEntity(entityId);
            if (entity != null) {
                EntityNBTHandler eHandler = new EntityNBTHandler(entity);
                String playerUuid = player.getUniqueId().toString();
                boolean isManager = eHandler.isManager(playerUuid)
                    || player.hasPermission(Permissions.USER_ADMIN.key());
                switch (item.getType()) {
                    case NAME_TAG -> {
                        if (eHandler.isProtected()) {
                            if (isManager) {
                                eHandler.clearOwner();
                            } else {
                                sendActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                            }
                        } else {
                            if (player.hasPermission(Permissions.USER.key())) {
                                eHandler.setOwner(playerUuid);
                            }
                        }
                        closeAndOpen(player, null);
                    }
                    case PLAYER_HEAD -> {
                        if (!isManager) break;
                        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                        closeAndOpen(player, new EntityFriendManageInventory().fill(player, entity, eHandler));
                    }
                    case REDSTONE -> {
                        if (!isManager) break;
                        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                        closeAndOpen(player, new EntityBlockSettingsInventory().fill(player, entity, eHandler));
                    }
                    case COMPASS -> {
                        if (!isManager) break;
                        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                        closeAndOpen(player, new EntityInfoInventory().fill(player, entity, eHandler));
                    }
                    case ENDER_PEARL -> {
                        if (!isManager) break;
                        sendActionBar(player, Translator.get(TranslationKey.INVENTORIES__ENTITY__TRANSFER_NOT_AVAILABLE));
                    }
                    case SPYGLASS -> {
                        if (!isManager) break;
                        if (!(entity instanceof InventoryHolder)) break;
                        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                        closeAndOpen(player, new EntityInspectContentsInventory(entity).fill());
                    }
                    case CLOCK -> {
                        if (!isManager) break;
                        state.origin = InventoryState.MenuOrigin.BLOCK_LOCK;
                        closeAndOpen(player, new AuditInventory().fillForEntity(player, entity));
                    }
                case BARRIER -> {
                    if (state.origin != InventoryState.MenuOrigin.NONE) {
                        goBack(player, state);
                    } else {
                        closeAndOpen(player, null);
                    }
                }
                    default -> {}
                }
            } else {
                closeAndOpen(player, null);
            }
            event.setCancelled(true);
            return;
        }

        if (block == null) return;
        if (BlockProt.getDefaultConfig().isLockable(block.getType(), block.getWorld()) && event.getSlot() == 0) {
            BlockNBTHandler nbtHandler = getNbtHandlerOrNull(block);
            if (nbtHandler != null) {
                LockReturnValue ret = nbtHandler.lockBlock(player);
                if (ret.success) {
                    nbtHandler.applyToOtherContainer();
                } else if (ret.reason != null) {
                    sendActionBar(player, Translator.get(ret.reason));
                }
            }
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
                        ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__PASTE_DONE));
                    }
                }
                case PAPER -> {
                    var handler = getNbtHandlerOrNull(block);
                    if (handler != null) {
                        PlayerInventoryClipboard.set(player.getUniqueId().toString(), handler.getNbtCopy());
                        ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__COPY_DONE));
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
                        ComponentMessages.sendLegacyActionBar(player, Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
                    }
                }
                case BARRIER -> {
                    if (state.origin != InventoryState.MenuOrigin.NONE) {
                        goBack(player, state);
                    } else {
                        closeAndOpen(player, null);
                    }
                }
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

        boolean isAdmin          = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isStorageBlock   = isStorageType(material);
        boolean isTraversalBlock = isTraversalType(material);
        boolean isOwnerOrAdmin   = handler.isOwner(player.getUniqueId()) || isAdmin;
        boolean canManage        = !isNotProtected && state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.MANAGER);

        if (state.menuPermissions.contains(BlockAccessMenuEvent.MenuPermission.LOCK)) {
            setItemStack(0, getProperMaterial(material),
                isNotProtected
                    ? Translator.get(TranslationKey.INVENTORIES__LOCK)
                    : Translator.get(TranslationKey.INVENTORIES__UNLOCK),
                Collections.emptyList());
        }

        if (canManage) {
            if (isStorageBlock || isTraversalBlock) {
                setItemStack(1, Material.REDSTONE, TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE);
            }

            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                setItemStack(2, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__MANAGE);
            }

            setItemStack(3, Material.NAME_TAG, TranslationKey.INVENTORIES__SET_BLOCK_NAME);

            setItemStack(4, Material.ENDER_PEARL, TranslationKey.INVENTORIES__TRANSFER__BUTTON);

            if (state.getBlock() != null && isWorkstation(state.getBlock().getType())) {
                setItemStack(5, Material.EMERALD, TranslationKey.INVENTORIES__LOCATE_VILLAGER);
            }
        }

        boolean showInspect = !isNotProtected && isStorageBlock
            && (state.getBlock() != null && state.getBlock().getState() instanceof InventoryHolder)
            && isOwnerOrAdmin;
        if (showInspect) {
            setItemStack(9, Material.SPYGLASS, TranslationKey.INVENTORIES__INSPECT_CONTENTS);
        }

        if (!isNotProtected && isOwnerOrAdmin && BlockProt.getAuditLogger() != null) {
            setItemStack(13, Material.CLOCK, TranslationKey.INVENTORIES__AUDIT__OPEN);
        }

        if (canManage) {
            if (PlayerInventoryClipboard.contains(player.getUniqueId().toString())) {
                setItemStack(14, Material.KNOWLEDGE_BOOK, TranslationKey.INVENTORIES__PASTE_CONFIGURATION);
            }
            setItemStack(15, Material.PAPER, TranslationKey.INVENTORIES__COPY_CONFIGURATION);
        }

        if (canManage || (!isNotProtected && isAdmin)) {
            setItemStack(16, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
        }

        if (state.origin != InventoryState.MenuOrigin.NONE) {
            setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
        } else {
            setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        }
        return inventory;
    }

    @Nullable
    public Inventory fillForEntity(@NotNull Player player, @NotNull EntityNBTHandler handler) {
        if (!player.hasPermission(Permissions.USER.key())) return null;

        boolean isAdmin     = player.hasPermission(Permissions.USER_ADMIN.key());
        boolean isProtected = handler.isProtected();
        boolean canManage   = handler.isManager(player.getUniqueId().toString()) || isAdmin;
        boolean hasInventory = currentEntityType == EntityMenuType.STORAGE_VEHICLE;

        setItemStack(0, Material.NAME_TAG,
            isProtected
                ? Translator.get(TranslationKey.INVENTORIES__UNLOCK)
                : Translator.get(TranslationKey.INVENTORIES__LOCK),
            Collections.emptyList());

        if (isProtected && canManage) {
            if (hasInventory) {
                setItemStack(1, Material.REDSTONE, TranslationKey.INVENTORIES__BLOCK_SETTINGS__TITLE);
            }

            if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
                setItemStack(2, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__MANAGE);
            }

            if (hasInventory) {
                setItemStack(4, Material.ENDER_PEARL, TranslationKey.INVENTORIES__TRANSFER__BUTTON);
            }

            if (hasInventory) {
                setItemStack(9, Material.SPYGLASS, TranslationKey.INVENTORIES__ENTITY__INSPECT_ENTITY);
            }

            if (BlockProt.getAuditLogger() != null) {
                setItemStack(10, Material.CLOCK, TranslationKey.INVENTORIES__AUDIT__OPEN);
            }

            setItemStack(13, Material.COMPASS, TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
        }

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null && state.origin != InventoryState.MenuOrigin.NONE) {
            setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__BACK);
        } else {
            setItemStack(17, Material.BARRIER, TranslationKey.INVENTORIES__ADMIN_MENU__CLOSE);
        }
        return inventory;
    }

    @Nullable
    public Inventory fillForEntity(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler) {
        this.currentEntityType = classifyEntity(entity);
        return fillForEntity(player, handler);
    }

    private EntityMenuType currentEntityType = EntityMenuType.ITEM_FRAME;

    private enum EntityMenuType { ITEM_FRAME, STORAGE_VEHICLE }

    @NotNull
    private static EntityMenuType classifyEntity(@NotNull Entity entity) {
        if (entity instanceof ItemFrame) return EntityMenuType.ITEM_FRAME;
        if (entity instanceof StorageMinecart || entity instanceof HopperMinecart) return EntityMenuType.STORAGE_VEHICLE;
        if (entity instanceof InventoryHolder) return EntityMenuType.STORAGE_VEHICLE;
        return EntityMenuType.ITEM_FRAME;
    }

    private void sendActionBar(@NotNull Player player, @NotNull String text) {
        ComponentMessages.sendLegacyActionBar(player, text);
    }

    private static boolean isStorageType(@NotNull Material m) {
        String n = m.name();
        return n.contains("CHEST") || n.equals("BARREL") || n.contains("SHULKER_BOX")
            || n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")
            || n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE")
            || n.equals("BREWING_STAND") || n.equals("JUKEBOX")
            || n.equals("CHISELED_BOOKSHELF") || n.equals("DECORATED_POT") || n.equals("CRAFTER")
            || n.endsWith("_SHELF");
    }

    private static boolean isTraversalType(@NotNull Material m) {
        String n = m.name();
        return (n.endsWith("_DOOR") && !n.contains("TRAP")) || n.contains("TRAPDOOR") || n.contains("FENCE_GATE");
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
