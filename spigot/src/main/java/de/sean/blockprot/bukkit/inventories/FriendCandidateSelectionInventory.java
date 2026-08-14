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

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.util.PlayerLookup.ScoredMatch;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Inventory counterpart of {@link de.sean.blockprot.bukkit.dialogs.FriendCandidateSelectionDialog}:
 * shows fuzzy search candidates as player skulls for the owner to pick from.
 */
public final class FriendCandidateSelectionInventory extends BlockProtInventory {

    private final List<ScoredMatch> candidates;
    private final Consumer<ScoredMatch> onSelect;
    private final Supplier<Inventory> onBack;

    private final int maxResults = getSize() - 1;

    public FriendCandidateSelectionInventory(
            @NotNull List<ScoredMatch> candidates,
            @NotNull Consumer<ScoredMatch> onSelect,
            @NotNull Supplier<Inventory> onBack
    ) {
        super(true);
        this.candidates = candidates;
        this.onSelect = onSelect;
        this.onBack = onBack;
    }

    @Override
    int getSize() {
        return InventoryConstants.tripleLine;
    }

    @NotNull
    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__SELECT_CANDIDATE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE ->
                closeAndOpen(player, onBack.get());
            case PLAYER_HEAD -> {
                final var meta = (SkullMeta) item.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID uuid = meta.getOwningPlayer().getUniqueId();
                    for (ScoredMatch match : candidates) {
                        if (match.uuid().equals(uuid)) {
                            onSelect.accept(match);
                            closeAndOpen(player, onBack.get());
                            break;
                        }
                    }
                }
            }
            default -> closeAndOpen(player, null);
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {

    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        for (int i = 0; i < Math.min(candidates.size(), maxResults); i++) {
            ScoredMatch match = candidates.get(i);
            int pct = (int) Math.round(match.similarity() * 100.0);
            String score = Translator.get(TranslationKey.DIALOGS__FRIENDS__SELECT_CANDIDATE_SCORE)
                .replace("{score}", String.valueOf(pct));
            setPlayerSkullAsync(i, player, match.uuid(), match.name(), score);
        }
        setBackButton();
        return inventory;
    }
}