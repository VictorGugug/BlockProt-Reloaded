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

package de.sean.blockprot.bukkit.integrations;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Integration for the ViaVersion plugin family.
 *
 * <p>Detects ViaVersion as the primary protocol translator. Additionally probes for
 * ViaBackwards and ViaRewind, which allow clients on older protocol versions to join
 * newer servers. All three are logged on enable so the session log clearly shows
 * which protocol bridges are active.</p>
 *
 * <p>Protocol version lookup always goes through the ViaVersion API regardless of
 * whether ViaBackwards or ViaRewind are present: they share the same API surface.</p>
 */
public final class ViaVersionIntegration extends PluginIntegration {

    private boolean enabled = false;

    /** ViaVersion main API class: always present when ViaVersion is active. */
    private static final String VIA_API_CLASS = "com.viaversion.viaversion.api.Via";

    public ViaVersionIntegration() {
        super("viaversion", false);
    }

    /**
     * ViaVersion has no integration config file: skip the file load entirely.
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
        if (!IntegrationConfig.getBoolean("viaversion.enabled", true)) return;
        Plugin via = BlockProt.getInstance().getPlugin("ViaVersion");
        if (via == null || !via.isEnabled()) return;

        try {
            Class.forName(VIA_API_CLASS);
            enabled = true;

            String viaVer = via.getPluginMeta().getVersion();
            BlockProtLogger.log("integration",
                Translator.get(TranslationKey.CONSOLE__VIAVERSION_DETECTED)
                    .replace("{version}", viaVer));

            // Log companion plugins: they extend ViaVersion but share its API.
            probeCompanion("ViaBackwards");
            probeCompanion("ViaRewind");

        } catch (ClassNotFoundException ignored) {
            BlockProtLogger.log("integration",
                Translator.get(TranslationKey.CONSOLE__VIAVERSION_API_MISSING));
        }
    }

    /**
     * Logs a companion plugin (ViaBackwards, ViaRewind) if installed and enabled.
     * Does not affect the enabled flag: detection is informational only.
     */
    private void probeCompanion(@NotNull String pluginName) {
        Plugin plugin = BlockProt.getInstance().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) return;
        String ver;
        try {
            ver = plugin.getPluginMeta().getVersion();
        } catch (NoSuchMethodError e) {
            @SuppressWarnings("deprecation")
            String fallback = plugin.getDescription().getVersion();
            ver = fallback;
        }
        BlockProtLogger.log("integration", pluginName + " detected: v" + ver
            + " (extends ViaVersion protocol translation)");
    }

    @Override
    @Nullable
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("ViaVersion");
    }

    /**
     * Returns a human-readable MC version string for the player's client.
     *
     * <p>Uses ViaVersion protocol map to convert protocol number to version string.
     * Returns the server MC version string when ViaVersion is not active or the
     * player is on the same version as the server.</p>
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

    /**
     * Returns the raw protocol version integer for the given player, or -1 if
     * ViaVersion is not active or the lookup fails.
     */
    public int getPlayerProtocolVersion(@NotNull Player player) {
        if (!enabled) return -1;
        try {
            Class<?> viaClass = Class.forName(VIA_API_CLASS);
            Object   viaApi   = viaClass.getMethod("getAPI").invoke(null);
            // Try UUID-based lookup first (preferred), then Player-based fallback.
            try {
                Object result = viaApi.getClass()
                    .getMethod("getPlayerVersion", java.util.UUID.class)
                    .invoke(viaApi, player.getUniqueId());
                if (result instanceof Integer i) return i;
            } catch (Exception e) {
                try {
                    Object result = viaApi.getClass()
                        .getMethod("getPlayerVersion", Object.class)
                        .invoke(viaApi, player);
                    if (result instanceof Integer i) return i;
                } catch (Exception e2) {
                    Object result = viaApi.getClass()
                        .getMethod("getPlayerVersion", Player.class)
                        .invoke(viaApi, player);
                    if (result instanceof Integer i) return i;
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * Returns a summary string listing ViaVersion and any active companion plugins.
     * Used by the debug command and integrations menu.
     */
    @NotNull
    public String getDetailedStatus() {
        if (!enabled) return "ViaVersion: not detected";
        StringBuilder sb = new StringBuilder("ViaVersion");
        Plugin via = getPlugin();
        if (via != null) {
            try { sb.append(" v").append(via.getPluginMeta().getVersion()); }
            catch (NoSuchMethodError e) {
                @SuppressWarnings("deprecation")
                String fallback = via.getDescription().getVersion();
                sb.append(" v").append(fallback);
            }
        }
        appendCompanion(sb, "ViaBackwards");
        appendCompanion(sb, "ViaRewind");
        return sb.toString();
    }

    private void appendCompanion(@NotNull StringBuilder sb, @NotNull String name) {
        Plugin p = BlockProt.getInstance().getPlugin(name);
        if (p == null || !p.isEnabled()) return;
        sb.append(" + ").append(name);
        try { sb.append(" v").append(p.getPluginMeta().getVersion()); }
        catch (NoSuchMethodError e) {
            @SuppressWarnings("deprecation")
            String fallback = p.getDescription().getVersion();
            sb.append(" v").append(fallback);
        }
    }
}