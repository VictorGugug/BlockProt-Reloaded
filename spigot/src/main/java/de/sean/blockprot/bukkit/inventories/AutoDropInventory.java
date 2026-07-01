package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class AutoDropInventory extends BlockProtInventory {

    private record FamilyEntry(BlockFamilyParser.Family family, Material icon, int slot) {}

    private static final int SLOT_BACK = 26;

    private static final List<FamilyEntry> FAMILIES = List.of(
        new FamilyEntry(BlockFamilyParser.Family.TILE_ENTITIES, Material.CHEST, 10),
        new FamilyEntry(BlockFamilyParser.Family.SHULKER_BOXES, Material.SHULKER_BOX, 12),
        new FamilyEntry(BlockFamilyParser.Family.BLOCKS, Material.ANVIL, 14),
        new FamilyEntry(BlockFamilyParser.Family.DOORS, Material.OAK_DOOR, 16),
        new FamilyEntry(BlockFamilyParser.Family.ENTITIES, Material.ITEM_FRAME, 18)
    );

    public AutoDropInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() { return Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__TITLE); }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        Set<Material> autoDropBlocks = cfg.getAutoDropToInventoryBlocks();

        for (FamilyEntry fe : FAMILIES) {
            Set<Material> members = BlockFamilyParser.getFamilyMembers(fe.family());
            long active = members.stream().filter(autoDropBlocks::contains).count();
            long total = members.size();
            String label = friendlyName(fe.family().name());
            boolean allActive = active == total;
            boolean noneActive = active == 0;

            ItemStack stack = new ItemStack(fe.icon());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                NamedTextColor nameColor = allActive ? NamedTextColor.GREEN : (noneActive ? NamedTextColor.RED : NamedTextColor.GOLD);
                meta.displayName(Component.text(label).color(nameColor));

                NamedTextColor statusColor = allActive ? NamedTextColor.GREEN : (noneActive ? NamedTextColor.RED : NamedTextColor.GOLD);
                String statusText = allActive
                    ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_ACTIVE)
                    : (noneActive
                        ? Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__STATUS_INACTIVE)
                        : active + "/" + total);

                meta.lore(List.of(
                    Component.text(statusText).color(statusColor),
                    Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__LEFT_CLICK_HINT)).color(NamedTextColor.GREEN),
                    Component.text(Translator.get(TranslationKey.INVENTORIES__AUTO_DROP__RIGHT_CLICK_HINT)).color(NamedTextColor.YELLOW)
                ));
                stack.setItemMeta(meta);
            }
            inventory.setItem(fe.slot(), stack);
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
            InventoryState ns = InventoryState.builder()
                .origin(state.origin)
                .build();
            ns.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), ns);
            player.openInventory(new LockablesInventory().fill(player, 0));
            return;
        }

        for (FamilyEntry fe : FAMILIES) {
            if (slot == fe.slot()) {
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    BlockProt.getDefaultConfig().toggleAutoDropFamily(fe.family(), player);
                    player.openInventory(fill(player));
                } else {
                    player.openInventory(new AutoDropFamilyInventory().fill(player, fe.family(), 0, state));
                }
                return;
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    @NotNull
    private static String friendlyName(@NotNull String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}