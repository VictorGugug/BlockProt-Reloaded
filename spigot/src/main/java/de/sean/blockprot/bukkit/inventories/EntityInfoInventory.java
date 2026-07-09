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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Info panel for a protected entity.
 *
 * <p>Mirrors {@link BlockInfoInventory}'s layout exactly: owner skull (slot 0),
 * a name/type label (slot 1), pagination controls, and the paginated friend list.
 */
public final class EntityInfoInventory extends BlockProtInventory {

    private Entity entity;
    private EntityNBTHandler handler;

    private final int maxSkulls = getSize() - InventoryConstants.lineLength;

    public EntityInfoInventory() { super(true); }

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) { event.setCancelled(true); return; }

        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE -> {
                state.currentPageIndex = 0;
                if (entity != null && handler != null) {
                    closeAndOpen(player, new BlockLockInventory().fillForEntity(player, entity, handler));
                } else {
                    closeAndOpen(player, null);
                }
            }
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    closeAndOpen(player, fill(player, entity, handler));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                ItemStack last = event.getInventory().getItem(maxSkulls - 1);
                if (last != null && last.getAmount() != 0) {
                    state.currentPageIndex++;
                    closeAndOpen(player, fill(player, entity, handler));
                }
            }
            default -> {}
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @Nullable
    public Inventory fill(@NotNull Player player, @Nullable Entity entity, @Nullable EntityNBTHandler handler) {
        this.entity  = entity;
        this.handler = handler;

        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null || handler == null || entity == null) return inventory;

        inventory.clear();
        state.friendResultCache.clear();

        String ownerUuid  = handler.getOwner();
        List<String> friendUuids = handler.getFriendUuids();
        int pageOffset = maxSkulls * state.currentPageIndex;

        if (!ownerUuid.isEmpty()) {
            try {
                var profile = BlockProt.getProfileService().findByUuid(UUID.fromString(ownerUuid));
                String ownerName = (profile != null && profile.getName() != null)
                    ? profile.getName() : ownerUuid.substring(0, 8);

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setPlayerProfile(
                        BlockProtInventory.createPlayerProfile(UUID.fromString(ownerUuid), ownerName));
                    meta.displayName(Component.text(
                        Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)));
                    meta.lore(List.of(
                        LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LORE)
                                .replace("{player}", ownerName)),
                        LegacyComponentSerializer.legacySection().deserialize(
                            Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__FRIEND_COUNT)
                                .replace("{count}", String.valueOf(friendUuids.size())))
                    ));
                    skull.setItemMeta(meta);
                }
                inventory.setItem(0, skull);
            } catch (Exception ignored) {}
        }

        var loc = entity.getLocation();
        String locLore = (loc.getWorld() != null ? loc.getWorld().getName() : "?") + " "
            + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        setItemStack(1, Material.OAK_SIGN, entity.getType().name(), List.of("\u00a77" + locLore));

        for (int i = 0; i < Math.min(friendUuids.size() - pageOffset, maxSkulls); i++) {
            String uuid = friendUuids.get(pageOffset + i);
            EntityNBTHandler.FriendEntry entry = handler.getFriendEntry(uuid);
            boolean isManager = entry != null && entry.manager();
            setItemStack(InventoryConstants.lineLength + i, Material.SKELETON_SKULL,
                uuid + (isManager ? " " + Translator.get(TranslationKey.INVENTORIES__FRIENDS__PERMISSION__MANAGER) : ""));
            state.friendResultCache.add(UUID.fromString(uuid));
        }

        setItemStack(maxSkulls,     Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
        setItemStack(maxSkulls + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        setBackButton();

        final List<UUID> uuidSnapshot = new ArrayList<>(state.friendResultCache);
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            try {
                var profiles = BlockProt.getProfileService().findAllByUuid(uuidSnapshot);
                for (var profile : profiles) {
                    int idx = uuidSnapshot.indexOf(profile.getUniqueId());
                    if (idx < 0) continue;
                    String name = profile.getName() != null ? profile.getName() : profile.getUniqueId().toString();

                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    if (meta != null) {
                        meta.setPlayerProfile(
                            BlockProtInventory.createPlayerProfile(profile.getUniqueId(), name));
                        EntityNBTHandler.FriendEntry entry = handler.getFriendEntry(profile.getUniqueId().toString());
                        boolean isManager = entry != null && entry.manager();
                        meta.displayName(Component.text(name + (isManager ? " " + Translator.get(TranslationKey.INVENTORIES__FRIENDS__PERMISSION__MANAGER) : "")));
                        skull.setItemMeta(meta);
                    }
                    inventory.setItem(InventoryConstants.lineLength + idx, skull);
                }
            } catch (Exception ignored) {}
        });

        return inventory;
    }
}