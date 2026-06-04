package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.nbt.FriendSupportingHandler;
import de.sean.blockprot.bukkit.nbt.FriendHandler;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FriendSupportingHandlerTest {

    private static final String FRIENDS_KEY = "test_friends";

    private TestHandler handler;

    private static class TestHandler extends FriendSupportingHandler<NBTCompound> {
        int mutateCount = 0;

        TestHandler() {
            super(FRIENDS_KEY);
            this.container = new NBTContainer();
        }

        @Override
        protected void onFriendsMutated() {
            mutateCount++;
        }
    }

    @BeforeEach
    void setup() {
        handler = new TestHandler();
    }

    @Test
    void addFriendFiresHook() {
        String uuid = UUID.randomUUID().toString();
        handler.addFriend(uuid);
        assertEquals(1, handler.mutateCount);
    }

    @Test
    void addFriendPersistsInCompound() {
        String uuid = UUID.randomUUID().toString();
        handler.addFriend(uuid);
        assertTrue(handler.containsFriend(uuid));
    }

    @Test
    void removeFriendFiresHook() {
        String uuid = UUID.randomUUID().toString();
        handler.addFriend(uuid);
        int before = handler.mutateCount;
        handler.removeFriend(uuid);
        assertEquals(before + 1, handler.mutateCount);
    }

    @Test
    void removeFriendRemovesFromCompound() {
        String uuid = UUID.randomUUID().toString();
        handler.addFriend(uuid);
        handler.removeFriend(uuid);
        assertFalse(handler.containsFriend(uuid));
    }

    @Test
    void setFriendsClearsAndAdds() {
        String a = UUID.randomUUID().toString();
        String b = UUID.randomUUID().toString();
        handler.addFriend(a);
        handler.setFriends(List.of());
        assertFalse(handler.containsFriend(a));
        handler.setFriends(List.of());
        assertTrue(handler.getFriends().isEmpty());
    }

    @Test
    void setFriendsClearsExisting() {
        String uuid = UUID.randomUUID().toString();
        handler.addFriend(uuid);
        handler.setFriends(List.of());
        assertFalse(handler.containsFriend(uuid));
    }

    @Test
    void publicUuidRecognized() {
        handler.addEveryoneAsFriend();
        assertTrue(handler.containsFriend(FriendSupportingHandler.publicUuid.toString()));
    }

    @Test
    void getFriendFallsBackToPublic() {
        handler.addEveryoneAsFriend();
        var found = handler.getFriend(UUID.randomUUID().toString());
        assertTrue(found.isPresent());
        assertTrue(found.get().doesRepresentPublic());
    }

    @Test
    void emptyHandlerContainsNoFriends() {
        assertFalse(handler.containsFriend(UUID.randomUUID().toString()));
        assertTrue(handler.getFriends().isEmpty());
    }

    @Test
    void multipleDistinctFriends() {
        handler.addFriend(UUID.randomUUID().toString());
        handler.addFriend(UUID.randomUUID().toString());
        assertEquals(2, handler.getFriends().size());
    }
}
