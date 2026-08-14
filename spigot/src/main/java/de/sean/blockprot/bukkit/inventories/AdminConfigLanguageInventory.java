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

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BukkitCompat;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.LangConfig;
import de.sean.blockprot.bukkit.util.ComponentMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Language category of the inventory-based admin config editor: language
 * toggles, active language selection, and the translation fallback string.
 */
public final class AdminConfigLanguageInventory extends BlockProtInventory {

    private static final int SLOT_BACK = 49;

    private enum Screen { MAIN, TOGGLE, SELECTOR }

    private Screen currentScreen = Screen.MAIN;

    public AdminConfigLanguageInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.sextupletLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CAT_LANGUAGE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        currentScreen = Screen.MAIN;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10, 13, 16,17, 18,19,20,21,22,23,24,25,26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        DefaultConfig cfg = BlockProt.getDefaultConfig();

        inventory.setItem(11, buttonItem(Material.BOOKSHELF,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_CATEGORY),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_HINT)));
        inventory.setItem(12, buttonItem(Material.WRITTEN_BOOK,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_CATEGORY),
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__SELECTOR_HINT)));
        inventory.setItem(14, AdminConfigInventory.toggleItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__REPLACE_TRANSLATIONS_TITLE),
            "replace_translations",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__REPLACE_TRANSLATIONS),
            cfg.shouldReplaceTranslations()));
        String fallback = cfg.getTranslationFallbackString();
        inventory.setItem(15, AdminConfigInventory.valueItem(
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING_TITLE),
            fallback != null ? fallback : "",
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING),
            "fallback_string"));

        setBackButton(SLOT_BACK);
        return inventory;
    }

    @NotNull
    public Inventory fillToggle(@NotNull Player player) {
        currentScreen = Screen.TOGGLE;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9,10, 12,13,14,15,16,17, 35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

        boolean anyDisabled = false;
        for (String lang : allLangs) {
            if (!LangConfig.isLanguageEnabled(lang)) {
                anyDisabled = true;
                break;
            }
        }
        boolean allEnabled = !anyDisabled;
        String toggleLabel = allEnabled
            ? AdminConfigInventory.strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_DISABLE))
            : AdminConfigInventory.strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_ENABLE));
        inventory.setItem(11, buttonItem(Material.REPEATER, toggleLabel,
            Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__TOGGLE_ALL_HINT)));

        BlockProt plugin = BlockProt.getInstance();
        int slot = 19;
        for (String lang : allLangs) {
            boolean isEnabled = LangConfig.isLanguageEnabled(lang);
            String label = getLanguageLabel(plugin, lang);

            ItemStack stack = new ItemStack(Material.BOOK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                ComponentMessages.displayName(meta, Component.text(AdminConfigInventory.strip(label))
                    .color(isEnabled ? NamedTextColor.WHITE : AdminConfigInventory.SOFT_GRAY));
                String langStatus = AdminConfigInventory.strip(Translator.get(isEnabled
                    ? TranslationKey.DIALOGS__STATUS_ENABLED
                    : TranslationKey.DIALOGS__STATUS_DISABLED));
                String clickAction = AdminConfigInventory.strip(Translator.get(isEnabled
                    ? TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_DISABLE
                    : TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_ENABLE));
                ComponentMessages.lore(meta, List.of(
                    Component.text(AdminConfigInventory.strip(
                        Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__LANG_STATUS))
                        .replace("{status}", langStatus))
                        .color(isEnabled ? NamedTextColor.WHITE : AdminConfigInventory.SOFT_GRAY),
                    Component.text(clickAction).color(TextColor.color(0x888888))));
                if (isEnabled) {
                    meta.addEnchant(BukkitCompat.GLOW_ENCHANT, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                stack.setItemMeta(meta);
            }
            if (slot <= 34) {
                inventory.setItem(slot++, stack);
            }
        }
        setBackButton(SLOT_BACK);
        return inventory;
    }

    @NotNull
    public Inventory fillSelector(@NotNull Player player) {
        currentScreen = Screen.SELECTOR;
        inventory = createInventory();
        fillSeparators(new int[]{0,1,2,3,4,5,6,7,8, 9, 26, 27,28,29,30,31,32,33,34,35, 36,37,38,39,40,41,42,43,44, 45,46,47,48, 50,51,52,53});
        DefaultConfig cfg = BlockProt.getDefaultConfig();
        String currentLang = cfg.getLanguageFile();
        String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);

        BlockProt plugin = BlockProt.getInstance();
        int slot = 10;
        for (String lang : allLangs) {
            boolean isConfigLang = lang.equals(currentLang);
            boolean isEnabled = LangConfig.isLanguageEnabled(lang);
            String label = getLanguageLabel(plugin, lang);

            ItemStack stack = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component displayName = Component.text(AdminConfigInventory.strip(label),
                    isConfigLang ? NamedTextColor.WHITE
                        : (isEnabled ? NamedTextColor.WHITE : AdminConfigInventory.SOFT_GRAY));
                if (isConfigLang) {
                    displayName = displayName.append(Component.text(" "
                        + AdminConfigInventory.strip(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__ACTIVE_MARKER)),
                        AdminConfigInventory.PASTEL_GOLD));
                }
                ComponentMessages.displayName(meta, displayName);
                String configStatus = AdminConfigInventory.strip(Translator.get(isConfigLang
                    ? TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_ACTIVE
                    : TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CONFIG_STATUS_INACTIVE));
                String clickAction = isConfigLang
                    ? AdminConfigInventory.strip(Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__VALUE_CURRENT))
                    : (isEnabled
                        ? AdminConfigInventory.strip(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__CLICK_ENABLE))
                        : AdminConfigInventory.strip(Translator.get(
                            TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__LANG_STATUS)).replace("{status}",
                            AdminConfigInventory.strip(Translator.get(TranslationKey.DIALOGS__STATUS_DISABLED))));
                ComponentMessages.lore(meta, List.of(
                    Component.text(AdminConfigInventory.strip(
                        Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__CONFIG_YML_PREFIX)) + configStatus)
                        .color(isConfigLang
                            ? AdminConfigInventory.PASTEL_GOLD
                            : AdminConfigInventory.SOFT_GRAY),
                    Component.text(clickAction).color(TextColor.color(0x888888))));
                if (isConfigLang) {
                    meta.addEnchant(BukkitCompat.GLOW_ENCHANT, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                stack.setItemMeta(meta);
            }
            if (slot <= 25) {
                inventory.setItem(slot++, stack);
            }
        }
        setBackButton(SLOT_BACK);
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_BACK) {
            if (currentScreen == Screen.MAIN) {
                player.openInventory(new AdminConfigInventory().fill(player));
            } else {
                player.openInventory(fill(player));
            }
            return;
        }

        DefaultConfig cfg = BlockProt.getDefaultConfig();
        if (currentScreen == Screen.MAIN) {
            if (slot == 11) {
                player.openInventory(fillToggle(player));
            } else if (slot == 12) {
                player.openInventory(fillSelector(player));
            } else if (slot == 14) {
                cfg.setAndSave("replace_translations", !cfg.shouldReplaceTranslations());
                player.openInventory(fill(player));
            } else if (slot == 15) {
                String fallback = cfg.getTranslationFallbackString();
                TextInput.open(player, BlockProt.getInstance(),
                    Translator.get(TranslationKey.DIALOGS__ADMIN_CONFIG__LANGUAGE__FALLBACK_STRING_HINT), input -> {
                        if (input == null || input.isBlank()) return;
                        cfg.setAndSave("fallback_string", input.trim());
                        player.openInventory(fill(player));
                    });
            }

        } else if (currentScreen == Screen.TOGGLE) {
            String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);
            if (slot == 11) {
                boolean anyDisabled = false;
                for (String lang : allLangs) {
                    if (!LangConfig.isLanguageEnabled(lang)) {
                        anyDisabled = true;
                        break;
                    }
                }
                boolean allEnabled = !anyDisabled;
                for (String lang : allLangs) {
                    LangConfig.setLanguageEnabled(lang, !allEnabled);
                }
                player.openInventory(fillToggle(player));
            } else if (slot >= 19 && slot - 19 < allLangs.length) {
                String lang = allLangs[slot - 19];
                LangConfig.setLanguageEnabled(lang, !LangConfig.isLanguageEnabled(lang));
                player.openInventory(fillToggle(player));
            }

        } else if (currentScreen == Screen.SELECTOR) {
            String[] allLangs = Translator.DEFAULT_TRANSLATION_FILES.toArray(new String[0]);
            if (slot >= 10 && slot - 10 < allLangs.length) {
                String lang = allLangs[slot - 10];
                if (!lang.equals(cfg.getLanguageFile()) && LangConfig.isLanguageEnabled(lang)) {
                    cfg.setLanguageFile(lang);
                }
                player.openInventory(fillSelector(player));
            }
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private void fillSeparators(int[] separatorSlots) {
        ItemStack sep = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = sep.getItemMeta();
        if (meta != null) {
            ComponentMessages.displayName(meta, Component.text(""));
            sep.setItemMeta(meta);
        }
        for (int s : separatorSlots) {
            inventory.setItem(s, sep);
        }
    }

    private ItemStack buttonItem(Material material, String title, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        ComponentMessages.displayName(meta, Component.text(
            AdminConfigInventory.strip(title)).color(NamedTextColor.WHITE));
        ComponentMessages.lore(meta, List.of(Component.text(
            AdminConfigInventory.strip(lore)).color(AdminConfigInventory.SOFT_GRAY)));
        stack.setItemMeta(meta);
        return stack;
    }

    private static @Nullable YamlConfiguration loadLanguageFile(@NotNull BlockProt plugin, @NotNull String fileName) {
        File diskFile = new File(plugin.getDataFolder(), "lang/" + fileName);
        try {
            if (diskFile.exists()) {
                return YamlConfiguration.loadConfiguration(diskFile);
            }
            InputStream jarStream = plugin.getResource("lang/" + fileName);
            if (jarStream == null) return null;
            return YamlConfiguration.loadConfiguration(
                new BufferedReader(new InputStreamReader(jarStream, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getLanguageName(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return fileName;
        String name = langFile.getString("language_name");
        if (name != null && !name.isEmpty()) return name;
        return langFile.getString("locale", fileName);
    }

    private static int computeCompletion(@NotNull BlockProt plugin, @NotNull String fileName) {
        YamlConfiguration langFile = loadLanguageFile(plugin, fileName);
        if (langFile == null) return 0;
        return Translator.computeCompletionPercentage(langFile);
    }

    private static String getLanguageLabel(@NotNull BlockProt plugin, @NotNull String fileName) {
        return getLanguageName(plugin, fileName) + " (" + computeCompletion(plugin, fileName) + "%)";
    }
}