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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class WorldLockableDetailInventory extends BlockProtInventory {

    private static final int CONTENT_SLOTS = 45;
    private static final int SLOT_PREV     = 47;
    private static final int SLOT_NEXT     = 49;
    private static final int SLOT_BACK     = 53;

    @NotNull private final World world;

    private List<MaterialEntry> pagedList = List.of();
    private int cachedPage = 0;

    private record MaterialEntry(@NotNull Material material, boolean active) {}

    public WorldLockableDetailInventory(@NotNull World world) {
        super(false);
        this.world = world;
    }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return "§6" + Translator.get(TranslationKey.INVENTORIES__LOCKABLES__TITLE) + " §7- §e" + world.getName();
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        pagedList = buildAllEntries();
        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        cachedPage = 0;

        inventory = createInventory(getTranslatedInventoryName());

        int start = 0;
        int end = Math.min(CONTENT_SLOTS, pagedList.size());

        for (int i = start; i < end; i++) {
            MaterialEntry e = pagedList.get(i);
            inventory.setItem(i, blockItem(e.material(), e.active()));
        }

        if (totalPages > 1) {
            setItemStack(SLOT_PREV, Material.ARROW, Translator.get(TranslationKey.INVENTORIES__LAST_PAGE));
            setItemStack(SLOT_NEXT, Material.ARROW, Translator.get(TranslationKey.INVENTORIES__NEXT_PAGE));
        }
        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) {
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(new WorldLockableSelectionInventory().fill(player));
            return;
        }

        if (slot == SLOT_PREV && cachedPage > 0) {
            cachedPage--;
            state.currentPageIndex = cachedPage;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player));
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(pagedList.size() / (double) CONTENT_SLOTS));
        if (slot == SLOT_NEXT && cachedPage < totalPages - 1) {
            cachedPage++;
            state.currentPageIndex = cachedPage;
            InventoryState.set(player.getUniqueId(), state);
            player.openInventory(fill(player));
            return;
        }

        if (slot < CONTENT_SLOTS) {
            int absIdx = cachedPage * CONTENT_SLOTS + slot;
            if (absIdx < pagedList.size()) {
                MaterialEntry e = pagedList.get(absIdx);
                toggleInWorld(e.material(), player);
                state.currentPageIndex = cachedPage;
                InventoryState.set(player.getUniqueId(), state);
                player.openInventory(fill(player));
            }
        }
    }

    private void toggleInWorld(@NotNull Material material, @NotNull Player who) {
        File worldsFile = new File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
        if (!worldsFile.exists()) return;

        BlockFamilyParser.Family family = familyOf(material);
        if (family == null) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(worldsFile);
        String key = "worlds." + world.getName();
        String listKey = key + "." + DefaultConfig.configKeyForFamily(family);

        Set<Material> active = new LinkedHashSet<>(BlockFamilyParser.parse(cfg.get(listKey), family));
        boolean currentlyActive = active.contains(material);
        if (currentlyActive) {
            active.remove(material);
        } else {
            active.add(material);
        }

        // worlds.yml always uses the compact family-expression form, never a
        // flat list of individual material names: a per-world list toggled one
        // material at a time would otherwise grow into an unreadable wall of
        // plain names across many worlds.
        String expression = BlockFamilyParser.toFamilyExpression(active, family);
        cfg.set(listKey, List.of(expression != null ? expression : "[]"));

        if (BlockProt.getInstance().getFileWatcher() != null) {
            BlockProt.getInstance().getFileWatcher().suppressNext();
        }
        try {
            cfg.save(worldsFile);
        } catch (IOException ex) {
            BlockProtLogger.warn("Failed to save worlds.yml for " + world.getName() + ": " + ex.getMessage());
        }

        String actionText = currentlyActive
            ? Translator.get(TranslationKey.DISABLED)
            : Translator.get(TranslationKey.ENABLED);
        BlockProtLogger.log("world-lockables-toggle",
            actionText + " " + material.name() + " in world " + world.getName()
                + " (by " + who.getName() + ")");
        if (who.isOnline()) {
            who.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__LOCKABLES__TOGGLE_FEEDBACK)
                    .replace("{action}", actionText)
                    .replace("{name}", material.name() + " §7(" + world.getName() + ")")));
        }
    }

    @Nullable
    private static BlockFamilyParser.Family familyOf(@NotNull Material m) {
        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            if (BlockFamilyParser.getFamilyMembers(family).contains(m)) return family;
        }
        return null;
    }

    @NotNull
    private List<MaterialEntry> buildAllEntries() {
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        List<MaterialEntry> result = new ArrayList<>();

        File worldsFile = new File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
        YamlConfiguration wcfg = worldsFile.exists() ? YamlConfiguration.loadConfiguration(worldsFile) : null;

        for (BlockFamilyParser.Family family : BlockFamilyParser.Family.values()) {
            String configKey = DefaultConfig.configKeyForFamily(family);
            if (configKey == null) continue;

            String fullKey = "worlds." + world.getName() + "." + configKey;
            boolean worldHasList = wcfg != null && wcfg.contains(fullKey);
            Set<Material> worldActive = worldHasList
                ? BlockFamilyParser.parse(wcfg.get(fullKey), family)
                : Set.of();

            for (Material m : BlockFamilyParser.getFamilyMembers(family)) {
                boolean global = cfg.isLockable(m) || cfg.isLockableEntity(m);
                boolean active = worldHasList ? worldActive.contains(m) : global;
                result.add(new MaterialEntry(m, active));
            }
        }
        result.sort(Comparator.comparing(e -> e.material().name()));
        return result;
    }

    @NotNull
    private static ItemStack blockItem(@NotNull Material mat, boolean active) {
        Material display = resolveDisplayMaterial(mat);
        ItemStack stack;
        try {
            stack = new ItemStack(display, 1);
            if (stack.getType() == Material.AIR) stack = new ItemStack(Material.PAPER, 1);
        } catch (Exception e) {
            stack = new ItemStack(Material.PAPER, 1);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = active ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY;
            String suffix = active ? "" : " §7(" + Translator.get(TranslationKey.INVENTORIES__LOCKABLES__OFF_SUFFIX) + ")";
            String friendly = mat.name().toLowerCase().replace("_", " ");
            friendly = Character.toUpperCase(friendly.charAt(0)) + friendly.substring(1);
            for (int i = 0; i < friendly.length(); i++) {
                if (i > 0 && friendly.charAt(i - 1) == ' ') {
                    friendly = friendly.substring(0, i) + Character.toUpperCase(friendly.charAt(i)) + friendly.substring(i + 1);
                }
            }
            meta.displayName(Component.text(friendly).color(nameColor));
            meta.lore(List.of(
                Component.text(mat.name()).color(NamedTextColor.DARK_GRAY),
                Component.text(active ? "§a" + Translator.get(TranslationKey.INVENTORIES__LOCKABLES__STATUS_ACTIVE)
                                       : "§c" + Translator.get(TranslationKey.INVENTORIES__LOCKABLES__STATUS_INACTIVE))
            ));
        }
        return stack;
    }

    @NotNull
    private static Material resolveDisplayMaterial(@NotNull Material mat) {
        String name = mat.name();

        if (name.endsWith("_WALL_SIGN") && !name.endsWith("_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_SIGN", "_SIGN"));
            return m != null ? m : Material.OAK_SIGN;
        }
        if (name.endsWith("_WALL_HANGING_SIGN")) {
            Material m = Material.matchMaterial(name.replace("_WALL_HANGING_SIGN", "_HANGING_SIGN"));
            return m != null ? m : Material.OAK_HANGING_SIGN;
        }
        if (name.equals("WATER_CAULDRON") || name.equals("LAVA_CAULDRON")
                || name.equals("POWDER_SNOW_CAULDRON")) {
            return Material.CAULDRON;
        }
        return mat;
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}
}