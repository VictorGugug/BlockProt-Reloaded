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
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Players and friends category of the dialog-based admin config editor.
 */
public final class AdminConfigPlayersDialog {

    private AdminConfigPlayersDialog() {}

    public static void show(@NotNull Player player, @NotNull DialogOrigin backOrigin) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        Component title = Component.text(
            AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS)),
            AdminConfigDialog.PASTEL_MINT, TextDecoration.BOLD);

        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_PLAYERS), AdminConfigDialog.SOFT_GRAY)));

        List<DialogButton> buttons = new ArrayList<>();
        buttons.add(AdminConfigDialog.toggleBtn("lock_on_place_by_default", "lock_on_place_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__LOCK_ON_PLACE),
            cfg.lockOnPlaceByDefault(),
            p -> { cfg.setLockOnPlaceByDefault(!cfg.lockOnPlaceByDefault()); show(p, backOrigin); }));
        buttons.add(AdminConfigDialog.toggleBtn("public_is_friend_by_default", "public_is_friend_by_default",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__PUBLIC_IS_FRIEND),
            cfg.publicIsFriendByDefault(),
            p -> { cfg.setPublicIsFriendByDefault(!cfg.publicIsFriendByDefault()); show(p, backOrigin); }));

        int maxBlocks = cfg.getMaxLockedBlockCount() != null ? cfg.getMaxLockedBlockCount() : -1;
        buttons.add(AdminConfigDialog.valueBtn("player_max_locked_block_count", "player_max_locked_block_count",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS),
            String.valueOf(maxBlocks),
            p -> AdminConfigValueDialog.openInt(p, "player_max_locked_block_count",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__MAX_BLOCKS_HINT)), maxBlocks,
                v -> { cfg.setPlayerMaxLockedBlockCount(v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        int cooldown = (int) cfg.getLockHintCooldown();
        buttons.add(AdminConfigDialog.valueBtn("lock_hint_cooldown_in_seconds", "lock_hint_cooldown_in_seconds",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN),
            String.valueOf(cooldown),
            p -> AdminConfigValueDialog.openInt(p, "lock_hint_cooldown_in_seconds",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__HINT_COOLDOWN_HINT)), cooldown,
                v -> { cfg.setLockHintCooldown(v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        double similarity = cfg.getFriendSearchSimilarityPercentage();
        buttons.add(AdminConfigDialog.valueBtn("friend_search_similarity", "friend_search_similarity",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH),
            String.valueOf(similarity),
            p -> AdminConfigValueDialog.openDouble(p, "friend_search_similarity",
                AdminConfigDialog.stripColor(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__FRIEND_SEARCH_HINT)), similarity,
                v -> { cfg.setFriendSearchSimilarity(v); show(p, backOrigin); },
                () -> show(p, backOrigin))));

        buttons.add(AdminConfigDialog.toggleBtn("disable_friend_functionality", "disable_friend_functionality",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS_TITLE),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__PLAYERS__DISABLE_FRIENDS),
            cfg.isFriendFunctionalityDisabled(),
            p -> { cfg.setAndSave("disable_friend_functionality", !cfg.isFriendFunctionalityDisabled()); show(p, backOrigin); }));

        AdminConfigDialog.bridgeReturn(player, bridge, title, body, buttons, backOrigin);
    }
}