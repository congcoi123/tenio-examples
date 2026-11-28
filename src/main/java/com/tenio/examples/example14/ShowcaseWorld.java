/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.examples.example14;

import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.entity.Channel;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventBroadcastToChannel;
import com.tenio.core.handler.event.EventChannelCreated;
import com.tenio.core.handler.event.EventServerInitialization;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared room mutation monitor also guards recovery capture and delivery.
 */
@EventHandler
public final class ShowcaseWorld extends AbstractHandler implements EventServerInitialization, EventChannelCreated, EventBroadcastToChannel<Player, ZeroMap> {

    /**
     * Public room-discovery group used by this demonstration.
     */
    public static final String GROUP = "sdk-demo";
    /**
     * Identifier of the shared demonstration chat channel.
     */
    public static final String CHANNEL = "sdk-chat";
    private static final int ENTITY_LIMIT = 200, STATE_BYTES = 4096, SUBSCRIBERS = 200;
    private final java.security.SecureRandom random = new java.security.SecureRandom();
    private final Object mutation = new Object();
    private final AtomicLong ticks = new AtomicLong();
    private Room lobby;
    private Channel channel;

    /**
     * Creates the framework-instantiated ShowcaseWorld component.
     */
    public ShowcaseWorld() {
    }

    /**
     * Replaces the player UDP registration credential with fresh cryptographic random bytes.
     *
     * @param player player whose credential is replaced
     * @return the new opaque token, delivered privately to the client
     */
    public String rotateUdpToken(Player player) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        player.setProperty("sdk-udp-token", token);
        return token;
    }

    /**
     * Returns the shared monitor guarding room and recovery mutations.
     *
     * @return the application mutation monitor
     */
    public Object getMutationMonitor() {
        return mutation;
    }

    /**
     * Returns the initialized demonstration lobby.
     *
     * @return the lobby, or null before server initialization
     */
    public Room getLobby() {
        return lobby;
    }

    /**
     * Returns the initialized demonstration chat channel.
     *
     * @return the channel, or null before its creation event
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Returns the number of completed scheduled samples.
     *
     * @return the cumulative sample count
     */
    public long getTicks() {
        return ticks.get();
    }

    /**
     * Records one framework-owned scheduled invocation.
     */
    public void incrementTick() {
        ticks.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    public void onServerInitialization(String name) {
        api().enableRoomDiscovery(Set.of("rank"), ENTITY_LIMIT, SUBSCRIBERS);
        api().enableStateReplication(ENTITY_LIMIT, STATE_BYTES, SUBSCRIBERS);
        lobby = create("SDK lobby", null);
        api().createChannel(CHANNEL);
        info("SDK SHOWCASE", "READY");
    }

    /**
     * Creates a room and publishes its public discovery fields under the mutation monitor.
     *
     * @param name  room display name
     * @param owner creating player, or null for the server lobby
     * @return the created room
     * @throws IllegalStateException if room creation fails
     */
    public Room create(String name, Player owner) {
        synchronized (mutation) {
            var room = api().createRoom(roomSetting()
                            .setName(name)
                            .setMaxParticipants(32)
                            .setMaxSpectators(32)
                            .setActivated(true)
                            .setRoomRemoveMode(RoomRemoveMode.NEVER_REMOVE)
                            .build(),
                    owner);
            if (room == null) {
                throw new IllegalStateException("Room creation failed");
            }
            var state = api().getStateReplication().createRoom(
                    room.getId(),
                    owner == null ? null : owner.getIdentity(),
                    Map.of(
                            "ready",
                            new ReplicatedState.Definition(
                                    ReplicatedState.Type.BOOLEAN, ReplicatedState.Visibility.ROOM, false, 1),
                            "mode",
                            new ReplicatedState.Definition(
                                    ReplicatedState.Type.STRING,
                                    ReplicatedState.Visibility.LOBBY,
                                    false,
                                    32))
            );
            state.updateServer(Map.of("ready", false, "mode", "duel"), -1);
            api().getRoomDirectory().publish(room, GROUP, "duel", "waiting", Map.of("rank", 1));
            return room;
        }
    }

    /**
     * Creates initial player state and joins the shared lobby and channel.
     *
     * @param player newly authenticated demonstration player
     */
    public void enroll(Player player) {
        synchronized (mutation) {
            var state = api().getStateReplication()
                    .createPlayer(
                            player.getIdentity(),
                            Map.of(
                                    "ready",
                                    new ReplicatedState.Definition(
                                            ReplicatedState.Type.BOOLEAN,
                                            ReplicatedState.Visibility.OWNER,
                                            true,
                                            1))
                    );
            state.updateServer(Map.of("ready", false), -1);
            api().joinRoom(player, lobby);
            api().subscribeToChannel(channel, player);
        }
    }

    /**
     * Captures the player-visible state under the mutation monitor.
     *
     * @param player player whose state is captured
     * @return an independent protocol snapshot
     */
    public ZeroMap snapshot(Player player) {
        synchronized (mutation) {
            var audience = new ReplicatedState.Audience(player.getIdentity(), player.getCurrentRoom().isPresent(), false);
            var state = api().getStateReplication().player(player.getIdentity()).snapshot(audience);
            var result = map().putString("identity", player.getIdentity())
                    .putLong("revision", state.revision())
                    .putBoolean("ready", (Boolean) state.values().get("ready"))
                    .putLong("tick", getTicks());
            player.getCurrentRoom().ifPresent(room -> result.putLong("room", room.getId()));
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onChannelCreated(Channel created) {
        if (CHANNEL.equals(created.getId())) {
            channel = created;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onBroadcastToChannel(Channel source, Player player, ZeroMap message) {
        response().setContent(ShowcaseWire.response(message, player))
                .setRecipientPlayer(player)
                .write();
    }
}
