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

package de.sean.blockprot.bukkit.dialogs.impl;

import de.sean.blockprot.bukkit.dialogs.DialogBodyEntry;
import de.sean.blockprot.bukkit.dialogs.DialogBridge;
import de.sean.blockprot.bukkit.dialogs.DialogButton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaperDialogBridge implements DialogBridge {

    private static final ClickCallback.Options CLICK_OPTIONS =
        ClickCallback.Options.builder().uses(1).build();

    @Override
    public void showNotice(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @Nullable DialogButton ok
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .build()
            )
            .type(ok != null
                ? DialogType.notice(toActionButton(ok))
                : DialogType.notice())
        );
        player.showDialog(dialog);
    }

    @Override
    public void showConfirmation(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull DialogButton yes,
        @NotNull DialogButton no
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .build()
            )
            .type(DialogType.confirmation(toActionButton(yes), toActionButton(no)))
        );
        player.showDialog(dialog);
    }

    @Override
    public void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<Component> body,
        @NotNull List<DialogButton> actions
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(body.stream().map(DialogBody::plainMessage).toList())
                .build()
            )
            .type(DialogType.multiAction(
                actions.stream().map(this::toActionButton).toList()).build())
        );
        player.showDialog(dialog);
    }

    @Override
    public void showMultiAction(
        @NotNull Player player,
        @NotNull Component title,
        @NotNull List<DialogBodyEntry> body,
        @NotNull List<DialogButton> actions,
        @Nullable DialogButton exit,
        int columns
    ) {
        List<io.papermc.paper.registry.data.dialog.body.DialogBody> dialogBody
            = new ArrayList<>(body.size());
        for (DialogBodyEntry entry : body) {
            if (entry.text() != null) {
                dialogBody.add(DialogBody.plainMessage(entry.text()));
            } else if (entry.item() != null) {
                dialogBody.add(DialogBody.item(entry.item()).build());
            }
        }
        List<DialogButton> effectiveActions = new ArrayList<>(actions);
        if (effectiveActions.isEmpty() && exit != null) {
            effectiveActions.add(exit);
            exit = null;
        }
        ActionButton exitAction = exit != null ? toActionButton(exit) : null;
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(title)
                .body(dialogBody)
                .build()
            )
            .type(DialogType.multiAction(
                effectiveActions.stream().map(this::toActionButton).toList(),
                exitAction,
                columns))
        );
        player.showDialog(dialog);
    }

    private @NotNull ActionButton toActionButton(@NotNull DialogButton btn) {
        var handler = btn.onClick();
        if (handler == null) {
            handler = p -> {};
        }
        final var finalHandler = handler;
        var action = DialogAction.customClick(
            (view, audience) -> {
                if (audience instanceof Player player) {
                    finalHandler.handle(player);
                }
            },
            CLICK_OPTIONS
        );
        var builder = ActionButton.builder(btn.label()).action(action);
        if (btn.tooltip() != null) {
            builder.tooltip(btn.tooltip());
        }
        return builder.build();
    }
}
