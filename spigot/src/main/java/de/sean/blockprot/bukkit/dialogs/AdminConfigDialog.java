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

package de.sean.blockprot.bukkit.dialogs;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.LangConfig;
import de.sean.blockprot.bukkit.inventories.AnvilInput;
import de.sean.blockprot.bukkit.inventories.SignInput;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AdminConfigDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);
    private static final TextColor PASTEL_PURPLE = TextColor.color(0xC8A0E0);

    private AdminConfigDialog() {}

    public static void show(@NotNull Player player) {
        show(player, DialogOrigin.ADMIN_MENU);
    }

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        showCategories(player, backOrigin);
    }

    private static void showCategories(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__HINT)), TextColor.color(0x888888))));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE), "Language", p -> showLanguage(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS), "Worlds", p -> showWorlds(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS), "Players and friends", p -> showPlayers(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS), "Blocks and locking", p -> showBlocks(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY), "Entity protection", p -> showEntity(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY), "Expiry", p -> showExpiry(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID), "Raid detection", p -> showRaid(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS), "Notifications", p -> showNotif(p, backOrigin)));
        buttons.add(catBtn(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE), "Maintenance", p -> showMaintenance(p, backOrigin)));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__RETURN_ADMIN_MENU)), TextColor.color(0x888888)),
            exitOrigin == DialogOrigin.NONE ? p -> {} : p -> AdminMenuDialog.show(p));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 3);
    }

    // -- Language --
    private static void showLanguage(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__STATUS_HEADER)), SOFT_GRAY)));

        BlockProt plugin = BlockProt.getInstance();
        String currentLang = cfg.getLanguageFile();
        String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

        List<DialogButton> buttons = new ArrayList<>();

        boolean anyDisabled = false;
        for (String lang : allLangs) {
            if (!LangConfig.isLanguageEnabled(lang)) {
                anyDisabled = true;
                break;
            }
        }
        boolean allEnabled = !anyDisabled;
        String toggleLabel = allEnabled
            ? stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_DISABLE))
            : stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_ENABLE));
        String toggleHint = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_HINT));
        buttons.add(new DialogButton("toggle_all",
            Component.text()
                .append(Component.text(allEnabled ? "○ " : "● ", allEnabled ? PASTEL_CORAL : PASTEL_MINT))
                .append(Component.text(toggleLabel, NamedTextColor.WHITE))
                .build(),
            Component.text(toggleHint, TextColor.color(0x888888)),
            p -> {
                for (String lang : allLangs) {
                    LangConfig.setLanguageEnabled(lang, !allEnabled);
                }
                showLanguage(p, backOrigin);
            }));

        for (String lang : allLangs) {
            boolean isConfigLang = lang.equals(currentLang);
            boolean isEnabled = LangConfig.isLanguageEnabled(lang);
            String label = getLanguageLabel(plugin, lang);

            TextColor c = isEnabled ? PASTEL_MINT : PASTEL_CORAL;
            Component labelComp = isConfigLang
                ? Component.text().append(Component.text(label, isEnabled ? NamedTextColor.WHITE : SOFT_GRAY))
                    .append(Component.text(" "
                        + stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__ACTIVE_MARKER)),
                        PASTEL_GOLD)).build()
                : Component.text(label, isEnabled ? NamedTextColor.WHITE : SOFT_GRAY);
            String langStatus = isEnabled ? "enabled" : "disabled";
            String configStatus = isConfigLang
                ? stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_ACTIVE))
                : stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_INACTIVE));
            String clickAction = isEnabled
                ? stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_DISABLE))
                : stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_ENABLE));
            buttons.add(new DialogButton("lang_" + lang,
                Component.text()
                    .append(Component.text(isEnabled ? "● " : "○ ", c))
                    .append(labelComp)
                    .build(),
                Component.join(JoinConfiguration.newlines(),
                    Component.text(stripColor(Translator.get(
                        TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__LANG_STATUS))
                        .replace("{status}", langStatus), c),
                    Component.text("config.yml: " + configStatus, isConfigLang ? PASTEL_GOLD : SOFT_GRAY),
                    Component.text(clickAction, TextColor.color(0x888888))),
                p -> {
                    if (isEnabled) {
                        LangConfig.setLanguageEnabled(lang, false);
                    } else {
                        LangConfig.setLanguageEnabled(lang, true);
                        cfg.setLanguageFile(lang);
                    }
                    showLanguage(p, backOrigin);
                }));
        }

        buttons.add(toggleBtn("replace_translations", "replace_translations",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__REPLACE_TRANSLATIONS),
            cfg.shouldReplaceTranslations(),
            p -> { cfg.setAndSave("replace_translations", !cfg.shouldReplaceTranslations()); showLanguage(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    private static @Nullable YamlConfiguration loadLanguageFile(@NotNull BlockProt plugin, @NotNull String fileName) {
        File diskFile = new File(plugin.getDataFolder(), "lang/" + fileName);
        try {
            if (diskFile.exists()) {
                return YamlConfiguration.loadConfiguration(diskFile);
            }
            InputStream jarStream = plugin.getResource("lang/" + fileName);
            if (jarStream == null) return null;
            return YamlConfiguration.loadConfiguration(
                new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getLanguageName(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return fileName;
        String name = langFile.getString("language_name");
        if (name != null && !name.isEmpty()) return name;
        return langFile.getString("locale", fileName);
    }

    private static int computeCompletion(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return 0;
        return Translator.computeCompletionPercentage(langFile);
    }

    private static String getLanguageLabel(@NotNull BlockProt plugin, @NotNull String fileName) {
        return getLanguageName(plugin, fileName) + " (" + computeCompletion(plugin, fileName) + "%)";
    }

    // -- Worlds --
    private static void showWorlds(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS)),
            SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_WORLDS), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("per_worlds_config", "per_worlds_config",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__WORLDS__PER_WORLDS_CONFIG),
            cfg.isPerWorldsConfigEnabled(),
            p -> { cfg.setAndSave("per_worlds_config", !cfg.isPerWorldsConfigEnabled()); showWorlds(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Players and friends --
    private static void showPlayers(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS)),
            PASTEL_MINT, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("lock_on_place_by_default", "lock_on_place_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE),
            cfg.lockOnPlaceByDefault(),
            p -> { cfg.setLockOnPlaceByDefault(!cfg.lockOnPlaceByDefault()); showPlayers(p, backOrigin); }));
        buttons.add(toggleBtn("public_is_friend_by_default", "public_is_friend_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND),
            cfg.publicIsFriendByDefault(),
            p -> { cfg.setPublicIsFriendByDefault(!cfg.publicIsFriendByDefault()); showPlayers(p, backOrigin); }));

        int maxBlocks = cfg.getMaxLockedBlockCount() != null ? cfg.getMaxLockedBlockCount() : -1;
        buttons.add(valueBtn("player_max_locked_block_count", "player_max_locked_block_count",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS),
            String.valueOf(maxBlocks),
            p -> openIntInput(player, "player_max_locked_block_count", "Max blocks (-1 = unlimited)", v -> {
                cfg.setPlayerMaxLockedBlockCount(v);
                showPlayers(p, backOrigin);
            })));

        int cooldown = (int) cfg.getLockHintCooldown();
        buttons.add(valueBtn("lock_hint_cooldown_in_seconds", "lock_hint_cooldown_in_seconds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN),
            String.valueOf(cooldown),
            p -> openIntInput(player, "lock_hint_cooldown_in_seconds", "Cooldown in seconds", v -> {
                cfg.setLockHintCooldown(v);
                showPlayers(p, backOrigin);
            })));

        double similarity = cfg.getFriendSearchSimilarityPercentage();
        buttons.add(valueBtn("friend_search_similarity", "friend_search_similarity",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH),
            String.valueOf(similarity),
            p -> openDoubleInput(player, "friend_search_similarity", "Similarity (0.0 - 1.0)", v -> {
                cfg.setFriendSearchSimilarity(v);
                showPlayers(p, backOrigin);
            })));

        buttons.add(toggleBtn("disable_friend_functionality", "disable_friend_functionality",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS),
            cfg.isFriendFunctionalityDisabled(),
            p -> { cfg.setAndSave("disable_friend_functionality", !cfg.isFriendFunctionalityDisabled()); showPlayers(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Blocks and locking behavior --
    private static void showBlocks(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS)),
            PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_BLOCKS), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("modern_family_blocks", "modern_family_blocks",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__MODERN_FAMILY),
            cfg.isModernFamilyBlocks(),
            p -> { cfg.setAndSave("modern_family_blocks", !cfg.isModernFamilyBlocks()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("redstone_disallowed_by_default", "redstone_disallowed_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__REDSTONE_DISALLOWED),
            cfg.disallowRedstoneOnPlace(),
            p -> { cfg.setRedstoneDisallowedByDefault(!cfg.disallowRedstoneOnPlace()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("simplified_hopper_logic", "simplified_hopper_logic",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SIMPLIFIED_HOPPER),
            cfg.isSimplifiedHopperLogic(),
            p -> { cfg.setSimplifiedHopperLogic(!cfg.isSimplifiedHopperLogic()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("protect_locked_blocks_from_explosions", "protect_locked_blocks_from_explosions",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PROTECT_EXPLOSIONS),
            cfg.shouldProtectLockedBlocksFromExplosions(),
            p -> { cfg.setProtectFromExplosions(!cfg.shouldProtectLockedBlocksFromExplosions()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("block_protected_block_piston_movement", "block_protected_block_piston_movement",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__PISTON_MOVEMENT),
            cfg.shouldBlockProtectedBlockPistonMovement(),
            p -> { cfg.setBlockPistonMovement(!cfg.shouldBlockProtectedBlockPistonMovement()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("clear_protection_on_shulker_break", "clear_protection_on_shulker_break",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SHULKER_BREAK),
            cfg.shouldClearProtectionOnShulkerBreak(),
            p -> { cfg.setClearProtectionOnShulkerBreak(!cfg.shouldClearProtectionOnShulkerBreak()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("allow_break_protected_blocks", "allow_break_protected_blocks",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__ALLOW_BREAK),
            cfg.shouldAllowBreakProtectedBlocks(),
            p -> { cfg.setAllowBreakProtectedBlocks(!cfg.shouldAllowBreakProtectedBlocks()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("respect_spawn_protection", "respect_spawn_protection",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__SPAWN_PROTECTION),
            cfg.shouldRespectSpawnProtection(),
            p -> { cfg.setRespectSpawnProtection(!cfg.shouldRespectSpawnProtection()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("block_lock_effects", "block_lock_effects",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_EFFECTS),
            cfg.isLockEffectEnabled(),
            p -> { cfg.setLockEffects(!cfg.isLockEffectEnabled()); showBlocks(p, backOrigin); }));
        buttons.add(toggleBtn("block_lock_sounds", "block_lock_sounds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__LOCK_SOUNDS),
            cfg.isLockSoundEnabled(),
            p -> { cfg.setLockSounds(!cfg.isLockSoundEnabled()); showBlocks(p, backOrigin); }));
        boolean useMenus = cfg.getBukkitConfig().getBoolean("use_menus", false);
        buttons.add(dialogToggleBtn("use_menus", "use_menus [Dialog]",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_MENUS_DE),
            useMenus,
            p -> { cfg.setAndSave("use_menus", !useMenus); showBlocks(p, backOrigin); }));
        buttons.add(dialogToggleBtn("use_dialogs", "use_dialogs [Dialog]",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__USE_DIALOGS_DE),
            cfg.isDialogsEnabled(),
            p -> { cfg.setAndSave("use_dialogs", !cfg.isDialogsEnabled()); showBlocks(p, backOrigin); }));
        int timedAccessDays = cfg.getBukkitConfig().getInt("timed_access_max_duration_days", 90);
        buttons.add(valueBtn("timed_access_max_duration_days", "timed_access_max_duration_days",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__BLOCKS__TIMED_ACCESS),
            String.valueOf(timedAccessDays),
            p -> openIntInput(player, "timed_access_max_duration_days", "Max days", v -> {
                cfg.setAndSave("timed_access_max_duration_days", v);
                showBlocks(p, backOrigin);
            })));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Entity protection --
    private static void showEntity(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_ENTITY), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("entity_protection.enabled", "entity_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__PROTECTION_ENABLED),
            cfg.isEntityProtectionEnabled(),
            p -> { cfg.setEntityProtectionEnabled(!cfg.isEntityProtectionEnabled()); showEntity(p, backOrigin); }));
        buttons.add(toggleBtn("entity_protection.auto_protect_on_tame", "entity_protection.auto_protect_on_tame",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__AUTO_PROTECT_TAME),
            cfg.isEntityProtectionAutoProtectOnTame(),
            p -> { cfg.setAndSave("entity_protection.auto_protect_on_tame", !cfg.isEntityProtectionAutoProtectOnTame()); showEntity(p, backOrigin); }));
        buttons.add(toggleBtn("villager_workstation_protection.enabled", "villager_workstation_protection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__ENTITY__WORKSTATION_ENABLED),
            cfg.isVillagerWorkstationProtectionEnabled(),
            p -> { cfg.setAndSave("villager_workstation_protection.enabled", !cfg.isVillagerWorkstationProtectionEnabled()); showEntity(p, backOrigin); }));

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ? 
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                returnHint(),
            exitOrigin == DialogOrigin.NONE ? p -> {} : p -> showCategories(p, backOrigin));

        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    // -- Expiry --
    private static void showExpiry(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY)),
            SOFT_BLUE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_EXPIRY), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("world_expiry.enabled", "world_expiry.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__EXPIRY__ENABLED),
            cfg.isWorldExpiryEnabled(),
            p -> { cfg.setWorldExpiryEnabled(!cfg.isWorldExpiryEnabled()); showExpiry(p, backOrigin); }));

        int interval = cfg.getWorldExpiryCheckInterval();
        buttons.add(valueBtn("world_expiry.check_interval_minutes", "world_expiry.check_interval_minutes",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__EXPIRY__CHECK_INTERVAL),
            String.valueOf(interval),
            p -> openIntInput(player, "world_expiry.check_interval_minutes", "Minutes between checks", v -> {
                cfg.setAndSave("world_expiry.check_interval_minutes", v);
                showExpiry(p, backOrigin);
            })));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Raid detection --
    private static void showRaid(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID)),
            PASTEL_CORAL, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_RAID), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        boolean raidEnabled = BlockProt.getInstance().getConfig().getBoolean("raid_detection.enabled", true);
        buttons.add(toggleBtn("raid_detection.enabled", "raid_detection.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RAID__ENABLED),
            raidEnabled,
            p -> { cfg.setAndSave("raid_detection.enabled", !raidEnabled); showRaid(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Notifications --
    private static void showNotif(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS)),
            PASTEL_PURPLE, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_NOTIFICATIONS), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("notify_op_of_updates", "notify_op_of_updates",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__NOTIFY_OPS),
            cfg.shouldNotifyOpOfUpdates(),
            p -> { cfg.setNotifyOpOfUpdates(!cfg.shouldNotifyOpOfUpdates()); showNotif(p, backOrigin); }));
        buttons.add(toggleBtn("owner_notifications.enabled", "owner_notifications.enabled",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__NOTIFICATIONS__OWNER_ENABLED),
            cfg.isOwnerNotificationsEnabled(),
            p -> { cfg.setAndSave("owner_notifications.enabled", !cfg.isOwnerNotificationsEnabled()); showNotif(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- Maintenance --
    private static void showMaintenance(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE)),
            PASTEL_GOLD, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_MAINTENANCE), SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(toggleBtn("auto_reload_configs", "auto_reload_configs",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__AUTO_RELOAD),
            cfg.isAutoReloadEnabled(),
            p -> { cfg.setAutoReloadConfigs(!cfg.isAutoReloadEnabled()); showMaintenance(p, backOrigin); }));
        buttons.add(toggleBtn("enable_session_log", "enable_session_log",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__SESSION_LOG),
            cfg.isSessionLogEnabled(),
            p -> { cfg.setSessionLogEnabled(!cfg.isSessionLogEnabled()); showMaintenance(p, backOrigin); }));
        buttons.add(toggleBtn("enable_backups", "enable_backups",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__MAINTENANCE__BACKUPS),
            cfg.isBackupsEnabled(),
            p -> { cfg.setBackupsEnabled(!cfg.isBackupsEnabled()); showMaintenance(p, backOrigin); }));

        bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }

    // -- helpers --

    private static DialogButton valueBtn(String id, String configKey, String label, String currentValue,
                                          DialogButton.DialogClickHandler clickAction) {
        return new DialogButton(id,
            Component.text()
                .append(Component.text(configKey, NamedTextColor.WHITE))
                .append(Component.text(": ", SOFT_GRAY))
                .append(Component.text(currentValue, PASTEL_GOLD, TextDecoration.BOLD))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(label, SOFT_GRAY),
                Component.text("Current: " + currentValue, TextColor.color(0x888888)),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CLICK_EDIT)), TextColor.color(0x888888))),
            clickAction);
    }

    private static void openIntInput(@NotNull Player player, @NotNull String configKey,
                                      @NotNull String hint,
                                      @NotNull java.util.function.Consumer<Integer> onSet) {
        String current = BlockProt.getDefaultConfig().getBukkitConfig().getString(configKey, "");
        String prompt = "§7Current " + configKey + ": §f" + current + " §7- Enter new value:";
        player.closeInventory();
        if (SignInput.isSupported()) {
            SignInput.open(player, BlockProt.getInstance(), "new " + configKey, input -> {
                if (input == null || input.isBlank()) return;
                try {
                    int val = Integer.parseInt(input.trim());
                    onSet.accept(val);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number: " + input);
                }
            });
        } else {
            AnvilInput.open(player, BlockProt.getInstance(), current, prompt, input -> {
                if (input == null || input.isBlank()) return;
                try {
                    int val = Integer.parseInt(input.trim());
                    onSet.accept(val);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number: " + input);
                }
            });
        }
    }

    private static void openDoubleInput(@NotNull Player player, @NotNull String configKey,
                                         @NotNull String hint,
                                         @NotNull java.util.function.Consumer<Double> onSet) {
        String current = BlockProt.getDefaultConfig().getBukkitConfig().getString(configKey, "");
        player.closeInventory();
        if (SignInput.isSupported()) {
            SignInput.open(player, BlockProt.getInstance(), "new " + configKey, input -> {
                if (input == null || input.isBlank()) return;
                try {
                    double val = Double.parseDouble(input.trim());
                    onSet.accept(val);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number: " + input);
                }
            });
        } else {
            AnvilInput.open(player, BlockProt.getInstance(), current, hint, input -> {
                if (input == null || input.isBlank()) return;
                try {
                    double val = Double.parseDouble(input.trim());
                    onSet.accept(val);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number: " + input);
                }
            });
        }
    }

    private static DialogButton catBtn(String label, String desc, DialogButton.DialogClickHandler handler) {
        return new DialogButton(label.toLowerCase().replace(' ', '_'),
            Component.text(label, NamedTextColor.WHITE),
            Component.text(desc, TextColor.color(0x888888)),
            handler);
    }

    private static DialogButton toggleBtn(String id, String configKey, String label, boolean active,
                                           DialogButton.DialogClickHandler handler) {
        TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
        return new DialogButton(id,
            Component.text()
                .append(Component.text(active ? "● " : "○ ", c))
                .append(Component.text(configKey, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(label, SOFT_GRAY),
                Component.text(active ? "● true" : "○ false", c),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_BOOL)), TextColor.color(0x888888))),
            handler);
    }

    private static DialogButton dialogToggleBtn(String id, String configKey, String label, boolean active,
                                                  DialogButton.DialogClickHandler handler) {
        TextColor c = active ? PASTEL_MINT : PASTEL_CORAL;
        String marker = stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__DIALOG_MARKER));
        return new DialogButton(id,
            Component.text()
                .append(Component.text(active ? "● " : "○ ", c))
                .append(Component.text(configKey, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(marker, PASTEL_GOLD),
                Component.text(label, SOFT_GRAY),
                Component.text(active ? "● true" : "○ false", c),
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_BOOL)), TextColor.color(0x888888))),
            handler);
    }

    private static void bridgeReturn(@NotNull Player player, @NotNull DialogBridge bridge,
                                       @NotNull Component title, @NotNull List<DialogBodyEntry> body,
                                       @NotNull List<DialogButton> buttons, @NotNull DialogOrigin backOrigin) {
        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(backOrigin);
        DialogButton backBtn = new DialogButton("back",
            Component.text(stripColor(Translator.get(exitOrigin == DialogOrigin.NONE ? TranslationKey.DIALOGS__CLOSE : TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            exitOrigin == DialogOrigin.NONE ? 
                Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__CLOSE)), TextColor.color(0x888888)) :
                returnHint(),
            exitOrigin == DialogOrigin.NONE ? p -> {} : p -> showCategories(p, backOrigin));
        bridge.showMultiAction(player, title, body, buttons, backBtn, 2);
    }

    private static Component tooltip(String desc) {
        return Component.text(desc, TextColor.color(0x888888));
    }

    private static Component returnHint() {
        return Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__RETURN_CATEGORIES)), TextColor.color(0x888888));
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orx]", "");
    }
}
