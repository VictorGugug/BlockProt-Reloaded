package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.inventories.InventoryState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryStateBuilderTest {

    @Test
    void builderDefaults() {
        InventoryState state = InventoryState.builder().build();
        assertNull(state.getBlock());
        assertEquals(InventoryState.MenuOrigin.NONE, state.origin);
        assertEquals(InventoryState.FriendSearchState.FRIEND_SEARCH, state.friendSearchState);
    }

    @Test
    void builderOrigin() {
        InventoryState state = InventoryState.builder()
            .origin(InventoryState.MenuOrigin.BLOCK_LOCK)
            .build();
        assertEquals(InventoryState.MenuOrigin.BLOCK_LOCK, state.origin);
    }

    @Test
    void builderFriendSearchState() {
        InventoryState state = InventoryState.builder()
            .friendSearchState(InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH)
            .build();
        assertEquals(InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH, state.friendSearchState);
    }

    @Test
    void builderNullBlock() {
        InventoryState state = InventoryState.builder().block(null).build();
        assertNull(state.getBlock());
    }
}
