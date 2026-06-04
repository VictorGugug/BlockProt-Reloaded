package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.pets.PetNBTHandler;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The GUI shown when a player right-clicks on their tamed pet while holding
 * a stick (or the configured item) and pet-protection is enabled.
 *
 * <p>Layout (18-slot / double line):
 * <pre>
 *  0  PROTECT toggle     (bone       = main on/off)
 *  1  NO_DAMAGE toggle   (shield     = block damage)
 *  2  NO_INTERACT toggle (barrier    = block right-click)
 *  3  NO_LEASH toggle    (lead/string= block leash)
 *  4  NO_PICKUP toggle   (feather    = parrot shoulder)
 *  --  (slots 5-14 empty) --
 *  15 ENABLE ALL  (green glass)
 *  16 DISABLE ALL (red glass)
 *  17 BACK / CLOSE (black glass pane)
 * </pre>
 *
 * @since SP26-ZV
 */
public final class PetSettingsInventory extends BlockProtInventory {

    /** The entity UUID stored in the state so we can retrieve it on click/close. */
    public static final String STATE_ENTITY_KEY = "blockprot_pet_entity";

    private boolean protect;
    private boolean noDamage;
    private boolean noInteract;
    private boolean noLeash;
    private boolean noPickup;

    private static final int SLOT_PROTECT    = 0;
    private static final int SLOT_NO_DAMAGE  = 1;
    private static final int SLOT_NO_INTERACT= 2;
    private static final int SLOT_NO_LEASH   = 3;
    private static final int SLOT_NO_PICKUP  = 4;
    private static final int SLOT_ENABLE_ALL = 15;
    private static final int SLOT_DISABLE_ALL= 16;
    private static final int SLOT_BACK       = 17;

    public PetSettingsInventory() {
        super(true);
    }

    @Override
    int getSize() { return InventoryConstants.doubleLine; }

    @Override
    @NotNull String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__PET__SETTINGS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fill
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Populates the inventory with the current settings for {@code entity}.
     *
     * @param player The player opening the menu (must be the owner).
     * @param entity The pet entity whose settings will be shown.
     * @return The filled inventory, or null if the player is not allowed.
     */
    @Nullable
    public Inventory fill(@NotNull Player player, @NotNull Entity entity) {
        if (!(entity instanceof Tameable)) return null;

        PetNBTHandler handler = new PetNBTHandler(entity);
        UUID ownerUuid = handler.getOwner();

        // Only owner or admins can open this menu.
        boolean isOwner = ownerUuid != null && ownerUuid.equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("blockprot.admin");
        if (!isOwner && !isAdmin) return null;

        protect    = handler.isProtected();
        noDamage   = handler.isNoDamage();
        noInteract = handler.isNoInteract();
        noLeash    = handler.isNoLeash();
        noPickup   = handler.isNoPickup();

        // Store entity UUID in player's InventoryState for retrieval on click/close.
        InventoryState state = InventoryState.get(player.getUniqueId());
        if (state != null) state.setPetEntityId(entity.getUniqueId());

        renderAll();
        return inventory;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case SLOT_PROTECT -> {
                protect = !protect;
                renderToggle(SLOT_PROTECT, Material.BONE,
                    TranslationKey.INVENTORIES__PET__PROTECT, protect);
                // When main toggle goes OFF, sync all sub-settings to OFF visually.
                if (!protect) overrideAll(false);
                save(player, state);
            }
            case SLOT_NO_DAMAGE -> {
                noDamage = !noDamage;
                renderToggle(SLOT_NO_DAMAGE, Material.SHIELD,
                    TranslationKey.INVENTORIES__PET__NO_DAMAGE, noDamage);
                save(player, state);
            }
            case SLOT_NO_INTERACT -> {
                noInteract = !noInteract;
                renderToggle(SLOT_NO_INTERACT, Material.BARRIER,
                    TranslationKey.INVENTORIES__PET__NO_INTERACT, noInteract);
                save(player, state);
            }
            case SLOT_NO_LEASH -> {
                noLeash = !noLeash;
                renderToggle(SLOT_NO_LEASH, Material.STRING,
                    TranslationKey.INVENTORIES__PET__NO_LEASH, noLeash);
                save(player, state);
            }
            case SLOT_NO_PICKUP -> {
                noPickup = !noPickup;
                renderToggle(SLOT_NO_PICKUP, Material.FEATHER,
                    TranslationKey.INVENTORIES__PET__NO_PICKUP, noPickup);
                save(player, state);
            }
            case SLOT_ENABLE_ALL  -> { overrideAll(true);  save(player, state); }
            case SLOT_DISABLE_ALL -> { overrideAll(false); save(player, state); }
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {
        // Settings are saved on every click, so nothing extra needed here.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void renderAll() {
        renderToggle(SLOT_PROTECT,    Material.BONE,    TranslationKey.INVENTORIES__PET__PROTECT,     protect);
        renderToggle(SLOT_NO_DAMAGE,  Material.SHIELD,  TranslationKey.INVENTORIES__PET__NO_DAMAGE,   noDamage);
        renderToggle(SLOT_NO_INTERACT,Material.BARRIER, TranslationKey.INVENTORIES__PET__NO_INTERACT, noInteract);
        renderToggle(SLOT_NO_LEASH,   Material.STRING,  TranslationKey.INVENTORIES__PET__NO_LEASH,    noLeash);
        renderToggle(SLOT_NO_PICKUP,  Material.FEATHER, TranslationKey.INVENTORIES__PET__NO_PICKUP,   noPickup);

        setItemStack(SLOT_ENABLE_ALL,  Material.GREEN_STAINED_GLASS_PANE, TranslationKey.INVENTORIES__REDSTONE__ENABLE_ALL);
        setItemStack(SLOT_DISABLE_ALL, Material.RED_STAINED_GLASS_PANE,   TranslationKey.INVENTORIES__REDSTONE__DISABLE_ALL);
        setBackButton(SLOT_BACK);
    }

    private void renderToggle(int slot, Material mat, TranslationKey key, boolean value) {
        setEnchantedOptionItemStack(slot, mat, key, value);
    }

    private void overrideAll(boolean value) {
        protect    = value;
        noDamage   = value;
        noInteract = value;
        noLeash    = value;
        noPickup   = value;
        renderAll();
    }

    /** Persists current toggle state to the entity's PDC immediately. */
    private void save(@NotNull Player player, @NotNull InventoryState state) {
        UUID petId = state.getPetEntityId();
        if (petId == null) return;

        // Find the entity in the player's visible worlds.
        Entity pet = null;
        for (Entity e : player.getWorld().getEntities()) {
            if (e.getUniqueId().equals(petId)) { pet = e; break; }
        }
        if (pet == null) return;

        PetNBTHandler handler = PetNBTHandler.forEntityOrNull(pet);
        if (handler == null) return;

        if (protect) {
            // Ensure owner is set (first-time enable from menu).
            if (handler.getOwner() == null) handler.setOwner(player.getUniqueId());
        }
        handler.setProtected(protect);
        handler.setNoDamage(noDamage);
        handler.setNoInteract(noInteract);
        handler.setNoLeash(noLeash);
        handler.setNoPickup(noPickup);
    }
}
