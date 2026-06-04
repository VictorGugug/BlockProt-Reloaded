/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.listeners.BlockEventListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Single-row inventory with user settings.
 *
 * Layout (singleLine = 9 slots, 0-8):
 *   0 = Lock on place
 *   1 = Hints toggle        ← new, above the back button area
 *   2 = Friends skull
 *   8 = Back
 *
 * Hints semantics:
 *   hasPlayerInteractedWithMenu = false → hints are ENABLED (player hasn't dismissed them)
 *   hasPlayerInteractedWithMenu = true  → hints are DISABLED
 *   Toggle flips the value and updates the enchant visual.
 */
public class UserSettingsInventory extends BlockProtInventory {

    public UserSettingsInventory() { super(true); }

    // Slot indices
    private static final int SLOT_LOCK_ON_PLACE = 0;
    private static final int SLOT_HINTS         = 1;
    private static final int SLOT_FRIENDS        = 2;

    @Override
    int getSize() { return InventoryConstants.lineLength; }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_SETTINGS);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BARRIER -> {
                // Lock on place toggle
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                h.setLockOnPlace(!h.getLockOnPlace());
                BlockEventListener.invalidateSettings(player.getUniqueId());
                inventory.setItem(SLOT_LOCK_ON_PLACE, toggleOption(item, null));
            }
            case KNOWLEDGE_BOOK -> {
                // Hints toggle
                PlayerSettingsHandler h = new PlayerSettingsHandler(player);
                // hasPlayerInteractedWithMenu=true means hints are DISABLED; flip it
                boolean hintsCurrentlyEnabled = !h.hasPlayerInteractedWithMenu();
                h.setHasPlayerInteractedWithMenu(hintsCurrentlyEnabled); // true = disable, false = enable
                // Refresh the whole inventory so the enchant / name updates
                fill(player);
            }
            case PLAYER_HEAD -> {
                state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
                state.origin = InventoryState.MenuOrigin.USER_SETTINGS;
                closeAndOpen(player, new FriendManageInventory().fill(player));
            }
            case BLACK_STAINED_GLASS_PANE -> {
                // Back: return to wherever we came from.
                // If opened directly from a command (origin=NONE), just close.
                if (state.origin == InventoryState.MenuOrigin.NONE
                        || state.origin == InventoryState.MenuOrigin.USER_SETTINGS) {
                    player.closeInventory();
                } else {
                    player.openInventory(new UserMenuInventory().fill(player));
                }
            }
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    public Inventory fill(Player player) {
        PlayerSettingsHandler settings = new PlayerSettingsHandler(player);

        // Slot 0: Lock on place
        setEnchantedOptionItemStack(
            SLOT_LOCK_ON_PLACE,
            Material.BARRIER,
            TranslationKey.INVENTORIES__LOCK_ON_PLACE,
            settings.getLockOnPlace()
        );

        // Slot 1: Hints toggle
        // hintsEnabled = true when player has NOT yet dismissed them
        boolean hintsEnabled = !settings.hasPlayerInteractedWithMenu();
        setEnchantedOptionItemStack(
            SLOT_HINTS,
            Material.KNOWLEDGE_BOOK,
            TranslationKey.INVENTORIES__USER_MENU__HINTS,
            hintsEnabled
        );

        // Slot 2: Friends skull
        if (!BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) {
            setItemStack(
                SLOT_FRIENDS,
                Material.PLAYER_HEAD,
                Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)
            );
            Bukkit.getScheduler().runTaskAsynchronously(
                BlockProt.getInstance(),
                () -> {
                    try {
                        var profile = BlockProtInventory.createPlayerProfile(
                            player.getUniqueId(), player.getName());
                        Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                            setPlayerSkull(SLOT_FRIENDS, profile);
                            var stack = inventory.getItem(SLOT_FRIENDS);
                            if (stack != null) {
                                var meta = stack.getItemMeta();
                                if (meta != null) {
                                    meta.displayName(net.kyori.adventure.text.Component.text(
                                        Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE)
                                            .replaceAll("[§&][0-9a-fk-orx]", "")));
                                    stack.setItemMeta(meta);
                                    inventory.setItem(SLOT_FRIENDS, stack);
                                }
                            }
                        });
                    } catch (Exception e) {
                        BlockProt.getInstance().getLogger().warning(
                            "Failed to load player skull for UserSettings: " + e.getMessage());
                    }
                }
            );
        }

        setBackButton(); // slot 8
        return inventory;
    }
}
