package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for regular user operations. Opened via /bp user or /bp settings.
 *
 * Layout (tripleLine = 27 slots, 4 items centred in row 1: slots 11-14).
 */
public class UserMenuInventory extends BlockProtInventory {

    private static final int SLOT_SETTINGS = 11;
    private static final int SLOT_FRIENDS  = 12;
    private static final int SLOT_STATS    = 13;
    private static final int SLOT_ABOUT    = 14;

    public UserMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();

        inventory.setItem(SLOT_SETTINGS, item(Material.WRITABLE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS_LORE)));
        inventory.setItem(SLOT_FRIENDS, item(Material.PLAYER_HEAD,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS_LORE)));
        inventory.setItem(SLOT_ABOUT, item(Material.NETHER_STAR,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT_LORE)));

        // Back button only when opened from within another menu, not from a command.
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

        if (slot == SLOT_SETTINGS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new UserSettingsInventory().fill(player));
            new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
        } else if (slot == SLOT_FRIENDS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            var inv = new FriendManageInventory().fill(player);
            if (inv != null) player.openInventory(inv);
        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            newState.origin = InventoryState.MenuOrigin.USER_MENU;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));
        } else if (slot == SLOT_ABOUT) {
            player.closeInventory();
            player.performCommand("blockprot about");
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
        List<net.kyori.adventure.text.Component> l = new ArrayList<>();
        for (String s : lore) l.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacySection().deserialize(s));
        meta.lore(l);
        item.setItemMeta(meta);
        return item;
    }
}
