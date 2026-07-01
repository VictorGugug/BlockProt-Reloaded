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

import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.nbt.stats.ListStatisticItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Paginated inventory displaying a list of protected blocks and entities.
 */
public final class StatisticListInventory extends BlockProtInventory {
    public StatisticListInventory() { super(true); }
    private BukkitListStatistic<ListStatisticItem<?, Material>, ?> statistic;

    @Override
    int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__STATISTICS__STATISTICS);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;

        switch (item.getType()) {
            case CYAN_STAINED_GLASS_PANE -> {
                if (state.currentPageIndex >= 1) {
                    state.currentPageIndex--;
                    closeAndOpen(player, this.fill(player, null));
                }
            }
            case BLUE_STAINED_GLASS_PANE -> {
                state.currentPageIndex++;
                closeAndOpen(player, fill(player, null));
            }
            case BARRIER -> goBack(player, state);
            case BLACK_STAINED_GLASS_PANE -> goBack(player, state);
            default -> handleItemClick(event, player, state);
        }
    }

    private void handleItemClick(@NotNull InventoryClickEvent event,
                                 @NotNull Player player,
                                 @NotNull InventoryState state) {
        List<ListStatisticItem<?, Material>> fullList = getMergedList(player);
        int offset = (this.getSize() - 3) * state.currentPageIndex;
        int idx = offset + event.getSlot();
        if (idx < 0 || idx >= fullList.size()) return;

        ListStatisticItem<?, Material> entry = fullList.get(idx);

        if (entry instanceof EntityListEntry entityEntry) {
            Entity entity = entityEntry.getEntity();
            if (entity == null) return;
            EntityNBTHandler handler = new EntityNBTHandler(entity);
            if (!handler.isProtected()) return;
            if (!handler.isOwner(player.getUniqueId().toString())
                    && !player.hasPermission(Permissions.USER_ADMIN.key())) return;
            player.closeInventory();
            InventoryState newState = InventoryState.getOrCreate(player.getUniqueId());
            newState.entityUUID = entity.getUniqueId();
            var inv = new BlockLockInventory().fillForEntity(player, entity, handler);
            if (inv != null) player.openInventory(inv);
            return;
        }

        if (!(entry instanceof LocationListEntry locEntry)) return;
        Location loc = locEntry.get();
        if (loc.getWorld() == null) return;

        if (!player.hasPermission(Permissions.BLOCKS_TP.key())) {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION_TP)));
            return;
        }
        player.closeInventory();
        player.teleport(loc.clone().add(0.5, 1.0, 0.5));
        InventoryState.remove(player.getUniqueId());
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(@NotNull final Player player,
                          @Nullable final BukkitListStatistic<ListStatisticItem<?, Material>, ?> stat) {
        if (stat != null) this.statistic = stat;
        if (this.statistic == null) throw new RuntimeException("No cached statistic available.");

        List<ListStatisticItem<?, Material>> list = getMergedList(player);
        final InventoryState state = InventoryState.get(player.getUniqueId());
        if (state == null) return inventory;

        final int max = this.getSize() - 3;
        int offset = max * state.currentPageIndex;

        boolean canTp = player.hasPermission(Permissions.BLOCKS_TP.key());
        String loreTP = Translator.get(canTp ? TranslationKey.INVENTORIES__STATS__LORE_TP
                                             : TranslationKey.INVENTORIES__STATS__LORE_NO_TP);

        for (int i = 0; i < Math.min(list.size() - offset, max); ++i) {
            final ListStatisticItem<?, Material> entry = list.get(offset + i);
            if (entry instanceof EntityListEntry entityEntry) {
                setItemStackWithLore(i, entityEntry.getItemType(), entityEntry.getTitle(),
                    Translator.get(TranslationKey.INVENTORIES__STATS__CLICK_TO_OPEN), "", entityEntry.getContentsLore());
            } else if (entry instanceof LocationListEntry loc) {
                setItemStackWithLore(i, resolveDisplayMaterial(entry.getItemType()),
                    entry.getTitle(), loreTP, loc.getLockedAgoText(), loc.getContentsLore());
            } else {
                setItemStackWithLore(i, resolveDisplayMaterial(entry.getItemType()),
                    entry.getTitle(), loreTP, "", List.of());
            }
        }

        if (list.size() - offset > max) {
            setItemStack(max,     Material.CYAN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__LAST_PAGE);
            setItemStack(max + 1, Material.BLUE_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__NEXT_PAGE);
        }
        setBackButton();
        return inventory;
    }

    private List<ListStatisticItem<?, Material>> getMergedList(@NotNull Player player) {
        List<ListStatisticItem<?, Material>> result = new ArrayList<>(getFilteredBlockList());
        String playerUuid = player.getUniqueId().toString();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!isProtectableEntity(entity)) continue;
                EntityNBTHandler h = new EntityNBTHandler(entity);
                if (!h.isProtected()) continue;
                if (!h.isOwner(playerUuid) && !player.hasPermission(Permissions.USER_ADMIN.key())) continue;
                result.add(new EntityListEntry(entity));
            }
        }
        return result;
    }

    private static boolean isProtectableEntity(@NotNull Entity entity) {
        if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) return true;
        if (entity instanceof StorageMinecart || entity instanceof HopperMinecart) return true;
        try {
            Class<?> cb = Class.forName("org.bukkit.entity.boat.ChestBoat");
            if (cb.isInstance(entity)) return true;
        } catch (ClassNotFoundException ignored) {}
        try {
            Class<?> cb = Class.forName("org.bukkit.entity.ChestBoat");
            if (cb.isInstance(entity)) return true;
        } catch (ClassNotFoundException ignored) {}
        return false;
    }

    private List<ListStatisticItem<?, Material>> getFilteredBlockList() {
        return statistic.get()
            .stream()
            .filter(e -> {
                if (e instanceof LocationListEntry loc) {
                    try { return loc.get().getBlock().getType() != Material.AIR; }
                    catch (Exception ignored) { return false; }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private Material resolveDisplayMaterial(Material raw) {
        if (raw == null || raw == Material.AIR) return Material.CHEST;
        return raw;
    }

    private void setItemStackWithLore(int index, Material material, String name,
                                      String loreLine, String lockedAgo, List<String> contents) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) { inventory.setItem(index, stack); return; }
        meta.displayName(net.kyori.adventure.text.Component.text(
            name.replaceAll("[\u00a7&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize(loreLine));
        if (!lockedAgo.isEmpty()) {
            lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(lockedAgo));
        }
        if (!contents.isEmpty()) {
            lore.add(net.kyori.adventure.text.Component.empty());
            for (String line : contents) {
                lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(line));
            }
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        inventory.setItem(index, stack);
    }

    /**
     * A stats list entry for a protected entity (item frame, chest boat, minecart).
     * Resolved live from the world — entity protection is stored in PDC, not NBT file.
     */
    public static final class EntityListEntry extends ListStatisticItem<Entity, Material> {
        public EntityListEntry(@NotNull Entity entity) { super(entity); }

        @Nullable
        public Entity getEntity() {
            try { return Bukkit.getEntity(value.getUniqueId()); }
            catch (Exception e) { return null; }
        }

        @Override
        public @NotNull Material getItemType() {
            if (value instanceof ItemFrame || value instanceof GlowItemFrame) return Material.ITEM_FRAME;
            if (value instanceof StorageMinecart) return Material.CHEST_MINECART;
            if (value instanceof HopperMinecart)  return Material.HOPPER_MINECART;
            String typeName = value.getType().name();
            try {
                Material m = Material.valueOf(typeName);
                if (m != Material.AIR) return m;
            } catch (IllegalArgumentException ignored) {}
            return Material.OAK_CHEST_BOAT;
        }

        @Override
        public String getTitle() {
            Location loc = value.getLocation();
            String coords = "[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";
            String type = value.getType().name().replace('_', ' ');
            StringBuilder sb = new StringBuilder("\u00a77");
            boolean cap = true;
            for (char c : type.toCharArray()) {
                if (c == ' ') { sb.append(c); cap = true; }
                else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
                else sb.append(Character.toLowerCase(c));
            }
            return sb + " " + coords;
        }

        @NotNull
        public List<String> getContentsLore() {
            if (value instanceof ItemFrame frame) {
                ItemStack held = frame.getItem();
                if (held.getType() == Material.AIR) return List.of(Translator.get(TranslationKey.MESSAGES__LOCATION__EMPTY_CONTENTS));
                return List.of(Translator.get(TranslationKey.INVENTORIES__STATS__ITEM_FRAME_ENTRY)
                    .replace("{count}", String.valueOf(held.getAmount()))
                    .replace("{item}", held.getType().name().toLowerCase().replace('_', ' ')));
            }
            if (value instanceof InventoryHolder holder) {
                var inv = holder.getInventory();
                var counts = new java.util.LinkedHashMap<Material, Integer>();
                for (ItemStack s : inv.getContents()) {
                    if (s != null && !s.getType().isAir())
                        counts.merge(s.getType(), s.getAmount(), Integer::sum);
                }
                if (counts.isEmpty()) return List.of(Translator.get(TranslationKey.MESSAGES__LOCATION__EMPTY_CONTENTS));
                List<String> out = new ArrayList<>();
                int n = 0;
                for (var e : counts.entrySet()) {
                    if (n >= 5) {
                        out.add(Translator.get(TranslationKey.INVENTORIES__STATS__CONTENTS_MORE_FORMAT)
                            .replace("{count}", String.valueOf(counts.size() - n)));
                        break;
                    }
                    out.add(Translator.get(TranslationKey.INVENTORIES__STATS__CONTENTS_ITEM_FORMAT)
                        .replace("{count}", String.valueOf(e.getValue()))
                        .replace("{item}", e.getKey().name().toLowerCase().replace('_', ' ')));
                    n++;
                }
                return out;
            }
            return List.of();
        }
    }
}