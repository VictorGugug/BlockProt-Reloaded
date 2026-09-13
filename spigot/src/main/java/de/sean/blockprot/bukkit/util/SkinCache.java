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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous Mojang-API skin resolver using Bukkit's {@code PlayerProfile.update()}.
 */
@SuppressWarnings("deprecation")
public final class SkinCache {

    private static final Cache<String, PlayerProfile> cache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    private SkinCache() {}

    public static void getOrFetch(@NotNull String name, @NotNull UUID uuid) {
        getOrFetchAsync(name, uuid);
    }

    @Nullable
    public static PlayerProfile getCachedOrOnlineProfile(@NotNull String name, @NotNull UUID uuid) {
        String key = name.toLowerCase();
        PlayerProfile cached = cache.getIfPresent(key);
        if (cached != null && hasSkin(cached)) return cached;

        try {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null) online = Bukkit.getPlayerExact(name);
            if (online != null) {
                PlayerProfile op = online.getPlayerProfile();
                if (hasSkin(op)) {
                    cache.put(key, op);
                    return op;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @NotNull
    public static CompletableFuture<PlayerProfile> getOrFetchAsync(@NotNull String name, @NotNull UUID uuid) {
        String key = name.toLowerCase();
        PlayerProfile cached = cache.getIfPresent(key);
        if (cached != null && hasSkin(cached)) return CompletableFuture.completedFuture(cached);

        try {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null) online = Bukkit.getPlayerExact(name);
            if (online != null) {
                PlayerProfile op = online.getPlayerProfile();
                if (hasSkin(op)) {
                    cache.put(key, op);
                    return CompletableFuture.completedFuture(op);
                }
            }
        } catch (Throwable ignored) {}

        if (isBedrockPlayer(name, uuid)) {
            return CompletableFuture.supplyAsync(() -> {
                PlayerProfile bpProfile = resolveBedrockSkin(uuid, name);
                if (bpProfile != null) {
                    cache.put(key, bpProfile);
                    return bpProfile;
                }
                PlayerProfile srProfile = resolveSkinsRestorer(uuid, name);
                if (srProfile != null) {
                    cache.put(key, srProfile);
                    return srProfile;
                }
                try {
                    return Bukkit.createProfile(uuid, name);
                } catch (Throwable t) {
                    return null;
                }
            });
        }

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
            .exceptionally(ex -> profile)
            .thenApply(updated -> {
                if (hasSkin(updated)) {
                    cache.put(key, updated);
                    return updated;
                }
                return null;
            })
            .thenCompose(updated -> updated != null
                ? CompletableFuture.completedFuture(updated)
                : CompletableFuture.supplyAsync(() -> {
                    try {
                        PlayerProfile nameOnlyProfile = Bukkit.createProfile(name);
                        PlayerProfile updatedNameProfile = nameOnlyProfile.update().join();
                        if (hasSkin(updatedNameProfile)) {
                            cache.put(key, updatedNameProfile);
                            return updatedNameProfile;
                        }
                    } catch (Throwable ignored) {}

                    PlayerProfile srProfile = resolveSkinsRestorer(uuid, name);
                    if (srProfile != null) {
                        cache.put(key, srProfile);
                        return srProfile;
                    }
                    PlayerProfile pdProfile = resolvePlayerDb(name);
                    if (pdProfile != null) {
                        cache.put(key, pdProfile);
                        return pdProfile;
                    }
                    PlayerProfile bpProfile = resolveBedrockSkin(uuid, name);
                    if (bpProfile != null) {
                        cache.put(key, bpProfile);
                        return bpProfile;
                    }
                    return profile;
                }));
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

    /**
     * Third-tier fallback: resolves a real Mojang UUID from the player name via
     * the free PlayerDB API, then fetches the signed texture from the Mojang
     * sessionserver. Both calls are blocking, so callers must run them off the
     * primary thread.
     *
     * @return a populated {@link PlayerProfile}, or {@code null}.
     */
    @Nullable
    public static PlayerProfile resolvePlayerDb(@NotNull String name) {
        String mojangUuid = fetchPlayerDbUuid(name);
        if (mojangUuid == null) return null;
        String skinUrl = fetchSessionserverTexture(UUID.fromString(mojangUuid));
        if (skinUrl == null) return null;
        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.fromString(mojangUuid), name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(skinUrl).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static String fetchPlayerDbUuid(@NotNull String name) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://playerdb.co/api/player/minecraft/" + name))
                .header("User-Agent", "BlockProt-Reloaded-SkinCache")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var root = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("data") || !root.getAsJsonObject("data").has("player")) return null;
            var player = root.getAsJsonObject("data").getAsJsonObject("player");
            if (!player.has("id")) return null;
            return player.get("id").getAsString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static String fetchSessionserverTexture(@NotNull UUID uuid) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                .header("User-Agent", "BlockProt-Reloaded-SkinCache")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var root = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            if (!root.has("properties")) return null;
            for (var element : root.getAsJsonArray("properties")) {
                var property = element.getAsJsonObject();
                if (!property.has("name") || !"textures".equals(property.get("name").getAsString())) continue;
                if (!property.has("value")) return null;
                String decoded = new String(
                    java.util.Base64.getDecoder().decode(property.get("value").getAsString()),
                    java.nio.charset.StandardCharsets.UTF_8);
                var texturesRoot = com.google.gson.JsonParser.parseString(decoded).getAsJsonObject();
                if (!texturesRoot.has("textures") || !texturesRoot.getAsJsonObject("textures").has("SKIN")) return null;
                return texturesRoot.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN").get("url").getAsString();
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isBedrockPlayer(@NotNull String name, @NotNull UUID uuid) {
        if (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() != 0L) return true;
        if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.isBedrockPlayer(uuid)) return true;
        for (String prefix : BlockProt.getDefaultConfig().getBedrockUsernamePrefixes()) {
            if (prefix != null && !prefix.isEmpty() && name.startsWith(prefix)) return true;
        }
        return false;
    }

    @Nullable
    public static PlayerProfile resolveBedrockSkin(@NotNull UUID uuid, @NotNull String name) {
        String xuid = resolveXuid(uuid, name);
        if (xuid == null || xuid.isBlank()) return null;
        String textureId = fetchGeyserSkinTextureId(xuid);
        if (textureId == null || textureId.isBlank()) return null;
        try {
            PlayerProfile profile = Bukkit.createProfile(uuid, name);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create("http://textures.minecraft.net/texture/" + textureId).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static String resolveXuid(@NotNull UUID uuid, @NotNull String name) {
        if (de.sean.blockprot.bukkit.bedrock.BedrockBridge.isFloodgatePresent()) {
            try {
                var fp = org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid);
                if (fp != null && fp.getXuid() != null && !fp.getXuid().isBlank()) {
                    return fp.getXuid();
                }
            } catch (Throwable ignored) {}
        }
        if (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() != 0L) {
            return Long.toUnsignedString(uuid.getLeastSignificantBits());
        }
        String gamertag = name;
        for (String prefix : BlockProt.getDefaultConfig().getBedrockUsernamePrefixes()) {
            if (prefix != null && !prefix.isEmpty() && gamertag.startsWith(prefix)) {
                gamertag = gamertag.substring(prefix.length());
                break;
            }
        }
        return fetchGeyserXuid(gamertag);
    }

    @Nullable
    private static String fetchGeyserSkinTextureId(@NotNull String xuid) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.geysermc.org/v2/skin/" + xuid))
                .header("User-Agent", "BlockProt-Reloaded-SkinCache")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var root = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.has("texture_id") && !root.get("texture_id").isJsonNull()) {
                return root.get("texture_id").getAsString();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Nullable
    private static String fetchGeyserXuid(@NotNull String gamertag) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.geysermc.org/v2/xbox/xuid/"
                    + URLEncoder.encode(gamertag, StandardCharsets.UTF_8)))
                .header("User-Agent", "BlockProt-Reloaded-SkinCache")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            var root = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            if (root.has("xuid") && !root.get("xuid").isJsonNull()) {
                return root.get("xuid").getAsString();
            }
        } catch (Throwable ignored) {}
        return null;
    }
}