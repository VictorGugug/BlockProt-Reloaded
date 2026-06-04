package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.inventories.PetSettingsInventory;
import de.sean.blockprot.bukkit.pets.PetNBTHandler;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the {@link PetSettingsInventory} when a player right-clicks their own
 * tamed pet while holding a <strong>stick</strong> (or the item configured via
 * {@code pet-menu-item} in config.yml).
 *
 * <p>The trigger item defaults to {@link Material#STICK} so it does not conflict
 * with normal interactions (feeding, sitting, etc.) which require specific items
 * or an empty hand.
 *
 * @since SP26-ZV
 */
public final class PetMenuOpenListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!BlockProt.getDefaultConfig().isPetProtectionEnabled()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Tameable)) return;

        Player player = event.getPlayer();
        Material menuItem = BlockProt.getDefaultConfig().getPetMenuItem();
        if (player.getInventory().getItemInMainHand().getType() != menuItem) return;

        // Only owner or admin can open settings.
        PetNBTHandler handler = new PetNBTHandler(clicked);
        boolean isOwner = handler.isOwner(player.getUniqueId())
            || (((Tameable) clicked).getOwnerUniqueId() != null
                && ((Tameable) clicked).getOwnerUniqueId().equals(player.getUniqueId()));
        boolean isAdmin = player.hasPermission("blockprot.admin");
        if (!isOwner && !isAdmin) {
            player.sendMessage(BlockProt.getDefaultConfig().getPetDeniedMessage());
            event.setCancelled(true);
            return;
        }

        // Initialise InventoryState for the player.
        InventoryState state = InventoryState.getOrCreate(player.getUniqueId());
        state.setPetEntityId(clicked.getUniqueId());

        Inventory inv = new PetSettingsInventory().fill(player, clicked);
        if (inv != null) {
            event.setCancelled(true); // prevent vanilla interaction (sitting toggle, etc.)
            player.openInventory(inv);
        }
    }
}
