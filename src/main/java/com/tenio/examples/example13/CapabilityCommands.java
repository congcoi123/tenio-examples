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
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ExecutionMode;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.discovery.RoomDirectory;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import java.time.Duration;
import java.util.Map;

/** Separate commands demonstrate bounded execution and application-owned wire responses. */
public final class CapabilityCommands {
  private CapabilityCommands() {}

  @ClientCommand(
      value = CapabilityProtocol.READY,
      name = "Ready",
      execution = ExecutionMode.ROOM,
      mailboxCapacity = 64,
      requestsPerSecond = 2,
      burst = 4)
  public static final class Ready extends AbstractClientCommandHandler<Player, ZeroMap> {
    @Autowired private CapabilityFeatures features;

    @Override
    public void execute(Player player, ZeroMap message) {
      if (player.getCurrentRoom().orElse(null) != features.room()) {
        return;
      }
      synchronized (features.room()) {
        var state = api().getStateReplication().player(player.getIdentity());
        var audience = new ReplicatedState.Audience(player.getIdentity(), true, false);
        try (var changes = state.subscribe(player.getIdentity())) {
          changes.snapshot(audience);
          try {
            state.updateClient(
                audience,
                Map.of("ready", message.getBoolean("ready")),
                message.getLong("revision"));
          } catch (IllegalArgumentException | IllegalStateException | SecurityException conflict) {
            response()
                .setContent(features.snapshot(player).putString("kind", "conflict"))
                .setRecipientPlayer(player)
                .write();
            return;
          }
          var delta = changes.poll(audience);
          api()
              .getStateReplication()
              .room(features.room().getId())
              .updateServer(Map.of("ready", message.getBoolean("ready")), -1);
          var content =
              features
                  .snapshot(player)
                  .putString("kind", "ready")
                  .putBoolean("changed", delta != null);
          response().setContent(content).setRecipientPlayer(player).write();
        }
      }
    }
  }

  @ClientCommand(value = CapabilityProtocol.ROOMS, name = "Find rooms")
  public static final class Rooms extends AbstractClientCommandHandler<Player, ZeroMap> {
    @Override
    public void execute(Player player, ZeroMap message) {
      var directory = api().getRoomDirectory();
      var query = RoomDirectory.Query.group("demo");
      // Subscribe before querying so changes after the snapshot revision can be retained.
      try (var subscription = directory.subscribe(player.getIdentity(), "demo")) {
        var page = directory.query(query);
        subscription.acknowledgeSnapshot(page.revision());
        var rows = array();
        for (var room : page.rooms()) {
          rows.addZeroMap(
              map()
                  .putLong("id", room.id())
                  .putString("name", room.name())
                  .putInteger("slots", room.availableSlots())
                  .putString("state", room.state()));
        }
        response()
            .setContent(
                map()
                    .putString("kind", "rooms")
                    .putInteger("total", page.total())
                    .putLong("revision", page.revision())
                    .putZeroArray("rooms", rows))
            .setRecipientPlayer(player)
            .write();
      }
    }
  }

  @ClientCommand(
      value = CapabilityProtocol.STATE,
      name = "Read state",
      execution = ExecutionMode.SESSION)
  public static final class State extends AbstractClientCommandHandler<Player, ZeroMap> {
    @Autowired private CapabilityFeatures features;

    @Override
    public void execute(Player player, ZeroMap message) {
      synchronized (features.room()) {
        response()
            .setContent(features.snapshot(player).putString("kind", "state"))
            .setRecipientPlayer(player)
            .write();
      }
    }
  }

  @ClientCommand(
      value = CapabilityProtocol.DELIVERY,
      name = "Delivery demo",
      execution = ExecutionMode.CONCURRENT)
  public static final class Delivery extends AbstractClientCommandHandler<Player, ZeroMap> {
    private static final Duration UPDATE_TTL = Duration.ofSeconds(2);

    @Override
    public void execute(Player player, ZeroMap message) {
      response()
          .delivery(DeliveryOptions.important())
          .setContent(map().putString("kind", "important"))
          .setRecipientPlayer(player)
          .write();
      response()
          .delivery(DeliveryOptions.replaceable("demo-position", UPDATE_TTL))
          .setContent(map().putString("kind", "position").putInteger("x", 1))
          .setRecipientPlayer(player)
          .write();
      response()
          .delivery(DeliveryOptions.bestEffort(UPDATE_TTL))
          .setContent(map().putString("kind", "effect"))
          .setRecipientPlayer(player)
          .write();
    }
  }
}
