/*
 * Copyright (C) 2021 - 2026 spnda
 * Modifications Copyright (C) 2025 - 2026 Zaynr (Zar)
 * This file is part of BlockProt Reloaded <https://github.com/VictorGugug/BlockProt-Reloaded>.
 * Based on BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt Reloaded is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt Reloaded is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt Reloaded. If not, see <https://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.integrations;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtLogger;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.events.BlockAccessMenuEvent;
import de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lands integration. Registers a custom role-flag, "Protect Containers (BlockProt)",
 * that land owners can grant or deny per role. When configured to restrict access,
 * players without that flag in the area covering a protected block are denied
 * locking or accessing it. Wilderness (unclaimed land) is never restricted.
 *
 * <p>The custom flag must be registered while Lands is loaded but not yet enabled,
 * so registration happens in {@link #load()}, called from {@code BlockProt.onLoad()}
 * for every registered integration, mirroring the timing requirement WorldGuard has
 * for its own custom flags.
 */
public final class LandsPluginIntegration extends PluginIntegration implements Listener {

    private static final String FLAG_NAME = "blockprot_protect_containers";
    private static final String FRIENDS_FLAG_NAME = "blockprot_require_protect_for_friends";
    private static final String ALLOW_IN_WILDERNESS = "allow_protecting_containers_in_wilderness";
    private static final String REQUIRE_PROTECT_FOR_FRIENDS = "require_protect_for_friends_flag";

    @Nullable
    private LandsIntegration lands;

    @Nullable
    private RoleFlag protectFlag;

    @Nullable
    private RoleFlag requireProtectForFriendsFlag;

    private boolean enabled = false;

    public LandsPluginIntegration() {
        super("lands");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void load() {
        final Plugin plugin = getPlugin();
        if (plugin == null) return;
        try {
            lands = LandsIntegration.of(BlockProt.getInstance());
            protectFlag = RoleFlag.of(lands, FlagTarget.PLAYER, RoleFlagCategory.ACTION, FLAG_NAME);
            BlockProtLogger.log("integration", "Lands: registered custom role-flag '" + FLAG_NAME + "'.");

            if (isRequireProtectForFriendsEnabled()) {
                requireProtectForFriendsFlag = RoleFlag.of(lands, FlagTarget.PLAYER, RoleFlagCategory.ACTION, FRIENDS_FLAG_NAME);
                BlockProtLogger.log("integration", "Lands: registered custom role-flag '" + FRIENDS_FLAG_NAME + "'.");
            }
        } catch (NoClassDefFoundError | IllegalStateException e) {
            // IllegalStateException: flag already registered by a prior BlockProt session
            // on this same server run (e.g. a plugin reload). Safe to ignore.
            lands = null;
            protectFlag = null;
            requireProtectForFriendsFlag = null;
        }
    }

    @Override
    public void enable() {
        if (!configuration.getBoolean("enabled", true)) return;
        final Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled() || lands == null) {
            return;
        }
        if (protectFlag != null) {
            protectFlag.setDisplayName(Translator.get(TranslationKey.INTEGRATIONS__LANDS__PROTECT_CONTAINERS_FLAG_NAME));
            protectFlag.setDescription(Translator.get(TranslationKey.INTEGRATIONS__LANDS__PROTECT_CONTAINERS_DESC));
        }
        if (requireProtectForFriendsFlag != null) {
            requireProtectForFriendsFlag.setDisplayName(Translator.get(TranslationKey.INTEGRATIONS__LANDS__REQUIRE_PROTECT_FOR_FRIENDS_FLAG_NAME));
            requireProtectForFriendsFlag.setDescription(Translator.get(TranslationKey.INTEGRATIONS__LANDS__REQUIRE_PROTECT_FOR_FRIENDS_DESC));
        }
        this.registerListener(this);
        enabled = true;
    }

    @Nullable
    @Override
    public Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("Lands");
    }

    @Override
    public void reload() {
        super.reload();
        if (protectFlag != null) {
            protectFlag.setDisplayName(Translator.get(TranslationKey.INTEGRATIONS__LANDS__PROTECT_CONTAINERS_FLAG_NAME));
            protectFlag.setDescription(Translator.get(TranslationKey.INTEGRATIONS__LANDS__PROTECT_CONTAINERS_DESC));
        }
        if (requireProtectForFriendsFlag != null) {
            requireProtectForFriendsFlag.setDisplayName(Translator.get(TranslationKey.INTEGRATIONS__LANDS__REQUIRE_PROTECT_FOR_FRIENDS_FLAG_NAME));
            requireProtectForFriendsFlag.setDescription(Translator.get(TranslationKey.INTEGRATIONS__LANDS__REQUIRE_PROTECT_FOR_FRIENDS_DESC));
        }
    }

    private boolean isAllowedInWilderness() {
        return configuration.getBoolean(ALLOW_IN_WILDERNESS, true);
    }

    private boolean isRequireProtectForFriendsEnabled() {
        return configuration.getBoolean(REQUIRE_PROTECT_FOR_FRIENDS, false);
    }

    /**
     * True if {@code who} holds the "Protect Containers (BlockProt)" role-flag for
     * the claimed area covering {@code block}. Unclaimed wilderness follows the
     * {@code allow_protecting_containers_in_wilderness} config option instead.
     */
    private boolean hasProtectFlag(@NotNull Player who, @NotNull Block block) {
        if (lands == null || protectFlag == null) return true;
        Location location = block.getLocation();
        LandWorld world = lands.getWorld(block.getWorld());
        if (world == null) return true; // Lands not enabled in this world
        var area = world.getArea(location);
        if (area == null) return isAllowedInWilderness();
        return area.hasRoleFlag(who, protectFlag, block.getType(), true);
    }

    @Override
    protected boolean filterFriendByUuid(@NotNull final UUID friend,
                                          @NotNull final Player player,
                                          @NotNull final Block block) {
        if (lands == null || requireProtectForFriendsFlag == null) return true;
        Location location = block.getLocation();
        LandWorld world = lands.getWorld(block.getWorld());
        if (world == null) return true;
        var area = world.getArea(location);
        if (area == null) return true;
        if (!area.hasRoleFlag(player, requireProtectForFriendsFlag, block.getType(), true)) return true;
        var friendPlayer = lands.getLandPlayer(friend);
        if (friendPlayer == null) return false;
        return area.hasRoleFlag(player, protectFlag, block.getType(), true)
            && area.hasRoleFlag(friendPlayer, protectFlag, block.getType(), true);
    }

    @EventHandler
    public void onAccess(@NotNull final BlockAccessEvent event) {
        if (!hasProtectFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAccessEditMenu(@NotNull final BlockAccessMenuEvent event) {
        if (!hasProtectFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLockOnPlace(@NotNull final BlockLockOnPlaceEvent event) {
        if (!hasProtectFlag(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
