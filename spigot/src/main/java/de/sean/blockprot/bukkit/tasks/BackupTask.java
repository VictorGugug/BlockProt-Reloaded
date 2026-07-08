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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtConsole;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates a ZIP backup of the plugin data folder before critical operations.
 *
 * <p>Backup is ONLY created when existing data is detected (blockprot.db,
 * or any nbt data file). This prevents noise on completely fresh installations.
 * When a backup is made, the console receives a clear notice and a reminder to
 * review the new config options.</p>
 *
 * <p>Backups are saved to: {@code plugins/BlockProt/backups/YYYY-MM-DD_HH-mm.zip}</p>
 *
 * <p>Version metadata is written from the cached {@link UpdateChecker#latestVersion}
 * value when available, avoiding a redundant HTTP request at startup.</p>
 *
 * @since SP26
 */
public final class BackupTask implements Runnable {

    private static final SimpleDateFormat DATE_FMT  = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    private static final int              MAX_BACKUPS = 10;

    /** Files whose presence signals that the server has pre-existing plugin data. */
    private static final String[] DATA_SENTINELS = {
        "data.yml",
        "blockprot.db",
        "blockprot_audit.sqlite",
        "stats.yml"
    };

    private final File    dataFolder;
    /**
     * When true, the caller explicitly requested a backup (e.g. /bp reload).
     * When false, the backup is opportunistic: only runs if prior data exists.
     */
    private final boolean forced;

    public BackupTask(@NotNull File dataFolder) {
        this(dataFolder, false);
    }

    public BackupTask(@NotNull File dataFolder, boolean forced) {
        this.dataFolder = dataFolder;
        this.forced     = forced;
    }

    @Override
    public void run() {
        boolean hasPriorData = forced || hasPriorData(dataFolder);

        if (!hasPriorData) {
            // Fresh installation: no backup needed.
            return;
        }

        File backupDir = new File(dataFolder, "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__BACKUP_DIR_FAILED));
            return;
        }

        pruneOldBackups(backupDir);

        String version = "unknown";
        try {
            version = BlockProt.getPluginVersion();
            if (version.isBlank()) version = "unknown";
        } catch (Exception ignored) {}

        String timestamp = DATE_FMT.format(new Date());
        String suffix = version.isBlank() ? "" : "_v" + version.replaceAll("\\s+", "_");

        File zipFile = new File(backupDir, timestamp + suffix + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addDirectory(dataFolder, dataFolder.getName(), zos, backupDir);

            // Write minimal metadata: no network calls on the main thread.
            String meta = "plugin: BlockProt\nversion: " + version + "\n";

            // Use the cached latest-version result from UpdateChecker if available,
            // to avoid a redundant HTTP request at startup.
            de.sean.blockprot.util.SemanticVersion cached = UpdateChecker.latestVersion;
            if (cached != null) {
                boolean isLatest = cached.compareTo(new de.sean.blockprot.util.SemanticVersion(version)) <= 0;
                meta += "latest_release: " + cached + "\nis_latest: " + isLatest + "\n";
                BlockProtLogger.log("backup", "Version check (cached): latest=" + cached
                    + ", running=" + version + ", isLatest=" + isLatest);
            }

            zos.putNextEntry(new ZipEntry("release_info.txt"));
            zos.write(meta.getBytes());
            zos.closeEntry();

            BlockProtLogger.log("backup", "Backup created: " + zipFile.getName());
            if (!forced) {
                BlockProtLogger.log("backup", Translator.get(TranslationKey.CONSOLE__BACKUP_REVIEW_CONFIG));
            }
        } catch (IOException e) {
            BlockProt.getInstance().getLogger().warning(
                Translator.get(TranslationKey.CONSOLE__BACKUP_FAILED)
                    .replace("{error}", e.getMessage()));
        }
    }

    /**
     * Returns true if any known data-sentinel file exists in the data folder,
     * meaning this is an upgrade rather than a fresh installation.
     */
    public static boolean hasPriorData(@NotNull File dataFolder) {
        for (String name : DATA_SENTINELS) {
            if (new File(dataFolder, name).exists()) return true;
        }
        File[] children = dataFolder.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            String name = child.getName();
            if (name.equalsIgnoreCase("backups") || name.equalsIgnoreCase("lang")) continue;
            if (containsLegacyData(child)) return true;
        }
        return false;
    }

    private static boolean containsLegacyData(@NotNull File dir) {
        File[] children = dir.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (child.isDirectory()) {
                if (child.getName().equalsIgnoreCase("DIM-1") || child.getName().equalsIgnoreCase("DIM1")) return true;
                if (containsLegacyData(child)) return true;
                continue;
            }
            String name = child.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".yml") || name.endsWith(".db") || name.endsWith(".sqlite") || name.endsWith(".dat")) {
                return true;
            }
        }
        return false;
    }

    private void addDirectory(@NotNull File dir, @NotNull String base,
                              @NotNull ZipOutputStream zos, @NotNull File backupDir)
            throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            // Never recurse into the backups folder itself, and skip session logs.
            if (file.equals(backupDir) || file.getName().endsWith(".log")) continue;

            String entry = base + "/" + file.getName();
            if (file.isDirectory()) {
                addDirectory(file, entry, zos, backupDir);
            } else {
                zos.putNextEntry(new ZipEntry(entry));
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.transferTo(zos);
                }
                zos.closeEntry();
            }
        }
    }

    private void pruneOldBackups(@NotNull File backupDir) {
        File[] zips = backupDir.listFiles((d, name) -> name.endsWith(".zip"));
        if (zips == null || zips.length < MAX_BACKUPS) return;

        Arrays.sort(zips, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i <= zips.length - MAX_BACKUPS; i++) {
            //noinspection ResultOfMethodCallIgnored
            zips[i].delete();
        }
    }
}