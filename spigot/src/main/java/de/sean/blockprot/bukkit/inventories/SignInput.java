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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Opens the native sign editor so the player can type text on line 0.
 *
 * Compatible with Paper/Spigot 1.20.x through 26.1.x.
 * Zero NMS — uses only the public Bukkit API.
 *
 * Strategy:
 *   1. Temporarily set a real OAK_WALL_SIGN at a safe location above the player.
 *   2. Set the prompt text on the sign's front side.
 *   3. Call player.openSign(sign, Side.FRONT) — public API since 1.20.
 *   4. Restore the original block type immediately after opening.
 *   5. Listen for SignChangeEvent to capture line 0 as the user's input.
 *   6. Cancel the event so no sign text is actually saved to the world.
 */
public final class SignInput implements Listener {

    /** Returns true when the server supports player.openSign() (1.20+, always true for our compile target). */
    public static boolean isSupported() {
        try {
            Class.forName("org.bukkit.block.sign.Side");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private final UUID playerUuid;
    private final Location fakeSignLocation;
    private final Material originalType;
    private final @Nullable Consumer<String> onConfirm;
    private boolean consumed = false;

    private SignInput(
        @NotNull Player player,
        @NotNull Plugin plugin,
        @NotNull String prompt,
        @Nullable Consumer<String> onConfirm
    ) {
        this.playerUuid = player.getUniqueId();
        this.onConfirm  = onConfirm;

        Location feet = player.getLocation().getBlock().getLocation();
        this.fakeSignLocation = feet.clone().add(0, 2, 0);

        Block block = fakeSignLocation.getBlock();
        this.originalType = block.getType();

        block.setType(Material.OAK_WALL_SIGN, false);

        try {
            Sign sign = (Sign) block.getState();
            if (!prompt.isEmpty()) {
                Component promptComponent = LegacyComponentSerializer.legacySection().deserialize(prompt);
                sign.getSide(Side.FRONT).line(0, promptComponent);
                sign.update(true, false);
            }

            Bukkit.getPluginManager().registerEvents(this, plugin);
            player.openSign(sign, Side.FRONT);
        } catch (Exception e) {
            block.setType(originalType, false);
            HandlerList.unregisterAll(this);
            return;
        }

        block.setType(originalType, false);
    }

    /**
     * Opens the sign editor for the given player.
     *
     * @param player    Player to open the editor for.
     * @param plugin    Owning plugin (for listener registration).
     * @param prompt    Text shown on line 0 as a hint (max 15 chars for clients).
     * @param onConfirm Called with the trimmed line-0 text when the player confirms.
     */
    public static void open(
        @NotNull Player player,
        @NotNull Plugin plugin,
        @NotNull String prompt,
        @Nullable Consumer<String> onConfirm
    ) {
        new SignInput(player, plugin, prompt, onConfirm);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(@NotNull SignChangeEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUuid)) return;
        if (!event.getBlock().getLocation().equals(fakeSignLocation)) return;

        event.setCancelled(true);

        Component lineComp = event.line(0);
        String result = lineComp != null
            ? LegacyComponentSerializer.legacySection().serialize(lineComp).trim()
            : "";
        result = result.replaceAll("\u00a7r$", "").trim();

        unregister();
        if (onConfirm != null) onConfirm.accept(result);
    }

    private void unregister() {
        if (!consumed) {
            consumed = true;
            HandlerList.unregisterAll(this);
        }
    }
}