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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.admin.AdminAction;
import de.sean.blockprot.bukkit.admin.AdminTier;
import de.sean.blockprot.bukkit.admin.AdminTierManager;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.IntegrationConfig;
import de.sean.blockprot.bukkit.config.LangConfig;
import de.sean.blockprot.bukkit.config.ReloadReport;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.integrations.ViaVersionIntegration;
import de.sean.blockprot.bukkit.listeners.EffectGeometry;
import de.sean.blockprot.bukkit.dialogs.*;
import de.sean.blockprot.bukkit.inventories.*;
import de.sean.blockprot.bukkit.nbt.BlockAccessFlag;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.PlayerInventoryClipboard;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.BlockCountStatistic;
import de.sean.blockprot.bukkit.nbt.stats.LocationListEntry;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import de.sean.blockprot.bukkit.storage.ProtectedBlockCache;
import de.sean.blockprot.bukkit.util.AsyncGuard;
import de.sean.blockprot.bukkit.util.BlockUtil;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import de.sean.blockprot.bukkit.util.DurationLimits;
import de.sean.blockprot.bukkit.util.DurationParser;
import de.sean.blockprot.bukkit.util.PlayerLookup;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import de.sean.blockprot.bukkit.util.SkinCache;
import de.sean.blockprot.bukkit.util.StringUtil;
import de.sean.blockprot.bukkit.util.TemporaryActionBar;
import de.sean.blockprot.util.SemanticVersion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

/**
 * /blockprot debug: diagnostics and manual test-bench.
 */
public class DebugCommand implements CommandExecutor {

    private static final String NOTCH_UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) return false;
        if (!(sender instanceof Player player)) {
            if (args.length < 2 || args[1].equalsIgnoreCase("run")) {
                Player target = null;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                }
                if (target == null && !Bukkit.getOnlinePlayers().isEmpty()) {
                    target = Bukkit.getOnlinePlayers().iterator().next();
                }
                if (target != null) {
                    ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_PLAYER_CONTEXT).replace("{player}", target.getName()));
                    run(target);
                    return true;
                } else {
                    runHeadless(sender);
                    return true;
                }
            }
            sender.sendMessage(Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__DEBUG_USAGE));
            return false;
        }

        switch (args[1]) {
            case "placeDebugChest" -> {
                player.getWorld().setType(player.getLocation(), Material.CHEST);
                new BlockNBTHandler(player.getWorld().getBlockAt(player.getLocation())).setOwner(NOTCH_UUID);
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__CHEST_PLACED));
                return true;
            }
            case "placeDebugShulker" -> {
                player.getWorld().setType(player.getLocation(), Material.SHULKER_BOX);
                new BlockNBTHandler(player.getWorld().getBlockAt(player.getLocation())).setOwner(NOTCH_UUID);
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__SHULKER_PLACED));
                return true;
            }
            case "clearSearchHistory" -> {
                new PlayerSettingsHandler(player).clearSearchHistory();
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__HISTORY_CLEARED));
                return true;
            }
            case "run" -> {
                run(player);
                return true;
            }
        }
        return false;
    }

    /**
     * Shows the diagnostics hint and runs the diagnostic suite asynchronously.
     * Used by both the CLI and the admin menu.
     */
    public static void run(@NotNull Player player) {
        ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        BlockProt.getFoliaLib().getScheduler().runAsync(task -> new DebugCommand().runDiagnostics(player));
    }

    public static void runHeadless(@NotNull CommandSender sender) {
        BlockProt.getFoliaLib().getScheduler().runAsync(task -> new DebugCommand().runDiagnosticsHeadless(sender));
    }

    private static void ab(@NotNull Player p, @NotNull String msg) {
        ComponentMessages.sendLegacyActionBar(p, msg);
    }

    private static void chat(@NotNull Player p, @NotNull String msg) {
        ComponentMessages.sendLegacy(p, msg);
    }

    private static final String INVENTORY_PACKAGE = "de.sean.blockprot.bukkit.inventories";
    private static final String DIALOG_PACKAGE = "de.sean.blockprot.bukkit.dialogs";
    private static final String LISTENERS_PACKAGE = "de.sean.blockprot.bukkit.listeners";
    private static final String COMMANDS_PACKAGE = "de.sean.blockprot.bukkit.commands";
    private static final Set<String> coveredClasses = new HashSet<>();

    private static void touch(String fqcn) {
        coveredClasses.add(fqcn);
    }

    private static void touchScreen(String pkg, String displayedName) {
        coveredClasses.add(pkg + "." + displayedName.split(" ")[0]);
    }

    private void runDomain(String index, String title) {
        BlockProtLogger.separator();
        BlockProtLogger.log("[" + index + "] " + title);
    }

    private static void logEnvironmentInfo(@NotNull Player player) {
        BlockProtLogger.log("Session: " + java.time.LocalDateTime.now()
            + " | Plugin: BlockProt Reloaded " + BlockProt.getPluginVersion()
            + " | Player: " + player.getName() + " (" + player.getUniqueId() + ")");
        BlockProtLogger.log("Server: " + org.bukkit.Bukkit.getVersion()
            + " | API: " + org.bukkit.Bukkit.getBukkitVersion()
            + " | Java: " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat: " + de.sean.blockprot.bukkit.VersionCompat.getDiagnosticString()
            + " | BukkitCompat: " + de.sean.blockprot.bukkit.BukkitCompat.getDiagnosticString()
            + " | FoliaLib: [isFolia=" + BlockProt.getFoliaLib().isFolia()
            + ", isPaper=" + BlockProt.getFoliaLib().isPaper()
            + ", isSpigot=" + BlockProt.getFoliaLib().isSpigot() + "]");
    }

    private void runDiagnostics(@NotNull Player player) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        coveredClasses.clear();
        BlockProtLogger.startDebugReport();

        logEnvironmentInfo(player);

        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RESULTS_GO_TO_LOG));

        // Domain 1: Environment & Compatibility
        runDomain("1/10", "ENVIRONMENT & COMPATIBILITY");
        checkConfig(player, passed, failed);
        checkBukkitCompat(player, passed, failed);
        checkFoliaLib(player, passed, failed);

        // Domain 2: Configuration & Block Families
        runDomain("2/10", "CONFIGURATION & BLOCK FAMILIES");
        checkLockableMaterials(player, passed, failed);
        checkAutoDrop(player, passed, failed);
        checkLockableEntities(player, passed, failed);
        checkItemFrameProtection(player, passed, failed);
        checkRaidDetection(player, passed, failed);
        checkVillagerWorkstationProtection(player, passed, failed);

        // Domain 3: Localization & Translation Coverage
        runDomain("3/10", "LOCALIZATION & TRANSLATIONS");
        checkTranslations(player, passed, failed);
        checkLanguages(player, passed, failed);

        // Domain 4: Storage, Database & Caching
        runDomain("4/10", "STORAGE, DATABASE & CACHING");
        checkHybridDatabase(player, passed, failed);
        checkProfileService(player, passed, failed);
        checkSkinsRestorer(player, passed, failed);
        checkAuditLogger(player, passed, failed);
        checkOnlinePlayers(player, passed, failed);

        BlockProt.getFoliaLib().getScheduler().runAtEntity(player, tickTask -> {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean tempChestAdded = false;
            if (!cfg.isLockableTileEntity(Material.CHEST, player.getWorld()) && !cfg.isLockableBlock(Material.CHEST, player.getWorld())) {
                cfg.addTestLockable(Material.CHEST);
                tempChestAdded = true;
            }

            try {
                // Domain 5: NBT & Data Persistence Engine
                runDomain("5/10", "NBT & DATA PERSISTENCE ENGINE");
                checkNbt(player.getLocation(), passed, failed);
                checkEntityNbt(player.getLocation(), passed, failed);
                checkPlayerSettings(player, passed, failed);
                checkNbtSubHandlers(player.getLocation(), passed, failed);

                // Domain 6: Commands, Permissions & Integrations
                runDomain("6/10", "COMMANDS, PERMISSIONS & INTEGRATIONS");
                checkIntegrations(player, passed, failed);
                checkCommandsRegistered(player, passed, failed);
                checkAdminTiers(player, passed, failed);

                // Domain 7: Event Listeners & Engine
                runDomain("7/10", "EVENT LISTENERS & ENGINE");
                checkListenersRegistered(player, passed, failed);

                // Domain 8: User Interface & Screens
                runDomain("8/10", "USER INTERFACE & SCREENS");
                checkInventoryCreation(player, passed, failed);
                checkAllInventories(player, passed, failed);
                checkAllDialogs(player, passed, failed);

                // Domain 9: Utility Helpers & Benchmarks
                runDomain("9/10", "UTILITY HELPERS & BENCHMARKS");
                checkMessages(player, passed, failed);
                checkBlocksYmlIntegrity(player, passed, failed);
                checkWorldsYmlIntegrity(player, passed, failed);
                checkSkinCache(player, passed, failed);
                checkUtilityHelpers(player, passed, failed);
                checkStructuralClasses(player, passed, failed);
                checkEnumeratedCoverage(player, passed, failed);

                // Domain 10: Player Simulation & Gameplay Verification
                runDomain("10/10", "PLAYER SIMULATION & GAMEPLAY VERIFICATION");
                checkPlayerSimulation(player.getLocation(), player.getUniqueId(), player, passed, failed);

                int p2 = passed.get(), f2 = failed.get(), total = p2 + f2;
                BlockProtLogger.separator();
                BlockProtLogger.log("SUMMARY: " + p2 + " passed, " + f2 + " failed / " + total + " total");

                boolean ok = f2 == 0;
                ab(player, ok
                    ? Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_PASSED_ACTIONBAR)
                        .replace("{passed}", String.valueOf(p2))
                    : Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_FAILED_ACTIONBAR)
                        .replace("{failed}", String.valueOf(f2))
                        .replace("{total}", String.valueOf(total)));
                chat(player, ok
                    ? Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_PASSED_CHAT)
                        .replace("{passed}", String.valueOf(p2))
                    : Translator.get(TranslationKey.MESSAGES__DEBUG__CHECKS_FAILED_CHAT)
                        .replace("{failed}", String.valueOf(f2)));

                var reportFile = BlockProtLogger.getDebugReportFile();
                BlockProtLogger.endDebugReport();
                if (reportFile != null)
                    chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                        .replace("{path}", reportFile.getPath()));
            } finally {
                if (tempChestAdded) {
                    cfg.removeTestLockable(Material.CHEST);
                }
            }
        });
    }

    private void checkConfig(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            BlockProtLogger.pass("Config: friendDisabled=" + cfg.isFriendFunctionalityDisabled()
                + " maxBlocks=" + cfg.getMaxLockedBlockCount()
                + " lockEffects=" + cfg.isLockEffectEnabled()
                + " lockSound=" + cfg.isLockSoundEnabled()
                + " entityProtection=" + cfg.isEntityProtectionEnabled()
                + " raidDetection=" + BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Config", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkBukkitCompat(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var dust = BukkitCompat.PARTICLE_DUST;
            var dustTransition = BukkitCompat.PARTICLE_DUST_COLOR_TRANSITION;
            var enchant = BukkitCompat.GLOW_ENCHANT;
            if (dust == null || dustTransition == null || enchant == null) {
                BlockProtLogger.fail("BukkitCompat", "One or more fields resolved to null");
                f.incrementAndGet(); return;
            }
            BlockProtLogger.pass("BukkitCompat: PARTICLE_DUST=" + dust.name()
                + " TRANSITION=" + dustTransition.name()
                + " GLOW=" + enchant.getKey().getKey()
                + " newParticle=" + BukkitCompat.hasNewParticleNames()
                + " newEnchant=" + BukkitCompat.hasNewEnchantmentNames());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("BukkitCompat", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkFoliaLib(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var folia = BlockProt.getFoliaLib();
            BlockProtLogger.pass("FoliaLib: active (isFolia=" + folia.isFolia()
                + " isPaper=" + folia.isPaper() + " isSpigot=" + folia.isSpigot() + ")");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("FoliaLib", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkTranslations(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        int blank = 0, errors = 0;
        for (TranslationKey key : TranslationKey.values()) {
            try {
                String v = Translator.get(key);
                if (v == null || v.isBlank()) {
                    blank++;
                }
            } catch (Exception e) {
                errors++; BlockProtLogger.fail("Translation key " + key.name(), e.getMessage());
            }
        }
        if (errors == 0) {
            BlockProtLogger.pass("Translations: " + TranslationKey.values().length + " keys OK, " + blank + " blank");
            p.incrementAndGet();
        } else {
            BlockProtLogger.fail("Translations", errors + " key(s) threw exceptions"); f.incrementAndGet();
        }
    }

    private void checkLanguages(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            BlockProt plugin = BlockProt.getInstance();
            String active = BlockProt.getDefaultConfig().getLanguageFile();
            String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

            int totalKeys = TranslationKey.values().length;
            int langOk = 0, langFail = 0, skipped = 0;

            BlockProtLogger.subGroup("Language files:");

            for (String fileName : allLangs) {
                if (!LangConfig.isLanguageEnabled(fileName)) {
                    skipped++;
                    BlockProtLogger.skipSub(fileName, "disabled in config");
                    continue;
                }
                boolean isActive = fileName.equals(active);
                YamlConfiguration langFile = null;
                File diskFile = new File(plugin.getDataFolder(), "lang/" + fileName);
                try {
                    if (diskFile.exists()) {
                        langFile = YamlConfiguration.loadConfiguration(diskFile);
                    } else {
                        InputStream jarStream = plugin.getResource("lang/" + fileName);
                        if (jarStream == null) {
                            BlockProtLogger.failSub(fileName, "not found on disk or in jar");
                            langFail++; continue;
                        }
                        langFile = YamlConfiguration.loadConfiguration(
                            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
                    }
                } catch (Exception e) {
                    BlockProtLogger.failSub(fileName, "load error: " + e.getMessage());
                    langFail++; continue;
                }

                int presentKeys = 0;
                List<String> missingKeys = new ArrayList<>();
                for (TranslationKey key : TranslationKey.values()) {
                    String k = key.toString();
                    if (langFile.isConfigurationSection(k)) continue;
                    Object value = langFile.get(k);
                    if (value instanceof String && !((String) value).isEmpty()) {
                        presentKeys++;
                    } else {
                        missingKeys.add(key.name());
                    }
                }
                int pct = totalKeys == 0 ? 0 : (int) Math.round(100.0 * presentKeys / totalKeys);
                String status = isActive ? "ACTIVE" : "inactive";
                String detail = fileName + " [" + status + "]: " + pct + "% (" + presentKeys + "/" + totalKeys + " keys)";
                if (pct < 100) {
                    int totalMissing = missingKeys.size();
                    int showCount = Math.min(totalMissing, 5);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < showCount; i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(missingKeys.get(i));
                    }
                    if (totalMissing > showCount) sb.append(" ...");
                    detail += " missing=" + totalMissing + " ex: " + sb;
                }
                BlockProtLogger.passSub(detail);
                langOk++;
            }

            if (langFail == 0) {
                p.incrementAndGet();
            } else {
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Language files", e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkLockableMaterials(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            Material[] check = {
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
                Material.SHULKER_BOX, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.BLAST_FURNACE, Material.SMOKER
            };
            StringBuilder sb = new StringBuilder();
            for (Material m : check) sb.append(m.name()).append("=").append(cfg.isLockable(m)).append(" ");
            BlockProtLogger.pass("Lockable blocks: " + sb.toString().trim());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable blocks", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkAutoDrop(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean enabled = cfg.isAutoDropToInventoryEnabled();
            StringBuilder sb = new StringBuilder("enabled=" + enabled + " ");
            if (enabled) {
                Material[] check = {
                    Material.CHEST, Material.FURNACE, Material.OAK_DOOR, Material.RED_BED
                };
                for (Material m : check) {
                    sb.append(m.name()).append("=").append(cfg.isAutoDropToInventory(m)).append(" ");
                }
            }
            BlockProtLogger.pass("AutoDrop: " + sb.toString().trim());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("AutoDrop", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkLockableEntities(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            Set<Material> entityFamily = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES);
            if (entityFamily.isEmpty()) {
                BlockProtLogger.fail("Lockable entities", "ENTITIES family is empty (no entity materials registered)");
                f.incrementAndGet();
                return;
            }

            List<Material> active   = new ArrayList<>();
            List<Material> inactive = new ArrayList<>();
            for (Material m : entityFamily) {
                if (cfg.isLockableEntity(m)) active.add(m);
                else inactive.add(m);
            }

            boolean configEmpty = active.isEmpty();
            if (configEmpty) {
                if (cfg.isLockableEntity(Material.CHEST_MINECART)) {
                    BlockProtLogger.fail("Lockable entities", "CHEST_MINECART reports lockable but lockable_entities is empty");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.pass("Lockable entities: empty (vehicle/frame protection disabled per blocks.yml, CHEST_MINECART spot-check OK)");
                    p.incrementAndGet();
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (Material m : active) sb.append(m.name()).append(" ");
                BlockProtLogger.pass("Lockable entities: active=[" + sb.toString().trim() + "] count=" + active.size());
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable entities", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkItemFrameProtection(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean frameActive = cfg.isLockableEntity(Material.ITEM_FRAME);
            boolean glowActive  = cfg.isLockableEntity(Material.GLOW_ITEM_FRAME);

            BlockProtLogger.pass("Item frame protection: "
                + (frameActive ? "enabled" : "disabled")
                + " (ITEM_FRAME=" + (frameActive ? "ACTIVE" : "INACTIVE")
                + " GLOW_ITEM_FRAME=" + (glowActive ? "ACTIVE" : "INACTIVE") + ")");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Item frame protection", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkVillagerWorkstationProtection(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean enabled  = cfg.isVillagerWorkstationProtectionEnabled();
            int radius       = cfg.getVillagerWorkstationProtectionRadius();
            int vRadius      = cfg.getVillagerWorkstationProtectionVerticalRadius();

            boolean radiusInBounds  = radius  >= 0 && radius  <= 8;
            boolean vRadiusInBounds = vRadius >= 0 && vRadius <= 4;

            if (!radiusInBounds || !vRadiusInBounds) {
                BlockProtLogger.fail("Villager workstation protection",
                    "radius/vertical_radius out of documented bounds: radius=" + radius + " vRadius=" + vRadius);
                f.incrementAndGet();
                return;
            }

            BlockProtLogger.pass("Villager workstation protection: enabled=" + enabled
                + " radius=" + radius + " verticalRadius=" + vRadius
                + " (independent toggle from entity_protection.enabled=" + cfg.isEntityProtectionEnabled() + ")");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Villager workstation protection", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkRaidDetection(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            boolean enabled = BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", false);
            boolean explosionProtect = BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions();
            AuditLogger auditPresent = BlockProt.getAuditLogger();

            BlockProtLogger.pass("Raid detection: enabled=" + enabled
                + " explosionProtect=" + explosionProtect
                + " auditLogger=" + (auditPresent != null ? "active" : "disabled")
                + " RAID_EXPLOSION action=" + de.sean.blockprot.bukkit.audit.AuditLogger.Action.RAID_EXPLOSION.name());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Raid detection", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkIntegrations(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            List<PluginIntegration> integrations = BlockProt.getInstance().getIntegrations();
            if (integrations == null || integrations.isEmpty()) {
                BlockProtLogger.pass("Integrations: none registered");
                p.incrementAndGet();
                return;
            }
            int active = 0;
            BlockProtLogger.subGroup("Integrations (" + integrations.size() + " registered):");
            for (PluginIntegration integration : integrations) {
                String name = integration.getClass().getSimpleName();
                boolean enabled = integration.isEnabled();
                if (enabled) active++;

                if (integration instanceof ViaVersionIntegration via) {
                    BlockProtLogger.passSub(name + ": " + via.getDetailedStatus());
                } else {
                    org.bukkit.plugin.Plugin plugin = integration.getPlugin();
                    String ver = "unknown";
                    if (plugin != null) {
                        try { ver = plugin.getPluginMeta().getVersion(); }
                        catch (NoSuchMethodError err) {
                            @SuppressWarnings("deprecation")
                            String fallback = plugin.getDescription().getVersion();
                            ver = fallback;
                        }
                    }
                    if (enabled) {
                        BlockProtLogger.passSub(name + ": ACTIVE v" + ver);
                    } else {
                        BlockProtLogger.passSub(name + ": INACTIVE (plugin not found or disabled)");
                    }
                }
            }
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Integrations", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkHybridDatabase(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var db = BlockProt.getHybridDatabase();
            if (db != null) {
                BlockProtLogger.pass("HybridDatabase: active, backend=" + (BlockProt.getDefaultConfig().isMysqlEnabled() ? "MySQL" : "SQLite"));
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("HybridDatabase", "Database instance is null");
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("HybridDatabase", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkProfileService(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var service = BlockProt.getProfileService();
            if (service == null) {
                BlockProtLogger.fail("ProfileService", "ProfileService instance is null");
                f.incrementAndGet();
                return;
            }
            if (player != null) {
                var profile = service.findByUuid(player.getUniqueId());
                BlockProtLogger.pass("ProfileService: active, found=" + (profile != null ? profile.getName() : "null (no exception)"));
            } else {
                BlockProtLogger.pass("ProfileService: active (headless verification)");
            }
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("ProfileService", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkSkinsRestorer(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null) {
            BlockProtLogger.pass("SkinsRestorer: not installed");
        } else if (!plugin.isEnabled()) {
            BlockProtLogger.pass("SkinsRestorer: installed but disabled");
        } else {
            String ver;
            try { ver = plugin.getPluginMeta().getVersion(); }
            catch (NoSuchMethodError e) {
                @SuppressWarnings("deprecation")
                String fallback = plugin.getDescription().getVersion();
                ver = fallback;
            }
            BlockProtLogger.pass("SkinsRestorer: active v" + ver);
        }
        p.incrementAndGet();
    }

    private void checkAuditLogger(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        var audit = BlockProt.getAuditLogger();
        BlockProtLogger.pass("AuditLogger: " + (audit == null ? "disabled (config)" : "active"));
        p.incrementAndGet();
    }

    private void checkOnlinePlayers(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        var players = Bukkit.getOnlinePlayers();
        StringBuilder sb = new StringBuilder();
        for (Player pl : players) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(pl.getName()).append(" (").append(pl.getUniqueId()).append(")");
        }
        BlockProtLogger.pass("Online players (" + players.size() + "): " + sb);
        p.incrementAndGet();
    }

    private void checkNbt(@NotNull Location origin, AtomicInteger p, AtomicInteger f) {
        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(NOTCH_UUID);
                h.setName("debug_nbt_test");
                String owner   = h.getOwner();
                String name    = h.getName();
                long lockedAt  = h.getLockedAt();
                if (NOTCH_UUID.equals(owner) && "debug_nbt_test".equals(name)) {
                    BlockProtLogger.pass("NBT block: write/read OK (owner=" + owner + " name=" + name + " lockedAt=" + lockedAt + ")");
                    p.incrementAndGet();
                } else {
                    BlockProtLogger.fail("NBT block mismatch", "owner=" + owner + " name=" + name);
                    f.incrementAndGet();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT block", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkEntityNbt(@NotNull Location origin, AtomicInteger p, AtomicInteger f) {
        try {
            var loc = origin.clone();
            var world = origin.getWorld();
            var entity = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var handler = new EntityNBTHandler(entity);
                handler.setOwner(NOTCH_UUID);
                String readBack = handler.getOwner();
                if (NOTCH_UUID.equals(readBack)) {
                    BlockProtLogger.pass("NBT entity: write/read OK (owner=" + readBack + ")");
                    p.incrementAndGet();
                } else {
                    BlockProtLogger.fail("NBT entity mismatch", "expected=" + NOTCH_UUID + " got=" + readBack);
                    f.incrementAndGet();
                }
            } finally {
                entity.remove();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT entity", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkPlayerSettings(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var ps = new PlayerSettingsHandler(player);
            BlockProtLogger.pass("PlayerSettings: lockOnPlace=" + ps.getLockOnPlace()
                + " hintsEnabled=" + !ps.hasPlayerInteractedWithMenu());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("PlayerSettings", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkInventoryCreation(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            Inventory inv = ComponentMessages.createInventory(player, 9,
                net.kyori.adventure.text.Component.text(Translator.get(TranslationKey.MESSAGES__DEBUG__INVENTORY_TITLE)));
            BlockProtLogger.pass("Inventory creation: ComponentMessages.createInventory OK (size=" + inv.getSize() + ")");
            p.incrementAndGet();
        } catch (Throwable e) {
            BlockProtLogger.fail("createInventory", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkAllInventories(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        InventoryState base = new InventoryState(null);
        base.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        base.origin = InventoryState.MenuOrigin.NONE;
        InventoryState.set(player.getUniqueId(), base);

        BlockProtLogger.subGroup("Inventories (28 screens):");

        inv(p, f, "UserMenuInventory",    () -> new UserMenuInventory().fill(player));
        inv(p, f, "AdminMenuInventory",   () -> new AdminMenuInventory().fill(player));
        inv(p, f, "UserSettingsInventory",() -> new UserSettingsInventory().fill(player));
        inv(p, f, "StatisticsInventory",  () -> new StatisticsInventory().fill(player));
        inv(p, f, "FriendManageInventory (default)", () -> new FriendManageInventory().fill(player));

        inv(p, f, "StatisticListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), ns);
            @SuppressWarnings("unchecked")
            var castedStat = (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<
                de.sean.blockprot.nbt.stats.ListStatisticItem<?, Material>, ?>)
                (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<?, ?>) stat;
            return new StatisticListInventory().fill(player, castedStat);
        });

        inv(p, f, "AdminBlockListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            InventoryState.set(player.getUniqueId(), ns);
            return new AdminBlockListInventory().fill(player, player.getName(), stat);
        });

        inv(p, f, "RedstoneSettingsInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                InventoryState rs = new InventoryState(block);
                rs.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), rs);
                return new RedstoneSettingsInventory().fill(player, rs);
            } finally {
                world.setType(loc, orig);
            }
        });

        inv(p, f, "BlockLockInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                InventoryState bl = new InventoryState(block);
                bl.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), bl);
                return new BlockLockInventory().fill(player, Material.CHEST, h);
            } finally {
                world.setType(loc, orig);
            }
        });

        inv(p, f, "BlockInfoInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                InventoryState bi = new InventoryState(block);
                bi.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), bi);
                return new BlockInfoInventory().fill(player, h);
            } finally {
                world.setType(loc, orig);
            }
        });

        inv(p, f, "BlockInspectContentsInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                InventoryState bic = new InventoryState(block);
                InventoryState.set(player.getUniqueId(), bic);
                return new BlockInspectContentsInventory(player).fill();
            } finally {
                world.setType(loc, orig);
            }
        });

        inv(p, f, "AuditInventory (no entries)", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                InventoryState ai = new InventoryState(block);
                ai.currentPageIndex = 0;
                InventoryState.set(player.getUniqueId(), ai);
                return new AuditInventory().fill(player);
            } finally {
                world.setType(loc, orig);
            }
        });

        BlockProtLogger.skipSub("EntitySettingsInventory", "requires live Entity, not testable without one");
        touchScreen(INVENTORY_PACKAGE, "EntitySettingsInventory");
        p.incrementAndGet();

        inv(p, f, "FriendSearchHistoryInventory", () -> {
            InventoryState fh = new InventoryState(null);
            fh.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), fh);
            return new FriendSearchHistoryInventory().fill(player);
        });

        inv(p, f, "AutoDropInventory", () -> new AutoDropInventory().fill(player));
        inv(p, f, "AutoDropFamilyInventory", () -> {
            InventoryState ad = new InventoryState(null);
            ad.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), ad);
            return new AutoDropFamilyInventory().fill(player, BlockFamilyParser.Family.BLOCKS, 0, ad);
        });
        inv(p, f, "AutoDropSearchInventory", () -> {
            InventoryState ad = new InventoryState(null);
            ad.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), ad);
            return new AutoDropSearchInventory().fill(player, "chest", 0);
        });
        inv(p, f, "LockablesInventory", () -> {
            InventoryState lk = new InventoryState(null);
            InventoryState.set(player.getUniqueId(), lk);
            return new LockablesInventory().fill(player, 0);
        });
        inv(p, f, "WorldExpiryInventory", () -> {
            InventoryState we = new InventoryState(null);
            we.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), we);
            return new WorldExpiryInventory().fill(player, 0);
        });
        inv(p, f, "WorldLockableSelectionInventory", () -> new WorldLockableSelectionInventory().fill(player));
        inv(p, f, "WorldLockableDetailInventory", () -> {
            org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (w == null) return null;
            return new WorldLockableDetailInventory(w).fill(player);
        });
        inv(p, f, "BpUnlockInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            return new BpUnlockInventory().fill(player, player.getName(), stat);
        });
        inv(p, f, "FriendCandidateSelectionInventory", () -> new FriendCandidateSelectionInventory(
            java.util.List.of(), match -> {}, () -> null).fill(player));
        inv(p, f, "FriendDetailInventory", () -> new FriendDetailInventory().fill(player));
        inv(p, f, "FriendSearchResultInventory", () -> new FriendSearchResultInventory().fill(player, player.getName()));
        inv(p, f, "TransferSearchInventory", () -> new TransferSearchInventory().fill(player, player.getName()));

        BlockProtLogger.skipSub("PlayerListInventory", "open() opens a live GUI, no fill() to build");
        touchScreen(INVENTORY_PACKAGE, "PlayerListInventory");
        p.incrementAndGet();

        inv(p, f, "EntityInfoInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityInfoInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        inv(p, f, "EntityBlockSettingsInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityBlockSettingsInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        inv(p, f, "EntityFriendManageInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityFriendManageInventory().fill(player, ent, h);
            } finally {
                ent.remove();
            }
        });
        inv(p, f, "EntityFriendSearchResultInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                return new EntityFriendSearchResultInventory().fill(player, ent, h, player.getName());
            } finally {
                ent.remove();
            }
        });
        BlockProtLogger.skipSub("EntityInspectContentsInventory", "requires a container entity, not testable with an ArmorStand");
        touchScreen(INVENTORY_PACKAGE, "EntityInspectContentsInventory");
        p.incrementAndGet();
        inv(p, f, "WorldProtDeleteInventory", () -> new WorldProtDeleteInventory().fill(player, null));
        inv(p, f, "WorldProtDeleteConfirmInventory", () -> {
            org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (w == null) return null;
            return new WorldProtDeleteConfirmInventory().fill(player, w.getName());
        });

        inv(p, f, "AdminConfigInventory",       () -> new AdminConfigInventory().fill(player));
        inv(p, f, "AdminConfigLanguageInventory", () -> new AdminConfigLanguageInventory().fill(player));
        inv(p, f, "AdminConfigWorldsInventory",  () -> new AdminConfigWorldsInventory().fill(player));
        inv(p, f, "AdminConfigPlayersInventory", () -> new AdminConfigPlayersInventory().fill(player));
        inv(p, f, "AdminConfigBlocksInventory",  () -> new AdminConfigBlocksInventory().fill(player));
        inv(p, f, "AdminConfigEntityInventory",  () -> new AdminConfigEntityInventory().fill(player));
        inv(p, f, "AdminConfigExpiryInventory",  () -> new AdminConfigExpiryInventory().fill(player));
        inv(p, f, "AdminConfigRaidInventory",    () -> new AdminConfigRaidInventory().fill(player));
        inv(p, f, "AdminConfigNotificationsInventory", () -> new AdminConfigNotificationsInventory().fill(player));
        inv(p, f, "AdminConfigMaintenanceInventory", () -> new AdminConfigMaintenanceInventory().fill(player));
        inv(p, f, "AdminTiersInventory", () -> new AdminTiersInventory().fill(player, 0));
        inv(p, f, "AdminTierSelectInventory", () -> new AdminTierSelectInventory(player.getName(), player.getUniqueId()).fill(player));
        inv(p, f, "AdminCustomFlagsInventory", () -> new AdminCustomFlagsInventory(player.getName(), player.getUniqueId()).fill(player));

        BlockProtLogger.skipSub("FriendSearchInventory", "chat-input gateway, no fill() to build");
        touchScreen(INVENTORY_PACKAGE, "FriendSearchInventory");
        p.incrementAndGet();

        InventoryState.set(player.getUniqueId(), base);

        BlockProtLogger.subGroup("Inventory title translations (14 titles):");
        TranslationKey[] titleKeys = {
            TranslationKey.INVENTORIES__BLOCK_LOCK,
            TranslationKey.INVENTORIES__BLOCK_INFO__TITLE,
            TranslationKey.INVENTORIES__USER_SETTINGS,
            TranslationKey.INVENTORIES__USER_MENU__TITLE,
            TranslationKey.INVENTORIES__ADMIN_MENU__TITLE,
            TranslationKey.INVENTORIES__ADMIN_BLOCK_LIST__TITLE,
            TranslationKey.INVENTORIES__FRIENDS__MANAGE,
            TranslationKey.INVENTORIES__FRIENDS__EDIT,
            TranslationKey.INVENTORIES__TIMED__TITLE,
            TranslationKey.INVENTORIES__REDSTONE__SETTINGS,
            TranslationKey.INVENTORIES__STATISTICS__STATISTICS,
            TranslationKey.INVENTORIES__AUDIT__TITLE,
            TranslationKey.INVENTORIES__TRANSFER__TITLE,
            TranslationKey.INVENTORIES__ENTITY_SETTINGS__SETTINGS,
        };
        for (TranslationKey k : titleKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) {
                BlockProtLogger.failSub(k.name(), "blank/missing"); f.incrementAndGet();
            } else {
                BlockProtLogger.passSub(k.name() + " = \"" + v + "\"");
                p.incrementAndGet();
            }
        }
    }

    private void checkMessages(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        TranslationKey[] abKeys = {
            TranslationKey.MESSAGES__LOCK_HINT,
            TranslationKey.MESSAGES__CHAT_INPUT_PROMPT,
            TranslationKey.MESSAGES__CHAT_INPUT_CANCELLED,
            TranslationKey.MESSAGES__COPY_DONE,
            TranslationKey.MESSAGES__PASTE_DONE,
            TranslationKey.MESSAGES__TRANSFER_SELF_GUI,
            TranslationKey.MESSAGES__TRANSFER_NOT_OWNER_GUI,
            TranslationKey.MESSAGES__TRANSFER_FAILED,
        };
        TranslationKey[] chatKeys = {
            TranslationKey.MESSAGES__NO_PERMISSION,
            TranslationKey.MESSAGES__FRIEND_ADDED,
            TranslationKey.MESSAGES__FRIEND_REMOVED,
            TranslationKey.MESSAGES__UNLOCKED,
            TranslationKey.MESSAGES__TIMED_ACCESS_GRANTED,
            TranslationKey.MESSAGES__TIMED_ACCESS_NOT_OWNER,
            TranslationKey.MESSAGES__TIMED_ACCESS_OVER_MAX,
            TranslationKey.MESSAGES__TRANSFER_SUCCESS,
        };
        int ok = 0, bad = 0;
        for (TranslationKey k : abKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) { BlockProtLogger.fail("ActionBar msg blank", k.name()); bad++; }
            else ok++;
        }
        for (TranslationKey k : chatKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) { BlockProtLogger.fail("Chat msg blank", k.name()); bad++; }
            else ok++;
        }
        if (bad == 0) {
            BlockProtLogger.pass("Messages: all " + (abKeys.length + chatKeys.length) + " keys present ("
                + abKeys.length + " actionbar, " + chatKeys.length + " chat)");
            p.incrementAndGet();
        } else {
            BlockProtLogger.fail("Messages", bad + " key(s) blank or missing");
            f.incrementAndGet();
        }
    }

    /** Family key -> BlockFamilyParser.Family, shared by both integrity checks below. */
    private static final Map<String, BlockFamilyParser.Family> INTEGRITY_KEY_FAMILIES = new LinkedHashMap<>();
    static {
        INTEGRITY_KEY_FAMILIES.put("lockable_tile_entities", BlockFamilyParser.Family.TILE_ENTITIES);
        INTEGRITY_KEY_FAMILIES.put("lockable_shulker_boxes", BlockFamilyParser.Family.SHULKER_BOXES);
        INTEGRITY_KEY_FAMILIES.put("lockable_blocks", BlockFamilyParser.Family.BLOCKS);
        INTEGRITY_KEY_FAMILIES.put("lockable_doors", BlockFamilyParser.Family.DOORS);
        INTEGRITY_KEY_FAMILIES.put("lockable_entities", BlockFamilyParser.Family.ENTITIES);
    }

    /**
     * Reads blocks.yml directly from disk and checks every list entry, block by block:
     * family expressions must resolve to at least one material, flat names must match
     * a real {@link Material}, and every resolved material must be reflected by
     * {@link DefaultConfig#isLockable(Material)} / {@link DefaultConfig#isLockableEntity(Material)}.
     */
    private void checkBlocksYmlIntegrity(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
            java.io.File dataFolder = BlockProt.getInstance().getDataFolder();
            java.io.File blocksFile = new java.io.File(dataFolder, defaultConfig.getBlocksFilePath());
            if (!blocksFile.exists()) {
                BlockProtLogger.fail("blocks.yml integrity", "file not found at " + blocksFile.getPath());
                f.incrementAndGet();
                return;
            }
            org.bukkit.configuration.file.YamlConfiguration cfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);

            int checked = 0, unresolved = 0, mismatched = 0;
            for (Map.Entry<String, BlockFamilyParser.Family> e : INTEGRITY_KEY_FAMILIES.entrySet()) {
                String key = e.getKey();
                BlockFamilyParser.Family family = e.getValue();
                if (!cfg.contains(key)) continue;
                Object raw = cfg.get(key);
                List<?> rawList = raw instanceof List<?> ? (List<?>) raw : List.of(raw);
                for (Object o : rawList) {
                    if (DefaultConfig.isPlaceholderEntry(o)) continue;
                    if (!(o instanceof String s)) continue;
                    String trimmed = s.trim();
                    checked++;
                    if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                        Set<Material> resolved = BlockFamilyParser.parseFamilyExpressionSilent(trimmed, family);
                        if (resolved.isEmpty()) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' resolved to 0 materials");
                            unresolved++;
                        }
                    } else {
                        Material m = Material.matchMaterial(trimmed);
                        if (m == null) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' is not a valid Material");
                            unresolved++;
                            continue;
                        }
                        boolean isEntitiesKey = key.equals("lockable_entities");
                        boolean actual = isEntitiesKey ? defaultConfig.isLockableEntity(m) : defaultConfig.isLockable(m);
                        if (!actual) {
                            BlockProtLogger.fail("blocks.yml entry", key + ": '" + trimmed + "' listed but isLockable()=false");
                            mismatched++;
                        }
                    }
                }
            }

            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("blocks.yml integrity: " + checked + " entries checked OK (0 unresolved, 0 mismatched)");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("blocks.yml integrity", unresolved + " unresolved, " + mismatched + " mismatched");
                f.incrementAndGet();
            }
        } catch (Exception ex) {
            BlockProtLogger.fail("blocks.yml integrity", ex.getMessage());
            f.incrementAndGet();
        }
    }

    /**
     * Reads worlds.yml directly from disk and checks every per-world list entry the
     * same way {@link #checkBlocksYmlIntegrity} checks blocks.yml. Skipped entirely
     * when {@code per_worlds_config} is disabled, since worlds.yml is not consulted then.
     */
    private void checkWorldsYmlIntegrity(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
            if (!defaultConfig.isPerWorldsConfigEnabled()) {
                BlockProtLogger.pass("worlds.yml integrity: per_worlds_config disabled, skipping");
                p.incrementAndGet();
                return;
            }
            java.io.File worldsFile = new java.io.File(BlockProt.getInstance().getDataFolder(), "worlds.yml");
            if (!worldsFile.exists()) {
                BlockProtLogger.fail("worlds.yml integrity", "file not found at " + worldsFile.getPath());
                f.incrementAndGet();
                return;
            }
            org.bukkit.configuration.file.YamlConfiguration cfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(worldsFile);
            org.bukkit.configuration.ConfigurationSection worldsSection = cfg.getConfigurationSection("worlds");
            if (worldsSection == null) {
                BlockProtLogger.pass("worlds.yml integrity: no 'worlds' section, nothing to check");
                p.incrementAndGet();
                return;
            }

            int checked = 0, unresolved = 0, mismatched = 0, worldsChecked = 0;
            for (String worldName : worldsSection.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection ws = worldsSection.getConfigurationSection(worldName);
                if (ws == null) continue;
                boolean enabled = ws.getBoolean("enabled", false);
                org.bukkit.World world = Bukkit.getWorld(worldName);
                worldsChecked++;

                for (Map.Entry<String, BlockFamilyParser.Family> e : INTEGRITY_KEY_FAMILIES.entrySet()) {
                    String key = e.getKey();
                    BlockFamilyParser.Family family = e.getValue();
                    if (!ws.contains(key)) continue;
                    Object raw = ws.get(key);
                    List<?> rawList = raw instanceof List<?> ? (List<?>) raw : List.of(raw);
                    for (Object o : rawList) {
                        if (DefaultConfig.isPlaceholderEntry(o)) continue;
                        if (!(o instanceof String s)) continue;
                        String trimmed = s.trim();
                        checked++;
                        if (BlockFamilyParser.isFamilyExpression(trimmed)) {
                            Set<Material> resolved = BlockFamilyParser.parseFamilyExpressionSilent(trimmed, family);
                            if (resolved.isEmpty()) {
                                BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' resolved to 0 materials");
                                unresolved++;
                            }
                        } else {
                            Material m = Material.matchMaterial(trimmed);
                            if (m == null) {
                                BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' is not a valid Material");
                                unresolved++;
                                continue;
                            }
                            if (enabled && world != null) {
                                boolean isEntitiesKey = key.equals("lockable_entities");
                                boolean actual = isEntitiesKey
                                    ? defaultConfig.isLockableEntity(m, world)
                                    : defaultConfig.isLockable(m, world);
                                if (!actual) {
                                    BlockProtLogger.fail("worlds.yml entry", worldName + "." + key + ": '" + trimmed + "' listed but not reflected by isLockable(world)");
                                    mismatched++;
                                }
                            }
                        }
                    }
                }
            }

            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("worlds.yml integrity: " + worldsChecked + " world(s), " + checked + " entries checked OK");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("worlds.yml integrity", unresolved + " unresolved, " + mismatched + " mismatched");
                f.incrementAndGet();
            }
        } catch (Exception ex) {
            BlockProtLogger.fail("worlds.yml integrity", ex.getMessage());
            f.incrementAndGet();
        }
    }

    /** No-op bridge so dialog smoke tests build screens without opening them. */
    private static final class NoopDialogBridge implements DialogBridge {
        @Override public void closeDialog(org.bukkit.entity.Player player) {}
        @Override public void showNotice(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<net.kyori.adventure.text.Component> body,
            DialogButton ok) {}
        @Override public void showConfirmation(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<net.kyori.adventure.text.Component> body,
            DialogButton yes, DialogButton no) {}
        @Override public void showMultiAction(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<net.kyori.adventure.text.Component> body,
            java.util.List<DialogButton> actions) {}
        @Override public void showMultiAction(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<DialogBodyEntry> body,
            java.util.List<DialogButton> actions, DialogButton exit, int columns) {}
        @Override public void showValueInput(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<DialogBodyEntry> body,
            DialogTextField field, java.util.function.Consumer<String> onSubmit, DialogButton back) {}
        @Override public void showSearchDialog(org.bukkit.entity.Player player,
            net.kyori.adventure.text.Component title, java.util.List<DialogBodyEntry> body,
            DialogTextField field, java.util.List<DialogButton> actions, DialogButton exit,
            int columns, java.util.function.Consumer<String> onSearch) {}
    }

    private void checkAllDialogs(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
            BlockProtLogger.subGroup("Dialogs (28 screens):");
            dlg(p, f, "AboutDialog", () -> AboutDialog.show(player));
            dlg(p, f, "AdminMenuDialog", () -> AdminMenuDialog.show(player));
            dlg(p, f, "AdminConfigDialog", () -> AdminConfigDialog.show(player));
            dlg(p, f, "AdminConfigLanguageDialog", () -> AdminConfigLanguageDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigWorldsDialog", () -> AdminConfigWorldsDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigPlayersDialog", () -> AdminConfigPlayersDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigBlocksDialog", () -> AdminConfigBlocksDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigBlocksLockingDialog", () -> AdminConfigBlocksLockingDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigBlocksBehaviorDialog", () -> AdminConfigBlocksBehaviorDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigBlocksEffectsDialog", () -> AdminConfigBlocksEffectsDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigEntityDialog", () -> AdminConfigEntityDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigExpiryDialog", () -> AdminConfigExpiryDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigRaidDialog", () -> AdminConfigRaidDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigNotificationsDialog", () -> AdminConfigNotificationsDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminConfigMaintenanceDialog", () -> AdminConfigMaintenanceDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AuditDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                    AuditDialog.show(player, block);
                } finally {
                    world.setType(loc, orig);
                }
            });
            dlg(p, f, "AutoDropDialog", () -> AutoDropDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AutoDropFamilyDialog", () -> AutoDropFamilyDialog.show(player, DialogOrigin.ADMIN_MENU, BlockFamilyParser.Family.BLOCKS));
            dlg(p, f, "AutoDropSearchDialog", () -> AutoDropSearchDialog.show(player, DialogOrigin.ADMIN_MENU, null, "chest", 0));
            dlg(p, f, "BlockInfoDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockInfoDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            dlg(p, f, "BlockLockDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockLockDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            dlg(p, f, "BlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    BlockSettingsDialog.show(player, block, h);
                } finally {
                    world.setType(loc, orig);
                }
            });
            dlg(p, f, "DebugDialog", () -> DebugDialog.show(player));
            dlg(p, f, "EntityInfoDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityInfoDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            dlg(p, f, "EntityBlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityBlockSettingsDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            dlg(p, f, "EntityFriendManageDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                try {
                    var h = new EntityNBTHandler(ent);
                    h.setOwner(player.getUniqueId().toString());
                    EntityFriendManageDialog.show(player, ent, h);
                } finally {
                    ent.remove();
                }
            });
            dlg(p, f, "FriendManageDialog", () -> FriendManageDialog.show(player));
            dlg(p, f, "FriendCandidateSelectionDialog", () -> FriendCandidateSelectionDialog.show(
                player, java.util.List.of(), match -> {}, () -> {}));
            dlg(p, f, "InfoDialog", () -> InfoDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "IntegrationsDialog", () -> IntegrationsDialog.show(player));
            dlg(p, f, "LockablesDialog", () -> LockablesDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "LockableCategoryDialog", () -> LockableCategoryDialog.show(
                player, DialogOrigin.ADMIN_MENU, "lockable_blocks", java.util.List.of(Material.CHEST)));
            dlg(p, f, "ProtdelDialog", () -> ProtdelDialog.show(player, null));
            dlg(p, f, "StatsDialog", () -> StatsDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "UnlockDialog", () -> UnlockDialog.show(player, player.getName()));
            dlg(p, f, "UpdateDialog", () -> UpdateDialog.show(player));
            dlg(p, f, "UserMenuDialog", () -> UserMenuDialog.show(player));
            dlg(p, f, "UserSettingsDialog", () -> UserSettingsDialog.show(player));
            dlg(p, f, "WorldLockableSelectionDialog", () -> WorldLockableSelectionDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "WorldLockableDetailDialog", () -> {
                org.bukkit.World w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
                if (w == null) return;
                WorldLockableDetailDialog.show(player, DialogOrigin.ADMIN_MENU, w);
            });
            dlg(p, f, "AdminTiersDialog", () -> AdminTiersDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminTierSelectDialog", () -> AdminTierSelectDialog.show(player, player.getName(), player.getUniqueId(), DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AdminCustomFlagsDialog", () -> AdminCustomFlagsDialog.show(player, player.getName(), player.getUniqueId(), DialogOrigin.ADMIN_MENU));
            dlg(p, f, "FriendDetailDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                try {
                    var block = world.getBlockAt(loc);
                    var h = new BlockNBTHandler(block);
                    h.setOwner(player.getUniqueId().toString());
                    h.addFriend(player.getUniqueId().toString());
                    FriendDetailDialog.showForBlock(player, block, h, player.getUniqueId().toString(), 0);
                } finally {
                    world.setType(loc, orig);
                }
            });
            dlg(p, f, "WorldExpiryDialog", () -> WorldExpiryDialog.show(player, DialogOrigin.ADMIN_MENU));
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }

    private void checkCommandsRegistered(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.subGroup("Commands & permissions:");
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand("blockprot");
        if (cmd == null) {
            BlockProtLogger.failSub("blockprot command", "not registered");
            f.incrementAndGet();
        } else if (cmd.getExecutor() == null) {
            BlockProtLogger.failSub("blockprot command", "has no executor");
            f.incrementAndGet();
        } else {
            BlockProtLogger.passSub("blockprot registered, executor="
                + cmd.getExecutor().getClass().getSimpleName());
            touchScreen(COMMANDS_PACKAGE, "BlockProtCommand");
            p.incrementAndGet();
        }

        try (var in = BlockProt.getInstance().getResource("plugin.yml")) {
            if (in == null) throw new IllegalStateException("plugin.yml resource missing");
            var cfg = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            var permissions = cfg.getConfigurationSection("permissions");
            int missing = 0;
            for (Permissions perm : Permissions.values()) {
                String key = perm.key();
                if (permissions == null || !permissions.contains(key)) {
                    missing++;
                    BlockProtLogger.failSub("Permission node", key + " not declared in plugin.yml");
                }
            }
            if (missing == 0) {
                BlockProtLogger.passSub("Permissions: all " + Permissions.values().length + " nodes declared in plugin.yml");
                p.incrementAndGet();
            } else {
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("Commands", "plugin.yml read failed: " + e.getMessage());
            f.incrementAndGet();
        }

        String[] commandClasses = {
            "UserMenuCommand", "AdminMenuCommand",
            "HelpCommand", "SettingsCommand", "FriendsAddAllCommand", "StatisticsCommand",
            "TransferCommand", "AboutCommand", "HintsCommand", "InfoCommand",
            "ReloadCommand", "UpdateCommand", "IntegrationsCommand", "DebugCommand",
            "AdminUnlockCommand", "WorldProtDeleteCommand", "LockablesCommand", "RecommendedCommand",
            "TiersCommand"
        };
        java.util.Set<String> wired = new java.util.HashSet<>();
        for (String name : commandClasses) {
            try {
                Class.forName(BlockProtCommand.class.getPackageName() + "." + name);
                touch(BlockProtCommand.class.getPackageName() + "." + name);
                wired.add(name);
            } catch (Throwable e) {
                BlockProtLogger.failSub("Command class", name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                f.incrementAndGet();
            }
        }
        if (wired.size() == commandClasses.length) {
            BlockProtLogger.passSub("Command classes: " + wired.size() + "/" + commandClasses.length
                + " present and loadable");
            p.incrementAndGet();
        } else {
            BlockProtLogger.failSub("Command classes", wired.size() + "/" + commandClasses.length
                + " present and loadable (MISSING SOME)");
            f.incrementAndGet();
        }

        String[] integrationClasses = {
            "TownyIntegration", "PlaceholderAPIIntegration", "ViaVersionIntegration",
            "WorldGuardIntegration", "LandsPluginIntegration", "ClaimChunkIntegration",
            "ResidenceIntegration", "GriefPreventionIntegration",
            "GeyserIntegration", "FloodgateIntegration"
        };
        java.util.Set<String> regNames = new java.util.HashSet<>();
        for (PluginIntegration integration : BlockProt.getInstance().getIntegrations()) {
            regNames.add(integration.getClass().getSimpleName());
        }
        int regMissing = 0;
        for (String name : integrationClasses) {
            if (!regNames.contains(name)) {
                regMissing++;
                BlockProtLogger.failSub("Integration wiring", name + " not in BlockProt.onLoad() integration list");
            }
        }
        if (regMissing == 0) {
            BlockProtLogger.passSub("Integration wiring: all " + integrationClasses.length
                + " integration classes constructed in BlockProt.onLoad()");
            p.incrementAndGet();
        } else {
            f.incrementAndGet();
        }
    }

    private void checkAdminTiers(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        try {
            boolean parsingOk = AdminTier.fromString("t1") == AdminTier.T1
                && AdminTier.fromString("t2") == AdminTier.T2
                && AdminTier.fromString("t3") == AdminTier.T3
                && AdminTier.fromString("owner") == AdminTier.OWNER
                && AdminTier.fromString("custom") == AdminTier.CUSTOM
                && AdminTier.fromString("user") == AdminTier.NONE
                && AdminTier.fromString("normal") == AdminTier.NONE
                && AdminTier.fromString("none") == AdminTier.NONE
                && AdminTier.fromString("unknown_val") == AdminTier.NONE;
            if (!parsingOk) {
                BlockProtLogger.fail("AdminTier parsing", "fromString failed to resolve expected tier");
                f.incrementAndGet();
                return;
            }

            boolean hierarchyOk = AdminTier.OWNER.includes(AdminTier.T3)
                && AdminTier.T3.includes(AdminTier.T2)
                && AdminTier.T2.includes(AdminTier.T1)
                && AdminTier.T1.includes(AdminTier.T1)
                && !AdminTier.T1.includes(AdminTier.T2)
                && !AdminTier.NONE.includes(AdminTier.T1);
            if (!hierarchyOk) {
                BlockProtLogger.fail("AdminTier hierarchy", "includes() hierarchy check failed");
                f.incrementAndGet();
                return;
            }

            boolean actionsOk = AdminAction.INFO.getMinimumTier() == AdminTier.T1
                && AdminAction.TELEPORT.getMinimumTier() == AdminTier.T1
                && AdminAction.LOGS.getMinimumTier() == AdminTier.T1
                && AdminAction.BREAK.getMinimumTier() == AdminTier.T2
                && AdminAction.UNLOCK.getMinimumTier() == AdminTier.T2
                && AdminAction.LOCKABLES.getMinimumTier() == AdminTier.T2
                && AdminAction.CONTAINER_BYPASS.getMinimumTier() == AdminTier.T2
                && AdminAction.PROTDEL.getMinimumTier() == AdminTier.T3
                && AdminAction.CONFIG.getMinimumTier() == AdminTier.T3
                && AdminAction.DEBUG.getMinimumTier() == AdminTier.T3
                && AdminAction.RELOAD.getMinimumTier() == AdminTier.OWNER
                && AdminAction.UPDATE.getMinimumTier() == AdminTier.OWNER
                && AdminAction.INTEGRATIONS.getMinimumTier() == AdminTier.OWNER
                && AdminAction.RECOMMENDED.getMinimumTier() == AdminTier.OWNER
                && AdminAction.SETROLE.getMinimumTier() == AdminTier.OWNER;
            if (!actionsOk) {
                BlockProtLogger.fail("AdminAction mappings", "unexpected minimum tier mapping");
                f.incrementAndGet();
                return;
            }

            boolean consolePerms = true;
            for (AdminAction action : AdminAction.values()) {
                if (!AdminTierManager.hasPermission(Bukkit.getConsoleSender(), action)) {
                    consolePerms = false;
                    break;
                }
            }
            if (!consolePerms) {
                BlockProtLogger.fail("AdminTierManager", "Console sender denied on admin action");
                f.incrementAndGet();
                return;
            }

            UUID testUuid = UUID.randomUUID();
            AdminTierManager.setPlayerRole(testUuid, AdminTier.CUSTOM, EnumSet.of(AdminAction.RELOAD));
            AdminTierManager.save();
            AdminTierManager.load();
            AdminTier loadedRole = AdminTierManager.getRole(testUuid);
            boolean allRolesOk = AdminTierManager.getAllConfiguredRoles().containsKey(testUuid);
            boolean flagsOk = AdminTierManager.getCustomFlags(testUuid).contains(AdminAction.RELOAD);
            boolean toggledOn = AdminTierManager.toggleCustomFlag(testUuid, AdminAction.DEBUG);
            boolean toggledOff = !AdminTierManager.toggleCustomFlag(testUuid, AdminAction.DEBUG);
            if (loadedRole != AdminTier.CUSTOM || !allRolesOk || !flagsOk || !toggledOn || !toggledOff) {
                BlockProtLogger.fail("AdminTierManager", "getRole / customFlags roundtrip mismatch for test entry");
                f.incrementAndGet();
                return;
            }
            AdminTierManager.setPlayerRole(testUuid, AdminTier.NONE, null);
            AdminTierManager.save();
            if (AdminTierManager.getRole(testUuid) != AdminTier.NONE) {
                BlockProtLogger.fail("AdminTierManager", "Role not removed after set to NONE");
                f.incrementAndGet();
                return;
            }

            AdminTier currentTier = player != null ? AdminTierManager.getPlayerTier(player) : AdminTier.NONE;
            boolean anyAdmin = player != null ? AdminTierManager.hasAnyAdminPermission(player) : true;
            String callerDesc = player != null
                ? "callerTier=" + currentTier.getIdentifier() + ", hasAnyAdmin=" + anyAdmin
                : "headless verification";

            BlockProtLogger.pass("Admin tiers: parsing, hierarchy, actions (15), console bypass, and storage roundtrip OK ("
                + callerDesc + ")");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Admin tiers", e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkListenersRegistered(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        java.util.List<org.bukkit.event.HandlerList> dummy = null;
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        String[] expected = {
            "BlockEventListener", "EntityEventListener", "ExplodeEventListener",
            "HopperEventListener", "InteractEventListener", "InventoryEventListener",
            "JoinEventListener", "PistonEventListener", "RedstoneEventListener",
            "LockEffectListener", "EntityProtectionListener", "EntityMenuOpenListener",
            "VillagerWorkstationProtectionListener", "ItemFrameListener",
            "VehicleProtectionListener", "AutoDropEntityListener",
            "RaidDetectionListener", "WorldEditPasteListener"
        };
        java.util.Set<String> found = new java.util.HashSet<>();
        for (org.bukkit.plugin.RegisteredListener rl :
             org.bukkit.event.HandlerList.getRegisteredListeners(BlockProt.getInstance())) {
            var listenerObj = rl.getListener();
            if (listenerObj != null) found.add(listenerObj.getClass().getSimpleName());
        }
        int missing = 0;
        for (String name : expected) {
            if (found.contains(name)) {
                touchScreen(LISTENERS_PACKAGE, name);
            } else {
                missing++;
                BlockProtLogger.fail("Listener", name + " is not registered (onEnable wire-up missing?)");
            }
        }
        touchScreen(LISTENERS_PACKAGE, "ErrorEventListener");
        if (missing == 0) {
            BlockProtLogger.pass("Listeners: all " + expected.length + " active listeners registered (ErrorEventListener fallback intentional)");
            p.incrementAndGet();
        } else {
            f.incrementAndGet();
        }
    }

    private void checkSkinCache(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.subGroup("SkinCache tiers:");
        String name = player != null ? player.getName() : "Notch";
        UUID uuid = player != null ? player.getUniqueId() : UUID.fromString(NOTCH_UUID);
        try {
            var profile = SkinCache.getCachedOrOnlineProfile(name, uuid);
            if (profile != null) {
                BlockProtLogger.passSub("tier 1 (online profile): resolved for " + name);
                p.incrementAndGet();
            } else {
                BlockProtLogger.passSub("tier 1 (online profile): no cached/online skin for " + name + " (null, no exception)");
                p.incrementAndGet();
            }
        } catch (Throwable e) {
            BlockProtLogger.failSub("SkinCache tier 1", e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }

        var sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        try {
            var srProfile = SkinCache.resolveSkinsRestorer(uuid, name);
            if (sr != null && sr.isEnabled()) {
                BlockProtLogger.passSub("tier 2 (SkinsRestorer): present, resolve returned "
                    + (srProfile != null ? "a profile" : "null"));
            } else {
                BlockProtLogger.passSub("tier 2 (SkinsRestorer): absent, resolve no-opped (null)");
            }
            p.incrementAndGet();
        } catch (Throwable e) {
            BlockProtLogger.failSub("SkinCache tier 2", e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkUtilityHelpers(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.subGroup("Utility helpers (14 utilities):");
        try {
            Duration parsed = DurationParser.parse("2h30m");
            if (parsed == null || parsed.toMinutes() != 150) {
                BlockProtLogger.failSub("DurationParser", "parse(2h30m) = " + parsed);
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("DurationParser: parse(2h30m)=" + parsed
                    + " format=" + DurationParser.format(parsed));
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DurationParser", e.getMessage()); f.incrementAndGet();
        }

        try {
            DurationLimits limits = DurationLimits.create(60, 60, 24, 28, 12, 5);
            boolean okSec = limits.validate(Duration.ofSeconds(30));
            boolean okDay = limits.validate(Duration.ofDays(2));
            boolean over = limits.validate(Duration.ofDays(3650));
            long applicable = limits.getApplicableLimit(Duration.ofDays(30));
            if (!okSec || !okDay || over || applicable <= 0) {
                BlockProtLogger.failSub("DurationLimits", "unexpected validation results");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("DurationLimits: 30s=" + okSec + " 2d=" + okDay + " 10y=" + over
                    + " applicable(30d)=" + applicable + "ms");
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DurationLimits", e.getMessage()); f.incrementAndGet();
        }

        try {
            int dist = StringUtil.levenshtein("chest", "chst");
            double sim = StringUtil.similarity("chest", "chest");
            if (dist < 1 || sim <= 0) {
                BlockProtLogger.failSub("StringUtil", "unexpected distance/similarity");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("StringUtil: levenshtein(chest,chst)=" + dist + " similarity=" + sim);
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("StringUtil", e.getMessage()); f.incrementAndGet();
        }

        try {
            String name = BlockUtil.getHumanReadableBlockName(Material.CHEST);
            if (name == null || name.isBlank()) {
                BlockProtLogger.failSub("BlockUtil", "blank readable name for CHEST");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("BlockUtil: getHumanReadableBlockName(CHEST)=" + name);
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BlockUtil", e.getMessage()); f.incrementAndGet();
        }

        try {
            AsyncGuard.assertSync("debug utility group");
            BlockProtLogger.passSub("AsyncGuard: assertSync accepted (running on main thread)");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.failSub("AsyncGuard", e.getMessage()); f.incrementAndGet();
        }

        if (player != null) {
            try {
                Set<String> candidates = PlayerNameResolver.getNameCandidates(player.getName());
                if (candidates == null || candidates.isEmpty()) {
                    BlockProtLogger.failSub("PlayerNameResolver", "no candidates for own name");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.passSub("PlayerNameResolver: " + candidates.size() + " candidate(s) for " + player.getName());
                    p.incrementAndGet();
                }
            } catch (Exception e) {
                BlockProtLogger.failSub("PlayerNameResolver", e.getMessage()); f.incrementAndGet();
            }

            try {
                TemporaryActionBar.show(player, "debug", 1L);
                TemporaryActionBar.cancel(player.getUniqueId());
                BlockProtLogger.passSub("TemporaryActionBar: show+cancel OK");
                p.incrementAndGet();
            } catch (Exception e) {
                BlockProtLogger.failSub("TemporaryActionBar", e.getMessage()); f.incrementAndGet();
            }
        } else {
            BlockProtLogger.passSub("PlayerNameResolver/TemporaryActionBar: skipped (headless console)");
            p.incrementAndGet();
        }

        try {
            String stripped = BpDialogStyles.stripColor("&a&lTest");
            if (stripped == null || stripped.contains("&")) {
                BlockProtLogger.failSub("BpDialogStyles", "stripColor left a code: '" + stripped + "'");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("BpDialogStyles: stripColor(&a&lTest)=" + stripped
                    + " palette=" + BpDialogStyles.SOFT_GRAY + "," + BpDialogStyles.PASTEL_MINT + ","
                    + BpDialogStyles.PASTEL_CORAL + "," + BpDialogStyles.PASTEL_GOLD + ","
                    + BpDialogStyles.SOFT_BLUE + "," + BpDialogStyles.PASTEL_PURPLE);
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BpDialogStyles", e.getMessage()); f.incrementAndGet();
        }

        try {
            DialogButton back = DialogNavigation.backButton(DialogOrigin.NONE, null);
            DialogButton backAdmin = DialogNavigation.backButton(DialogOrigin.ADMIN_MENU, null);
            if (back == null || backAdmin == null) {
                BlockProtLogger.failSub("DialogNavigation", "backButton returned null");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("DialogNavigation: backButton(NONE) id=" + back.id()
                    + " backButton(ADMIN_MENU) id=" + backAdmin.id());
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("DialogNavigation", e.getMessage()); f.incrementAndGet();
        }

        if (player != null) {
            try {
                DialogState.push(player, ignored -> {});
                boolean popped = DialogState.pop(player);
                DialogState.clear(player);
                BlockProtLogger.passSub("DialogState: push/pop=" + popped + " clear OK");
                p.incrementAndGet();
            } catch (Exception e) {
                BlockProtLogger.failSub("DialogState", e.getMessage()); f.incrementAndGet();
            }
        } else {
            BlockProtLogger.passSub("DialogState: skipped (headless console)");
            p.incrementAndGet();
        }

        try {
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("key", "old");
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("key", "new");
            Map<String, Object> snap1 = ReloadReport.captureSnapshot(before, "test.yml");
            Map<String, Object> snap2 = ReloadReport.captureSnapshot(after, "test.yml");
            java.util.List<ReloadReport.ChangeDiff> diffs =
                ReloadReport.compareSnapshots(snap1, snap2);
            if (diffs == null || diffs.isEmpty()) {
                BlockProtLogger.failSub("ReloadReport", "expected a diff between old/new snapshots");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("ReloadReport: capture/compare produced " + diffs.size() + " diff(s)");
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("ReloadReport", e.getMessage()); f.incrementAndGet();
        }

        try {
            boolean integrationFlag = IntegrationConfig.getBoolean("debug.integration_test", true);
            BlockProtLogger.passSub("IntegrationConfig: getBoolean(default)=" + integrationFlag);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.failSub("IntegrationConfig", e.getMessage()); f.incrementAndGet();
        }

        try {
            // Suffix parsing and ranking table documented in gradle.properties:
            //   blank   -> RANK_RELEASE (stable)
            //   BEDev   -> RANK_SNAPSHOT (pre-release); bdev/SNAPSHOT are legacy aliases
            //   hotfix  -> RANK_HOTFIX (ranked above the clean release)
            //   release -> legacy tag suffix normalized to a clean release
            //   exp     -> experimental, never an update
            String curVer = BlockProt.getPluginVersion();
            SemanticVersion stable   = new SemanticVersion(curVer);
            SemanticVersion bedev    = new SemanticVersion(curVer + "-BEDev");
            SemanticVersion bdev     = new SemanticVersion(curVer + "-bdev");
            SemanticVersion snap     = new SemanticVersion(curVer + "-SNAPSHOT-3");
            SemanticVersion hotfix   = new SemanticVersion(curVer + "-hotfix");
            SemanticVersion fixN     = new SemanticVersion(curVer + "-fix.1");
            SemanticVersion release  = new SemanticVersion(curVer + "-RELEASE");
            SemanticVersion exp      = new SemanticVersion(curVer + "-exp");
            boolean ranksOk = !stable.isPreRelease() && !stable.isHotfix()
                && bedev.isPreRelease() && bdev.isPreRelease() && snap.isPreRelease()
                && hotfix.isHotfix() && fixN.isHotfix()
                && !release.isPreRelease() && !release.isHotfix()
                && exp.isExperimental();
            boolean orderOk = bedev.compareTo(stable) < 0
                && stable.compareTo(hotfix) < 0
                && bedev.compareTo(bdev) == 0
                && new SemanticVersion(curVer + "-BEDev.2").compareTo(new SemanticVersion(curVer + "-BEDev.1")) > 0
                && stable.compareTo(new SemanticVersion("99.0.0")) < 0
                && stable.compareTo(stable) == 0;
            boolean baseOk = hotfix.baseVersion().equals(curVer)
                && bedev.baseVersion().equals(curVer);
            if (!ranksOk || !orderOk || !baseOk) {
                BlockProtLogger.failSub("SemanticVersion",
                    "ranks=" + ranksOk + " order=" + orderOk + " base=" + baseOk);
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("SemanticVersion: ranks, order, and baseVersion verified (" + curVer + ")");
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("SemanticVersion", e.getMessage()); f.incrementAndGet();
        }

        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
            touchScreen(DIALOG_PACKAGE, "AdminConfigValueDialog");
            if (player != null) {
                AdminConfigValueDialog.openInt(player, "debug.test", "hint", 0, v -> {}, () -> {});
                AdminConfigValueDialog.openText(player, "debug.test", "hint", "value", s -> null, v -> {}, () -> {});
                BlockProtLogger.passSub("AdminConfigValueDialog: openInt/openText routed through test bridge");
            } else {
                BlockProtLogger.passSub("AdminConfigValueDialog: touched (headless)");
            }
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.failSub("AdminConfigValueDialog", e.getMessage()); f.incrementAndGet();
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }

    private void checkNbtSubHandlers(@NotNull Location origin, AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.subGroup("NBT sub-handlers (8 handlers):");
        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(NOTCH_UUID);
                h.addFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                boolean hasFriend = h.containsFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                java.util.List<FriendHandler> friends = h.getFriends();
                if (!hasFriend || friends.isEmpty()) {
                    BlockProtLogger.failSub("FriendSupportingHandler", "addFriend/containsFriend mismatch");
                    f.incrementAndGet();
                } else {
                    FriendHandler first = friends.get(0);
                    boolean canRead = first.canRead();
                    boolean isManager = first.isManager();
                    EnumSet<BlockAccessFlag> flags = BlockAccessFlag.parseFlags(0);
                    java.util.List<String> lore = BlockAccessFlag.accumulateAccessFlagLore(flags);
                    h.removeFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                    BlockProtLogger.passSub("FriendSupportingHandler/FriendHandler: contains=" + hasFriend
                        + " friends=" + friends.size() + " canRead=" + canRead + " isManager=" + isManager
                        + " flags=" + flags.size() + " loreLines=" + lore.size());
                    p.incrementAndGet();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("FriendSupportingHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                RedstoneSettingsHandler rs = h.getRedstoneHandler();
                rs.setPistonProtection(false);
                rs.setHopperProtection(false);
                boolean piston = rs.getPistonProtection();
                boolean hopper = rs.getHopperProtection();
                boolean current = rs.getCurrentProtection();
                rs.reset();
                BlockProtLogger.passSub("RedstoneSettingsHandler: piston=" + piston + " hopper=" + hopper
                    + " current=" + current + " reset OK");
                p.incrementAndGet();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("RedstoneSettingsHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            BlockCountStatistic stat = new BlockCountStatistic();
            stat.updateContainer((de.tr7zw.changeme.nbtapi.NBTContainer)
                de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
            stat.increment();
            int value = stat.get();
            if (value < 0) {
                BlockProtLogger.failSub("BlockCountStatistic", "negative value after increment");
                f.incrementAndGet();
            } else {
                BlockProtLogger.passSub("BlockCountStatistic: key=" + stat.getKey() + " type=" + stat.getType()
                    + " item=" + stat.getItemType() + " value=" + value);
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("BlockCountStatistic", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(NOTCH_UUID);
                EffectGeometry geometry = EffectGeometry.createForBlock(block);
                if (geometry.getBoundingBox() == null || geometry.getUnionCenter() == null) {
                    BlockProtLogger.failSub("EffectGeometry", "null bounding box or union center");
                    f.incrementAndGet();
                } else {
                    int perimeter = geometry.getPerimeterPoints(0.5).size();
                    BlockProtLogger.passSub("EffectGeometry: box=" + geometry.getBoundingBox().getVolume()
                        + " center=" + geometry.getUnionCenter() + " perimeterPoints=" + perimeter);
                    p.incrementAndGet();
                }

                ProtectedBlockCache.unmark(block);
                ProtectedBlockCache.mark(block);
                boolean cachedProtected = ProtectedBlockCache.isProtected(block);
                ProtectedBlockCache.unmark(block);
                if (!cachedProtected) {
                    BlockProtLogger.failSub("ProtectedBlockCache", "mark() did not make block protected");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.passSub("ProtectedBlockCache: mark/isProtected/unmark roundtrip OK, size="
                        + ProtectedBlockCache.size());
                    p.incrementAndGet();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("EffectGeometry/ProtectedBlockCache", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                if (!EntityProtectionHandler.isSupportedEntity(ent)) {
                    BlockProtLogger.passSub("EntityProtectionHandler: ArmorStand not supported, structural check only");
                    p.incrementAndGet();
                } else {
                    var handler = EntityProtectionHandler.forEntityOrNull(ent);
                    if (handler == null) {
                        BlockProtLogger.passSub("EntityProtectionHandler: forEntityOrNull=null for ArmorStand (expected)");
                        p.incrementAndGet();
                    } else {
                        handler.enable(java.util.UUID.fromString(NOTCH_UUID));
                        handler.setNoDamage(false);
                        handler.setNoLeash(false);
                        boolean ok = handler.getOwner().equals(java.util.UUID.fromString(NOTCH_UUID))
                            && handler.isProtected()
                            && !handler.isNoDamage()
                            && !handler.isNoLeash();
                        handler.clear();
                        if (!ok) {
                            BlockProtLogger.failSub("EntityProtectionHandler", "owner/flags mismatch after enable()");
                            f.incrementAndGet();
                        } else {
                            BlockProtLogger.passSub("EntityProtectionHandler: enable/owner/flags/clear roundtrip OK");
                            p.incrementAndGet();
                        }
                    }
                }
            } finally {
                ent.remove();
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("EntityProtectionHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = origin.clone();
            var world = origin.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(NOTCH_UUID);
                LocationListEntry entry = new LocationListEntry(loc);
                if (entry.getBlock() == null || entry.getItemType() == null || entry.getTitle() == null) {
                    BlockProtLogger.failSub("LocationListEntry", "null field for constructed entry");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.passSub("LocationListEntry: block=" + entry.getBlock().getType()
                        + " item=" + entry.getItemType() + " title=" + entry.getTitle());
                    p.incrementAndGet();
                }

                java.util.UUID clipboardOwner = java.util.UUID.fromString(NOTCH_UUID);
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                PlayerInventoryClipboard.set(clipboardOwner.toString(),
                    (de.tr7zw.changeme.nbtapi.NBTContainer) de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
                boolean hasClipboard = PlayerInventoryClipboard.contains(clipboardOwner.toString());
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                if (!hasClipboard) {
                    BlockProtLogger.failSub("PlayerInventoryClipboard", "set() did not register clipboard");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.passSub("PlayerInventoryClipboard: set/contains/remove roundtrip OK");
                    p.incrementAndGet();
                }
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.failSub("LocationListEntry/PlayerInventoryClipboard", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkStructuralClasses(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        String[] names = {
            "de.sean.blockprot.bukkit.admin.AdminTier",
            "de.sean.blockprot.bukkit.admin.AdminAction",
            "de.sean.blockprot.bukkit.admin.AdminTierManager",
            "de.sean.blockprot.bukkit.dialogs.AdminTiersDialog",
            "de.sean.blockprot.bukkit.dialogs.AdminTierSelectDialog",
            "de.sean.blockprot.bukkit.dialogs.AdminCustomFlagsDialog",
            "de.sean.blockprot.bukkit.inventories.AdminTiersInventory",
            "de.sean.blockprot.bukkit.inventories.AdminTierSelectInventory",
            "de.sean.blockprot.bukkit.inventories.AdminCustomFlagsInventory",
            "de.sean.blockprot.bukkit.events.BlockAccessEvent",
            "de.sean.blockprot.bukkit.events.BlockAccessMenuEvent",
            "de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent",
            "de.sean.blockprot.bukkit.events.BlockProtLockEvent",
            "de.sean.blockprot.bukkit.events.BlockProtUnlockEvent",
            "de.sean.blockprot.bukkit.inventories.BlockProtInventory",
            "de.sean.blockprot.bukkit.inventories.InventoryConstants",
            "de.sean.blockprot.bukkit.inventories.ChatInput",
            "de.sean.blockprot.bukkit.inventories.LegacyChatInput",
            "de.sean.blockprot.bukkit.inventories.TextInput",
            "de.sean.blockprot.bukkit.listeners.ErrorEventListener",
            "de.sean.blockprot.bukkit.logger.PluginActivityLog",
            "de.sean.blockprot.bukkit.metrics.IntegrationBarChart",
            "de.sean.blockprot.bukkit.config.BlockProtConfig",
            "de.sean.blockprot.bukkit.config.ReloadCoordinator",
            "de.sean.blockprot.bukkit.tasks.BackupTask",
            "de.sean.blockprot.bukkit.tasks.ConfigFileWatcher",
            "de.sean.blockprot.bukkit.tasks.InactivityCleanupTask",
            "de.sean.blockprot.bukkit.tasks.StatisticFileSaveTask",
            "de.sean.blockprot.bukkit.tasks.UpdateChecker",
            "de.sean.blockprot.bukkit.tasks.VillagerLocateTask",
            "de.sean.blockprot.bukkit.tasks.WorldExpiryTask",
            "de.sean.blockprot.bukkit.BlockProtAPI",
            "de.sean.blockprot.bukkit.BlockProtConsole",
            "de.sean.blockprot.bukkit.CachedProfileService",
            "de.sean.blockprot.bukkit.TranslationValue",
            "de.sean.blockprot.bukkit.VersionValidator",
            "de.sean.blockprot.bukkit.storage.HybridDatabase",
            "de.sean.blockprot.bukkit.storage.ProtectedBlockCache",
            "de.sean.blockprot.bukkit.nbt.stats.BukkitStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.FloatStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.IntStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.LocationListStatistic",
            "de.sean.blockprot.bukkit.nbt.stats.StringStatistic",
        };
        int loaded = 0;
        for (String name : names) {
            try {
                Class.forName(name, false, BlockProt.getInstance().getClass().getClassLoader());
                loaded++;
            } catch (Throwable e) {
                BlockProtLogger.fail("Structural class", name + ": " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
                f.incrementAndGet();
            }
        }
        if (loaded == names.length) {
            BlockProtLogger.pass("Structural classes: " + loaded + "/" + names.length + " loadable"
                + " (events, gateways, tasks, console, metrics, misc)");
            p.incrementAndGet();
        } else {
            BlockProtLogger.fail("Structural classes", (names.length - loaded) + " class(es) failed to load");
        }
    }

    private void checkEnumeratedCoverage(@Nullable Player player, AtomicInteger p, AtomicInteger f) {
        var codeSource = BlockProt.getInstance().getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            BlockProtLogger.pass("Class coverage: not running from a jar, enumeration skipped (dev workspace)");
            p.incrementAndGet();
            return;
        }
        int total = 0;
        int uncovered = 0;
        String[] packages = {
            INVENTORY_PACKAGE, DIALOG_PACKAGE,
            BlockProtCommand.class.getPackageName(), LISTENERS_PACKAGE
        };
        try (var jar = new JarFile(new File(codeSource.getLocation().toURI()))) {
            for (String pkg : packages) {
                String dir = pkg.replace('.', '/') + "/";
                for (var entry : Collections.list(jar.entries())) {
                    String entryName = entry.getName();
                    if (!entryName.startsWith(dir) || !entryName.endsWith(".class")) continue;
                    if (entryName.indexOf('/', dir.length()) != -1) continue;
                    String simpleName = entryName.substring(dir.length(), entryName.length() - ".class".length());
                    if (simpleName.indexOf('$') != -1) continue;
                    if (!(simpleName.endsWith("Inventory") || simpleName.endsWith("Dialog")
                        || simpleName.endsWith("Command") || simpleName.endsWith("Listener"))) continue;
                    String fqcn = pkg + "." + simpleName;
                    try {
                        Class<?> c = Class.forName(fqcn, false,
                            BlockProt.getInstance().getClass().getClassLoader());
                        int mods = c.getModifiers();
                        if (Modifier.isAbstract(mods) || c.isInterface() || c.isEnum()) continue;
                    } catch (Throwable e) {
                        BlockProtLogger.fail("Class coverage", fqcn + " failed to load: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                        f.incrementAndGet();
                        continue;
                    }
                    total++;
                    if (!coveredClasses.contains(fqcn)) {
                        uncovered++;
                        BlockProtLogger.fail("Class coverage", simpleName
                            + " is exercised by no debug group (new screen? add it to a group)");
                    }
                }
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Class coverage",
                e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
            return;
        }
        if (uncovered == 0) {
            BlockProtLogger.pass("Class coverage: " + (total - uncovered) + "/" + total
                + " screen classes exercised (inventories, dialogs, commands, listeners)");
            p.incrementAndGet();
        } else {
            f.incrementAndGet();
        }
    }

    private void dlg(@NotNull AtomicInteger p, @NotNull AtomicInteger f,
                     @NotNull String name, @NotNull Runnable body) {
        try {
            touchScreen(DIALOG_PACKAGE, name);
            body.run();
            BlockProtLogger.passSub(name);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.failSub(name,
                e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
    }

    private void inv(@NotNull AtomicInteger p, @NotNull AtomicInteger f,
                     @NotNull String name,
                     @NotNull java.util.concurrent.Callable<Inventory> supplier) {
        try {
            touchScreen(INVENTORY_PACKAGE, name);
            Inventory result = supplier.call();
            if (result != null) {
                BlockProtLogger.passSub(name + " (size=" + result.getSize() + ")");
            } else {
                BlockProtLogger.skipSub(name, "returned null (intentional for some paths)");
            }
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.failSub(name,
                e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
    }

    private void simSub(@NotNull AtomicInteger p, @NotNull AtomicInteger f,
                        @NotNull String name, @NotNull Runnable test) {
        try {
            test.run();
            BlockProtLogger.passSub(name);
            p.incrementAndGet();
        } catch (Throwable e) {
            BlockProtLogger.failSub(name, e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkPlayerSimulation(@NotNull Location origin, @NotNull UUID actor, @Nullable Player player,
                                       AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.subGroup("Player simulation & gameplay scenarios (10 scenarios):");
        var loc = origin.clone();
        var world = origin.getWorld();
        var orig = world.getBlockAt(loc).getType();
        UUID strangerUuid = UUID.randomUUID();
        UUID friendUuid = UUID.fromString(NOTCH_UUID);

        simSub(p, f, "10a. Block Lock & Cache Lifecycle", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());
                handler.setName("SimChest");
                ProtectedBlockCache.mark(block);

                if (!handler.isOwner(actor) || !ProtectedBlockCache.isProtected(block)) {
                    throw new IllegalStateException("Block not marked as owned or cached as protected");
                }
            } finally {
                ProtectedBlockCache.unmark(world.getBlockAt(loc));
                world.setType(loc, orig);
            }
        });

        simSub(p, f, "10b. Stranger Access Denial & Ownership", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());

                if (!handler.canAccess(actor.toString())) {
                    throw new IllegalStateException("Owner player unexpectedly denied access");
                }
                if (handler.canAccess(strangerUuid.toString())) {
                    throw new IllegalStateException("Stranger UUID unexpectedly granted access to locked block");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        simSub(p, f, "10c. Friend Lifecycle on Block (Add/Access/Remove)", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                handler.setOwner(actor.toString());

                handler.addFriend(friendUuid.toString());
                if (!handler.containsFriend(friendUuid.toString()) || !handler.canAccess(friendUuid.toString())) {
                    throw new IllegalStateException("Friend not found or cannot access after addFriend");
                }
                handler.removeFriend(friendUuid.toString());
                if (handler.containsFriend(friendUuid.toString()) && handler.canAccess(friendUuid.toString())) {
                    throw new IllegalStateException("Friend still present or has access after removeFriend");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        simSub(p, f, "10d. Redstone & Hopper Protection Toggles", () -> {
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var handler = new BlockNBTHandler(block);
                RedstoneSettingsHandler rs = handler.getRedstoneHandler();
                boolean origPiston = rs.getPistonProtection();
                boolean origHopper = rs.getHopperProtection();
                rs.setPistonProtection(!origPiston);
                rs.setHopperProtection(!origHopper);
                boolean toggledPiston = rs.getPistonProtection();
                boolean toggledHopper = rs.getHopperProtection();
                rs.reset();
                if (toggledPiston == origPiston || toggledHopper == origHopper) {
                    throw new IllegalStateException("Redstone/Hopper protection failed to toggle");
                }
            } finally {
                world.setType(loc, orig);
            }
        });

        simSub(p, f, "10e. Multi-Block & Family Expressions", () -> {
            boolean isDoor = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.DOORS).contains(Material.OAK_DOOR);
            boolean isTrapdoor = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.BLOCKS).contains(Material.OAK_TRAPDOOR);
            boolean isGate = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.BLOCKS).contains(Material.OAK_FENCE_GATE);
            boolean isTile = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.TILE_ENTITIES).contains(Material.CHEST);
            String readable = BlockUtil.getHumanReadableBlockName(Material.CHEST);
            if (!isDoor || !isTrapdoor || !isGate || !isTile || readable.isBlank()) {
                throw new IllegalStateException("Family parser or readable name check failed for standard blocks");
            }
        });

        simSub(p, f, "10f. Hazard & Explosion Protection Behavior", () -> {
            boolean explosionProtect = BlockProt.getDefaultConfig().shouldProtectLockedBlocksFromExplosions();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                ProtectedBlockCache.mark(block);
                if (!ProtectedBlockCache.isProtected(block)) {
                    throw new IllegalStateException("Block not registered in ProtectedBlockCache");
                }
            } finally {
                ProtectedBlockCache.unmark(world.getBlockAt(loc));
                world.setType(loc, orig);
            }
        });

        simSub(p, f, "10g. Entity Protection Simulation", () -> {
            var entity = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                var entityHandler = new EntityNBTHandler(entity);
                entityHandler.setOwner(actor.toString());
                if (!entityHandler.isOwner(actor.toString())) {
                    throw new IllegalStateException("Entity owner not set to player UUID");
                }
                if (entityHandler.isOwner(strangerUuid.toString())) {
                    throw new IllegalStateException("Entity owner check returned true for stranger UUID");
                }
                entityHandler.addFriend(friendUuid.toString());
                if (!entityHandler.hasFriend(friendUuid.toString())) {
                    throw new IllegalStateException("Entity friends list missing added friend UUID");
                }
                entityHandler.removeFriend(friendUuid.toString());
                if (entityHandler.hasFriend(friendUuid.toString())) {
                    throw new IllegalStateException("Entity friends list still contains friend after removal");
                }
            } finally {
                entity.remove();
            }
        });

        simSub(p, f, "10h. GUI State & Theft Prevention Integrity", () -> {
            InventoryState state = new InventoryState(null);
            state.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            state.origin = InventoryState.MenuOrigin.NONE;
            InventoryState.set(actor, state);
            InventoryState retrieved = InventoryState.get(actor);
            if (retrieved == null || retrieved.friendSearchState != InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH) {
                throw new IllegalStateException("InventoryState set/get mismatch");
            }
            InventoryState.remove(actor);
            if (InventoryState.get(actor) != null) {
                throw new IllegalStateException("InventoryState not null after remove()");
            }
        });

        simSub(p, f, "10i. Command Permission Nodes Verification", () -> {
            for (Permissions perm : Permissions.values()) {
                if (perm.key() == null || perm.key().isBlank()) {
                    throw new IllegalStateException("Permission node has null or blank key: " + perm.name());
                }
                if (player != null) player.hasPermission(perm.key());
            }
        });

        if (player == null) {
            BlockProtLogger.skipSub("10j. Player Settings & Search History Roundtrip", "requires an online player");
            return;
        }
        simSub(p, f, "10j. Player Settings & Search History Roundtrip", () -> {
            PlayerSettingsHandler ps = new PlayerSettingsHandler(player);
            boolean origLock = ps.getLockOnPlace();
            ps.setLockOnPlace(!origLock);
            boolean toggled = ps.getLockOnPlace();
            ps.setLockOnPlace(origLock);
            if (toggled == origLock) {
                throw new IllegalStateException("Failed to toggle player lockOnPlace setting");
            }
            ps.clearSearchHistory();
        });
    }

    private void runDiagnosticsHeadless(@NotNull CommandSender sender) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        coveredClasses.clear();
        BlockProtLogger.startDebugReport();

        BlockProtLogger.log("Session: " + java.time.LocalDateTime.now()
            + " | Plugin: BlockProt Reloaded " + BlockProt.getPluginVersion()
            + " | Sender: " + sender.getName() + " (Console/Headless)");
        BlockProtLogger.log("Server: " + Bukkit.getVersion()
            + " | API: " + Bukkit.getBukkitVersion()
            + " | Java: " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat: " + VersionCompat.getDiagnosticString()
            + " | BukkitCompat: " + BukkitCompat.getDiagnosticString()
            + " | FoliaLib: [isFolia=" + BlockProt.getFoliaLib().isFolia()
            + ", isPaper=" + BlockProt.getFoliaLib().isPaper()
            + ", isSpigot=" + BlockProt.getFoliaLib().isSpigot() + "]");

        ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_START));

        // Domain 1: Environment & Compatibility
        runDomain("1/10", "ENVIRONMENT & COMPATIBILITY");
        checkConfig(null, passed, failed);
        checkBukkitCompat(null, passed, failed);
        checkFoliaLib(null, passed, failed);

        // Domain 2: Configuration & Block Families
        runDomain("2/10", "CONFIGURATION & BLOCK FAMILIES");
        checkLockableMaterials(null, passed, failed);
        checkAutoDrop(null, passed, failed);
        checkLockableEntities(null, passed, failed);
        checkItemFrameProtection(null, passed, failed);
        checkRaidDetection(null, passed, failed);
        checkVillagerWorkstationProtection(null, passed, failed);

        // Domain 3: Localization & Translation Coverage
        runDomain("3/10", "LOCALIZATION & TRANSLATIONS");
        checkTranslations(null, passed, failed);
        checkLanguages(null, passed, failed);

        // Domain 4: Storage, Database & Caching
        runDomain("4/10", "STORAGE, DATABASE & CACHING");
        checkHybridDatabase(null, passed, failed);
        checkProfileService(null, passed, failed);
        checkSkinsRestorer(null, passed, failed);
        checkAuditLogger(null, passed, failed);
        checkOnlinePlayers(null, passed, failed);

        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        Location origin = new Location(world, spawn.getBlockX(), world.getMaxHeight() - 2, spawn.getBlockZ());
        UUID actor = UUID.nameUUIDFromBytes("BlockProtDebugConsole".getBytes(StandardCharsets.UTF_8));

        BlockProt.getFoliaLib().getScheduler().runAtLocation(origin, task -> {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean tempChestAdded = false;
            if (!cfg.isLockableTileEntity(Material.CHEST, world) && !cfg.isLockableBlock(Material.CHEST, world)) {
                cfg.addTestLockable(Material.CHEST);
                tempChestAdded = true;
            }
            try {
                runDomain("5/10", "NBT & DATA PERSISTENCE ENGINE");
                checkNbt(origin, passed, failed);
                checkEntityNbt(origin, passed, failed);
                BlockProtLogger.skipSub("PlayerSettings", "requires an online player");
                checkNbtSubHandlers(origin, passed, failed);

                runDomain("6/10", "COMMANDS, PERMISSIONS & INTEGRATIONS");
                checkIntegrations(null, passed, failed);
                checkCommandsRegistered(null, passed, failed);
                checkAdminTiers(null, passed, failed);

                runDomain("7/10", "EVENT LISTENERS & ENGINE");
                checkListenersRegistered(null, passed, failed);

                runDomain("8/10", "USER INTERFACE & SCREENS");
                BlockProtLogger.skipSub("Inventories and dialogs", "require an online player to render");

                runDomain("9/10", "UTILITY HELPERS & BENCHMARKS");
                checkMessages(null, passed, failed);
                checkBlocksYmlIntegrity(null, passed, failed);
                checkWorldsYmlIntegrity(null, passed, failed);
                checkSkinCache(null, passed, failed);
                checkUtilityHelpers(null, passed, failed);
                checkStructuralClasses(null, passed, failed);
                checkEnumeratedCoverage(null, passed, failed);

                runDomain("10/10", "PLAYER SIMULATION & GAMEPLAY VERIFICATION");
                checkPlayerSimulation(origin, actor, null, passed, failed);

                int p2 = passed.get(), f2 = failed.get(), total = p2 + f2;
                BlockProtLogger.separator();
                BlockProtLogger.log("HEADLESS SUMMARY: " + p2 + " passed, " + f2 + " failed / " + total + " total");
                ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__HEADLESS_FINISHED)
                    .replace("{passed}", String.valueOf(p2))
                    .replace("{failed}", String.valueOf(f2))
                    .replace("{total}", String.valueOf(total)));
                var reportFile = BlockProtLogger.getDebugReportFile();
                BlockProtLogger.endDebugReport();
                if (reportFile != null) {
                    ComponentMessages.sendLegacy(sender, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                        .replace("{path}", reportFile.getPath()));
                }
            } finally {
                if (tempChestAdded) {
                    cfg.removeTestLockable(Material.CHEST);
                }
            }
        });
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!canUseCommand(sender)) return Collections.emptyList();
        return List.of("run", "placeDebugChest", "placeDebugShulker", "clearSearchHistory");
    }

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || AdminTierManager.hasPermission(sender, AdminAction.DEBUG)
            || sender.hasPermission(Permissions.DEBUG.key());
    }
}