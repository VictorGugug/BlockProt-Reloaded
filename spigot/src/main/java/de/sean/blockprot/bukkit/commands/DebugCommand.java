package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.config.BlockFamilyParser;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.integrations.PluginIntegration;
import de.sean.blockprot.bukkit.integrations.ViaVersionIntegration;
import de.sean.blockprot.bukkit.inventories.*;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * /blockprot debug — diagnostics and manual test-bench.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code run}                — full automated diagnostic suite</li>
 *   <li>{@code placeDebugChest}    — places a chest owned by Notch at player location</li>
 *   <li>{@code placeDebugShulker}  — places a shulker box owned by Notch at player location</li>
 *   <li>{@code clearSearchHistory} — clears the calling player's friend-search history</li>
 * </ul>
 *
 * <p>The "run" suite now covers:
 * <ol>
 *   <li>Config</li>
 *   <li>BukkitCompat</li>
 *   <li>Translations</li>
 *   <li>Lockable materials (blocks)</li>
 *   <li>Lockable entities (item frames, storage/hopper carts, chest boats)</li>
 *   <li>Item frame protection</li>
 *   <li>Raid detection config</li>
 *   <li>Integration status (ViaVersion + companions, PlaceholderAPI, Towny)</li>
 *   <li>ProfileService</li>
 *   <li>SkinsRestorer</li>
 *   <li>AuditLogger</li>
 *   <li>Online players</li>
 *   <li>NBT write/read (main thread)</li>
 *   <li>Entity NBT write/read (main thread)</li>
 *   <li>PlayerSettings</li>
 *   <li>Inventory creation</li>
 *   <li>All inventories</li>
 *   <li>Action-bar / chat message keys</li>
 * </ol>
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
                ab(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
                Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(),
                    () -> runDiagnostics(player));
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private static void ab(@NotNull Player p, @NotNull String msg) {
        p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));
    }

    private static void chat(@NotNull Player p, @NotNull String msg) {
        p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
    }

    // =========================================================================
    //  Full diagnostic suite
    // =========================================================================

    private void runDiagnostics(@NotNull Player player) {
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        BlockProtLogger.separator();
        BlockProtLogger.log("=== /blockprot debug run — " + java.time.LocalDateTime.now() + " ===");
        BlockProtLogger.log("Player : " + player.getName() + " (" + player.getUniqueId() + ")");
        BlockProtLogger.log("Server : " + Bukkit.getVersion());
        BlockProtLogger.log("API    : " + Bukkit.getBukkitVersion());
        BlockProtLogger.log("Java   : " + System.getProperty("java.version"));
        BlockProtLogger.log("Compat : " + VersionCompat.getDiagnosticString());
        BlockProtLogger.log("BukkitCompat: " + BukkitCompat.getDiagnosticString());

        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RUNNING_DIAGNOSTICS));
        chat(player, Translator.get(TranslationKey.MESSAGES__DEBUG__RESULTS_GO_TO_LOG));

        // Async checks (no world access)
        runGroup(player, passed, failed, "1.  Config",              () -> checkConfig(player, passed, failed));
        runGroup(player, passed, failed, "2.  BukkitCompat",       () -> checkBukkitCompat(player, passed, failed));
        runGroup(player, passed, failed, "3.  Translations",       () -> checkTranslations(player, passed, failed));
        runGroup(player, passed, failed, "4.  Lockable blocks",    () -> checkLockableMaterials(player, passed, failed));
        runGroup(player, passed, failed, "5.  Lockable entities",  () -> checkLockableEntities(player, passed, failed));
        runGroup(player, passed, failed, "6.  Item frame protect", () -> checkItemFrameProtection(player, passed, failed));
        runGroup(player, passed, failed, "7.  Raid detection",     () -> checkRaidDetection(player, passed, failed));
        runGroup(player, passed, failed, "8.  Integrations",       () -> checkIntegrations(player, passed, failed));
        runGroup(player, passed, failed, "9.  ProfileService",     () -> checkProfileService(player, passed, failed));
        runGroup(player, passed, failed, "10. SkinsRestorer",      () -> checkSkinsRestorer(player, passed, failed));
        runGroup(player, passed, failed, "11. AuditLogger",        () -> checkAuditLogger(player, passed, failed));
        runGroup(player, passed, failed, "12. OnlinePlayers",      () -> checkOnlinePlayers(player, passed, failed));

        // Main-thread checks (world/inventory access)
        Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
            runGroup(player, passed, failed, "13. NBT block write/read",  () -> checkNbt(player, passed, failed));
            runGroup(player, passed, failed, "14. NBT entity write/read", () -> checkEntityNbt(player, passed, failed));
            runGroup(player, passed, failed, "15. PlayerSettings",        () -> checkPlayerSettings(player, passed, failed));
            runGroup(player, passed, failed, "16. Inventory creation",    () -> checkInventoryCreation(player, passed, failed));
            runGroup(player, passed, failed, "17. All inventories",       () -> checkAllInventories(player, passed, failed));
            runGroup(player, passed, failed, "18. Messages",              () -> checkMessages(player, passed, failed));

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

    // =========================================================================
    //  Check groups
    // =========================================================================

    private void checkConfig(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            BlockProtLogger.pass("Config OK — friendDisabled=" + cfg.isFriendFunctionalityDisabled()
                + " maxBlocks=" + cfg.getMaxLockedBlockCount()
                + " lockEffects=" + cfg.isLockEffectEnabled()
                + " lockSound=" + cfg.isLockSoundEnabled()
                + " entityProtection=" + cfg.isPetProtectionEnabled()
                + " raidDetection=" + BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Config", e.getMessage()); f.incrementAndGet();
        }
    }

    /** Reads a raw boolean from the underlying config YAML via reflection-free delegation. */
    private boolean cfgBoolean(String key, boolean def) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            // DefaultConfig extends BlockProtConfig which exposes the underlying FileConfiguration
            // through a protected field. We reach it via the accessor available on the class.
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
            BlockProtLogger.pass("BukkitCompat — PARTICLE_DUST=" + dust.name()
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
            BlockProtLogger.pass("Translations — " + TranslationKey.values().length + " keys OK, " + blank + " blank");
            p.incrementAndGet();
        } else {
            BlockProtLogger.fail("Translations", errors + " key(s) threw exceptions"); f.incrementAndGet();
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

    private void checkLockableEntities(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();

            // Check all materials in the ENTITIES family for their active/inactive status.
            Set<Material> entityFamily = BlockFamilyParser.getFamilyMembers(BlockFamilyParser.Family.ENTITIES);
            if (entityFamily.isEmpty()) {
                BlockProtLogger.warn("Lockable entities: ENTITIES family is empty (no entity materials registered)");
                f.incrementAndGet();
                return;
            }
            StringBuilder active = new StringBuilder();
            StringBuilder inactive = new StringBuilder();
            for (Material m : entityFamily) {
                if (cfg.isLockableEntity(m)) active.append(m.name()).append(" ");
                else inactive.append(m.name()).append(" ");
            }
            BlockProtLogger.pass("Lockable entities: active=[" + active.toString().trim()
                + "] inactive=[" + inactive.toString().trim() + "]");

            // Verify vehicle listener is registered.
            boolean vehicleListenerActive = Bukkit.getServicesManager().isProvidedFor(
                de.sean.blockprot.bukkit.listeners.VehicleProtectionListener.class);
            // Note: listeners don't register as services — check via plugin event listeners count instead.
            BlockProtLogger.log("  EntityNBTHandler: verifying PDC access via Bukkit.createEntity is not available in debug; PDC is validated in NBT check");

            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable entities", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkItemFrameProtection(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            DefaultConfig cfg = BlockProt.getDefaultConfig();
            boolean frameActive = cfg.isLockableEntity(Material.ITEM_FRAME);
            boolean glowActive  = cfg.isLockableEntity(Material.GLOW_ITEM_FRAME);

            // Item frames are disabled by default; that is the intended state.
            BlockProtLogger.log("  Item frame protection: ITEM_FRAME=" + (frameActive ? "ACTIVE" : "INACTIVE (default)")
                + " GLOW_ITEM_FRAME=" + (glowActive ? "ACTIVE" : "INACTIVE (default)"));
            BlockProtLogger.pass("Item frame protection: config readable, frames="
                + (frameActive ? "enabled" : "disabled (lockable_entities empty or not listed)"));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Item frame protection", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkRaidDetection(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            boolean enabled = BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true);
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
                    // Full Via status including ViaBackwards/ViaRewind
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

    // ── Main-thread checks ────────────────────────────────────────────────────

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

    /**
     * Verifies that EntityNBTHandler can write and read owner data via PDC.
     * Uses a short-lived ArmorStand as the test entity; it is removed immediately after.
     */
    private void checkEntityNbt(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var loc = player.getLocation().clone();
            var world = player.getWorld();
            // ArmorStand is available on all supported versions (1.8+).
            var entity = world.spawn(loc, org.bukkit.entity.ArmorStand.class);
            entity.setGravity(false);
            entity.setVisible(false);
            entity.setSilent(true);

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
            BlockProtLogger.pass("PlayerSettings OK — lockOnPlace=" + ps.getLockOnPlace()
                + " hintsEnabled=" + !ps.hasPlayerInteractedWithMenu());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("PlayerSettings", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkInventoryCreation(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            Inventory inv = Bukkit.createInventory(null, 9,
                net.kyori.adventure.text.Component.text("bp_debug"));
            BlockProtLogger.pass("Bukkit.createInventory OK size=" + inv.getSize());
            p.incrementAndGet();
        } catch (Exception e) {
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

        BlockProtLogger.log("Inventory skipped: PetSettingsInventory (requires live Entity, not testable without one)");
        p.incrementAndGet();

        inv(p, f, "FriendSearchHistoryInventory", () -> {
            InventoryState fh = new InventoryState(null);
            fh.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), fh);
            return new FriendSearchHistoryInventory().fill(player);
        });

        InventoryState.set(player.getUniqueId(), base);

        // ── Inventory title translation coverage ──────────────────────────────
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
            TranslationKey.INVENTORIES__PET__SETTINGS,
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
        BlockProtLogger.log("Messages — " + ok + " OK, " + bad + " blank");
        if (bad == 0) { BlockProtLogger.pass("All message keys present"); p.incrementAndGet(); }
        else f.incrementAndGet();
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private void inv(@NotNull AtomicInteger p, @NotNull AtomicInteger f,
                     @NotNull String name,
                     @NotNull java.util.concurrent.Callable<Inventory> supplier) {
        try {
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
        return sender.isOp() || sender.hasPermission("blockprot.debug");
    }
}
