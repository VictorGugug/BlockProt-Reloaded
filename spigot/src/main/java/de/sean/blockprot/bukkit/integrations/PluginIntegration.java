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

package de.sean.blockprot.bukkit.integrations;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A plugin integration that can register new event listeners and
 * add functionality to bring compatibility with another Bukkit
 * plugin. Integrations should be always registered and only its
 * listeners and other functionality loaded in the {@link #enable()}
 * function when the dependent plugin is loaded.
 *
 * @since 0.4.0
 */
public abstract class PluginIntegration {
    /**
     * The {@link YamlConfiguration} that backs this plugin
     * integration. The name supplied in {@link #PluginIntegration(String)}
     * is used as the filename to read this configuration.
     *
     * @since 0.4.0
     */
    @NotNull
    protected YamlConfiguration configuration;

    /**
     * {@link BlockProt}'s plugin manager to use to get the
     * instance of the plugin this is trying to integrate.
     *
     * @since 0.4.0
     */
    @NotNull
    private final PluginManager pluginManager;

    @NotNull
    public final String name;

    /**
     * Creates a new plugin integration.
     *
     * @param name      The name of the integration, as well as the name of the config file.
     * @param hasConfig Whether this integration has an optional config file in integrations/.
     *                  Pass {@code false} for integrations that need no config (e.g. ViaVersion)
     *                  to avoid a console warning about a missing file.
     * @since 0.4.0
     */
    public PluginIntegration(@NotNull final String name, boolean hasConfig) {
        this.name = name;
        if (hasConfig) {
            configuration =
                BlockProt.getInstance().saveAndLoadConfigFile("integrations/", name + ".yml", false);
        } else {
            configuration = new YamlConfiguration();
        }
        pluginManager = BlockProt.getInstance().getServer().getPluginManager();
    }

    /**
     * Creates a new plugin integration that has a config file.
     *
     * @param name The name of the integration, as well as the name of the config file.
     * @since 0.4.0
     */
    public PluginIntegration(@NotNull final String name) {
        this(name, true);
    }

    /**
     * This lets all registered plugin integrations filter out friends that they don't want
     * players to add to {@code block}.This method will check {@link PluginIntegration#filterFriendByUuid(UUID, Player, Block)}
     * and {@link PluginIntegration#filterFriendsInternal(ArrayList, Player, Block)} for backwards
     * compatibility. Note that we also use reflection to determine which of these functions to
     * call and therefore {@link PluginIntegration#filterFriendByUuidForAll(UUID, Player, Block)}
     * might be a more performant option.
     *
     * @param friendsInput The initial (default) list of friends that can be added. This ArrayList
     *                     is not modified within this function.
     * @param player       The player that is trying to add these friends.
     * @param block        The block these friends are being added to.
     * @see #filterFriendByUuidForAll(UUID, Player, Block)
     * @return The new filtered list.
     * @since 0.4.0
     * @deprecated
     */
    @CheckReturnValue
    @Deprecated
    public static @NotNull ArrayList<OfflinePlayer> filterFriends(@NotNull final ArrayList<OfflinePlayer> friendsInput,
                                                                   @NotNull final Player player,
                                                                   @NotNull final Block block) {
        var friends = new ArrayList<>(friendsInput); // Copy

        for (var integration : BlockProt.getInstance().getIntegrations()) {
            if (!integration.isEnabled())
                continue;

            try {
                var clazz = integration.getClass();

                if (clazz.equals(clazz.getMethod("filterFriendsByUUID", ArrayList.class, Player.class, Block.class).getDeclaringClass())) {
                    // The integration overrides filterFriendsByUUID. We prefer using that over
                    // filterFriendsInternal now that it is deprecated.
                    friends = new ArrayList<>(friends.stream()
                            .filter(p -> integration.filterFriendByUuid(p.getUniqueId(), player, block))
                            .toList());
                } else {
                    // filterFriendsByUUID was not overriden.
                    integration.filterFriendsInternal(friends, player, block);
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return friends;
    }

    /**
     * This lets all registered plugin integrations filter out friends that
     * they don't want players to add to {@code block}. This method will exclusively
     * check {@link PluginIntegration#filterFriendByUuid(UUID, Player, Block)}.
     *
     * @param friend The initial (default) list of friends that can be added. This ArrayList
     *                     is not modified within this function.
     * @param player       The player that is trying to add these friends.
     * @param block        The block these friends are being added to.
     * @return The new filtered list.
     * @since 1.1.0
     */
    public static boolean filterFriendByUuidForAll(@NotNull final UUID friend,
                                                   @NotNull final Player player,
                                                   @NotNull final Block block) {
        for (var integration : BlockProt.getInstance().getIntegrations()) {
            if (!integration.isEnabled())
                continue;

            // If any integration returns false, meaning that friend should be filtered out,
            // we return false instantly.
            if (!integration.filterFriendByUuid(friend, player, block))
                return false;
        }

        return true;
    }

    public abstract boolean isEnabled();

    public void load() {

    }

    public abstract void enable();

    /**
     * Called when the BlockProt plugin reloads configs and translations. Optimally, translations
     * should be updated and new config values should be updated in this function.
     * @since 1.0.7
     */
    public void reload() {
        configuration =
            BlockProt.getInstance().saveAndLoadConfigFile("integrations/", name + ".yml", false);
    }

    @Nullable
    public abstract Plugin getPlugin();

    /**
     * An integration can freely override this function to change the friends
     * that can be added for a {@code block} by {@code player}.
     *
     * @param friends The initial (default) list of friends that can be added.
     * @param player  The player that is trying to add these friends.
     * @param block   The block these friends are being added to.
     * @since 0.4.0
     */
    @Deprecated
    protected void filterFriendsInternal(@NotNull final ArrayList<OfflinePlayer> friends,
                                         @NotNull final Player player,
                                         @NotNull final Block block) {
    }

    /**
     * An integration can freely override this function to filter which friends can be added to a
     * {@code block} by {@code player}. In general, this function should be used together with
     * {@link java.util.stream.Stream#filter(Predicate)}.
     *
     * @param friend The UUID of the player to be checked for as a friend.
     * @param player The player that is trying to add these friends.
     * @param block  The block these friends are being added to.
     * @return True if the friend is allowed to be added, false if it should be filtered out of
     * the list.
     * @since 1.1.0
     */
    protected boolean filterFriendByUuid(@NotNull final UUID friend,
                                         @NotNull final Player player,
                                         @NotNull final Block block) {
        // By default, we allow all friends.
        return true;
    }

    protected void registerListener(@NotNull final Listener listener) {
        pluginManager.registerEvents(listener, BlockProt.getInstance());
    }
}