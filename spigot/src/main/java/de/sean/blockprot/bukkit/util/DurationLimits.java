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

package de.sean.blockprot.bukkit.util;

import de.sean.blockprot.bukkit.config.DefaultConfig;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Configurable duration limits for expiry and timed access.
 * Limits are in milliseconds and can be enforced via {@link #validate(Duration)}.
 *
 * <p>Limits are loaded from {@code config.yml} and can be overridden
 * per-language in the corresponding language file.
 */
public final class DurationLimits {

    private final long maxSeconds;      // e.g., 60,000 ms
    private final long maxMinutes;      // e.g., 3,600,000 ms
    private final long maxHours;        // e.g., 86,400,000 ms
    private final long maxDays;         // e.g., 2,419,200,000 ms (28 days)
    private final long maxMonths;       // Language-dependent
    private final long maxYears;        // Not recommended

    public DurationLimits(
            long maxSecondsMills,
            long maxMinutesMills,
            long maxHoursMills,
            long maxDaysMills,
            long maxMonthsMills,
            long maxYearsMills) {
        this.maxSeconds = maxSecondsMills;
        this.maxMinutes = maxMinutesMills;
        this.maxHours = maxHoursMills;
        this.maxDays = maxDaysMills;
        this.maxMonths = maxMonthsMills;
        this.maxYears = maxYearsMills;
    }

    /**
     * Validates that a duration does not exceed any configured limit.
     * Returns true if the duration is acceptable, false otherwise.
     */
    public boolean validate(@NotNull Duration duration) {
        long millis = duration.toMillis();
        return millis <= maxSeconds
            || millis <= maxMinutes
            || millis <= maxHours
            || millis <= maxDays
            || millis <= maxMonths
            || millis <= maxYears;
    }

    /**
     * Returns the smallest applicable limit for this duration, in milliseconds.
     * Used for error messages (e.g., "Max duration: 28 days").
     */
    public long getApplicableLimit(@NotNull Duration duration) {
        long millis = duration.toMillis();
        if (millis > maxYears) return maxYears;
        if (millis > maxMonths) return maxMonths;
        if (millis > maxDays) return maxDays;
        if (millis > maxHours) return maxHours;
        if (millis > maxMinutes) return maxMinutes;
        if (millis > maxSeconds) return maxSeconds;
        return maxYears; // Fallback
    }

    public long getMaxSeconds() { return maxSeconds; }
    public long getMaxMinutes() { return maxMinutes; }
    public long getMaxHours() { return maxHours; }
    public long getMaxDays() { return maxDays; }
    public long getMaxMonths() { return maxMonths; }
    public long getMaxYears() { return maxYears; }

    /**
     * Loads duration limits from the config file.
     * Format in config.yml:
     * <pre>
     * duration_limits:
     *   max_seconds: 60          # in seconds
     *   max_minutes: 60          # in minutes
     *   max_hours: 24            # in hours
     *   max_days: 28             # in days
     *   max_months: 12           # in months (28-day months)
     *   max_years: 5             # in years
     * </pre>
     */
    @NotNull
    public static DurationLimits fromConfig(@NotNull DefaultConfig config) {
        // Access the backing FileConfiguration directly (protected field via same package hierarchy).
        // DefaultConfig extends BlockProtConfig which exposes 'config' as a protected field.
        // We reach it by calling the Bukkit plugin's config through BlockProt.
        org.bukkit.configuration.file.FileConfiguration cfg =
            de.sean.blockprot.bukkit.BlockProt.getInstance().getConfig();

        int maxSecsDays   = cfg.getInt("duration_limits.max_seconds", 60);
        int maxMinsInt    = cfg.getInt("duration_limits.max_minutes", 60);
        int maxHoursInt   = cfg.getInt("duration_limits.max_hours",   24);
        int maxDaysInt    = cfg.getInt("duration_limits.max_days",    28);
        int maxMonthsInt  = cfg.getInt("duration_limits.max_months", 12);
        int maxYearsInt   = cfg.getInt("duration_limits.max_years",   5);

        return new DurationLimits(
            Duration.ofSeconds(maxSecsDays).toMillis(),
            Duration.ofMinutes(maxMinsInt).toMillis(),
            Duration.ofHours(maxHoursInt).toMillis(),
            Duration.ofDays(maxDaysInt).toMillis(),
            Duration.ofDays((long) maxMonthsInt * 28).toMillis(),
            Duration.ofDays((long) maxYearsInt * 365).toMillis()
        );
    }

    public static DurationLimits create(
            int maxSeconds,
            int maxMinutes,
            int maxHours,
            int maxDays,
            int maxMonths,
            int maxYears) {
        return new DurationLimits(
            Duration.ofSeconds(maxSeconds).toMillis(),
            Duration.ofMinutes(maxMinutes).toMillis(),
            Duration.ofHours(maxHours).toMillis(),
            Duration.ofDays(maxDays).toMillis(),
            Duration.ofDays((long) maxMonths * 28).toMillis(),
            Duration.ofDays((long) maxYears * 365).toMillis()
        );
    }
}