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
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ExecutionMode;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.PlayerLeaveRoomMode;
import com.tenio.core.entity.discovery.RoomDirectory;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;

import java.time.Duration;
import java.util.Map;

/**
 * Application operation codes, not SDK-reserved commands. Each operation has one registration.
 */
public final class ShowcaseCommands {

    private ShowcaseCommands() {
    }

    /**
     * Common response adapter preserving the recipient serialization format.
     */
    public abstract static class Command extends AbstractClientCommandHandler<Player, ZeroMap> {

        /**
         * Shared application state injected by the framework.
         */
        @Autowired
        protected ShowcaseWorld world;

        /**
         * Creates the common handler base before the framework injects its shared world.
         */
        protected Command() {
        }

        /**
         * Sends a response using the recipient format and the normal framework queue policy.
         *
         * @param player  response recipient
         * @param payload application response fields
         */
        protected void send(Player player, ZeroMap payload) {
            response().setContent(ShowcaseWire.response(payload, player))
                    .setRecipientPlayer(player)
                    .write();
        }
    }

    /**
     * Returns an application echo through the requested supported transport.
     */
    @ClientCommand(value = 1, name = "Echo", execution = ExecutionMode.SESSION)
    public static final class Echo extends Command {

        /**
         * Creates the framework-instantiated Echo component.
         */
        public Echo() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            var payload = map().putString("kind", data.containsKey("udp") && data.getBoolean("udp") ? "udp-echo" : "echo")
                            .putString("text", data.getString("text"))
                            .putLong("tick", world.getTicks());
            if (data.containsKey("udp") && data.getBoolean("udp")) {
                response().setContent(ShowcaseWire.response(payload, player))
                        .setRecipientPlayer(player)
                        .onlyUdp()
                        .write();
            } else {
                send(player, payload);
            }
        }
    }

    /**
     * Creates and publishes a discoverable demonstration room.
     */
    @ClientCommand(value = 2, name = "Create room", execution = ExecutionMode.SESSION)
    public static final class Create extends Command {

        /**
         * Creates the framework-instantiated Create component.
         */
        public Create() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            synchronized (world.getMutationMonitor()) {
                var room = world.create(data.getString("name"), player);
                send(player, map().putString("kind", "created").putLong("room", room.getId()));
            }
        }
    }

    /**
     * Joins an existing demonstration room.
     */
    @ClientCommand(value = 3, name = "Join room", execution = ExecutionMode.SESSION)
    public static final class Join extends Command {

        /**
         * Creates the framework-instantiated Join component.
         */
        public Join() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            synchronized (world.getMutationMonitor()) {
                var room = api().getRoomById(ShowcaseWire.number(data, "room")).orElseThrow();
                api().changeRoom(player, room);
                send(player, world.snapshot(player).putString("kind", "joined"));
            }
        }
    }

    /**
     * Broadcasts room chat under per-player token-bucket limits.
     */
    @ClientCommand(
            value = 4,
            name = "Room chat",
            requestsPerSecond = 2,
            burst = 4,
            execution = ExecutionMode.ROOM)
    public static final class Chat extends Command {

        /**
         * Creates the framework-instantiated Chat component.
         */
        public Chat() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            synchronized (world.getMutationMonitor()) {
                for (Player recipient : player.getCurrentRoom().orElseThrow().getSnapshotPlayersList()) {
                    send(recipient, map().putString("kind", "chat")
                                    .putString("from", player.getIdentity())
                                    .putString("text", data.getString("text")));
                }
            }
        }
    }

    /**
     * Applies an owner-writable boolean with an expected revision.
     */
    @ClientCommand(value = 5, name = "Ready state", execution = ExecutionMode.ROOM)
    public static final class Ready extends Command {

        /**
         * Creates the framework-instantiated Ready component.
         */
        public Ready() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            synchronized (world.getMutationMonitor()) {
                var state = api().getStateReplication().player(player.getIdentity());
                var audience = new ReplicatedState.Audience(player.getIdentity(), true, false);
                try (var subscription = state.subscribe(player.getIdentity())) {
                    subscription.snapshot(audience);
                    try {
                        state.updateClient(audience,
                                Map.of("ready", data.getBoolean("ready")),
                                ShowcaseWire.number(data, "revision"));
                    } catch (IllegalStateException conflict) {
                        send(player, world.snapshot(player).putString("kind", "conflict"));
                        return;
                    }
                    var delta = subscription.poll(audience);
                    api().getStateReplication()
                            .room(player.getCurrentRoom().orElseThrow().getId())
                            .updateServer(Map.of("ready", data.getBoolean("ready")), -1);
                    send(player, world.snapshot(player)
                                    .putString("kind", "ready")
                                    .putBoolean("changed", delta != null));
                }
            }
        }
    }

    /**
     * Returns a bounded public room-discovery snapshot.
     */
    @ClientCommand(value = 6, name = "Find rooms")
    public static final class Rooms extends Command {

        /**
         * Creates the framework-instantiated Rooms component.
         */
        public Rooms() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            var directory = api().getRoomDirectory();
            try (var subscription = directory.subscribe(player.getIdentity(), ShowcaseWorld.GROUP)) {
                var page = directory.query(RoomDirectory.Query.group(ShowcaseWorld.GROUP));
                subscription.acknowledgeSnapshot(page.revision());
                var rows = array();
                for (var room : page.rooms()) {
                    rows.addZeroMap(map().putLong("id", room.id())
                                    .putString("name", room.name())
                                    .putInteger("slots", room.availableSlots()));
                }
                send(player, map().putString("kind", "rooms")
                                .putInteger("total", page.total())
                                .putLong("revision", page.revision())
                                .putZeroArray("rooms", rows));
            }
        }
    }

    /**
     * Returns the caller-visible replicated state snapshot.
     */
    @ClientCommand(value = 7, name = "Read state", execution = ExecutionMode.SESSION)
    public static final class State extends Command {

        /**
         * Creates the framework-instantiated State component.
         */
        public State() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            send(player, world.snapshot(player).putString("kind", "state"));
        }
    }

    /**
     * Demonstrates important, replaceable and best-effort response policies.
     */
    @ClientCommand(value = 8, name = "Delivery modes", execution = ExecutionMode.CONCURRENT)
    public static final class Delivery extends Command {

        /**
         * Creates the framework-instantiated Delivery component.
         */
        public Delivery() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            for (String mode : new String[]{"important", "position", "effect"}) {
                var options = mode.equals("important")
                                ? DeliveryOptions.important()
                                : mode.equals("position")
                                ? DeliveryOptions.replaceable("sdk-position", Duration.ofSeconds(2))
                                : DeliveryOptions.bestEffort(Duration.ofSeconds(2));
                response().delivery(options)
                        .setContent(ShowcaseWire.response(map().putString("kind", mode), player))
                        .setRecipientPlayer(player)
                        .write();
            }
        }
    }

    /**
     * Removes the caller from the current room.
     */
    @ClientCommand(value = 9, name = "Leave room", execution = ExecutionMode.SESSION)
    public static final class Leave extends Command {

        /**
         * Creates the framework-instantiated Leave component.
         */
        public Leave() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            synchronized (world.getMutationMonitor()) {
                api().leaveRoom(player, PlayerLeaveRoomMode.UNKNOWN);
                send(player, map().putString("kind", "left"));
            }
        }
    }

    /**
     * Broadcasts to the demonstration channel.
     */
    @ClientCommand(value = 10, name = "Channel chat", execution = ExecutionMode.SESSION)
    public static final class ChannelChat extends Command {

        /**
         * Creates the framework-instantiated ChannelChat component.
         */
        public ChannelChat() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            api().broadcastToChannel(world.getChannel(),
                            map().putString("kind", "channel").putString("text", data.getString("text")));
        }
    }

    /**
     * Returns the demonstration chat-command rate-limit counters.
     */
    @ClientCommand(value = 11, name = "Show rate-limit counters")
    public static final class Limits extends Command {

        /**
         * Creates the framework-instantiated Limits component.
         */
        public Limits() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            var counts = clientCommand().limitCounts().get((short) 4);
            send(player, map().putString("kind", "limits")
                            .putLong("accepted", counts == null ? 0 : counts.accepted())
                            .putLong("rejected", counts == null ? 0 : counts.rejected()));
        }
    }

    /**
     * Handles application-owned stream and UDP keepalive payloads.
     */
    @ClientCommand(value = 12, name = "Application heartbeat")
    public static final class Ping extends Command {

        /**
         * Creates the framework-instantiated Ping component.
         */
        public Ping() {
        }

        /**
         * {@inheritDoc}
         */
        public void execute(Player player, ZeroMap data) {
            var payload = map().putString("kind",
                                    data.containsKey("udp") && data.getBoolean("udp")
                                            ? "udp-heartbeat"
                                            : "heartbeat");
            if (data.containsKey("udp") && data.getBoolean("udp")) {
                response().setContent(ShowcaseWire.response(payload, player))
                        .setRecipientPlayer(player)
                        .onlyUdp()
                        .write();
            } else {
                send(player, payload);
            }
        }
    }
}
