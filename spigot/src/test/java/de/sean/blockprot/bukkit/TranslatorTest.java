package de.sean.blockprot.bukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TranslatorTest {

    private static ServerMock server;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    @Test
    void allTranslationKeysHaveDefaultMapping() {
        for (TranslationKey key : TranslationKey.values()) {
            String result = Translator.get(key);
            assertNotNull(result, "TranslationKey." + key + " returned null");
        }
    }

    @Test
    void translatorReturnsNonEmptyForCommonKeys() {
        String noPermission = Translator.get(TranslationKey.MESSAGES__NO_PERMISSION);
        assertFalse(noPermission.isBlank(), "MESSAGES__NO_PERMISSION should not be blank");
    }

    @Test
    void translatorFallsBackGracefully() {
        String result = Translator.get(TranslationKey.MESSAGES__LOCK_ON_PLACE_SUCCESS);
        assertNotNull(result);
    }
}
