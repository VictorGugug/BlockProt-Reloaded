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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Blocks and locking behavior category of the inventory-based admin config
 * editor, with the same three sub-category screens as the dialog editor.
 */
public final class AdminConfigBlocksInventory extends BlockProtInventory {

    private static final int SLOT_BACK = 49;

    private enum Screen { MAIN, LOCKING, BEHAVIOR, EFFECTS }

    private Screen currentScreen = Screen.MAIN;

    private static final int LOCKING_SLOT_COUNT = 5;
    private static final int BEHAVIOR_SLOT_COUNT = 3;
    private static final int EFFECTS_SLOT_COUNT = 5;

    public AdminConfigBlocksInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        currentScreen = Screen.MAIN;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10,12,14,16,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});

        inventory.setItem(11, buttonItem(Material.SHIELD,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCKING_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)));
        inventory.setItem(13, buttonItem(Material.COMPARATOR,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__BEHAVIOR_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)));
        inventory.setItem(15, buttonItem(Material.NOTE_BLOCK,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__EFFECTS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @NotNull
    public Inventory fillLocking(@NotNull Player player) {
        currentScreen = Screen.LOCKING;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10, 16,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(11, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PROTECT_EXPLOSIONS_TITLE),
            "protect_locked_blocks_from_explosions",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PROTECT_EXPLOSIONS),
            cfg.shouldProtectLockedBlocksFromExplosions()));
        inventory.setItem(12, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PISTON_MOVEMENT_TITLE),
            "block_protected_block_piston_movement",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PISTON_MOVEMENT),
            cfg.shouldBlockProtectedBlockPistonMovement()));
        inventory.setItem(13, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SHULKER_BREAK_TITLE),
            "clear_protection_on_shulker_break",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SHULKER_BREAK),
            cfg.shouldClearProtectionOnShulkerBreak()));
        inventory.setItem(14, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__ALLOW_BREAK_TITLE),
            "allow_break_protected_blocks",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__ALLOW_BREAK),
            cfg.shouldAllowBreakProtectedBlocks()));
        inventory.setItem(15, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SPAWN_PROTECTION_TITLE),
            "respect_spawn_protection",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SPAWN_PROTECTION),
            cfg.shouldRespectSpawnProtection()));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @NotNull
    public Inventory fillBehavior(@NotNull Player player) {
        currentScreen = Screen.BEHAVIOR;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10,11, 15,16,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(12, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__MODERN_FAMILY_TITLE),
            "modern_family_blocks",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__MODERN_FAMILY),
            cfg.isModernFamilyBlocks()));
        inventory.setItem(13, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__REDSTONE_DISALLOWED_TITLE),
            "redstone_disallowed_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__REDSTONE_DISALLOWED),
            cfg.disallowRedstoneOnPlace()));
        inventory.setItem(14, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SIMPLIFIED_HOPPER_TITLE),
            "simplified_hopper_logic",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SIMPLIFIED_HOPPER),
            cfg.isSimplifiedHopperLogic()));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @NotNull
    public Inventory fillEffects(@NotNull Player player) {
        currentScreen = Screen.EFFECTS;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10, 16,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(11, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS_TITLE),
            "block_lock_effects",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS),
            cfg.isLockEffectEnabled()));
        inventory.setItem(12, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS_TITLE),
            "block_lock_sounds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS),
            cfg.isLockSoundEnabled()));

        boolean useMenus = cfg.getBukkitConfig().getBoolean("use_menus", false);
        inventory.setItem(13, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_MENUS_TITLE),
            "use_menus",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_MENUS_DE),
            useMenus));
        inventory.setItem(14, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_DIALOGS_TITLE),
            "use_dialogs",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_DIALOGS_DE),
            cfg.isDialogsEnabled()));

        int timedAccessDays = cfg.getBukkitConfig().getInt("timed_access_max_duration_days", 90);
        inventory.setItem(15, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS_TITLE),
            String.valueOf(timedAccessDays),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS),
            "timed_access_max_duration_days"));

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
            if (currentScreen == Screen.MAIN) {
                player.openInventory(new AdminConfigInventory().fill(player));
            } else {
                player.openInventory(fill(player));
            }
            return;
        }

        switch (currentScreen) {
            case MAIN -> {
                if (slot == 11) {
                    player.openInventory(fillLocking(player));
                } else if (slot == 13) {
                    player.openInventory(fillBehavior(player));
                } else if (slot == 15) {
                    player.openInventory(fillEffects(player));
                }
            }
            case LOCKING -> {
                if (slot >= 11 && slot <= 15) {
                    switch (slot) {
                        case 11 -> {
                            cfg.setProtectFromExplosions(!cfg.shouldProtectLockedBlocksFromExplosions());
                            player.openInventory(fillLocking(player));
                        }
                        case 12 -> {
                            cfg.setBlockPistonMovement(!cfg.shouldBlockProtectedBlockPistonMovement());
                            player.openInventory(fillLocking(player));
                        }
                        case 13 -> {
                            cfg.setClearProtectionOnShulkerBreak(!cfg.shouldClearProtectionOnShulkerBreak());
                            player.openInventory(fillLocking(player));
                        }
                        case 14 -> {
                            cfg.setAllowBreakProtectedBlocks(!cfg.shouldAllowBreakProtectedBlocks());
                            player.openInventory(fillLocking(player));
                        }
                        case 15 -> {
                            cfg.setRespectSpawnProtection(!cfg.shouldRespectSpawnProtection());
                            player.openInventory(fillLocking(player));
                        }
                    }
                }
            }
            case BEHAVIOR -> {
                if (slot >= 12 && slot <= 14) {
                    switch (slot) {
                        case 12 -> {
                            boolean toModern = !cfg.isModernFamilyBlocks();
                            cfg.setAndSave("modern_family_blocks", toModern);
                            cfg.convertBlocksFileFormat(toModern);
                            player.openInventory(fillBehavior(player));
                        }
                        case 13 -> {
                            cfg.setRedstoneDisallowedByDefault(!cfg.disallowRedstoneOnPlace());
                            player.openInventory(fillBehavior(player));
                        }
                        case 14 -> {
                            cfg.setSimplifiedHopperLogic(!cfg.isSimplifiedHopperLogic());
                            player.openInventory(fillBehavior(player));
                        }
                    }
                }
            }
            case EFFECTS -> {
                if (slot >= 11 && slot <= 15) {
                    switch (slot) {
                        case 11 -> {
                            cfg.setLockEffects(!cfg.isLockEffectEnabled());
                            player.openInventory(fillEffects(player));
                        }
                        case 12 -> {
                            cfg.setLockSounds(!cfg.isLockSoundEnabled());
                            player.openInventory(fillEffects(player));
                        }
                        case 13 -> {
                            boolean useMenus = cfg.getBukkitConfig().getBoolean("use_menus", false);
                            cfg.setAndSave("use_menus", !useMenus);
                            player.openInventory(fillEffects(player));
                        }
                        case 14 -> {
                            cfg.setAndSave("use_dialogs", !cfg.isDialogsEnabled());
                            player.openInventory(fillEffects(player));
                        }
                        case 15 -> TextInput.open(player, BlockProt.getInstance(),
                            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS_HINT), input -> {
                                if (input == null || input.isBlank()) return;
                                try {
                                    cfg.setAndSave("timed_access_max_duration_days", Integer.parseInt(input.trim()));
                                } catch (NumberFormatException e) {
                                    player.sendMessage(Translator.get(
                                        TranslationKey.DIALOGS__ADMIN_CONFIG__INVALID_NUMBER).replace("{input}", input));
                                    return;
                                }
                                player.openInventory(fillEffects(player));
                            });
                    }
                }
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators(int[] separatorSlots) {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : separatorSlots) {
            inventory.setItem(s, sep);
        }
    }

    private ItemStack buttonItem(Material material, String title, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        ComponentMessages.displayName(meta, Component.text(
            AdminConfigInventory.strip(title)).color(NamedTextColor.WHITE));
        ComponentMessages.lore(meta, List.of(Component.text(
            AdminConfigInventory.strip(lore)).color(AdminConfigInventory.SOFT_GRAY)));
        stack.setItemMeta(meta);
        return stack;
    }
}