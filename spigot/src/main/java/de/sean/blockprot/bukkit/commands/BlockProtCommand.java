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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Main dispatcher for /blockprot (alias /bp).
 *
 * <p>Visibility rules:
 * <ul>
 *   <li><b>use_menus=true</b>  — tab-complete shows only {@code user} and {@code admin}.
 *       Any other subcommand is blocked with a usage hint.</li>
 *   <li><b>use_menus=false</b> — tab-complete shows all CLI commands (help, settings,
 *       friends, stats, etc.). {@code user} and {@code admin} are hidden and blocked.</li>
 * </ul>
 */
public final class BlockProtCommand implements TabExecutor {

    private static final Map<String, CommandExecutor> GUI_COMMANDS   = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> CLI_COMMANDS   = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> ADMIN_COMMANDS = new LinkedHashMap<>();
    private static final Map<String, CommandExecutor> ALL_COMMANDS   = new LinkedHashMap<>();

    static {
        gui("user",         new UserMenuCommand());
        gui("admin",        new AdminMenuCommand());

        cli("help",         new HelpCommand());
        cli("settings",     new SettingsCommand());
        cli("friends",      new FriendsAddAllCommand());
        cli("stats",        new StatisticsCommand());
        cli("transferall",  new TransferCommand());
        cli("about",        new AboutCommand());
        cli("disablehints", new HintsCommand());
        cli("info",         new InfoCommand());
        cli("reload",       new ReloadCommand());
        cli("update",       new UpdateCommand());
        cli("integrations", new IntegrationsCommand());
        cli("debug",        new DebugCommand());
        cli("unlock",       new AdminUnlockCommand());
        cli("protdel",      new WorldProtDeleteCommand());

        admin("lockables",  new LockablesCommand());
        admin("recommended", new RecommendedCommand());
    }

    private static void gui(String name, CommandExecutor exec) {
        GUI_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    private static void cli(String name, CommandExecutor exec) {
        CLI_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    private static void admin(String name, CommandExecutor exec) {
        ADMIN_COMMANDS.put(name, exec);
        ALL_COMMANDS.put(name, exec);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        boolean menusEnabled = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();

        if (args.length == 0) {
            if (menusEnabled) {
                CommandExecutor exec = (sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key()))
                    ? GUI_COMMANDS.get("admin") : GUI_COMMANDS.get("user");
                return exec != null && exec.onCommand(sender, command, label, args);
            }
            CommandExecutor help = CLI_COMMANDS.get("help");
            return help != null && help.onCommand(sender, command, label, args);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        CommandExecutor adminExec = ADMIN_COMMANDS.get(sub);
        if (adminExec != null) {
            return adminExec.onCommand(sender, command, label, args);
        }

        if (menusEnabled) {
            CommandExecutor exec = GUI_COMMANDS.get(sub);
            if (exec == null) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    Translator.get(TranslationKey.MESSAGES__CMD_USAGE_MENUS)));
                return true;
            }
            return exec.onCommand(sender, command, label, args);
        } else {
            CommandExecutor exec = CLI_COMMANDS.get(sub);
            if (exec == null) {
                CommandExecutor help = CLI_COMMANDS.get("help");
                if (help != null) help.onCommand(sender, command, label, args);
                return true;
            }
            return exec.onCommand(sender, command, label, args);
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (args.length > 1) {
            CommandExecutor exec = ALL_COMMANDS.get(args[0].toLowerCase(Locale.ROOT));
            if (exec != null) {
                List<String> sub = exec.onTabComplete(sender, command, alias, args);
                return sub != null ? sub : Collections.emptyList();
            }
            return Collections.emptyList();
        }

        boolean menusEnabled = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();
        Map<String, CommandExecutor> visible = menusEnabled ? GUI_COMMANDS : CLI_COMMANDS;
        Map<String, CommandExecutor> combined = new LinkedHashMap<>(visible);
        combined.putAll(ADMIN_COMMANDS);

        String partial = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        List<String> result = new ArrayList<>();
        for (var entry : combined.entrySet()) {
            if (entry.getKey().startsWith(partial) && entry.getValue().canUseCommand(sender))
                result.add(entry.getKey());
        }
        return result;
    }
}