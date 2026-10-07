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

package de.sean.blockprot.bukkit.metrics;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.config.LangConfig;
import org.bstats.charts.SimplePie;

import java.util.concurrent.Callable;

public final class LanguagePieChart extends SimplePie {
    public LanguagePieChart() {
        super("language", new LanguagePieChartData());
    }

    public static class LanguagePieChartData implements Callable<String> {
        @Override
        public String call() {
            String activeFile = BlockProt.getDefaultConfig().getLanguageFile();
            if (activeFile == null || activeFile.isBlank()) {
                activeFile = BlockProt.defaultLanguageFile;
            }
            return LangConfig.fileNameToCode(activeFile);
        }
    }
}
