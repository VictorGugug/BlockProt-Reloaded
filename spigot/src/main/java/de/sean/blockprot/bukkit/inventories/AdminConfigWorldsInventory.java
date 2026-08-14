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
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Worlds category of the inventory-based admin config editor: per-world
 * config toggle and the excluded worlds list.
 */
public final class AdminConfigWorldsInventory extends BlockProtInventory {

    private static final int SLOT_PER_WORLDS_CONFIG = 12;
    private static final int SLOT_EXCLUDED_WORLDS = 14;
    private static final int SLOT_BACK = 49;

    private static final int[] SEPARATOR_SLOTS = {
        0,1,2,3,4,5,6,7,8,
        9,10,11, 13, 15,16,17,
        18,19,20,21,22,23,24,25,26,
        27,28,29,30,31,32,33,34,35,
        36,37,38,39,40,41,42,43,44,
        45,46,47,48, 50,51,52,53
    };

    public AdminConfigWorldsInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        fillSeparators();
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(SLOT_PER_WORLDS_CONFIG, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__PER_WORLDS_CONFIG_TITLE),
            "per_worlds_config",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__PER_WORLDS_CONFIG),
            cfg.isPerWorldsConfigEnabled()));

        List<String> excludedList = cfg.getBukkitConfig().getStringList("excluded_worlds");
        inventory.setItem(SLOT_EXCLUDED_WORLDS, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_TITLE),
            excludedList.isEmpty()
                ? AdminConfigInventory.strip(Translator.get(
                    TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_EMPTY))
                : String.join(", ", excludedList),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS),
            "excluded_worlds"));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (slot == SLOT_BACK) {
            player.openInventory(new AdminConfigInventory().fill(player));
        } else if (slot == SLOT_PER_WORLDS_CONFIG) {
            cfg.setAndSave("per_worlds_config", !cfg.isPerWorldsConfigEnabled());
            player.openInventory(fill(player));
        } else if (slot == SLOT_EXCLUDED_WORLDS) {
            TextInput.open(player, BlockProt.getInstance(),
                Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__EXCLUDED_WORLDS_HINT), input -> {
                    if (input == null || input.isBlank()) return;
                    List<String> parsed = input.trim().isBlank()
                        ? List.of()
                        : Arrays.stream(input.trim().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                    cfg.setAndSave("excluded_worlds", parsed);
                    player.openInventory(fill(player));
                });
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators() {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : SEPARATOR_SLOTS) {
            inventory.setItem(s, sep);
        }
    }
}