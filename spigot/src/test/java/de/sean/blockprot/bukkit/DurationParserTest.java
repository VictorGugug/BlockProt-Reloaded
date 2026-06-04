package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.util.DurationParser;
import de.sean.blockprot.bukkit.inventories.InventoryState;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DurationParserTest {

    @Test
    void parseDays() {
        Duration d = DurationParser.parse("30d");
        assertNotNull(d);
        assertEquals(30, d.toDays());
    }

    @Test
    void parseHoursAndMinutes() {
        Duration d = DurationParser.parse("2h30m");
        assertNotNull(d);
        assertEquals(150, d.toMinutes());
    }

    @Test
    void parseSeconds() {
        Duration d = DurationParser.parse("90s");
        assertNotNull(d);
        assertEquals(90, d.getSeconds());
    }

    @Test
    void parseMixed() {
        Duration d = DurationParser.parse("1d2h3m4s");
        assertNotNull(d);
        long expected = 86400 + 7200 + 180 + 4;
        assertEquals(expected, d.getSeconds());
    }

    @Test
    void parseEmpty() {
        assertNull(DurationParser.parse(""));
        assertNull(DurationParser.parse("   "));
    }

    @Test
    void parseInvalid() {
        assertNull(DurationParser.parse("abc"));
        assertNull(DurationParser.parse("0d0h"));
    }

    @Test
    void formatRoundTrip() {
        Duration d = Duration.ofDays(1).plusHours(2).plusMinutes(30);
        String s = DurationParser.format(d);
        Duration parsed = DurationParser.parse(s);
        assertNotNull(parsed);
        assertEquals(d.getSeconds(), parsed.getSeconds());
    }
}
