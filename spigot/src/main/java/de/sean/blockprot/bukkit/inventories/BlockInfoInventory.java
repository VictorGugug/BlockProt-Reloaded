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
import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import de.sean.blockprot.bukkit.util.BlockUtil;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.SkinCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Inventory showing block info: owner, friends, redstone settings, and linked item frame.
 */
@SuppressWarnings("deprecation")
public class BlockInfoInventory extends BlockProtInventory {
    public BlockInfoInventory() { super(true); }
    private final int maxSkulls = getSize() - InventoryConstants.lineLength;

    @Override
    int getSize() {
        return InventoryConstants.sextupletLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE:
                if (state.getBlock() != null) {
                    state.currentPageIndex = 0;
                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : new BlockLockInventory().fill(player, state.getBlock().getType(), handler)
                    );
                }
                break;
            case CYAN_STAINED_GLASS_PANE:
                if (state.getBlock() == null) break;
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;

                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : this.fill(player, handler)
                    );
                }
                break;
            case BLUE_STAINED_GLASS_PANE:
                if (state.getBlock() == null) break;
                final ItemStack lastFriendInInventory = inventory.getItem(maxSkulls - 1);
                if (lastFriendInInventory != null && lastFriendInInventory.getAmount() != 0) {
                    state.currentPageIndex++;

                    BlockNBTHandler handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(
                        player,
                        handler == null
                            ? null
                            : this.fill(player, handler)
                    );
                }
                break;
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {

    }

    @NotNull
    public Inventory fill(Player player, BlockNBTHandler handler) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        String owner = handler.getOwner();
        var friends = handler.getFriends();
        final Material blockMaterial = handler.block != null ? handler.block.getType() : null;
        final String blockTypeName = blockMaterial != null
            ? de.sean.blockprot.bukkit.util.BlockUtil.getHumanReadableBlockName(blockMaterial)
            : null;

        state.friendResultCache.clear();
        this.inventory.clear();

        var filteredFriends = friends.stream()
            .filter(f -> !f.getName().equals(owner))
            .toList();

        var pageOffset = maxSkulls * state.currentPageIndex;
        for (int i = 0; i < Math.min(filteredFriends.size() - pageOffset, maxSkulls); i++) {
            final var uuid = filteredFriends.get(pageOffset + i).getName();

            if (filteredFriends.get(pageOffset + i).doesRepresentPublic()) {
                this.setItemStack(InventoryConstants.lineLength + i, Material.PLAYER_HEAD, TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC);
            } else {
                this.setItemStack(InventoryConstants.lineLength + i, Material.SKELETON_SKULL, uuid);
            }
            state.friendResultCache.add(UUID.fromString(uuid));
        }

        if (!owner.isEmpty()) {
            UUID parsedOwnerUuid = null;
            try { parsedOwnerUuid = UUID.fromString(owner); } catch (Throwable ignored) {}
            final UUID ownerUuid = parsedOwnerUuid != null ? parsedOwnerUuid : player.getUniqueId();

            String initialOwnerName = owner;
            try {
                var off = Bukkit.getOfflinePlayer(ownerUuid);
                if (off.getName() != null) initialOwnerName = off.getName();
            } catch (Throwable ignored) {}

            final int friendCount = filteredFriends.size();

            // Slot 0 gets a skull immediately, from local/cached data only (no
            // network calls on the primary thread). The exact display name and
            // skin texture are resolved off-thread below and applied once, so
            // the head does not visibly change more than once after opening.
            placeOwnerSkull(ownerUuid, initialOwnerName, friendCount);

            final String initialNameForAsync = initialOwnerName;
            Bukkit.getScheduler().runTaskAsynchronously(
                BlockProt.getInstance(),
                () -> {
                    String resolvedName = initialNameForAsync;
                    try {
                        var profile = BlockProt.getProfileService().findByUuid(ownerUuid);
                        if (profile != null && profile.getName() != null) resolvedName = profile.getName();
                    } catch (Throwable ignored) {}
                    final String finalName = resolvedName;

                    SkinCache.getOrFetchAsync(finalName, ownerUuid).whenCompleteAsync(
                        (freshProfile, error) -> {
                            if (!player.isOnline()) return;
                            Inventory top = player.getOpenInventory().getTopInventory();
                            if (top == null || top.getHolder() != BlockInfoInventory.this) return;
                            updateOwnerSkull(top, finalName, friendCount, error == null ? freshProfile : null);
                        },
                        runnable -> Bukkit.getScheduler().runTask(BlockProt.getInstance(), runnable)
                    );
                }
            );
        }

        // Slot 1: show the block with its custom name (if set) as the primary
        // display, falling back to the actual block type (CHEST, FURNACE, ...).
        Material blockMat = handler.block != null ? handler.block.getType() : Material.OAK_SIGN;
        blockMat = getProperMaterial(blockMat);
        final String cleanName = handler.getName().replaceAll("[§&][0-9a-fk-orx]", "");
        final String materialDisplay = blockTypeName != null ? blockTypeName : cleanName;
        final boolean hasCustomName = !cleanName.isEmpty() && !cleanName.equalsIgnoreCase(
            handler.block != null ? handler.block.getType().name() : "");
        final String blockDisplay = hasCustomName ? cleanName : materialDisplay;

        String linkedFrameUuid = handler.getLinkedItemFrameUuid();
        if (!linkedFrameUuid.isEmpty()) {
            org.bukkit.entity.Entity frameEntity = null;
            try { frameEntity = org.bukkit.Bukkit.getEntity(java.util.UUID.fromString(linkedFrameUuid)); } catch (Exception ignored) {}
            String frameLore;
            if (frameEntity instanceof org.bukkit.entity.ItemFrame frame) {
                org.bukkit.inventory.ItemStack frameItem = frame.getItem();
                String itemName = frameItem.getType() == Material.AIR
                    ? Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__EMPTY_FRAME)
                    : frameItem.getType().name().toLowerCase().replace('_', ' ');
                frameLore = Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__LINKED_FRAME)
                    .replace("{item}", itemName);
            } else {
                frameLore = Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__LINKED_FRAME)
                    .replace("{item}", linkedFrameUuid.substring(0, 8) + "...");
            }
            ItemStack item = new ItemStack(blockMat, 1);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(blockDisplay));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                if (hasCustomName && !materialDisplay.equalsIgnoreCase(cleanName)) {
                    lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize("§7" + materialDisplay));
                }
                lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(frameLore));
                ComponentMessages.lore(meta, lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(1, item);
        } else {
            ItemStack item = new ItemStack(blockMat, 1);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                ComponentMessages.displayName(meta, net.kyori.adventure.text.Component.text(blockDisplay));
                if (hasCustomName && !materialDisplay.equalsIgnoreCase(cleanName)) {
                    ComponentMessages.lore(meta, java.util.List.of(
                        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize("§7" + materialDisplay)
                    ));
                }
                item.setItemMeta(meta);
            }
            inventory.setItem(1, item);
        }

        setItemStack(
            InventoryConstants.lineLength - 3,
            Material.CYAN_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__LAST_PAGE
        );
        setItemStack(
            InventoryConstants.lineLength - 2,
            Material.BLUE_STAINED_GLASS_PANE,
            TranslationKey.INVENTORIES__NEXT_PAGE
        );

        RedstoneSettingsHandler redstoneSettingsHandler = handler.getRedstoneHandler();
        setEnchantedOptionItemStack(
            2,
            Material.REDSTONE,
            TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION,
            redstoneSettingsHandler.getCurrentProtection()
        );
        setEnchantedOptionItemStack(
            3,
            Material.HOPPER,
            TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION,
            redstoneSettingsHandler.getHopperProtection()
        );
        setEnchantedOptionItemStack(
            4,
            Material.PISTON,
            TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION,
            redstoneSettingsHandler.getPistonProtection()
        );
        setBackButton(InventoryConstants.lineLength - 1);

        // Resolve friend profiles and refresh their skulls (placeholder first, real skin once it arrives).
        Bukkit.getScheduler().runTaskAsynchronously(
            BlockProt.getInstance(),
            () -> {
                try {
                    final var profiles = BlockProt.getProfileService().findAllByUuid(state.friendResultCache);

                    var offset = state.friendResultCache.contains(FriendSupportingHandler.publicUuid) ? 1 : 0;
                    int i = 0;
                    for (var profile : profiles) {
                        if (profile.getUniqueId().equals(FriendSupportingHandler.publicUuid)) continue;
                        if (i >= maxSkulls) break;
                        final int slot = InventoryConstants.lineLength + offset + i;
                        final String pName = profile.getName() != null
                            ? profile.getName()
                            : profile.getUniqueId().toString().substring(0, 8);
                        final UUID pUuid = profile.getUniqueId();
                        Bukkit.getScheduler().runTask(BlockProt.getInstance(),
                            () -> setPlayerSkullAsync(slot, player, pUuid, pName));
                        i++;
                    }
                } catch (Exception e) {
                    BlockProt.getInstance().getLogger().warning("Failed to update PlayerProfile: " + e.getMessage());
                }
            }
        );

        return inventory;
    }

    /**
     * Places the owner skull at slot 0 from local/cached data only. Catches
     * {@link Throwable}, not just {@link Exception}, since a PlayerProfile
     * API mismatch across server implementations can throw {@link NoSuchMethodError}.
     */
    private void placeOwnerSkull(@NotNull UUID ownerUuid, @NotNull String ownerName, int friendCount) {
        try {
            org.bukkit.profile.PlayerProfile ownerProfile = SkinCache.getCachedOrOnlineProfile(ownerName, ownerUuid);
            if (ownerProfile == null) {
                ownerProfile = createPlayerProfile(ownerUuid, ownerName);
            }
            inventory.setItem(0, buildOwnerSkullItem(ownerName, friendCount, ownerProfile));
        } catch (Throwable e) {
            BlockProt.getInstance().getLogger().warning("Failed to place owner skull in BlockInfoInventory: " + e.getMessage());
        }
    }

    /**
     * Applies the resolved owner name and, if present, skin texture to the
     * skull already sitting in slot 0. Only touches the texture when
     * {@code profile} is non-null, so a failed skin fetch never blanks a
     * skull that was already showing a placeholder texture.
     */
    private void updateOwnerSkull(@NotNull Inventory top, @NotNull String ownerName, int friendCount,
                                   @Nullable org.bukkit.profile.PlayerProfile profile) {
        try {
            ItemStack existing = top.getItem(0);
            if (existing == null || existing.getType() != Material.PLAYER_HEAD) return;
            org.bukkit.inventory.meta.SkullMeta skullMeta =
                (org.bukkit.inventory.meta.SkullMeta) existing.getItemMeta();
            if (skullMeta == null) return;
            if (profile != null) {
                try { skullMeta.setOwnerProfile(profile); } catch (Throwable ignored) {}
            }
            applyOwnerMeta(skullMeta, ownerName, friendCount);
            existing.setItemMeta(skullMeta);
        } catch (Throwable e) {
            BlockProt.getInstance().getLogger().warning("Failed to refresh owner skull in BlockInfoInventory: " + e.getMessage());
        }
    }

    @NotNull
    private ItemStack buildOwnerSkullItem(@NotNull String ownerName, int friendCount,
                                           @Nullable org.bukkit.profile.PlayerProfile ownerProfile) {
        final ItemStack ownerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
        final org.bukkit.inventory.meta.SkullMeta skullMeta =
            (org.bukkit.inventory.meta.SkullMeta) ownerSkull.getItemMeta();
        if (skullMeta != null) {
            if (ownerProfile != null) {
                try { skullMeta.setOwnerProfile(ownerProfile); } catch (Throwable ignored) {}
            }
            applyOwnerMeta(skullMeta, ownerName, friendCount);
            ownerSkull.setItemMeta(skullMeta);
        }
        return ownerSkull;
    }

    private void applyOwnerMeta(@NotNull org.bukkit.inventory.meta.SkullMeta skullMeta, @NotNull String ownerName, int friendCount) {
        ComponentMessages.displayName(skullMeta, net.kyori.adventure.text.Component.text(
            Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)));
        ComponentMessages.lore(skullMeta, java.util.List.of(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(
                    Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LORE)
                        .replace("{player}", ownerName)),
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(
                    Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__FRIEND_COUNT)
                        .replace("{count}", String.valueOf(friendCount)))
        ));
    }
}