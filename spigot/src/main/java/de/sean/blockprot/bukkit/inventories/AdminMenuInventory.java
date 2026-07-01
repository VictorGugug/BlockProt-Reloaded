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
import de.sean.blockprot.bukkit.BlockProtAPI;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Admin menu GUI. */
public class AdminMenuInventory extends BlockProtInventory {

    private static final int SLOT_LOCKABLES     = 10;
    private static final int SLOT_RELOAD        = 11;
    private static final int SLOT_UPDATE        = 12;
    private static final int SLOT_INTEGRATIONS  = 13;
    private static final int SLOT_STATS         = 14;
    private static final int SLOT_DEBUG         = 15;
    private static final int SLOT_INFO          = 16;
    private static final int SLOT_WORLD_EXPIRY  = 22;

    public AdminMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();

        inventory.setItem(SLOT_LOCKABLES, item(Material.CHEST,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__LOCKABLES_LORE)));
        inventory.setItem(SLOT_RELOAD, item(Material.COMPARATOR,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD_LORE)));
        inventory.setItem(SLOT_UPDATE, item(Material.SPYGLASS,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE_LORE)));
        inventory.setItem(SLOT_INTEGRATIONS, item(Material.CHAIN,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS_LORE)));
        inventory.setItem(SLOT_DEBUG, item(Material.COMMAND_BLOCK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG_LORE)));
        inventory.setItem(SLOT_INFO, item(Material.PLAYER_HEAD,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO_LORE)));
        inventory.setItem(SLOT_WORLD_EXPIRY, item(Material.CLOCK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__WORLD_EXPIRY_LORE)));

        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null && state.origin != InventoryState.MenuOrigin.NONE) {
            setBackButton(getSize() - 1);
        }

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_LOCKABLES) {
            InventoryState newState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            newState.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new LockablesInventory().fill(player, 0));

        } else if (slot == SLOT_RELOAD) {
            player.closeInventory();
            if (BlockProt.getDefaultConfig().isBackupsEnabled()) {
                new de.sean.blockprot.bukkit.tasks.BackupTask(
                    BlockProt.getInstance().getDataFolder(), true).run();
            }
            BlockProt.getInstance().reloadConfigAndTranslations();
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE)));

        } else if (slot == SLOT_UPDATE) {
            player.closeInventory();
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE_LORE)));
            Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(),
                new UpdateChecker(BlockProt.getPluginVersion(),
                    new ArrayList<>(Bukkit.getOnlinePlayers())));

        } else if (slot == SLOT_INTEGRATIONS) {
            player.closeInventory();
            var list = BlockProtAPI.getInstance().getIntegrations().stream()
                .filter(i -> i.isEnabled())
                .map(i -> i.name).toList();
            String names = list.isEmpty() ? Translator.get(TranslationKey.MESSAGES__INTEGRATIONS__NONE) : String.join(", ", list);
            String msg = Translator.get(TranslationKey.MESSAGES__ADMIN_INTEGRATIONS)
                .replace("{count}", String.valueOf(list.size()))
                .replace("{integrations}", names);
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));

        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));

        } else if (slot == SLOT_DEBUG) {
            player.closeInventory();
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ADMIN_DEBUG_HINT)));
            player.performCommand("blockprot debug run");

        } else if (slot == SLOT_WORLD_EXPIRY) {
            InventoryState newState = InventoryState.builder()
                .origin(InventoryState.MenuOrigin.ADMIN_MENU)
                .build();
            newState.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new WorldExpiryInventory().fill(player, 0));

        } else if (slot == SLOT_INFO) {
            player.closeInventory();
            Consumer<String> handleName = inputName -> {
                if (inputName == null || inputName.isBlank()) return;
                Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(inputName);
                    if (target == null) {
                        @SuppressWarnings("deprecation")
                        OfflinePlayer fallback = Bukkit.getOfflinePlayer(inputName);
                        if (fallback.hasPlayedBefore()) target = fallback;
                    }
                    final OfflinePlayer finalTarget = target;
                    Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                        if (finalTarget == null || finalTarget.getUniqueId() == null) {
                            player.sendActionBar(LegacyComponentSerializer.legacySection()
                                .deserialize(Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_PLAYER_NOT_FOUND)
                                .replace("{player}", inputName)));
                            return;
                        }
                        String displayName = finalTarget.getName() != null ? finalTarget.getName() : inputName;
                        PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
                        StatHandler.getStatisticByUuid(stat, finalTarget.getUniqueId());

                        InventoryState ns = new InventoryState(null);
                        ns.currentPageIndex = 0;
                        ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
                        InventoryState.set(player.getUniqueId(), ns);
                        player.openInventory(new AdminBlockListInventory().fill(player, displayName, stat));
                    });
                });
            };
            if (SignInput.isSupported()) {
                SignInput.open(player, BlockProt.getInstance(),
                    Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO), handleName);
            } else {
                AnvilInput.open(player, BlockProt.getInstance(), "",
                    Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO), handleName);
            }

        } else if (slot == getSize() - 1) {
            goBack(player, state);
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(net.kyori.adventure.text.Component.text(
            name.replaceAll("[§&][0-9a-fk-orx]", "")));
        List<net.kyori.adventure.text.Component> loreList = new ArrayList<>(lore.length);
        for (String s : lore) loreList.add(LegacyComponentSerializer.legacySection().deserialize(s));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}