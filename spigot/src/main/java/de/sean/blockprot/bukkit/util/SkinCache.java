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

package de.sean.blockprot.bukkit.util;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous Mojang-API skin resolver using Bukkit's {@code PlayerProfile.update()}.
 *
 * <p>On offline-mode servers the player UUID is derived from the name (offline UUID v3)
 * and does not correspond to any Mojang profile: the lookup still works because it
 * resolves by name.  For cracked players without a Mojang account this falls back to
 * SkinsRestorer (if installed).
 *
 * <p>Results are cached per username until the server restarts.
 */
@SuppressWarnings("deprecation")
public final class SkinCache {

    private static final ConcurrentHashMap<String, PlayerProfile> cache = new ConcurrentHashMap<>();

    private SkinCache() {}

    /**
     * Fire-and-forget pre-fetch: starts the async resolution and returns immediately.
     * The result will be cached for subsequent {@link #getOrFetchAsync} calls.
     */
    public static void getOrFetch(@NotNull String name, @NotNull UUID uuid) {
        getOrFetchAsync(name, uuid);
    }

    /**
     * Asynchronously returns a {@link PlayerProfile} with a resolved skin.
     *
     * <p>Uses Bukkit's built-in {@code PlayerProfile.update()} (Mojang API) and then
     * falls back to SkinsRestorer if no texture was returned.
     *
     * @param name Player name (case-insensitive cache key).
     * @param uuid The UUID to use when creating the profile.
     * @return A future that completes with the best available profile (may not have textures).
     */
    @NotNull
    public static CompletableFuture<PlayerProfile> getOrFetchAsync(@NotNull String name, @NotNull UUID uuid) {
        String key = name.toLowerCase();
        PlayerProfile cached = cache.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        // Bukkit.createProfile(UUID, String) returns org.bukkit.profile.PlayerProfile,
        // the standard cross-platform API present on Paper and Spigot alike. The
        // NoSuchMethodError catch here is a defensive fallback for older server builds.
        PlayerProfile profile;
        try {
            profile = Bukkit.createProfile(uuid, name);
        } catch (NoSuchMethodError e) {
            PlayerProfile srProfile = resolveSkinsRestorer(uuid, name);
            if (srProfile != null) {
                cache.put(key, srProfile);
                return CompletableFuture.completedFuture(srProfile);
            }
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<PlayerProfile> updateFuture = profile.update().thenApply(p -> (PlayerProfile) p);
        return updateFuture
            .exceptionally(ex -> {
                BlockProt.getInstance().getLogger().warning("Skin fetch failed for " + name + ": " + ex.getMessage());
                return profile;
            })
            .thenApply(updated -> {
                if (hasSkin(updated)) {
                    cache.put(key, updated);
                    return updated;
                }
                // Fallback: SkinsRestorer (offline-mode premium/custom skins)
                PlayerProfile srProfile = resolveSkinsRestorer(uuid, name);
                if (srProfile != null) {
                    cache.put(key, srProfile);
                    return srProfile;
                }
                // No texture available at all: cache the non-textured profile so we don't
                // hammer the API every time.  The skull will use the server's default (Steve/Alex).
                cache.put(key, updated);
                return updated;
            });
    }

    /**
     * Attempts to resolve a skin profile via the SkinsRestorer API (offline-safe).
     *
     * @return a populated {@link PlayerProfile}, or {@code null}.
     */
    @Nullable
    public static PlayerProfile resolveSkinsRestorer(@NotNull UUID uuid, @NotNull String name) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null || !plugin.isEnabled()) return null;
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            Object playerStorage = api.getClass().getMethod("getPlayerStorage").invoke(api);
            Object optional = playerStorage.getClass()
                .getMethod("getSkinForPlayer", UUID.class, String.class)
                .invoke(playerStorage, uuid, name);
            boolean present = (boolean) optional.getClass().getMethod("isPresent").invoke(optional);
            if (!present) return null;
            Object skinProperty = optional.getClass().getMethod("get").invoke(optional);
            String value     = (String) skinProperty.getClass().getMethod("getValue").invoke(skinProperty);
            String decoded = new String(
                java.util.Base64.getDecoder().decode(value),
                java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(decoded).getAsJsonObject();
            if (!root.has("textures") || !root.getAsJsonObject("textures").has("SKIN")) return null;
            String skinUrl = root.getAsJsonObject("textures")
                .getAsJsonObject("SKIN").get("url").getAsString();
            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(skinUrl).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hasSkin(@Nullable PlayerProfile profile) {
        if (profile == null) return false;
        try {
            return profile.getTextures() != null && profile.getTextures().getSkin() != null;
        } catch (Exception ignored) {
            return false;
        }
    }
}