package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.Permissions;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Displays the live BlockProt command list using the active language file.
 * Commands shown are based on the current mode (use_menus config and player permissions).
 *
 * @since SP26
 */
public final class HelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        send(sender, Translator.get(TranslationKey.HELP__HEADER));

        final boolean useMenus = !BlockProt.getDefaultConfig().areExtraCommandsEnabled();
        final boolean isOp = sender.isOp();
        final boolean isAdmin = isOp || sender.hasPermission(Permissions.USER_ADMIN.key());
        final boolean hasDebug = sender.hasPermission("blockprot.debug");

        if (useMenus) {
            send(sender, Translator.get(TranslationKey.HELP__GUI_MODE_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__GUI_USER));
            if (isAdmin) {
                send(sender, Translator.get(TranslationKey.HELP__GUI_ADMIN));
            }
        } else {
            send(sender, Translator.get(TranslationKey.HELP__BLOCK_PROTECTION_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__BLOCK_PROTECTION_SETTINGS));

            send(sender, Translator.get(TranslationKey.HELP__FRIENDS_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__FRIENDS_GUI));
            send(sender, Translator.get(TranslationKey.HELP__FRIENDS_ADDALL));
            send(sender, Translator.get(TranslationKey.HELP__TRANSFER));

            send(sender, Translator.get(TranslationKey.HELP__OTHER_TITLE));
            send(sender, Translator.get(TranslationKey.HELP__SETTINGS));
            send(sender, Translator.get(TranslationKey.HELP__STATS));
            send(sender, Translator.get(TranslationKey.HELP__ABOUT));
            send(sender, Translator.get(TranslationKey.HELP__DISABLE_HINTS));

            if (isAdmin) {
                send(sender, Translator.get(TranslationKey.HELP__ADMIN_INFO));
                send(sender, Translator.get(TranslationKey.HELP__UNLOCK));
            }
            if (isOp) {
                send(sender, Translator.get(TranslationKey.HELP__INTEGRATIONS));
                send(sender, Translator.get(TranslationKey.HELP__UPDATE));
                send(sender, Translator.get(TranslationKey.HELP__RELOAD));
            }
            if (hasDebug) {
                send(sender, Translator.get(TranslationKey.HELP__DEBUG));
            }
        }

        send(sender, Translator.get(TranslationKey.HELP__FOOTER));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private void send(@NotNull CommandSender sender, @NotNull String text) {
        if (text.isBlank()) return;
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
