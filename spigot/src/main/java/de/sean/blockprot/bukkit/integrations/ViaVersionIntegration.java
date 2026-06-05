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
        super("viaversion");
    }

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
