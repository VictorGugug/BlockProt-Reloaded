/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class WorldExpiryInventory extends BlockProtInventory {

    private static final int CONTENT_START = 0;
    private static final int SLOT_PREV = 47;
    private static final int SLOT_NEXT = 49;
    private static final int SLOT_BACK = 53;

    private int cachedPage = 0;
    private List<World> worldList = List.of();

    public WorldExpiryInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player, int page) {
        worldList = Bukkit.getWorlds().stream()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .toList();

        int totalPages = Math.max(1, (int) Math.ceil(worldList.size() / 45.0));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        cachedPage = safePage;
        int start = safePage * 45;
        int end = Math.min(start + 45, worldList.size());

        inventory = createInventory(Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__TITLE)
            + (totalPages > 1 ? " p" + (safePage + 1) + "/" + totalPages : ""));

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Map<String, String> worldExpiry = cfg.getWorldExpiryDurations();

        for (int i = start; i < end; i++) {
            World w = worldList.get(i);
            String duration = worldExpiry.getOrDefault(w.getName(), "0");
            boolean active = !"0".equals(duration) && !"-1".equals(duration);

            ItemStack stack = new ItemStack(active ? Material.CLOCK : Material.GRAY_DYE);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(w.getName())
                    .color(active ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY));
                List<Component> lore = new ArrayList<>();
                if (active) {
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__DURATION_LABEL) + duration));
                } else {
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__DISABLED)));
                }
                lore.add(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__WORLD_LORE)));
                lore.add(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.INVENTORIES__WORLD_EXPIRY__ENVIRONMENT_LABEL)
                    + w.getEnvironment().name()));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            inventory.setItem(i - start, stack);
        }

        if (safePage > 0) setItemStack(SLOT_PREV, Material.ARROW, TranslationKey.INVENTORIES__LAST_PAGE);
        if (safePage < totalPages - 1) setItemStack(SLOT_NEXT, Material.ARROW, TranslationKey.INVENTORIES__NEXT_PAGE);
        setBackButton(SLOT_BACK);

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) { goBack(player, state); return; }

        if (slot == SLOT_PREV && cachedPage > 0) {
            state.currentPageIndex = cachedPage - 1;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player, state.currentPageIndex));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(worldList.size() / 45.0));
        if (slot == SLOT_NEXT && cachedPage < totalPages - 1) {
            state.currentPageIndex = cachedPage + 1;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player, state.currentPageIndex));
            return;
        }

        if (slot < 45) {
            int idx = cachedPage * 45 + slot;
            if (idx >= 0 && idx < worldList.size()) {
                World w = worldList.get(idx);
                promptDuration(player, state, w);
            }
        }
    }

    private void promptDuration(@NotNull Player player, @NotNull InventoryState state, @NotNull World world) {
        Consumer<String> handleInput = input -> {
            if (input == null || input.isBlank()) return;
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            String trimmed = input.trim();
            if (trimmed.equals("0") || trimmed.equals("-1")) {
                cfg.setWorldExpiryDuration(world.getName(), "0");
                player.sendMessage(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__CLEARED)
                    .replace("{world}", world.getName()));
            } else if (trimmed.matches("\\d+[smhd]")) {
                cfg.setWorldExpiryDuration(world.getName(), trimmed);
                player.sendMessage(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__SET)
                    .replace("{world}", world.getName())
                    .replace("{duration}", trimmed));
            } else {
                player.sendMessage(Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__ERROR)
                    .replace("{input}", trimmed));
            }
            state.currentPageIndex = cachedPage;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player, cachedPage));
        };
        if (SignInput.isSupported()) {
            SignInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__USAGE), handleInput);
        } else {
            AnvilInput.open(player, BlockProt.getInstance(), "",
                Translator.get(TranslationKey.MESSAGES__WORLD_EXPIRY__USAGE), handleInput);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}
}