package de.sean.blockprot.bukkit.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and formats human-readable duration strings.
 *
 * <p>Accepted unit suffixes (case-insensitive):
 * <ul>
 *   <li>{@code s}          — seconds  (max: 60s)</li>
 *   <li>{@code m}          — minutes  (max: 60m)</li>
 *   <li>{@code h}          — hours    (max: 24h)</li>
 *   <li>{@code d}          — days     (max: 28d)</li>
 *   <li>{@code mo} / {@code mon} — months (language-dependent max, default 12mo)</li>
 *   <li>{@code y}          — years    (not recommended; max configurable)</li>
 * </ul>
 *
 * <p>Units may be combined freely: {@code 1d12h30m}, {@code 2mo3d}, {@code 1y6mo}.
 * The parser is order-independent (longest match wins per token).
 *
 * <p>Maximum values are <em>not</em> enforced by this class — they are the
 * responsibility of the caller (see {@link DurationLimits}).
 */
public final class DurationParser {

    /**
     * Tokenises a duration string into (value, unit) pairs.
     *
     * <p>Unit priority (longest suffix matched first to avoid "mo" being swallowed by "m"):
     * <ol>
     *   <li>mon  → months</li>
     *   <li>mo   → months</li>
     *   <li>y    → years</li>
     *   <li>d    → days</li>
     *   <li>h    → hours</li>
     *   <li>m    → minutes</li>
     *   <li>s    → seconds</li>
     * </ol>
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(\\d+)\\s*(mon|mo|y|d|h|m|s)",
        Pattern.CASE_INSENSITIVE
    );

    private DurationParser() {}

    /**
     * Parses a duration string.
     *
     * <p>Examples: {@code "30s"}, {@code "60m"}, {@code "24h"}, {@code "28d"},
     * {@code "12mo"}, {@code "3mon"}, {@code "1y"}, {@code "1d12h30m"}.
     *
     * @param input raw user input
     * @return parsed {@link Duration}, or {@code null} if the input is blank,
     *         contains no recognisable token, or the resulting duration is zero
     */
    @Nullable
    public static Duration parse(@NotNull String input) {
        input = input.trim();
        if (input.isEmpty()) return null;

        Matcher m = TOKEN_PATTERN.matcher(input);
        long years = 0, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
        boolean matched = false;

        while (m.find()) {
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase(java.util.Locale.ROOT);
            switch (unit) {
                case "y"   -> years   += value;
                case "mon",
                     "mo"  -> months  += value;
                case "d"   -> days    += value;
                case "h"   -> hours   += value;
                case "m"   -> minutes += value;
                case "s"   -> seconds += value;
            }
            matched = true;
        }

        if (!matched) return null;
        if (years == 0 && months == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            return null;
        }

        // 1 month  = 28 days
        // 1 year   = 365 days
        return Duration.ofDays(years * 365L)
            .plusDays(months * 28L)
            .plusDays(days)
            .plusHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds);
    }

    /**
     * Formats a {@link Duration} back into a compact human-readable string using
     * the canonical suffixes: {@code s}, {@code m}, {@code h}, {@code d},
     * {@code mo}, {@code y}.
     *
     * @param duration the duration to format
     * @return a non-empty string such as {@code "1d12h"}, {@code "30s"}, {@code "2mo3d"}
     */
    @NotNull
    public static String format(@NotNull Duration duration) {
        long total = duration.getSeconds();

        long y = total / (365L * 86400);
        long remaining = total % (365L * 86400);

        long mo = remaining / (28L * 86400);
        remaining = remaining % (28L * 86400);

        long d   = remaining / 86400;
        remaining = remaining % 86400;

        long h   = remaining / 3600;
        remaining = remaining % 3600;

        long min = remaining / 60;
        long s   = remaining % 60;

        StringBuilder sb = new StringBuilder();
        if (y   > 0) sb.append(y).append('y');
        if (mo  > 0) sb.append(mo).append("mo");
        if (d   > 0) sb.append(d).append('d');
        if (h   > 0) sb.append(h).append('h');
        if (min > 0) sb.append(min).append('m');
        if (s   > 0 || sb.isEmpty()) sb.append(s).append('s');

        return sb.toString();
    }
}
