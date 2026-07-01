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

package de.sean.blockprot.bukkit.nbt;

import de.sean.blockprot.bukkit.BlockProt;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The backbone of any handler that supports adding a list of friends/players.
 * 
 * @since 1.0.0
 */
public abstract class FriendSupportingHandler<T extends NBTCompound> extends NBTHandler<T> {
    private final @NotNull String friendNbtKey;

    public static final UUID publicUuid = new UUID(~0, ~0);

    public FriendSupportingHandler(@NotNull String friendNbtKey) {
        this.friendNbtKey = friendNbtKey;
    }

    /**
     * Called after any mutation to the friends compound.
     * Subclasses that back a tile entity override this to call
     * {@code NBT.modify(blockState, nbt -> { ... })} and flush the
     * in-memory compound to disk. The default implementation is a no-op
     * (NBTBlock and NBTEntity auto-persist on write).
     */
    protected void onFriendsMutated() {}

    private NBTCompound compound() {
        return container.getOrCreateCompound(friendNbtKey);
    }

    /**
     * A function called before friends are read from this NBT compound.
     * Useful for remapping old data to this new structure.
     */
    protected void preFriendReadCallback() {

    }

    /**
     * Gets a {@link Stream} of {@link FriendHandler} for this block.
     *
     * @return A stream of friend handlers for all NBT compounds under
     * the friend key.
     */
    public Stream<FriendHandler> getFriendsStream() {
        preFriendReadCallback();
        if (!this.container.hasTag(friendNbtKey)) return Stream.empty();
        if (BlockProt.getDefaultConfig().isFriendFunctionalityDisabled()) return Stream.empty();

        final NBTCompound compound = this.container.getOrCreateCompound(friendNbtKey);
        return compound
            .getKeys()
            .stream()
            .map((k) -> new FriendHandler(compound.getCompound(k)))
            // This is a weird Comparator, but it essentially just guarantees that the entry where
            // getName().equals(publicUuid.toString()) is at the front of the stream.
            .sorted((a, b) -> a.doesRepresentPublic() ? -1 : 1);
    }

    public List<FriendHandler> getFriends() {
        return this.getFriendsStream().collect(Collectors.toList());
    }

    /**
     * Set a new list of FriendHandler for the friends list.
     * Replaces any existing friends entirely.
     *
     * @param friends The new friend list. Passing an empty list clears all friends.
     */
    public void setFriends(@NotNull final List<FriendHandler> friends) {
        container.removeKey(friendNbtKey);
        friends.forEach(this::addFriend);
    }

    /**
     * Filters the results of {@link #getFriends()} for any entry whose
     * UUID qualifies for {@link String#equals(Object)} with given {@code id}.
     *
     * If the given {@code id} is not a friend of this block, this might return
     * the {@link FriendHandler} which represents all players.
     *
     * @param id The UUID to check for.
     * @return The first {@link FriendHandler} found, or none.
     */
    @NotNull
    public Optional<FriendHandler> getFriend(@NotNull final String id) {
        return getFriendsStream()
            .filter(f -> f.getName().equals(id) || f.doesRepresentPublic())
            // This is a weird Comparator, but it essentially just guarantees that the entry where
            // getName().equals(publicUuid.toString()) is at the end of the stream.
            .min((a, b) -> a.doesRepresentPublic() ? 1 : -1);
    }

    public void addFriend(@NotNull final String friend) {
        compound().addCompound(friend).setString("id", friend);
        onFriendsMutated();
    }

    public void addFriend(@NotNull final FriendHandler friend) {
        compound().addCompound(friend.getName()).mergeCompound(friend.container);
        onFriendsMutated();
    }

    /**
     * Add "everyone" as a friend to this handler.
     * @since 1.1.0
     */
    public void addEveryoneAsFriend() {
        addFriend(publicUuid.toString());
    }

    public void removeFriend(@NotNull final String friend) {
        compound().removeKey(friend);
        onFriendsMutated();
    }

    public boolean containsFriend(@NotNull final String friendUuid) {
        return containsFriend(getFriendsStream(), friendUuid);
    }

    /**
     * Checks whether {@code friends} contains {@code friend}.
     *
     * @param friends A list of all friends we want to filter.
     * @param friendUuid The UUID of a player we want to check for.
     * @return True, if the list does contain that friend.
     */
    public boolean containsFriend(@NotNull final Stream<FriendHandler> friends, @NotNull final String friendUuid) {
        return friends
            .anyMatch((f) -> f.getName().equals(friendUuid));
    }
}