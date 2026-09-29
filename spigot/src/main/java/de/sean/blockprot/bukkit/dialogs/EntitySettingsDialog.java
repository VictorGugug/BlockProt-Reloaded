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
import de.sean.blockprot.bukkit.entities.EntityProtectionHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.RegionTasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_CORAL;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.PASTEL_MINT;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_BLUE;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.SOFT_GRAY;
import static de.sean.blockprot.bukkit.dialogs.BpDialogStyles.stripColor;

public final class EntitySettingsDialog {

    private record Flags(boolean protect, boolean noDamage, boolean noInteract, boolean noLeash, boolean noPickup) {
        static final Flags DEFAULTS = new Flags(true, true, false, true, false);

        static Flags all(boolean value) {
            return new Flags(value, value, value, value, value);
        }

        Flags base() {
            return protect ? this : DEFAULTS;
        }

        Flags withNoDamage(boolean v)   { return new Flags(protect, v, noInteract, noLeash, noPickup); }
        Flags withNoInteract(boolean v) { return new Flags(protect, noDamage, v, noLeash, noPickup); }
        Flags withNoLeash(boolean v)    { return new Flags(protect, noDamage, noInteract, v, noPickup); }
        Flags withNoPickup(boolean v)   { return new Flags(protect, noDamage, noInteract, noLeash, v); }
    }

    private EntitySettingsDialog() {}

    public static void show(@NotNull Player player, @NotNull Entity entity) {
        DialogBridge bridge = DialogBridgeFactory.getBridge();
        if (bridge == null) return;
        EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(entity);
        if (handler == null) return;

        Flags current = new Flags(handler.isProtected(), handler.isNoDamage(), handler.isNoInteract(),
            handler.isNoLeash(), handler.isNoPickup());
        boolean colorblind = new PlayerSettingsHandler(player).getColorblindMode();

        Component title = Component.text(
            stripColor(Translator.get(TranslationKey.INVENTORIES__ENTITY_SETTINGS__SETTINGS)),
            SOFT_BLUE, TextDecoration.BOLD);
        List<DialogBodyEntry> body = new ArrayList<>();
        body.add(DialogBodyEntry.text(Component.text(formatName(entity.getType().name()), SOFT_GRAY)));

        List<DialogButton> actions = new ArrayList<>();
        actions.add(toggleBtn("protect", TranslationKey.INVENTORIES__ENTITY_SETTINGS__PROTECT, current.protect(), colorblind,
            p -> apply(p, entity, current.protect() ? Flags.all(false) : Flags.DEFAULTS)));
        actions.add(toggleBtn("no_damage", TranslationKey.INVENTORIES__ENTITY_SETTINGS__NO_DAMAGE, current.noDamage(), colorblind,
            p -> apply(p, entity, current.base().withNoDamage(!current.noDamage()))));
        actions.add(toggleBtn("no_interact", TranslationKey.INVENTORIES__ENTITY_SETTINGS__NO_INTERACT, current.noInteract(), colorblind,
            p -> apply(p, entity, current.base().withNoInteract(!current.noInteract()))));
        actions.add(toggleBtn("no_leash", TranslationKey.INVENTORIES__ENTITY_SETTINGS__NO_LEASH, current.noLeash(), colorblind,
            p -> apply(p, entity, current.base().withNoLeash(!current.noLeash()))));
        actions.add(toggleBtn("no_pickup", TranslationKey.INVENTORIES__ENTITY_SETTINGS__NO_PICKUP, current.noPickup(), colorblind,
            p -> apply(p, entity, current.base().withNoPickup(!current.noPickup()))));
        BpDialogStyles.padToRowEnd(actions, 3);
        actions.add(cmdBtn("enable_all", stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__ENABLE_ALL)),
            PASTEL_MINT, p -> apply(p, entity, Flags.all(true))));
        actions.add(cmdBtn("disable_all", stripColor(Translator.get(TranslationKey.INVENTORIES__REDSTONE__DISABLE_ALL)),
            PASTEL_CORAL, p -> apply(p, entity, Flags.all(false))));

        DialogButton exitBtn = DialogNavigation.backButton(DialogOrigin.NONE, null);
        bridge.showMultiAction(player, title, body, actions, exitBtn, 3);
    }

    private static void apply(@NotNull Player player, @NotNull Entity entity, @NotNull Flags flags) {
        BlockProt.getFoliaLib().getScheduler().runAtEntity(entity, task -> {
            EntityProtectionHandler handler = EntityProtectionHandler.forEntityOrNull(entity);
            if (handler == null) return;
            if (flags.protect()) {
                if (handler.getOwner() == null) handler.setOwner(player.getUniqueId());
                handler.setProtected(true);
                handler.setNoDamage(flags.noDamage());
                handler.setNoInteract(flags.noInteract());
                handler.setNoLeash(flags.noLeash());
                handler.setNoPickup(flags.noPickup());
            } else {
                handler.clear();
            }
            RegionTasks.runFor(player, () -> show(player, entity));
        });
    }

    private static DialogButton toggleBtn(String id, TranslationKey labelKey, boolean active, boolean colorblind,
                                          DialogButton.DialogClickHandler handler) {
        TextColor color = active ? PASTEL_MINT : PASTEL_CORAL;
        String label = stripColor(Translator.get(labelKey));
        return new DialogButton(id,
            Component.text()
                .append(Component.text(BpDialogStyles.indicatorIcon(active, colorblind), color))
                .append(Component.text(label, NamedTextColor.WHITE))
                .build(),
            Component.join(JoinConfiguration.newlines(),
                Component.text(label, SOFT_GRAY),
                Component.text(stripColor(Translator.get(active ? TranslationKey.ENABLED : TranslationKey.DISABLED)), color)),
            handler);
    }

    private static DialogButton cmdBtn(String id, String label, TextColor color, DialogButton.DialogClickHandler handler) {
        return new DialogButton(id, Component.text(label, color), Component.text(label, TextColor.color(0x888888)), handler);
    }

    private static String formatName(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') { sb.append(' '); nextUpper = true; }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }
}
