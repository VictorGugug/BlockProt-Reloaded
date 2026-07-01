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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server-side anvil text input, compatible with Paper/Spigot 1.20.x through 26.1.x.
 * Uses reflection for AnvilView (1.21.4+) with fallback for older servers.
 */
public final class AnvilInput implements Listener {

    private static final int OUTPUT_SLOT = 2;

    // Resolved once per JVM start. Null on servers older than 1.21.4.
    private static final Class<?> ANVIL_VIEW_CLASS;
    private static final Method GET_RENAME_TEXT;
    private static final Method SET_REPAIR_COST;

    static {
        Class<?> viewClass = null;
        Method getRenameText = null;
        Method setRepairCost = null;
        try {
            viewClass = Class.forName("org.bukkit.inventory.view.AnvilView");
            getRenameText = viewClass.getMethod("getRenameText");
            setRepairCost = viewClass.getMethod("setRepairCost", int.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Pre-1.21.4 server — typed views not available.
        }
        ANVIL_VIEW_CLASS = viewClass;
        GET_RENAME_TEXT  = getRenameText;
        SET_REPAIR_COST  = setRepairCost;
    }

    private static boolean hasTypedAnvilView() {
        return ANVIL_VIEW_CLASS != null;
    }

    @SuppressWarnings("deprecation")
    private static void trySetRepairCost(@NotNull InventoryView view) {
        // 1.21.4+: use typed AnvilView#setRepairCost via reflection.
        if (SET_REPAIR_COST != null && ANVIL_VIEW_CLASS != null && ANVIL_VIEW_CLASS.isInstance(view)) {
            try {
                SET_REPAIR_COST.invoke(view, 0);
                return;
            } catch (Exception ignored) {}
        }
        // Pre-1.21.4: InventoryView#setProperty exists since 1.9 and works on all older versions.
        try {
            view.setProperty(InventoryView.Property.REPAIR_COST, 0);
        } catch (Exception ignored) {}
    }

    @Nullable
    private static String tryGetRenameText(@NotNull InventoryView view) {
        if (GET_RENAME_TEXT == null || ANVIL_VIEW_CLASS == null) return null;
        if (!ANVIL_VIEW_CLASS.isInstance(view)) return null;
        try {
            return (String) GET_RENAME_TEXT.invoke(view);
        } catch (Exception ignored) {
            return null;
        }
    }

    private final UUID playerUuid;
    private final @Nullable Consumer<String> onConfirm;
    private boolean consumed = false;

    private AnvilInput(
        @NotNull Player player,
        @NotNull Plugin plugin,
        @NotNull String initialText,
        @Nullable Consumer<String> onConfirm
    ) {
        this.playerUuid = player.getUniqueId();
        this.onConfirm  = onConfirm;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // openAnvil exists since Bukkit 1.14 and returns InventoryView on all versions.
        InventoryView view = player.openAnvil(null, true);
        if (view != null) {
            // Place paper in slot 0 so the client pre-fills the rename field.
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(initialText));
                paper.setItemMeta(meta);
            }
            view.getTopInventory().setItem(0, paper);
            trySetRepairCost(view);
        }
    }

    public static void open(
        @NotNull Player player,
        @NotNull Plugin plugin,
        @NotNull String initialText,
        @NotNull String title,
        @Nullable Consumer<String> onConfirm
    ) {
        new AnvilInput(player, plugin, initialText, onConfirm);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        if (event.getViewers().stream().noneMatch(v -> v.getUniqueId().equals(playerUuid))) return;
        trySetRepairCost(event.getView());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!event.getWhoClicked().getUniqueId().equals(playerUuid)) return;
        if (event.getInventory().getType() != InventoryType.ANVIL) return;

        event.setCancelled(true);

        if (event.getRawSlot() == OUTPUT_SLOT) {
            String text = extractRenameText(event);
            event.getInventory().clear();
            unregister();
            event.getWhoClicked().closeInventory();
            if (onConfirm != null) onConfirm.accept(text);
        }
    }

    @NotNull
    private String extractRenameText(@NotNull InventoryClickEvent event) {
        // 1. Typed AnvilView — 1.21.4+ only, accessed via reflection.
        if (hasTypedAnvilView()) {
            String renamed = tryGetRenameText(event.getView());
            if (renamed != null && !renamed.isEmpty()) return renamed;
        }

        ItemStack output = event.getInventory().getItem(OUTPUT_SLOT);
        if (output != null && output.hasItemMeta()) {
            Component displayName = output.getItemMeta().displayName();
            if (displayName != null) {
                String text = PlainTextComponentSerializer.plainText().serialize(displayName);
                if (!text.isEmpty()) return text;
            }
        }

        ItemStack input = event.getInventory().getItem(0);
        if (input != null && input.hasItemMeta()) {
            Component displayName = input.getItemMeta().displayName();
            if (displayName != null) {
                return PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }

        return "";
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUuid)) return;
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        event.getInventory().clear();
        unregister();
    }

    private void unregister() {
        if (!consumed) {
            consumed = true;
            HandlerList.unregisterAll(this);
        }
    }
}