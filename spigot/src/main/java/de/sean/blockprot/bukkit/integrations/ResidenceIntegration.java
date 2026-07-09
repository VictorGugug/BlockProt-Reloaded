package de.sean.blockprot.bukkit.integrations;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ResidenceIntegration extends PluginIntegration implements Listener {

    private static final String RESTRICT_ACCESS = "restrict_access_to_residence_owner";

    private boolean enabled = false;

    public ResidenceIntegration() {
        super("residence");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        if (!configuration.getBoolean("enabled", true)) return;
        Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled()) return;
        try {
            this.registerListener(this);
            enabled = true;
        } catch (NoClassDefFoundError | IllegalStateException ignored) {
        }
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("Residence");
    }

    private boolean shouldRestrictAccessFully() {
        return configuration.getBoolean(RESTRICT_ACCESS, false);
    }

    @Nullable
    private UUID getResidenceOwner(@NotNull Block block) {
        try {
            ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(block.getLocation());
            if (res != null) {
                return res.getOwnerUUID();
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isOwner(@NotNull Player player, @NotNull Block block) {
        UUID owner = getResidenceOwner(block);
        return owner != null && owner.equals(player.getUniqueId());
    }

    @EventHandler
    public void onAccess(@NotNull final BlockAccessEvent event) {
        if (!shouldRestrictAccessFully()) return;
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAccessEditMenu(@NotNull final BlockAccessMenuEvent event) {
        if (!shouldRestrictAccessFully()) return;
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLockOnPlace(@NotNull final BlockLockOnPlaceEvent event) {
        if (!isOwner(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
