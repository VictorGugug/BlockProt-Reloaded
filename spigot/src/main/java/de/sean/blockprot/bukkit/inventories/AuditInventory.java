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
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.audit.AuditLogger.AuditEntry;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.SkinCache;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI that shows the access history for a protected block or entity.
 * Owners see denied attempts. Admins also get a teleport button.
 */
@SuppressWarnings("deprecation")
public final class AuditInventory extends BlockProtInventory {

    public AuditInventory() { super(true); }

    private static final int PAGE_SIZE = 45;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM HH:mm");

    private List<AuditEntry> entries = new ArrayList<>();
    private String blockWorld;
    private int blockX, blockY, blockZ;
    private String selectedPlayerUuid;

    private Entity entitySource = null;

    private static final int GROUP_PAGE_SIZE = PAGE_SIZE;

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__AUDIT__TITLE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack item = event.getCurrentItem();
        if (item == null) {
            event.setCancelled(true);
            return;
        }

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> {
                if (selectedPlayerUuid != null) {
                    selectedPlayerUuid = null;
                    state.currentPageIndex = 0;
                    closeAndOpen(player, fill(player));
                    break;
                }

                if (entitySource != null) {
                    EntityNBTHandler eHandler = new EntityNBTHandler(entitySource);
                    closeAndOpen(player, new BlockLockInventory().fillForEntity(player, entitySource, eHandler));
                    break;
                }

                if (state.getBlock() != null) {
                    var handler = getNbtHandlerOrNull(state.getBlock());
                    closeAndOpen(player, handler == null ? null
                        : new BlockLockInventory().fill(player, state.getBlock().getType(), handler));
                } else {
                    closeAndOpen(player, null);
                }
            }
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex > 0) {
                    state.currentPageIndex--;
                    closeAndOpen(player, fill(player));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                int maxPage = (int) Math.ceil(entries.size() / (double) PAGE_SIZE);
                if (state.currentPageIndex < maxPage - 1) {
                    state.currentPageIndex++;
                    closeAndOpen(player, fill(player));
                }
            }
            case COMPASS -> {
                if (player.hasPermission(Permissions.USER_ADMIN.key())) {
                    var world = Bukkit.getWorld(blockWorld);
                    if (world != null) {
                        player.closeInventory();
                        player.teleport(new Location(world, blockX + 0.5, blockY + 1, blockZ + 0.5));
                    }
                }
            }
            case PLAYER_HEAD -> {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && selectedPlayerUuid == null) {
                    NamespacedKey key = new NamespacedKey(BlockProt.getInstance(), "audit_player_uuid");
                    if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        selectedPlayerUuid = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        state.currentPageIndex = 0;
                        closeAndOpen(player, fill(player));
                    }
                }
            }
            default -> {}
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @Nullable
    public Inventory fill(@NotNull Player player) {
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return null;

        AuditLogger audit = BlockProt.getAuditLogger();
        Block block = state.getBlock();

        inventory.clear();

        if (audit == null || (block == null && entitySource == null)) {
            setItemStack(22, Material.BARRIER, Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES));
            setBackButton();
            return inventory;
        }

        if (block != null) {
            blockWorld = block.getWorld().getName();
            blockX = block.getX();
            blockY = block.getY();
            blockZ = block.getZ();
        }
        if (entries.isEmpty()) {
            entries = audit.getEntriesForBlock(blockWorld, blockX, blockY, blockZ, 500);
        }

        String ownerUuid = null;
        if (block != null) {
            try {
                BlockNBTHandler ownerHandler = new BlockNBTHandler(block);
                if (ownerHandler.isProtected()) ownerUuid = ownerHandler.getOwner();
            } catch (RuntimeException ignored) {}
        } else if (entitySource != null) {
            try {
                EntityNBTHandler ownerHandler = new EntityNBTHandler(entitySource);
                if (ownerHandler.isProtected()) ownerUuid = ownerHandler.getOwner();
            } catch (RuntimeException ignored) {}
        }
        final String ownerFilter = ownerUuid;
        if (ownerFilter != null) {
            entries.removeIf(e -> ownerFilter.equals(e.playerUuid()));
        }

        if (entries.isEmpty()) {
            setItemStack(22, Material.PAPER, Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES));
            setBackButton();
            return inventory;
        }

        Map<String, List<AuditEntry>> groupedEntries = new LinkedHashMap<>();
        for (AuditEntry entry : entries) {
            groupedEntries.computeIfAbsent(entry.playerUuid(), k -> new ArrayList<>()).add(entry);
        }

        int offset = state.currentPageIndex * GROUP_PAGE_SIZE;
        List<AuditEntry> displayEntries;
        if (selectedPlayerUuid == null) {
            displayEntries = new ArrayList<>();
            groupedEntries.values().forEach(group -> displayEntries.add(group.get(0)));
        } else {
            displayEntries = groupedEntries.getOrDefault(selectedPlayerUuid, new ArrayList<>());
        }

        if (displayEntries.isEmpty()) {
            setItemStack(22, Material.PAPER, Translator.get(TranslationKey.INVENTORIES__AUDIT__NO_ENTRIES));
            setBackButton();
            return inventory;
        }

        int count = Math.min(GROUP_PAGE_SIZE, displayEntries.size() - offset);
        for (int i = 0; i < count; i++) {
            AuditEntry entry = displayEntries.get(offset + i);
            @SuppressWarnings("deprecation")
            PlayerProfile profile = BlockProtInventory.createPlayerProfile(UUID.fromString(entry.playerUuid()),
                entry.playerName() != null ? entry.playerName() : entry.playerUuid());

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
            var skullMeta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
            if (skullMeta != null) {
                try {
                    applyOwnerProfile(skullMeta, profile);
                    NamespacedKey key = new NamespacedKey(BlockProt.getInstance(), "audit_player_uuid");
                    skullMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, entry.playerUuid());
                } catch (Exception ignored) { }

                String displayName;
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                if (selectedPlayerUuid == null) {
                    List<AuditEntry> group = groupedEntries.get(entry.playerUuid());
                    int total = group == null ? 1 : group.size();
                    AuditEntry latest = group == null ? entry : group.get(0);
                    displayName = actionLabel(latest.action()) + ": " + (latest.playerName() != null ? latest.playerName() : latest.playerUuid());
                    String actionCountLabel = Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_COUNT).replace("{count}", String.valueOf(total));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(DATE_FMT.format(new Date(latest.timestamp())) + ", " + actionCountLabel));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(latest.world() + " " + latest.x() + "," + latest.y() + "," + latest.z()));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(Translator.get(TranslationKey.INVENTORIES__AUDIT__CLICK_HINT)));
                } else {
                    displayName = actionLabel(entry.action()) + ": " + (entry.playerName() != null ? entry.playerName() : entry.playerUuid());
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(DATE_FMT.format(new Date(entry.timestamp())) + ": " + actionLabel(entry.action())));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(entry.world() + " " + entry.x() + "," + entry.y() + "," + entry.z()));
                }

                ComponentMessages.displayName(skullMeta, net.kyori.adventure.text.Component.text(displayName));
                ComponentMessages.lore(skullMeta, lore);
                skull.setItemMeta(skullMeta);
            }

            inventory.setItem(i, skull);

            final int skullSlot = i;
            final String skinName = entry.playerName() != null ? entry.playerName() : entry.playerUuid();
            final UUID skinUuid = UUID.fromString(entry.playerUuid());
            SkinCache.getOrFetchAsync(skinName, skinUuid).thenAcceptAsync(freshProfile -> {
                if (inventory.getViewers().isEmpty()) return;
                ItemStack existing = inventory.getItem(skullSlot);
                if (existing == null || existing.getType() != Material.PLAYER_HEAD) return;
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) existing.getItemMeta();
                if (sm != null) {
                    try { sm.setOwnerProfile(freshProfile); } catch (Throwable ignored2) {}
                    existing.setItemMeta(sm);
                }
            }, runnable -> Bukkit.getScheduler().runTask(BlockProt.getInstance(), runnable));
        }

        setItemStack(45, Material.CYAN_STAINED_GLASS_PANE,  TranslationKey.INVENTORIES__LAST_PAGE);
        setItemStack(46, Material.BLUE_STAINED_GLASS_PANE,  TranslationKey.INVENTORIES__NEXT_PAGE);
        if (selectedPlayerUuid == null && player.hasPermission(Permissions.USER_ADMIN.key())) {
            setItemStack(49, Material.COMPASS, TranslationKey.INVENTORIES__AUDIT__TELEPORT);
        }

        if (selectedPlayerUuid != null) {
            setItemStack(52, Material.PAPER, TranslationKey.INVENTORIES__AUDIT__PLAYER_HISTORY);
        }

        setBackButton(53);
        return inventory;
    }

    @Nullable
    public Inventory fillForEntity(@NotNull Player player, @NotNull Entity entity) {
        this.entitySource = entity;
        var loc = entity.getLocation();
        this.blockWorld = loc.getWorld() != null ? loc.getWorld().getName() : "";
        this.blockX = loc.getBlockX();
        this.blockY = loc.getBlockY();
        this.blockZ = loc.getBlockZ();
        return fill(player);
    }

    /** Returns a human-readable label for each audit action type. */
    private static String actionLabel(@NotNull AuditLogger.Action action) {
        return switch (action) {
            case ACCESS_DENIED -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_ACCESS_DENIED);
            case OPENED        -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_OPENED);
            case ITEM_TAKEN    -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_ITEM_TAKEN);
            case ITEM_PLACED   -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_ITEM_PLACED);
            case RAID_EXPLOSION -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_RAID_EXPLOSION);
            default            -> Translator.get(TranslationKey.INVENTORIES__AUDIT__ACTION_UNKNOWN);
        };
    }

    private static void applyOwnerProfile(
            @NotNull org.bukkit.inventory.meta.SkullMeta meta,
            @NotNull PlayerProfile profile) {
        meta.setOwnerProfile(profile);
    }
}