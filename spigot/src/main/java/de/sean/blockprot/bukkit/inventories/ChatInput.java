/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
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
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chat-based text input. Closes the player's inventory, shows a prompt in chat,
 * and calls onConfirm on the main thread. Typing the cancel word aborts silently.
 * The input session expires automatically after 15 seconds.
 *
 * <p>Tab-completion via {@code AsyncTabCompleteEvent} is not supported on Paper 26.x
 * (the class was removed). The prompt works normally without it.
 *
 * <p>Requires Paper. On plain Spigot servers the anvil-based fallback
 * ({@link AnvilInput}) is used instead.
 */
public final class ChatInput implements Listener {

    private final UUID playerUuid;
    private final Plugin plugin;
    private final @Nullable Consumer<String> onConfirm;
    private final String cancelWord;
    private boolean consumed = false;

    private ChatInput(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable Consumer<String> onConfirm
    ) {
        this.playerUuid = player.getUniqueId();
        this.plugin = plugin;
        this.onConfirm = onConfirm;
        this.cancelWord = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCEL_WORD).trim().toLowerCase();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.closeInventory();

        String prompt = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_PROMPT)
            .replace("{cancel}", cancelWord);
        // Send prompt only in action bar to avoid chat spam.
        var component = LegacyComponentSerializer.legacySection().deserialize(prompt);
        player.sendActionBar(component);

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
        new ChatInput(player, plugin, onConfirm);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUuid)) return;
        event.setCancelled(true);

        final String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (text.equalsIgnoreCase(cancelWord)) {
            event.getPlayer().sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCELLED)
            ));
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
