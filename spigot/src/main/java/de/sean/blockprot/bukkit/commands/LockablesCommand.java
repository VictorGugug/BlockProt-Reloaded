package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import de.sean.blockprot.bukkit.inventories.LockablesInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * CLI command: /bp lockables
 * Opens the lockable blocks browser. Requires blockprot.user.admin or OP.
 */
public final class LockablesCommand implements CommandExecutor {

    @Override
    public boolean canUseCommand(@NotNull CommandSender sender) {
        return sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!canUseCommand(sender)) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__NO_PERMISSION));
            return true;
        }
        InventoryState state = InventoryState.builder()
            .origin(InventoryState.MenuOrigin.NONE)
            .build();
        state.currentPageIndex = 0;
        InventoryState.set(player.getUniqueId(), state);
        player.openInventory(new LockablesInventory().fill(player, 0));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
