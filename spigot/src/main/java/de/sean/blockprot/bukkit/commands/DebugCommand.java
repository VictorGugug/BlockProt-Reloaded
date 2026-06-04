package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.VersionCompat;
import de.sean.blockprot.bukkit.inventories.*;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Subcommands:
 *   run                — full automated diagnostic suite (actionbar: short, chat: detail)
 *   placeDebugChest    — places a chest owned by Notch at the player's location
 *   placeDebugShulker  — places a shulker box owned by Notch at the player's location
 *   clearSearchHistory — clears the calling player's friend-search history
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

        // Run each group sequentially. Groups that touch world/inventory must run on main thread.
        runGroup(player, passed, failed, "1. Config",         () -> checkConfig(player, passed, failed));
        runGroup(player, passed, failed, "2. BukkitCompat",  () -> checkBukkitCompat(player, passed, failed));
        runGroup(player, passed, failed, "3. Translations",  () -> checkTranslations(player, passed, failed));
        runGroup(player, passed, failed, "4. Lockable mats", () -> checkLockableMaterials(player, passed, failed));
        runGroup(player, passed, failed, "5. ProfileService",() -> checkProfileService(player, passed, failed));
        runGroup(player, passed, failed, "6. SkinsRestorer", () -> checkSkinsRestorer(player, passed, failed));
        runGroup(player, passed, failed, "7. AuditLogger",   () -> checkAuditLogger(player, passed, failed));
        runGroup(player, passed, failed, "8. OnlinePlayers", () -> checkOnlinePlayers(player, passed, failed));

        // Main-thread groups
        Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
            runGroup(player, passed, failed, "9.  NBT write/read",      () -> checkNbt(player, passed, failed));
            runGroup(player, passed, failed, "10. PlayerSettings",      () -> checkPlayerSettings(player, passed, failed));
            runGroup(player, passed, failed, "11. Inventory creation",  () -> checkInventoryCreation(player, passed, failed));
            runGroup(player, passed, failed, "12. All inventories",     () -> checkAllInventories(player, passed, failed));
            runGroup(player, passed, failed, "13. Action-bar/chat msg", () -> checkMessages(player, passed, failed));

            // Final summary
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
    //  Individual check groups
    // =========================================================================

    private void checkConfig(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            BlockProtLogger.pass("Config OK — friendDisabled=" + cfg.isFriendFunctionalityDisabled()
                + " maxBlocks=" + cfg.getMaxLockedBlockCount()
                + " lockEffects=" + cfg.isLockEffectEnabled()
                + " lockSound=" + cfg.isLockSoundEnabled()
                + " petProtection=" + cfg.isPetProtectionEnabled());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Config", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkBukkitCompat(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            // Verify the fields resolve without exception and have a non-null value
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
                    blank++;
                    BlockProtLogger.warn("Translation blank: " + key.name());
                }
            } catch (Exception e) {
                errors++;
                BlockProtLogger.fail("Translation key " + key.name(), e.getMessage());
            }
        }
        if (errors == 0) {
            BlockProtLogger.pass("Translations — " + TranslationKey.values().length
                + " keys OK, " + blank + " blank");
            p.incrementAndGet();
        } else {
            BlockProtLogger.fail("Translations", errors + " key(s) threw exceptions");
            f.incrementAndGet();
        }
    }

    private void checkLockableMaterials(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var cfg = BlockProt.getDefaultConfig();
            Material[] check = {
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.FURNACE, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
                Material.SHULKER_BOX, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.BLAST_FURNACE, Material.SMOKER
            };
            StringBuilder sb = new StringBuilder();
            for (Material m : check)
                sb.append(m.name()).append("=").append(cfg.isLockable(m)).append(" ");
            BlockProtLogger.pass("Lockable materials: " + sb.toString().trim());
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("Lockable materials", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkProfileService(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var profile = BlockProt.getProfileService().findByUuid(player.getUniqueId());
            BlockProtLogger.pass("ProfileService OK: " + (profile != null ? profile.getName() : "null (but no exception)"));
            p.incrementAndGet();
        } catch (Exception e) {
            BlockProtLogger.fail("ProfileService", e.getMessage()); f.incrementAndGet();
        }
    }

    private void checkSkinsRestorer(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        var plugin = Bukkit.getPluginManager().getPlugin("SkinsRestorer");
        if (plugin == null)           BlockProtLogger.log("SkinsRestorer: not installed");
        else if (!plugin.isEnabled()) BlockProtLogger.warn("SkinsRestorer installed but disabled");
        else {
            // Use Paper 1.20+ PluginMeta when available; fall back to getDescription() otherwise.
            String ver;
            try {
                ver = plugin.getPluginMeta().getVersion();
            } catch (NoSuchMethodError e) {
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

    // -- Main-thread checks ---------------------------------------------------

    private void checkNbt(@NotNull Player player, AtomicInteger p, AtomicInteger f) {
        try {
            var loc  = player.getLocation().clone();
            var world = player.getWorld();
            var orig = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var h = new BlockNBTHandler(world.getBlockAt(loc));
            h.setOwner(NOTCH_UUID);
            h.setName("debug_nbt_test");
            String owner = h.getOwner();
            String name  = h.getName();
            long lockedAt = h.getLockedAt();
            world.setType(loc, orig);

            if (NOTCH_UUID.equals(owner) && "debug_nbt_test".equals(name)) {
                BlockProtLogger.pass("NBT write/read OK (owner=" + owner + " name=" + name + " lockedAt=" + lockedAt + ")");
                p.incrementAndGet();
            } else {
                BlockProtLogger.fail("NBT mismatch", "owner=" + owner + " name=" + name);
                f.incrementAndGet();
            }
        } catch (Exception e) {
            BlockProtLogger.fail("NBT block", e.getMessage()); f.incrementAndGet();
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
        // Set up a neutral InventoryState so inventories don't NPE
        InventoryState base = new InventoryState(null);
        base.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
        base.origin = InventoryState.MenuOrigin.NONE;
        InventoryState.set(player.getUniqueId(), base);

        // ── Inventories that need no block context ────────────────────────────
        inv(p, f, "UserMenuInventory",
            () -> new UserMenuInventory().fill(player));

        inv(p, f, "AdminMenuInventory",
            () -> new AdminMenuInventory().fill(player));

        inv(p, f, "UserSettingsInventory",
            () -> new UserSettingsInventory().fill(player));

        inv(p, f, "StatisticsInventory",
            () -> new StatisticsInventory().fill(player));

        inv(p, f, "FriendManageInventory (default)",
            () -> new FriendManageInventory().fill(player));


        // StatisticListInventory — needs a filled statistic
        inv(p, f, "StatisticListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.currentPageIndex = 0;
            InventoryState.set(player.getUniqueId(), ns);
            @SuppressWarnings("unchecked")
            de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<
                de.sean.blockprot.nbt.stats.ListStatisticItem<?, Material>, ?> castedStat =
                (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<
                    de.sean.blockprot.nbt.stats.ListStatisticItem<?, Material>, ?>)
                (de.sean.blockprot.bukkit.nbt.stats.BukkitListStatistic<?, ?>) stat;
            return new StatisticListInventory().fill(player, castedStat);
        });

        // AdminBlockListInventory
        inv(p, f, "AdminBlockListInventory", () -> {
            PlayerBlocksStatistic stat = new PlayerBlocksStatistic();
            StatHandler.getStatistic(stat, player);
            InventoryState ns = new InventoryState(null);
            ns.origin = InventoryState.MenuOrigin.ADMIN_MENU;
            InventoryState.set(player.getUniqueId(), ns);
            return new AdminBlockListInventory().fill(player, player.getName(), stat);
        });

        // RedstoneSettingsInventory — place a temporary chest, lock it, open redstone
        inv(p, f, "RedstoneSettingsInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            var h = new BlockNBTHandler(block);
            h.setOwner(player.getUniqueId().toString());
            InventoryState rs = new InventoryState(block);
            rs.friendSearchState = InventoryState.FriendSearchState.FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), rs);
            Inventory result = new RedstoneSettingsInventory().fill(player, rs);
            world.setType(loc, orig);
            return result;
        });

        // BlockLockInventory — place a temporary chest, lock it
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

        // BlockInfoInventory — same chest
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

        // BlockInspectContentsInventory — use a chest
        inv(p, f, "BlockInspectContentsInventory", () -> {
            var loc   = player.getLocation().clone();
            var world = player.getWorld();
            var orig  = world.getBlockAt(loc).getType();
            world.setType(loc, Material.CHEST);
            var block = world.getBlockAt(loc);
            var h = new BlockNBTHandler(block);
            h.setOwner(player.getUniqueId().toString());
            InventoryState bic = new InventoryState(block);
            InventoryState.set(player.getUniqueId(), bic);
            Inventory result = new BlockInspectContentsInventory(player).fill();
            world.setType(loc, orig);
            return result;
        });

        // AuditInventory — no entries case
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

        // PetSettingsInventory — requiere Player + Entity, skip en debug
        BlockProtLogger.log("Inventory skipped: PetSettingsInventory (requires live Entity, not testable without one)");
        p.incrementAndGet();

        // FriendSearchHistoryInventory
        inv(p, f, "FriendSearchHistoryInventory", () -> {
            InventoryState fh = new InventoryState(null);
            fh.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), fh);
            return new FriendSearchHistoryInventory().fill(player);
        });

        // Restore neutral state
        InventoryState.set(player.getUniqueId(), base);

        // ── Inventory title translation coverage ──────────────────────────────
        BlockProtLogger.separator();
        BlockProtLogger.log("--- Inventory title translation coverage ---");
        TranslationKey[] titleKeys = {
            TranslationKey.INVENTORIES__BLOCK_LOCK,
            TranslationKey.INVENTORIES__BLOCK_INFO,
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
        // Verify that actionbar and chat message keys exist and are non-blank
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
            if (v == null || v.isBlank()) {
                BlockProtLogger.fail("ActionBar msg blank", k.name()); bad++;
            } else { ok++; }
        }
        for (TranslationKey k : chatKeys) {
            String v = Translator.get(k);
            if (v == null || v.isBlank()) {
                BlockProtLogger.fail("Chat msg blank", k.name()); bad++;
            } else { ok++; }
        }
        BlockProtLogger.log("Messages — " + ok + " OK, " + bad + " blank");
        if (bad == 0) { BlockProtLogger.pass("All message keys present"); p.incrementAndGet(); }
        else { f.incrementAndGet(); }
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
