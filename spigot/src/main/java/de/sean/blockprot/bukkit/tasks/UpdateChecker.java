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

package de.sean.blockprot.bukkit.tasks;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.util.SemanticVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Checks for new plugin releases on GitHub and notifies ops/admins.
 */
public final class UpdateChecker implements Runnable {

    /**
     * GitHub Releases API endpoint for this fork.
     * {@code /releases/latest} only returns stable releases.
     * We use {@code /releases} (list) so we can also detect pre-releases.
     */
    private static final String GITHUB_API_URL =
        "https://api.github.com/repos/VictorGugug/BlockProt-Reloaded/releases";

    /** Release page shown to players when an update is available. */
    private static final String RELEASE_URL =
        "https://github.com/VictorGugug/BlockProt-Reloaded/releases/latest";

    /**
     * Cached result of the last successful GitHub API call.
     * Package-accessible so {@link BackupTask} can read the cached value
     * without issuing a redundant HTTP request.
     */
    @Nullable
    public static volatile SemanticVersion latestVersion;

    @Nullable
    private final List<Player> recipients;

    @NotNull
    private final String pluginVersion;

    @NotNull
    private final SemanticVersion currentVersion;

    public UpdateChecker(@NotNull final String version) {
        this.pluginVersion = version;
        this.recipients = null;
        this.currentVersion = new SemanticVersion(version);
    }

    @Deprecated
    public UpdateChecker(@NotNull final PluginDescriptionFile description) {
        this.pluginVersion = description.getVersion();
        this.recipients = null;
        this.currentVersion = new SemanticVersion(description.getVersion());
    }

    @Deprecated
    public UpdateChecker(@NotNull final PluginDescriptionFile description,
                         @Nullable final List<Player> recipients) {
        this.recipients = recipients;
        this.pluginVersion = description.getVersion();
        this.currentVersion = new SemanticVersion(description.getVersion());
    }

    public UpdateChecker(@NotNull final String version,
                         @Nullable final List<Player> recipients) {
        this.recipients = recipients;
        this.pluginVersion = version;
        this.currentVersion = new SemanticVersion(version);
    }

    @Override
    public void run() {
        if (latestVersion != null) {
            this.sendMessage(currentVersion, latestVersion);
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .header("User-Agent", "BlockProt-Reloaded-UpdateChecker")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return;

            // Parse the list of releases and find the highest version
            // that is compatible with the current build type.
            // If running a pre-release we consider all releases.
            // If running a stable release we only consider stable releases
            // (but still show ahead-of-release info).
            GitHubRelease[] releases = new com.google.gson.Gson().fromJson(
                response.body(), GitHubRelease[].class);

            SemanticVersion best = null;
            for (GitHubRelease rel : releases) {
                if (rel.draft) continue;
                SemanticVersion v = rel.asSemantic();
                if (v.isExperimental()) continue;
                // If running stable, prefer stable releases; still track
                // pre-release channels when we ourselves are a pre-release.
                if (!currentVersion.isPreRelease() && rel.prerelease) continue;
                if (best == null || v.compareTo(best) > 0) best = v;
            }

            if (best == null) return;
            UpdateChecker.latestVersion = best;
            this.sendMessage(currentVersion, best);

        } catch (Exception ignored) {
        }
    }

    private void sendMessage(SemanticVersion currentVersion, SemanticVersion latestVersion) {
        boolean isOutdated = latestVersion.compareTo(currentVersion) > 0;

        if (this.recipients != null && !this.recipients.isEmpty()) {
            String message;
            if (isOutdated) {
                message = "BlockProt Reloaded v" + currentVersion
                    + " is installed, but v" + latestVersion + " is available.";
            } else if (latestVersion.compareTo(currentVersion) < 0) {
                message = "BlockProt Reloaded is running v" + currentVersion
                    + " (ahead of the latest release v" + latestVersion + ")";
            } else {
                message = "BlockProt Reloaded is up to date (v" + currentVersion + ")";
            }
            var comp = Component.text(message);
            if (isOutdated) {
                comp = comp.color(NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.openUrl(RELEASE_URL))
                    .hoverEvent(HoverEvent.showText(
                        Component.text("Click to visit the GitHub Releases page")));
            }
            for (Player player : recipients) {
                player.sendMessage(comp);
            }
        } else {
            if (isOutdated) {
                BlockProt.getInstance().getLogger().warning(
                    "BlockProt Reloaded v" + currentVersion
                        + " — update available: v" + latestVersion
                        + " | " + RELEASE_URL);
            } else if (latestVersion.compareTo(currentVersion) < 0) {
                // Running ahead of latest release — do not warn, just log.
                BlockProtLogger.log("update-checker",
                    "BlockProt Reloaded v" + currentVersion
                        + " is ahead of the latest release (v" + latestVersion + ").");
            } else {
                BlockProtLogger.log("update-checker",
                    "BlockProt Reloaded is up to date (v" + currentVersion + ")");
            }
        }
    }

    /**
     * Represents one entry from {@code GET /repos/{owner}/{repo}/releases}.
     */
    public static final class GitHubRelease {
        @SerializedName("tag_name")
        String tagName;

        @SerializedName("prerelease")
        boolean prerelease;

        @SerializedName("draft")
        boolean draft;

        @Contract(" -> new")
        public @NotNull SemanticVersion asSemantic() {
            String version = tagName != null ? tagName : "0.0.0";
            if (version.startsWith("v") || version.startsWith("V")) {
                version = version.substring(1);
            }
            return new SemanticVersion(version);
        }
    }
}