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

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chat-based text input for servers that lack Paper's {@code AsyncChatEvent}
 * (plain Spigot/CraftBukkit). Mirrors {@link ChatInput} exactly, but listens
 * on the legacy {@link AsyncPlayerChatEvent}, which every Bukkit-family
 * server implements. Only references standard Bukkit API classes, so it is
 * safe to load on any server software.
 *
 * <p>Together with {@link ChatInput}, this replaces the earlier anvil-inventory
 * fallback as the default text-input path everywhere in the plugin: the
 * anvil GUI had been observed to open and close in a loop and crash on some
 * Spigot builds. Use {@link TextInput#open} rather than this class directly.
 */
public final class LegacyChatInput implements Listener {

    private final UUID playerUuid;
    private final Plugin plugin;
    private final @Nullable Consumer<String> onConfirm;
    private final String cancelWord;
    private boolean consumed = false;

    private LegacyChatInput(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable String promptSubject,
            @Nullable Consumer<String> onConfirm
    ) {
        this.playerUuid = player.getUniqueId();
        this.plugin = plugin;
        this.onConfirm = onConfirm;
        this.cancelWord = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCEL_WORD).trim().toLowerCase();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.closeInventory();

        String prompt;
        if (promptSubject != null) {
            String suffix = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_SUFFIX)
                .replace("{cancel}", cancelWord);
            String subject = promptSubject.endsWith(":") ? promptSubject.substring(0, promptSubject.length() - 1) : promptSubject;
            prompt = subject + " " + suffix;
        } else {
            prompt = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_PROMPT)
                .replace("{cancel}", cancelWord);
        }
        ComponentMessages.sendLegacyActionBar(player, prompt);

        // Schedule expiry after 15 seconds (300 ticks).
        BlockProt.getFoliaLib().getScheduler().runLater(() -> {
            if (!consumed) {
                unregister();
            }
        }, 300L);
    }

    public static void open(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable Consumer<String> onConfirm
    ) {
        new LegacyChatInput(player, plugin, null, onConfirm);
    }

    public static void open(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @NotNull String promptSubject,
            @Nullable Consumer<String> onConfirm
    ) {
        new LegacyChatInput(player, plugin, promptSubject, onConfirm);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUuid)) return;
        event.setCancelled(true);

        final String text = event.getMessage().trim();

        if (text.equalsIgnoreCase(cancelWord)) {
            ComponentMessages.sendLegacy(
                event.getPlayer(),
                Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCELLED)
            );
            unregister();
            return;
        }

        unregister();

        BlockProt.getFoliaLib().getScheduler().runNextTick(task -> {
            if (onConfirm != null) onConfirm.accept(text);
        });
    }

    private void unregister() {
        if (!consumed) {
            consumed = true;
            HandlerList.unregisterAll(this);
        }
    }
}
