/*
 * Copyright (C) 2021 - 2025 spnda
 * Modifications Copyright (C) 2025 Zaynr (Zar)
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous Mojang-API skin resolver for offline/cracked servers.
 *
 * <p>On cracked (offline-mode) servers the player UUID is derived from the name
 * (offline UUID v3) and does not correspond to any Mojang profile — so skin
 * fetches via the standard Bukkit profile API silently return no texture.
 *
 * <p>This cache resolves skins by username against the Mojang API:
 * <ol>
 *   <li>{@code GET https://api.mojang.com/users/profiles/minecraft/{name}} → real UUID</li>
 *   <li>{@code GET https://sessionserver.mojang.com/session/minecraft/profile/{uuid}?unsigned=false} → skin</li>
 * </ol>
 * Results are cached per username until the server restarts. The first time a
 * name is requested a placeholder profile is returned instantly and the real
 * skin is applied when the async fetch completes (on the next GUI open).
 *
 * <p>Fetches are silently skipped if the network is unavailable or rate-limited.
 */
public final class SkinCache {

    private static final String MOJANG_UUID_URL  = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SKIN_URL  = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    /** Cached resolved profiles, keyed by lowercase player name. */
    private static final ConcurrentHashMap<String, PlayerProfile> cache = new ConcurrentHashMap<>();
    /** Names currently being fetched — prevents duplicate requests. */
    private static final ConcurrentHashMap<String, Boolean> inflight = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private SkinCache() {}

    /**
     * Returns the best available {@link PlayerProfile} for the given player.
     *
     * <p>If a cached profile with a skin is available it is returned immediately.
     * Otherwise a plain profile is returned and an async skin fetch is kicked off —
     * the next call after the fetch completes will return the skinned profile.
     *
     * @param name        The player's current username (case-insensitive).
     * @param offlineUuid The offline UUID to use when no real UUID can be resolved.
     * @return A {@link PlayerProfile}, never {@code null}.
     */
    @NotNull
    public static PlayerProfile getOrFetch(@NotNull String name, @NotNull UUID offlineUuid) {
        String key = name.toLowerCase();
        PlayerProfile cached = cache.get(key);
        if (cached != null) return cached;

        // Return a plain profile now; start async fetch (Mojang or SkinsRestorer) in background.
        PlayerProfile placeholder = Bukkit.getServer().createPlayerProfile(offlineUuid, name);
        if (inflight.putIfAbsent(key, Boolean.TRUE) == null) {
            CompletableFuture.runAsync(() -> fetchAndCache(key, name, offlineUuid));
        }
        return placeholder;
    }

    /**
     * Attempts to resolve a skin profile via the SkinsRestorer API (offline-safe).
     * Must only be called from an async thread.
     *
     * @return a populated {@link PlayerProfile}, or {@code null} if SkinsRestorer is not present
     *         or has no skin for this player.
     */
    @Nullable
    public static PlayerProfile resolveSkinsRestorer(@NotNull UUID uuid, @NotNull String name) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null || !plugin.isEnabled()) return null;
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object api = providerClass.getMethod("get").invoke(null);
            Object playerStorage = api.getClass().getMethod("getPlayerStorage").invoke(api);
            // getSkinForPlayer blocks on HTTP if the skin is not cached — must be async.
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
            PlayerProfile profile = Bukkit.getServer().createPlayerProfile(uuid, name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(skinUrl).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Exception ignored) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void fetchAndCache(@NotNull String key, @NotNull String name, @NotNull UUID fallbackUuid) {
        try {
            // 1. Try SkinsRestorer first (already async-safe here).
            PlayerProfile srProfile = resolveSkinsRestorer(fallbackUuid, name);
            if (srProfile != null) {
                cache.put(key, srProfile);
                return;
            }

            // 2. Resolve via Mojang API.
            String mojangUuidStr = fetchMojangUuid(name);
            UUID mojangUuid = mojangUuidStr != null ? parseUuid(mojangUuidStr) : null;

            if (mojangUuid == null) {
                // No Mojang UUID found (name does not exist online), cache the plain profile so
                // we don't hammer the API on every GUI open.
                PlayerProfile plain = Bukkit.getServer().createPlayerProfile(fallbackUuid, name);
                cache.put(key, plain);
                return;
            }

            // Step 2 — fetch the full profile with skin texture.
            String profileJson = fetchProfileJson(mojangUuid);
            if (profileJson == null) {
                cache.put(key, Bukkit.getServer().createPlayerProfile(mojangUuid, name));
                return;
            }

            // Step 3 — parse the textures property and build a PlayerProfile.
            PlayerProfile profile = buildProfileFromJson(mojangUuid, name, profileJson);
            cache.put(key, profile);
        } catch (Exception ignored) {
            // Network error, rate limit, etc. — silently skip; next call will retry.
        } finally {
            inflight.remove(key);
        }
    }

    /** Returns the raw Mojang UUID string (no hyphens) or null on failure. */
    @Nullable
    private static String fetchMojangUuid(@NotNull String name) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(MOJANG_UUID_URL + name))
            .header("User-Agent", "BlockProt-SkinCache")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 204 || resp.statusCode() == 404) return null;
        if (resp.statusCode() != 200) return null;

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        return obj.has("id") ? obj.get("id").getAsString() : null;
    }

    /** Fetches the full session-server profile JSON or null on failure. */
    @Nullable
    private static String fetchProfileJson(@NotNull UUID mojangUuid) throws Exception {
        String uuidNoDashes = mojangUuid.toString().replace("-", "");
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(String.format(MOJANG_SKIN_URL, uuidNoDashes)))
            .header("User-Agent", "BlockProt-SkinCache")
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        return resp.body();
    }

    /**
     * Parses the Mojang session-server JSON and returns a {@link PlayerProfile}
     * with the skin texture URL applied.
     */
    @NotNull
    private static PlayerProfile buildProfileFromJson(
            @NotNull UUID uuid, @NotNull String name, @NotNull String json) {

        PlayerProfile profile = Bukkit.getServer().createPlayerProfile(uuid, name);

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("properties")) return profile;
            JsonArray properties = root.getAsJsonArray("properties");
            for (var el : properties) {
                JsonObject prop = el.getAsJsonObject();
                if (!"textures".equals(prop.get("name").getAsString())) continue;

                String encoded = prop.get("value").getAsString();
                String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                JsonObject texObj = JsonParser.parseString(decoded).getAsJsonObject();

                if (!texObj.has("textures")) return profile;
                JsonObject textures = texObj.getAsJsonObject("textures");
                if (!textures.has("SKIN")) return profile;

                String skinUrl = textures.getAsJsonObject("SKIN").get("url").getAsString();
                PlayerTextures pt = profile.getTextures();
                pt.setSkin(URI.create(skinUrl).toURL());
                profile.setTextures(pt);
                return profile;
            }
        } catch (Exception ignored) {}
        return profile;
    }

    /** Parses a UUID string with or without hyphens. */
    @Nullable
    private static UUID parseUuid(@NotNull String raw) {
        try {
            if (raw.length() == 32) {
                // Add hyphens: 8-4-4-4-12
                raw = raw.substring(0, 8) + "-"
                    + raw.substring(8, 12) + "-"
                    + raw.substring(12, 16) + "-"
                    + raw.substring(16, 20) + "-"
                    + raw.substring(20);
            }
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
