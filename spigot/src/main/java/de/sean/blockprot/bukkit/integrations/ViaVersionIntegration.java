package de.sean.blockprot.bukkit.integrations;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ViaVersionIntegration extends PluginIntegration {

    private boolean enabled = false;

    private static final String VIA_API_CLASS = "com.viaversion.viaversion.api.Via";

    public ViaVersionIntegration() {
        super("viaversion", false);
    }

    /**
     * ViaVersion has no integration config file — skip the file load entirely.
     * Overriding reload() prevents the parent from trying to load integrations/viaversion.yml
     * on every /bp reload, which would produce a console warning.
     */
    @Override
    public void reload() { /* no config to reload */ }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        Plugin via = BlockProt.getInstance().getPlugin("ViaVersion");
        if (via == null || !via.isEnabled()) return;

        try {
            Class.forName(VIA_API_CLASS);
            enabled = true;
            BlockProtLogger.log("integration",
                Translator.get(TranslationKey.CONSOLE__VIAVERSION_DETECTED)
                    .replace("{version}", via.getDescription().getVersion()));
        } catch (ClassNotFoundException ignored) {
            BlockProtLogger.log("integration",
                Translator.get(TranslationKey.CONSOLE__VIAVERSION_API_MISSING));
        }
    }

    @Override
    @Nullable
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("ViaVersion");
    }

    /**
     * Returns a human-readable MC version string for the player's client.
     * Uses ViaVersion protocol map to convert protocol number to version string.
     * Returns the server MC version string when ViaVersion is not active or the
     * player is on the same version as the server.
     */
    @NotNull
    public String getPlayerVersionString(@NotNull Player player) {
        int protocol = getPlayerProtocolVersion(player);
        if (protocol <= 0) return de.sean.blockprot.bukkit.VersionCompat.getVersionString();
        try {
            Class<?> protoClass = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Object pv = protoClass.getMethod("getProtocol", int.class).invoke(null, protocol);
            Object ver = pv.getClass().getMethod("getName").invoke(pv);
            String name = ver != null ? ver.toString() : String.valueOf(protocol);
            return name.isBlank() ? String.valueOf(protocol) : name;
        } catch (Exception ignored) {
            return String.valueOf(protocol);
        }
    }

    public int getPlayerProtocolVersion(@NotNull Player player) {
        if (!enabled) return -1;
        try {
            Class<?> viaClass   = Class.forName(VIA_API_CLASS);
            Object   viaApi     = viaClass.getMethod("getAPI").invoke(null);
            Object   playerInfo = viaApi.getClass()
                .getMethod("getPlayerVersion", Player.class)
                .invoke(viaApi, player);
            return playerInfo instanceof Integer i ? i : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }
}
