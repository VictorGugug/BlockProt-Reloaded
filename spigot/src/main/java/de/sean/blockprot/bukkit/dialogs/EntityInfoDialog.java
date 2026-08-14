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
import de.sean.blockprot.bukkit.nbt.EntityNBTHandler;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_GOLD;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_PURPLE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EntityInfoDialog {

    private EntityInfoDialog() {}

    public static void show(@NotNull Player player, @NotNull Entity entity, @NotNull EntityNBTHandler handler) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        String entityName = formatMaterialName(entity.getType().name());
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(entityName, NamedTextColor.WHITE, TextDecoration.BOLD))
            .build()));

        String ownerUuid = handler.getOwner();
        if (ownerUuid != null && !ownerUuid.isEmpty()) {
            String ownerName = getPlayerName(ownerUuid);
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)), SOFT_GRAY))
                .append(Component.text(" " + (ownerName != null ? ownerName : ownerUuid), NamedTextColor.WHITE))
                .build()));
        }

        List<String> friends = handler.getFriendUuids();
        int friendCount = friends.size();
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__FRIEND_COUNT))
                .replace("{count}", String.valueOf(friendCount)), SOFT_GRAY))
            .build()));

        for (String friendUuid : friends) {
            String name = getPlayerName(friendUuid);
            boolean isManager = handler.isManager(friendUuid);
            body.add(DialogBodyEntry.text(Component.text(
                "  " + (name != null ? name : friendUuid) + (isManager ? stripColor(Translator.get(TranslationKey.INVENTORIES__ENTITY__MANAGER_SUFFIX)) : ""),
                TextColor.color(0x888888))));
        }

        // Always returns to the parent BlockLockDialog: this is one level of internal
        // navigation within the same entity menu, not an external-origin exit, so it must
        // not be gated by DialogBridgeFactory.resolveOrigin()/areExtraCommandsEnabled().
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__BACK)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            p -> BlockLockDialog.showForEntity(player, entity, handler));

        bridge.showMultiAction(player, title, body, List.of(exitBtn), null, 1);
    }

    private static String getPlayerName(String uuidStr) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (op.getName() != null) return op.getName();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private static String formatMaterialName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') { sb.append(' '); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }
}
