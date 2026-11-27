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

package com.tenio.examples.example13;

import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventServerInitialization;

import java.util.Map;
import java.util.Set;

/**
 * Enables bounded discovery/replication and explicitly publishes one public demonstration room.
 */
@EventHandler
public final class CapabilityFeatures extends AbstractHandler implements EventServerInitialization {

    private static final int MAX_ENTITIES = 100;
    private static final int MAX_STATE_BYTES = 4096;
    private static final int MAX_SUBSCRIBERS = 100;
    private Room room;

    @Override
    public void onServerInitialization(String name) {
        var replication = api().enableStateReplication(MAX_ENTITIES, MAX_STATE_BYTES, MAX_SUBSCRIBERS);
        var directory = api().enableRoomDiscovery(Set.of("rank"), MAX_ENTITIES, MAX_SUBSCRIBERS);
        room = api().createRoom(roomSetting()
                        .setName("Capability demo")
                        .setMaxParticipants(16)
                        .setMaxSpectators(16)
                        .setActivated(true)
                        .setRoomRemoveMode(RoomRemoveMode.NEVER_REMOVE)
                        .build(),
                null);
        if (room == null) {
            throw new IllegalStateException("Could not create demonstration room");
        }
        var state = replication.createRoom(room.getId(),
                null,
                Map.of("ready",
                        new ReplicatedState.Definition(ReplicatedState.Type.BOOLEAN, ReplicatedState.Visibility.ROOM, false, 1),
                        "mode",
                        new ReplicatedState.Definition(ReplicatedState.Type.STRING, ReplicatedState.Visibility.LOBBY, false, 32))
        );
        state.updateServer(Map.of("ready", false, "mode", "duel"), -1);
        directory.publish(room, "demo", "duel", "waiting", Map.of("rank", 1));
    }

    /**
     * Shared monitor for all state writers and recovery capture/delivery.
     */
    public Room room() {
        return room;
    }

    /**
     * Initializes owner-writable state after an authenticated login.
     */
    public void enroll(Player player) {
        var state = api().getStateReplication()
                .createPlayer(
                        player.getIdentity(),
                        Map.of("ready", new ReplicatedState.Definition(ReplicatedState.Type.BOOLEAN, ReplicatedState.Visibility.OWNER, true, 1))
                );
        state.updateServer(Map.of("ready", false), -1);
        api().joinRoom(player, room);
    }

    /**
     * Application wire snapshot; arbitrary properties and recovery tokens are never copied.
     */
    public ZeroMap snapshot(Player player) {
        var audience = new ReplicatedState.Audience(player.getIdentity(), player.getCurrentRoom().orElse(null) == room, false);
        var state = api().getStateReplication().player(player.getIdentity()).snapshot(audience);
        var roomState = api().getStateReplication().room(room.getId()).snapshot(audience);
        var data = map().putString("identity", player.getIdentity())
                .putLong("room", room.getId())
                .putLong("revision", state.revision())
                .putBoolean("ready", (Boolean) state.values().get("ready"));
        if (roomState.values().containsKey("ready")) {
            data.putBoolean("room-ready", (Boolean) roomState.values().get("ready"));
        }
        return data;
    }
}
