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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.sean.blockprot.bukkit.nbt.RedstoneSettingsHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BlockInfoDialog {

    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor PASTEL_GOLD = TextColor.color(0xD2B48C);
    private static final TextColor PASTEL_MINT = TextColor.color(0x8FE3B0);
    private static final TextColor PASTEL_CORAL = TextColor.color(0xF0A0A0);
    private static final TextColor SOFT_BLUE = TextColor.color(0xA0C4E8);

    private BlockInfoDialog() {}

    public static void show(@NotNull Player player, @NotNull Block block, @NotNull BlockNBTHandler handler) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__TITLE)),
            SOFT_BLUE, TextDecoration.BOLD
        );

        List<DialogBodyEntry> body = new ArrayList<>();
        String materialName = formatMaterialName(block.getType().name());
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(materialName + " ", NamedTextColor.WHITE, TextDecoration.BOLD))
            .append(Component.text("(" + block.getType().name() + ")", SOFT_GRAY))
            .build()));

        String ownerUuid = handler.getOwner();
        if (ownerUuid != null && !ownerUuid.isEmpty()) {
            String ownerName = getPlayerName(ownerUuid);
            body.add(DialogBodyEntry.text(Component.text()
                .append(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__OWNER_LABEL)), SOFT_GRAY))
                .append(Component.text(" " + (ownerName != null ? ownerName : ownerUuid), NamedTextColor.WHITE))
                .build()));
        }

        List<FriendHandler> friends = handler.getFriends();
        int friendCount = friends.size();
        boolean hasPublic = friends.stream().anyMatch(FriendHandler::doesRepresentPublic);
        body.add(DialogBodyEntry.text(Component.text()
            .append(Component.text(stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__FRIEND_COUNT))
                .replace("{count}", String.valueOf(friendCount)), SOFT_GRAY))
            .build()));
        if (hasPublic) {
            body.add(DialogBodyEntry.text(Component.text(
                "  " + stripColor(Translator.get(TranslationKey.INVENTORIES__FRIENDS__THE_PUBLIC)), TextColor.color(0x888888))));
        }

        if (!handler.isNotProtected()) {
            RedstoneSettingsHandler rs = handler.getRedstoneHandler();
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__REDSTONE_PROTECTION)) + ": "
                    + (rs.getCurrentProtection() ? "●" : "○"), SOFT_GRAY)));
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__HOPPER_PROTECTION)) + ": "
                    + (rs.getHopperProtection() ? "●" : "○"), SOFT_GRAY)));
            body.add(DialogBodyEntry.text(Component.text(
                stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__PISTON_PROTECTION)) + ": "
                    + (rs.getPistonProtection() ? "●" : "○"), SOFT_GRAY)));
        }

        String linkedFrame = handler.getLinkedItemFrameUuid();
        if (linkedFrame != null && !linkedFrame.isEmpty()) {
            body.add(DialogBodyEntry.text(Component.text(
                "  " + stripColor(Translator.get(TranslationKey.INVENTORIES__BLOCK_INFO__LINKED_FRAME)), TextColor.color(0x888888))));
        }

        DialogOrigin exitOrigin = DialogBridgeFactory.resolveOrigin(DialogOrigin.NONE);
        DialogButton exitBtn = new DialogButton("exit",
            Component.text(stripColor(Translator.get(exitOrigin != DialogOrigin.NONE ? TranslationKey.DIALOGS__BACK : TranslationKey.DIALOGS__CLOSE)), SOFT_GRAY),
            Component.text(stripColor(Translator.get(TranslationKey.DIALOGS__RETURN_PREVIOUS)), TextColor.color(0x888888)),
            exitOrigin != DialogOrigin.NONE ? p -> BlockLockDialog.show(p, block, handler) : null);

        bridge.showMultiAction(player, title, body, List.of(exitBtn), null, 1);
    }

    private static String getPlayerName(String uuidStr) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            if (op.getName() != null) return op.getName();
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private static String stripColor(String s) {
        return s.replaceAll("[§&][0-9a-fk-orxA-F]", "");
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
