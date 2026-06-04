package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.util.DurationParser;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StatHandlerDirtyFlagTest {

    @Test
    void durationParserZeroIsNull() {
        assertNull(DurationParser.parse("0d0h0m0s"));
    }

    @Test
    void durationParserNegativeStringIsNull() {
        assertNull(DurationParser.parse("abc"));
    }

    @Test
    void durationParserOnlySeconds() {
        Duration d = DurationParser.parse("45s");
        assertNotNull(d);
        assertEquals(45L, d.getSeconds());
    }

    @Test
    void durationParserLargeValue() {
        Duration d = DurationParser.parse("365d");
        assertNotNull(d);
        assertEquals(365L, d.toDays());
    }

    @Test
    void formatThenParse() {
        Duration original = Duration.ofHours(3).plusMinutes(15);
        String formatted = DurationParser.format(original);
        Duration reparsed = DurationParser.parse(formatted);
        assertNotNull(reparsed);
        assertEquals(original.getSeconds(), reparsed.getSeconds());
    }
}
