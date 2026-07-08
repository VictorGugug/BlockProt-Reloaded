package de.sean.blockprot.bukkit.integrations;

import com.cjburkey.claimchunk.ClaimChunk;
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

public final class ClaimChunkIntegration extends PluginIntegration implements Listener {

    private static final String RESTRICT_ACCESS = "restrict_access_to_chunk_owner";

    private boolean enabled = false;

    public ClaimChunkIntegration() {
        super("claimchunk");
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
            ClaimChunk.getInstance();
            this.registerListener(this);
            enabled = true;
        } catch (NoClassDefFoundError | IllegalStateException ignored) {
        }
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("ClaimChunk");
    }

    private boolean shouldRestrictAccessFully() {
        return configuration.getBoolean(RESTRICT_ACCESS, false);
    }

    @Nullable
    private UUID getChunkOwner(@NotNull Block block) {
        try {
            com.cjburkey.claimchunk.chunk.ChunkHandler handler =
                ClaimChunk.getInstance().getChunkHandler();
            return handler.getOwner(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isOwner(@NotNull Player player, @NotNull Block block) {
        UUID owner = getChunkOwner(block);
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
