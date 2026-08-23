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
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(),
            () -> new DebugCommand().runDiagnostics(player));
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

    private void runDiagnostics(@NotNull Player player) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        coveredClasses.clear();
        BlockProtLogger.startDebugReport();

        BlockProtLogger.separator();
        BlockProtLogger.log("=== /blockprot debug run: " + java.time.LocalDateTime.now() + " ===");
        BlockProtLogger.log("Plugin  : BlockProt Reloaded " + BlockProt.getPluginVersion());
        BlockProtLogger.log("Player  : " + player.getName() + " (" + player.getUniqueId() + ")");
        BlockProtLogger.log("Server : " + Bukkit.getVersion());
        BlockProtLogger.log("API    : " + Bukkit.getBukkitVersion());
        BlockProtLogger.log("Java   : " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat : " + VersionCompat.getDiagnosticString());
        BlockProtLogger.log("BukkitCompat: " + BukkitCompat.getDiagnosticString());

        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RESULTS_GO_TO_LOG));

        runGroup(player, passed, failed, "1.  Config",              () -> checkConfig(player, passed, failed));
        runGroup(player, passed, failed, "2.  BukkitCompat",       () -> checkBukkitCompat(player, passed, failed));
        runGroup(player, passed, failed, "3.  Translations",       () -> checkTranslations(player, passed, failed));
        runGroup(player, passed, failed, "3b. Language files",     () -> checkLanguages(player, passed, failed));
        runGroup(player, passed, failed, "4.  Lockable blocks",    () -> checkLockableMaterials(player, passed, failed));
        runGroup(player, passed, failed, "4b. AutoDrop",           () -> checkAutoDrop(player, passed, failed));
        runGroup(player, passed, failed, "5.  Lockable entities",  () -> checkLockableEntities(player, passed, failed));
        runGroup(player, passed, failed, "6.  Item frame protect", () -> checkItemFrameProtection(player, passed, failed));
        runGroup(player, passed, failed, "7.  Raid detection",     () -> checkRaidDetection(player, passed, failed));
        runGroup(player, passed, failed, "7b. Villager workstation", () -> checkVillagerWorkstationProtection(player, passed, failed));
        runGroup(player, passed, failed, "8.  Integrations",       () -> checkIntegrations(player, passed, failed));
        runGroup(player, passed, failed, "9.  ProfileService",     () -> checkProfileService(player, passed, failed));
        runGroup(player, passed, failed, "10. SkinsRestorer",      () -> checkSkinsRestorer(player, passed, failed));
        runGroup(player, passed, failed, "11. AuditLogger",        () -> checkAuditLogger(player, passed, failed));
        runGroup(player, passed, failed, "12. OnlinePlayers",      () -> checkOnlinePlayers(player, passed, failed));

        Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
            runGroup(player, passed, failed, "13. NBT block write/read",  () -> checkNbt(player, passed, failed));
            runGroup(player, passed, failed, "14. NBT entity write/read", () -> checkEntityNbt(player, passed, failed));
            runGroup(player, passed, failed, "15. PlayerSettings",        () -> checkPlayerSettings(player, passed, failed));
            runGroup(player, passed, failed, "16. Inventory creation",    () -> checkInventoryCreation(player, passed, failed));
            runGroup(player, passed, failed, "17. All inventories",       () -> checkAllInventories(player, passed, failed));
            runGroup(player, passed, failed, "18. Messages",              () -> checkMessages(player, passed, failed));
            runGroup(player, passed, failed, "19. blocks.yml integrity",  () -> checkBlocksYmlIntegrity(player, passed, failed));
            runGroup(player, passed, failed, "20. worlds.yml integrity",  () -> checkWorldsYmlIntegrity(player, passed, failed));
            runGroup(player, passed, failed, "21. All dialogs",           () -> checkAllDialogs(player, passed, failed));
            runGroup(player, passed, failed, "22. Commands registered",   () -> checkCommandsRegistered(player, passed, failed));
            runGroup(player, passed, failed, "23. Listeners registered",  () -> checkListenersRegistered(player, passed, failed));
            runGroup(player, passed, failed, "24. SkinCache tiers",       () -> checkSkinCache(player, passed, failed));
            runGroup(player, passed, failed, "25. Utility helpers",       () -> checkUtilityHelpers(player, passed, failed));
            runGroup(player, passed, failed, "26. NBT sub-handlers",      () -> checkNbtSubHandlers(player, passed, failed));
            runGroup(player, passed, failed, "27. Structural classes",    () -> checkStructuralClasses(player, passed, failed));
            runGroup(player, passed, failed, "28. Class coverage",        () -> checkEnumeratedCoverage(player, passed, failed));

            int p2 = passed.get(), f2 = failed.get(), total = p2 + f2;
            BlockProtLogger.separator();
            BlockProtLogger.log("=== SUMMARY: " + p2 + " passed, " + f2 + " failed / " + total + " total ===");

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

            var logFile = BlockProtLogger.getCurrentLogFile();
            if (logFile != null)
                chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                    .replace("{path}", logFile.getPath()));

            var reportFile = BlockProtLogger.getDebugReportFile();
            BlockProtLogger.endDebugReport();
            if (reportFile != null)
                chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__LOG_PATH)
                    .replace("{path}", reportFile.getPath()));
        });
    }

    private void runGroup(@NotNull Player player, @NotNull AtomicInteger passed,
                          @NotNull AtomicInteger failed, @NotNull String name,
                          @NotNull Runnable body) {
        BlockProtLogger.separator();
        BlockProtLogger.log("--- " + name + " ---");
        try {
            body.run();
        } catch (Exception e) {
            BlockProtLogger.fail(name + " (group)", e.getClass().getSimpleName() + ": " + e.getMessage());
            failed.incrementAndGet();
        }
    }

    private void checkConfig(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            BlockProtLogger.pass("Config OK: friendDisabled=" + cfg.isFriendFunctionalityDisabled()
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

    private boolean cfgBoolean(String key, boolean def) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            var method = cfg.getClass().getSuperclass().getDeclaredMethod("getBoolean", String.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(cfg, key, def);
        } catch (Exception ignored) {
            return def;
        }
    }

    private void checkBukkitCompat(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

    private void checkTranslations(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        int blank = 0, errors = 0;
        for (TranslationKey key : TranslationKey.values()) {
            try {
                String v = Translator.get(key);
                if (v == null || v.isBlank()) {
                    blank++; BlockProtLogger.warn("Translation blank: " + key.name());
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

    private void checkLanguages(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            BlockProt plugin = BlockProt.getInstance();
            String active = BlockProt.getDefaultConfig().getLanguageFile();
            String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

            int totalKeys = TranslationKey.values().length;
            int langOk = 0, langFail = 0, skipped = 0;

            for (String fileName : allLangs) {
                if (!LangConfig.isLanguageEnabled(fileName)) {
                    skipped++;
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
                            BlockProtLogger.fail("Language file", fileName + ": not found on disk or in jar");
                            langFail++; continue;
                        }
                        langFile = YamlConfiguration.loadConfiguration(
                            new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
                    }
                } catch (Exception e) {
                    BlockProtLogger.fail("Language file", fileName + ": load error: " + e.getMessage());
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
                BlockProtLogger.log("  [" + status + "] " + fileName + "  (" + pct + "% - " + presentKeys + "/" + totalKeys + ")");
                if (pct < 100) {
                    int totalMissing = missingKeys.size();
                    int showCount = Math.min(totalMissing, 5);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < showCount; i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(missingKeys.get(i));
                    }
                    if (totalMissing > showCount) sb.append(" ...");
                    BlockProtLogger.log("    missing=" + totalMissing + " ex: " + sb);
                }
                langOk++;
            }

            if (langFail == 0) {
                BlockProtLogger.pass("Language files: " + langOk + " enabled/" + (allLangs.length - skipped) + " checked, " + skipped + " disabled (skipped)");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("Language files", langFail + " file(s) could not be loaded");
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Language files", e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkLockableMaterials(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

    private void checkAutoDrop(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

    private void checkLockableEntities(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            Set<Material> entityFamily = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES);
            if (entityFamily.isEmpty()) {
                BlockProtLogger.warn("Lockable entities: ENTITIES family is empty (no entity materials registered)");
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
                BlockProtLogger.pass("Lockable entities: EMPTY: vehicle/frame protection disabled (correct per blocks.yml)");
                if (cfg.isLockableEntity(Material.CHEST_MINECART)) {
                    BlockProtLogger.fail("Lockable entities", "CHEST_MINECART reports lockable but lockable_entities is empty");
                    f.incrementAndGet();
                } else {
                    BlockProtLogger.pass("Spot-check CHEST_MINECART: correctly NOT lockable");
                    p.incrementAndGet();
                }
            } else {
                StringBuilder sb = new StringBuilder();
                for (Material m : active) sb.append(m.name()).append(" ");
                BlockProtLogger.pass("Lockable entities active=[" + sb.toString().trim() + "] count=" + active.size());
                BlockProtLogger.log("  inactive=[" + inactive.stream()
                    .map(Material::name).reduce("", (a, b) -> a.isBlank() ? b : a + " " + b) + "]");
                p.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable entities", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkItemFrameProtection(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean frameActive = cfg.isLockableEntity(Material.ITEM_FRAME);
            boolean glowActive  = cfg.isLockableEntity(Material.GLOW_ITEM_FRAME);

            BlockProtLogger.log("  Item frame protection: ITEM_FRAME=" + (frameActive ? "ACTIVE" : "INACTIVE (default)")
                + " GLOW_ITEM_FRAME=" + (glowActive ? "ACTIVE" : "INACTIVE (default)"));
            BlockProtLogger.pass("Item frame protection: config readable, frames="
                + (frameActive ? "enabled" : "disabled (lockable_entities empty or not listed)"));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Item frame protection", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkVillagerWorkstationProtection(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

    private void checkRaidDetection(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

    private void checkIntegrations(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            List<PluginIntegration> integrations = BlockProt.getInstance().getIntegrations();
            if (integrations == null || integrations.isEmpty()) {
                BlockProtLogger.log("Integrations: none registered");
                p.incrementAndGet();
                return;
            }
            int active = 0;
            for (PluginIntegration integration : integrations) {
                String name = integration.getClass().getSimpleName();
                boolean enabled = integration.isEnabled();
                if (enabled) active++;

                if (integration instanceof ViaVersionIntegration via) {
                    BlockProtLogger.log("  " + name + ": " + via.getDetailedStatus());
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
                    BlockProtLogger.log("  " + name + ": " + (enabled ? "ACTIVE v" + ver : "INACTIVE (plugin not found or disabled)"));
                }
            }
            BlockProtLogger.pass("Integrations: " + active + "/" + integrations.size() + " active");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Integrations", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkProfileService(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var profile = BlockProt.getProfileService().findByUuid(player.getUniqueId());
            BlockProtLogger.pass("ProfileService OK: " + (profile != null ? profile.getName() : "null (no exception)"));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("ProfileService", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkSkinsRestorer(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null) BlockProtLogger.log("SkinsRestorer: not installed");
        else if (!plugin.isEnabled()) BlockProtLogger.warn("SkinsRestorer installed but disabled");
        else {
            String ver;
            try { ver = plugin.getPluginMeta().getVersion(); }
            catch (NoSuchMethodError e) {
                @SuppressWarnings("deprecation")
                String fallback = plugin.getDescription().getVersion();
                ver = fallback;
            }
            BlockProtLogger.pass("SkinsRestorer v" + ver);
        }
        p.incrementAndGet();
    }

    private void checkAuditLogger(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        var audit = BlockProt.getAuditLogger();
        BlockProtLogger.pass("AuditLogger: " + (audit == null ? "disabled (config)" : "active"));
        p.incrementAndGet();
    }

    private void checkOnlinePlayers(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        BlockProtLogger.log("Online players: " + Bukkit.getOnlinePlayers().size());
        for (Player pl : Bukkit.getOnlinePlayers())
            BlockProtLogger.log("  - " + pl.getName() + " (" + pl.getUniqueId() + ")");
        p.incrementAndGet();
    }

    private void checkNbt(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var h = new BlockNBTHandler(world.getBlockAt(loc));
            h.setOwner(NOTCH_UUID);
            h.setName("debug_nbt_test");
            String owner   = h.getOwner();
            String name    = h.getName();
            long lockedAt  = h.getLockedAt();
            world.setType(loc, orig);
            if (NOTCH_UUID.equals(owner) && "debug_nbt_test".equals(name)) {
                BlockProtLogger.pass("NBT block write/read OK (owner=" + owner + " name=" + name + " lockedAt=" + lockedAt + ")");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("NBT block mismatch", "owner=" + owner + " name=" + name);
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT block", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkEntityNbt(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var entity = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });

            var handler = new EntityNBTHandler(entity);
            handler.setOwner(NOTCH_UUID);

            String readBack = handler.getOwner();

            entity.remove();

            if (NOTCH_UUID.equals(readBack)) {
                BlockProtLogger.pass("NBT entity write/read OK (owner=" + readBack + ")");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("NBT entity mismatch", "expected=" + NOTCH_UUID + " got=" + readBack);
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT entity", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkPlayerSettings(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var ps = new PlayerSettingsHandler(player);
            BlockProtLogger.pass("PlayerSettings OK: lockOnPlace=" + ps.getLockOnPlace()
                + " hintsEnabled=" + !ps.hasPlayerInteractedWithMenu());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("PlayerSettings", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkInventoryCreation(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            Inventory inv = Bukkit.createInventory(null, 9,
                net.kyori.adventure.text.Component.text(Translator.get(TranslationKey.MESSAGES__DEBUG__INVENTORY_TITLE)));
            BlockProtLogger.pass("Bukkit.createInventory OK size=" + inv.getSize());
            p.incrementAndGet();
        } catch (Throwable e) {
            // Catches NoSuchMethodError too (e.g. the Component-title overload missing
            // on Spigot/CraftBukkit), not just Exception, so this self-test reports a
            // clean fail instead of crashing the whole diagnostics task.
            BlockProtLogger.fail("createInventory", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkAllInventories(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        InventoryState base = new InventoryState(null);
        base.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        base.origin = InventoryState.MenuOrigin.NONE;
        InventoryState.set(player.getUniqueId(), base);

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
            var block = world.getBlockAt(loc);
            new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
            InventoryState rs = new InventoryState(block);
            rs.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), rs);
            Inventory result = new RedstoneSettingsInventory().fill(player, rs);
            world.setType(loc, orig);
            return result;
        });

        inv(p, f, "BlockLockInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            var h = new BlockNBTHandler(block);
            h.setOwner(player.getUniqueId().toString());
            InventoryState bl = new InventoryState(block);
            bl.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), bl);
            Inventory result = new BlockLockInventory().fill(player, Material.CHEST, h);
            world.setType(loc, orig);
            return result;
        });

        inv(p, f, "BlockInfoInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            var h = new BlockNBTHandler(block);
            h.setOwner(player.getUniqueId().toString());
            InventoryState bi = new InventoryState(block);
            bi.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), bi);
            Inventory result = new BlockInfoInventory().fill(player, h);
            world.setType(loc, orig);
            return result;
        });

        inv(p, f, "BlockInspectContentsInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
            InventoryState bic = new InventoryState(block);
            InventoryState.set(player.getUniqueId(), bic);
            Inventory result = new BlockInspectContentsInventory(player).fill();
            world.setType(loc, orig);
            return result;
        });

        inv(p, f, "AuditInventory (no entries)", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            InventoryState ai = new InventoryState(block);
            ai.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), ai);
            Inventory result = new AuditInventory().fill(player);
            world.setType(loc, orig);
            return result;
        });

        BlockProtLogger.log("Inventory skipped: EntitySettingsInventory (requires live Entity, not testable without one)");
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

        BlockProtLogger.log("Inventory skipped: PlayerListInventory (open() opens a live GUI, no fill() to build)");
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
            var h = new EntityNBTHandler(ent);
            h.setOwner(player.getUniqueId().toString());
            Inventory result = new EntityInfoInventory().fill(player, ent, h);
            ent.remove();
            return result;
        });
        inv(p, f, "EntityBlockSettingsInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            var h = new EntityNBTHandler(ent);
            h.setOwner(player.getUniqueId().toString());
            Inventory result = new EntityBlockSettingsInventory().fill(player, ent, h);
            ent.remove();
            return result;
        });
        inv(p, f, "EntityFriendManageInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            var h = new EntityNBTHandler(ent);
            h.setOwner(player.getUniqueId().toString());
            Inventory result = new EntityFriendManageInventory().fill(player, ent, h);
            ent.remove();
            return result;
        });
        inv(p, f, "EntityFriendSearchResultInventory", () -> {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            var h = new EntityNBTHandler(ent);
            h.setOwner(player.getUniqueId().toString());
            Inventory result = new EntityFriendSearchResultInventory().fill(player, ent, h, player.getName());
            ent.remove();
            return result;
        });
        BlockProtLogger.log("Inventory skipped: EntityInspectContentsInventory (requires a container entity, not testable with an ArmorStand)");
        touchScreen(INVENTORY_PACKAGE, "EntityInspectContentsInventory");
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

        BlockProtLogger.log("Inventory skipped: FriendSearchInventory (chat-input gateway, no fill() to build)");
        touchScreen(INVENTORY_PACKAGE, "FriendSearchInventory");
        p.incrementAndGet();

        InventoryState.set(player.getUniqueId(), base);

        BlockProtLogger.separator();
        BlockProtLogger.log("--- Inventory title translation coverage ---");
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
                BlockProtLogger.fail("Title blank/missing", k.name()); f.incrementAndGet();
            } else {
                BlockProtLogger.pass("Title OK: " + k.name() + " = \"" + v + "\"");
                p.incrementAndGet();
            }
        }
    }

    private void checkMessages(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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
        BlockProtLogger.log("Messages: " + ok + " OK, " + bad + " blank");
        if (bad == 0) { BlockProtLogger.pass("All message keys present"); p.incrementAndGet(); }
        else f.incrementAndGet();
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
    private void checkBlocksYmlIntegrity(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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

            BlockProtLogger.log("blocks.yml integrity: " + checked + " entries checked, "
                + unresolved + " unresolved, " + mismatched + " mismatched");
            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("blocks.yml integrity OK (" + checked + " entries)");
                p.incrementAndGet();
            } else {
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
    private void checkWorldsYmlIntegrity(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig defaultConfig = BlockProt.getDefaultConfig();
            if (!defaultConfig.isPerWorldsConfigEnabled()) {
                BlockProtLogger.log("worlds.yml integrity: per_worlds_config disabled, skipping");
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
                BlockProtLogger.log("worlds.yml integrity: no 'worlds' section, nothing to check");
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

            BlockProtLogger.log("worlds.yml integrity: " + worldsChecked + " world(s), " + checked
                + " entries checked, " + unresolved + " unresolved, " + mismatched + " mismatched");
            if (unresolved == 0 && mismatched == 0) {
                BlockProtLogger.pass("worlds.yml integrity OK");
                p.incrementAndGet();
            } else {
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
    }

    private void checkAllDialogs(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
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
                var block = world.getBlockAt(loc);
                new BlockNBTHandler(block).setOwner(player.getUniqueId().toString());
                AuditDialog.show(player, block);
                world.setType(loc, orig);
            });
            dlg(p, f, "AutoDropDialog", () -> AutoDropDialog.show(player, DialogOrigin.ADMIN_MENU));
            dlg(p, f, "AutoDropFamilyDialog", () -> AutoDropFamilyDialog.show(player, DialogOrigin.ADMIN_MENU, BlockFamilyParser.Family.BLOCKS));
            dlg(p, f, "AutoDropSearchDialog", () -> AutoDropSearchDialog.show(player, DialogOrigin.ADMIN_MENU, null, "chest", 0));
            dlg(p, f, "BlockInfoDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                BlockInfoDialog.show(player, block, h);
                world.setType(loc, orig);
            });
            dlg(p, f, "BlockLockDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                BlockLockDialog.show(player, block, h);
                world.setType(loc, orig);
            });
            dlg(p, f, "BlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var orig = world.getBlockAt(loc).getType();
                world.setType(loc, Material.CHEST);
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(player.getUniqueId().toString());
                BlockSettingsDialog.show(player, block, h);
                world.setType(loc, orig);
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
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                EntityInfoDialog.show(player, ent, h);
                ent.remove();
            });
            dlg(p, f, "EntityBlockSettingsDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                EntityBlockSettingsDialog.show(player, ent, h);
                ent.remove();
            });
            dlg(p, f, "EntityFriendManageDialog", () -> {
                var loc = player.getLocation().clone();
                var world = player.getWorld();
                var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                    stand.setGravity(false);
                    stand.setVisible(false);
                    stand.setSilent(true);
                });
                var h = new EntityNBTHandler(ent);
                h.setOwner(player.getUniqueId().toString());
                EntityFriendManageDialog.show(player, ent, h);
                ent.remove();
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
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }

    private void checkCommandsRegistered(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand("blockprot");
        if (cmd == null) {
            BlockProtLogger.fail("Commands", "blockprot command not registered");
            f.incrementAndGet();
        } else if (cmd.getExecutor() == null) {
            BlockProtLogger.fail("Commands", "blockprot command has no executor");
            f.incrementAndGet();
        } else {
            BlockProtLogger.pass("Commands: blockprot registered, executor="
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
                    BlockProtLogger.fail("Permission node", key + " not declared in plugin.yml");
                }
            }
            if (missing == 0) {
                BlockProtLogger.pass("Permissions: all " + Permissions.values().length + " nodes declared in plugin.yml");
                p.incrementAndGet();
            } else {
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("Commands", "plugin.yml read failed: " + e.getMessage());
            f.incrementAndGet();
        }

        String[] commandClasses = {
            "UserMenuCommand", "AdminMenuCommand",
            "HelpCommand", "SettingsCommand", "FriendsAddAllCommand", "StatisticsCommand",
            "TransferCommand", "AboutCommand", "HintsCommand", "InfoCommand",
            "ReloadCommand", "UpdateCommand", "IntegrationsCommand", "DebugCommand",
            "AdminUnlockCommand", "WorldProtDeleteCommand", "LockablesCommand", "RecommendedCommand"
        };
        java.util.Set<String> wired = new java.util.HashSet<>();
        for (String name : commandClasses) {
            try {
                Class.forName(BlockProtCommand.class.getPackageName() + "." + name);
                touch(BlockProtCommand.class.getPackageName() + "." + name);
                wired.add(name);
            } catch (Throwable e) {
                BlockProtLogger.fail("Command class", name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                f.incrementAndGet();
            }
        }
        BlockProtLogger.pass("Command classes: " + wired.size() + "/" + commandClasses.length
            + " present and loadable" + (wired.size() == commandClasses.length ? "" : " MISSING SOME"));
        if (wired.size() != commandClasses.length) f.incrementAndGet();
        else p.incrementAndGet();

        String[] integrationClasses = {
            "TownyIntegration", "PlaceholderAPIIntegration", "ViaVersionIntegration",
            "WorldGuardIntegration", "LandsPluginIntegration", "ClaimChunkIntegration",
            "ResidenceIntegration", "GriefPreventionIntegration"
        };
        java.util.Set<String> regNames = new java.util.HashSet<>();
        for (PluginIntegration integration : BlockProt.getInstance().getIntegrations()) {
            regNames.add(integration.getClass().getSimpleName());
        }
        int regMissing = 0;
        for (String name : integrationClasses) {
            if (!regNames.contains(name)) {
                regMissing++;
                BlockProtLogger.fail("Integration wiring", name + " not in BlockProt.onLoad() integration list");
            }
        }
        if (regMissing == 0) {
            BlockProtLogger.pass("Integration wiring: all " + integrationClasses.length
                + " integration classes constructed in BlockProt.onLoad()");
            p.incrementAndGet();
        } else {
            f.incrementAndGet();
        }
    }

    private void checkListenersRegistered(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
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
        BlockProtLogger.log("Listener: ErrorEventListener is intentional (CraftBukkit fallback only)");
        if (missing == 0) {
            BlockProtLogger.pass("Listeners: all " + expected.length + " active listeners registered");
            p.incrementAndGet();
        } else {
            f.incrementAndGet();
        }
    }

    private void checkSkinCache(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var profile = SkinCache.getCachedOrOnlineProfile(player.getName(), player.getUniqueId());
            BlockProtLogger.log("SkinCache tier 1: online profile " + (profile != null ? "resolved" : "null (no exception)"));
            if (profile != null) {
                BlockProtLogger.pass("SkinCache tier 1 OK: " + player.getName());
                p.incrementAndGet();
            } else {
                BlockProtLogger.log("SkinCache tier 1: no cached/online skin for " + player.getName());
                p.incrementAndGet();
            }
        } catch (Throwable e) {
            BlockProtLogger.fail("SkinCache tier 1", e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }

        var sr = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        try {
            var srProfile = SkinCache.resolveSkinsRestorer(player.getUniqueId(), player.getName());
            if (sr != null && sr.isEnabled()) {
                BlockProtLogger.pass("SkinCache tier 2 OK: SkinsRestorer present, resolve returned "
                    + (srProfile != null ? "a profile" : "null"));
            } else {
                BlockProtLogger.pass("SkinCache tier 2 OK: SkinsRestorer absent, resolve no-opped (null)");
            }
            p.incrementAndGet();
        } catch (Throwable e) {
            BlockProtLogger.fail("SkinCache tier 2", e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
    }

    private void checkUtilityHelpers(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            Duration parsed = DurationParser.parse("2h30m");
            if (parsed == null || parsed.toMinutes() != 150) {
                BlockProtLogger.fail("DurationParser", "parse(2h30m) = " + parsed);
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("DurationParser: parse(2h30m)=" + parsed
                + " format=" + DurationParser.format(parsed));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("DurationParser", e.getMessage()); f.incrementAndGet();
        }

        try {
            DurationLimits limits = DurationLimits.create(60, 60, 24, 28, 12, 5);
            boolean okSec = limits.validate(Duration.ofSeconds(30));
            boolean okDay = limits.validate(Duration.ofDays(2));
            boolean over = limits.validate(Duration.ofDays(3650));
            long applicable = limits.getApplicableLimit(Duration.ofDays(30));
            if (!okSec || !okDay || over || applicable <= 0) {
                BlockProtLogger.fail("DurationLimits", "unexpected validation results");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("DurationLimits: 30s=" + okSec + " 2d=" + okDay + " 10y=" + over
                + " applicable(30d)=" + applicable + "ms");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("DurationLimits", e.getMessage()); f.incrementAndGet();
        }

        try {
            int dist = StringUtil.levenshtein("chest", "chst");
            double sim = StringUtil.similarity("chest", "chest");
            if (dist < 1 || sim <= 0) {
                BlockProtLogger.fail("StringUtil", "unexpected distance/similarity");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("StringUtil: levenshtein(chest,chst)=" + dist + " similarity=" + sim);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("StringUtil", e.getMessage()); f.incrementAndGet();
        }

        try {
            String name = BlockUtil.getHumanReadableBlockName(Material.CHEST);
            if (name == null || name.isBlank()) {
                BlockProtLogger.fail("BlockUtil", "blank readable name for CHEST");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("BlockUtil: getHumanReadableBlockName(CHEST)=" + name);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("BlockUtil", e.getMessage()); f.incrementAndGet();
        }

        try {
            AsyncGuard.assertSync("debug utility group");
            BlockProtLogger.pass("AsyncGuard: assertSync accepted (running on main thread)");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("AsyncGuard", e.getMessage()); f.incrementAndGet();
        }

        try {
            Set<String> candidates = PlayerNameResolver.getNameCandidates(player.getName());
            if (candidates == null || candidates.isEmpty()) {
                BlockProtLogger.fail("PlayerNameResolver", "no candidates for own name");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("PlayerNameResolver: " + candidates.size() + " candidate(s) for " + player.getName());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("PlayerNameResolver", e.getMessage()); f.incrementAndGet();
        }

        try {
            TemporaryActionBar.show(player, "debug", 1L);
            TemporaryActionBar.cancel(player.getUniqueId());
            BlockProtLogger.pass("TemporaryActionBar: show+cancel OK");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("TemporaryActionBar", e.getMessage()); f.incrementAndGet();
        }

        try {
            String stripped = BpDialogStyles.stripColor("&a&lTest");
            if (stripped == null || stripped.contains("&")) {
                BlockProtLogger.fail("BpDialogStyles", "stripColor left a code: '" + stripped + "'");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("BpDialogStyles: stripColor(&a&lTest)=" + stripped
                + " palette=" + BpDialogStyles.SOFT_GRAY + "," + BpDialogStyles.PASTEL_MINT + ","
                + BpDialogStyles.PASTEL_CORAL + "," + BpDialogStyles.PASTEL_GOLD + ","
                + BpDialogStyles.SOFT_BLUE + "," + BpDialogStyles.PASTEL_PURPLE);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("BpDialogStyles", e.getMessage()); f.incrementAndGet();
        }

        try {
            DialogButton back = DialogNavigation.backButton(DialogOrigin.NONE, null);
            DialogButton backAdmin = DialogNavigation.backButton(DialogOrigin.ADMIN_MENU, null);
            if (back == null || backAdmin == null) {
                BlockProtLogger.fail("DialogNavigation", "backButton returned null");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("DialogNavigation: backButton(NONE) id=" + back.id()
                + " backButton(ADMIN_MENU) id=" + backAdmin.id());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("DialogNavigation", e.getMessage()); f.incrementAndGet();
        }

        try {
            DialogState.push(player, ignored -> {});
            boolean popped = DialogState.pop(player);
            DialogState.clear(player);
            BlockProtLogger.pass("DialogState: push/pop=" + popped + " clear OK");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("DialogState", e.getMessage()); f.incrementAndGet();
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
                BlockProtLogger.fail("ReloadReport", "expected a diff between old/new snapshots");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("ReloadReport: capture/compare produced " + diffs.size() + " diff(s)");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("ReloadReport", e.getMessage()); f.incrementAndGet();
        }

        try {
            boolean integrationFlag = IntegrationConfig.getBoolean("debug.integration_test", true);
            BlockProtLogger.pass("IntegrationConfig: getBoolean(default)=" + integrationFlag);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("IntegrationConfig", e.getMessage()); f.incrementAndGet();
        }

        try {
            // Suffix parsing and ranking table documented in gradle.properties:
            //   blank   -> RANK_RELEASE (stable)
            //   BEDev   -> RANK_SNAPSHOT (pre-release); bdev/SNAPSHOT are legacy aliases
            //   hotfix  -> RANK_HOTFIX (ranked above the clean release)
            //   release -> legacy tag suffix normalized to a clean release
            //   exp     -> experimental, never an update
            SemanticVersion stable   = new SemanticVersion("1.3.4");
            SemanticVersion bedev    = new SemanticVersion("1.3.4-BEDev");
            SemanticVersion bdev     = new SemanticVersion("1.3.4-bdev");
            SemanticVersion snap     = new SemanticVersion("1.3.4-SNAPSHOT-3");
            SemanticVersion hotfix   = new SemanticVersion("1.3.4-hotfix");
            SemanticVersion fixN     = new SemanticVersion("1.3.4-fix.1");
            SemanticVersion release  = new SemanticVersion("1.3.4-RELEASE");
            SemanticVersion exp      = new SemanticVersion("1.3.4-exp");
            boolean ranksOk = !stable.isPreRelease() && !stable.isHotfix()
                && bedev.isPreRelease() && bdev.isPreRelease() && snap.isPreRelease()
                && hotfix.isHotfix() && fixN.isHotfix()
                && !release.isPreRelease() && !release.isHotfix()
                && exp.isExperimental();
            boolean orderOk = bedev.compareTo(stable) < 0
                && stable.compareTo(hotfix) < 0
                && bedev.compareTo(bdev) == 0
                && new SemanticVersion("1.3.4-BEDev.2").compareTo(new SemanticVersion("1.3.4-BEDev.1")) > 0
                && stable.compareTo(new SemanticVersion("1.3.5")) < 0
                && stable.compareTo(stable) == 0;
            boolean baseOk = hotfix.baseVersion().equals("1.3.4")
                && bedev.baseVersion().equals("1.3.4");
            if (!ranksOk || !orderOk || !baseOk) {
                BlockProtLogger.fail("SemanticVersion",
                    "ranks=" + ranksOk + " order=" + orderOk + " base=" + baseOk);
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("SemanticVersion: ranks/order/baseVersion OK"
                + " (stable=" + stable + " bedev=" + bedev + " hotfix=" + hotfix + " exp=" + exp + ")");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("SemanticVersion", e.getMessage()); f.incrementAndGet();
        }

        DialogBridgeFactory.setTestBridge(new NoopDialogBridge());
        try {
            touchScreen(DIALOG_PACKAGE, "AdminConfigValueDialog");
            AdminConfigValueDialog.openInt(player, "debug.test", "hint", 0, v -> {}, () -> {});
            AdminConfigValueDialog.openText(player, "debug.test", "hint", "value", s -> null, v -> {}, () -> {});
            BlockProtLogger.pass("AdminConfigValueDialog: openInt/openText routed through test bridge");
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("AdminConfigValueDialog", e.getMessage()); f.incrementAndGet();
        } finally {
            DialogBridgeFactory.setTestBridge(null);
        }
    }

    private void checkNbtSubHandlers(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(NOTCH_UUID);
                h.addFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                boolean hasFriend = h.containsFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                java.util.List<FriendHandler> friends = h.getFriends();
                if (!hasFriend || friends.isEmpty()) {
                    BlockProtLogger.fail("FriendSupportingHandler", "addFriend/containsFriend mismatch");
                    f.incrementAndGet();
                    return;
                }
                FriendHandler first = friends.get(0);
                boolean canRead = first.canRead();
                boolean isManager = first.isManager();
                EnumSet<BlockAccessFlag> flags = BlockAccessFlag.parseFlags(0);
                java.util.List<String> lore = BlockAccessFlag.accumulateAccessFlagLore(flags);
                h.removeFriend("069a79f4-44e9-4726-a5be-fca90e38aaf5");
                BlockProtLogger.pass("FriendSupportingHandler/FriendHandler: contains=" + hasFriend
                    + " friends=" + friends.size() + " canRead=" + canRead + " isManager=" + isManager
                    + " flags=" + flags.size() + " loreLines=" + lore.size());
                p.incrementAndGet();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("FriendSupportingHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
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
                BlockProtLogger.pass("RedstoneSettingsHandler: piston=" + piston + " hopper=" + hopper
                    + " current=" + current + " reset OK");
                p.incrementAndGet();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("RedstoneSettingsHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            BlockCountStatistic stat = new BlockCountStatistic();
            stat.updateContainer((de.tr7zw.changeme.nbtapi.NBTContainer)
                de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
            stat.increment();
            int value = stat.get();
            if (value < 0) {
                BlockProtLogger.fail("BlockCountStatistic", "negative value after increment");
                f.incrementAndGet();
                return;
            }
            BlockProtLogger.pass("BlockCountStatistic: key=" + stat.getKey() + " type=" + stat.getType()
                + " item=" + stat.getItemType() + " value=" + value);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("BlockCountStatistic", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var block = world.getBlockAt(loc);
                var h = new BlockNBTHandler(block);
                h.setOwner(NOTCH_UUID);
                EffectGeometry geometry = EffectGeometry.createForBlock(block);
                if (geometry.getBoundingBox() == null || geometry.getUnionCenter() == null) {
                    BlockProtLogger.fail("EffectGeometry", "null bounding box or union center");
                    f.incrementAndGet();
                    return;
                }
                int perimeter = geometry.getPerimeterPoints(0.5).size();
                BlockProtLogger.pass("EffectGeometry: box=" + geometry.getBoundingBox().getVolume()
                    + " center=" + geometry.getUnionCenter() + " perimeterPoints=" + perimeter);
                p.incrementAndGet();

                ProtectedBlockCache.unmark(block);
                ProtectedBlockCache.mark(block);
                boolean cachedProtected = ProtectedBlockCache.isProtected(block);
                ProtectedBlockCache.unmark(block);
                if (!cachedProtected) {
                    BlockProtLogger.fail("ProtectedBlockCache", "mark() did not make block protected");
                    f.incrementAndGet();
                    return;
                }
                BlockProtLogger.pass("ProtectedBlockCache: mark/isProtected/unmark roundtrip OK, size="
                    + ProtectedBlockCache.size());
                p.incrementAndGet();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("EffectGeometry/ProtectedBlockCache", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var ent = world.spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setSilent(true);
            });
            try {
                if (!EntityProtectionHandler.isSupportedEntity(ent)) {
                    BlockProtLogger.pass("EntityProtectionHandler: ArmorStand not supported, structural check only");
                    p.incrementAndGet();
                    return;
                }
                var handler = EntityProtectionHandler.forEntityOrNull(ent);
                if (handler == null) {
                    BlockProtLogger.pass("EntityProtectionHandler: forEntityOrNull=null for ArmorStand (expected)");
                    p.incrementAndGet();
                    return;
                }
                handler.enable(java.util.UUID.fromString(NOTCH_UUID));
                handler.setNoDamage(false);
                handler.setNoLeash(false);
                boolean ok = handler.getOwner().equals(java.util.UUID.fromString(NOTCH_UUID))
                    && handler.isProtected()
                    && !handler.isNoDamage()
                    && !handler.isNoLeash();
                handler.clear();
                if (!ok) {
                    BlockProtLogger.fail("EntityProtectionHandler", "owner/flags mismatch after enable()");
                    f.incrementAndGet();
                    return;
                }
                BlockProtLogger.pass("EntityProtectionHandler: enable/owner/flags/clear roundtrip OK");
                p.incrementAndGet();
            } finally {
                ent.remove();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("EntityProtectionHandler", e.getMessage()); f.incrementAndGet();
        }

        try {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            try {
                var h = new BlockNBTHandler(world.getBlockAt(loc));
                h.setOwner(NOTCH_UUID);
                LocationListEntry entry = new LocationListEntry(loc);
                if (entry.getBlock() == null || entry.getItemType() == null || entry.getTitle() == null) {
                    BlockProtLogger.fail("LocationListEntry", "null field for constructed entry");
                    f.incrementAndGet();
                    return;
                }
                BlockProtLogger.pass("LocationListEntry: block=" + entry.getBlock().getType()
                    + " item=" + entry.getItemType() + " title=" + entry.getTitle());
                p.incrementAndGet();

                java.util.UUID clipboardOwner = java.util.UUID.fromString(NOTCH_UUID);
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                PlayerInventoryClipboard.set(clipboardOwner.toString(),
                    (de.tr7zw.changeme.nbtapi.NBTContainer) de.tr7zw.changeme.nbtapi.NBT.createNBTObject());
                boolean hasClipboard = PlayerInventoryClipboard.contains(clipboardOwner.toString());
                PlayerInventoryClipboard.remove(clipboardOwner.toString());
                if (!hasClipboard) {
                    BlockProtLogger.fail("PlayerInventoryClipboard", "set() did not register clipboard");
                    f.incrementAndGet();
                    return;
                }
                BlockProtLogger.pass("PlayerInventoryClipboard: set/contains/remove roundtrip OK");
                p.incrementAndGet();
            } finally {
                world.setType(loc, orig);
            }
        } catch (Exception e) {
            BlockProtLogger.fail("LocationListEntry/PlayerInventoryClipboard", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkStructuralClasses(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        String[] names = {
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
        BlockProtLogger.pass("Structural classes: " + loaded + "/" + names.length + " loadable"
            + " (events, gateways, tasks, console, metrics, misc)");
        p.incrementAndGet();
    }

    private void checkEnumeratedCoverage(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        var codeSource = BlockProt.getInstance().getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            BlockProtLogger.log("Class coverage: not running from a jar, enumeration skipped (dev workspace)");
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
            BlockProtLogger.pass("Dialog OK: " + name);
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Dialog FAIL: " + name,
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
                BlockProtLogger.pass("Inventory OK: " + name + " (size=" + result.getSize() + ")");
            } else {
                BlockProtLogger.log("Inventory null/skipped: " + name + " (null is intentional for some paths)");
            }
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Inventory FAIL: " + name,
                e.getClass().getSimpleName() + ": " + e.getMessage());
            f.incrementAndGet();
        }
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
        return sender.isOp() || sender.hasPermission(Permissions.DEBUG.key());
    }
}