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

import com.tenio.common.data.DataUtility;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.examples.client.TCP;
import com.tenio.examples.server.SharedEventKey;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Finite localhost walkthrough: login, directory, revisions/deltas, delivery and token recovery.
 */
public final class TestClientCapabilities {

    private static final int PORT = 8032;
    private static final int TIMEOUT_SECONDS = 5;

    private TestClientCapabilities() {
    }

    public static void main(String[] args) throws Exception {
        exercise("localhost", PORT);
        System.out.println("PASS: command routing, rooms, state revisions/deltas, delivery and recovery");
        System.out.println("Performance: select the monitor Performance tab or inspect logs/performance/");
    }

    /**
     * Runs the walkthrough and fails if a server response violates the demonstrated contract.
     */
    public static void exercise(String host, int port) throws Exception {
        String identity = "capability-" + UUID.randomUUID();
        String token;
        long revision;
        try (var connection = new Connection(host, port)) {
            connection.tcp.send(DataUtility.newZeroMap().putString(SharedEventKey.KEY_PLAYER_LOGIN, identity));
            var welcome = connection.await("welcome");
            token = welcome.getString("resume-token");
            revision = welcome.getLong("revision");
            connection.tcp.send(command(CapabilityProtocol.ROOMS));
            require(connection.await("rooms").getInteger("total") == 1, "One public room is expected");
            connection.tcp.send(command(CapabilityProtocol.READY)
                            .putBoolean("ready", true)
                            .putLong("revision", revision));
            var ready = connection.await("ready");
            require(ready.getBoolean("ready") && ready.getBoolean("changed"), "Ready delta was not applied");
            require(ready.getLong("revision") == revision + 1, "Revision must advance once");
            connection.tcp.send(command(CapabilityProtocol.READY)
                            .putBoolean("ready", false)
                            .putLong("revision", revision));
            var conflict = connection.await("conflict");
            require(conflict.getBoolean("ready"), "Stale revision changed state");
            revision = conflict.getLong("revision");
            connection.tcp.send(command(CapabilityProtocol.DELIVERY));
            connection.await("important");
            connection.await("position");
            connection.await("effect");
            // Rejections must not break subsequent requests or accidentally invoke a legacy handler.
            connection.tcp.send(command((short) 999));
            connection.tcp.send(DataUtility.newZeroMap().putString("op", "wrong wire type"));
            connection.tcp.send(DataUtility.newZeroMap().putString("text", "missing command"));
            connection.tcp.send(command(CapabilityProtocol.STATE));
            require(connection.await("state").getLong("revision") == revision, "State read failed");
        }
        // Snapshot recovery can also replace an old connection whose EOF has not yet been observed.
        try (var resumed = new Connection(host, port)) {
            resumed.tcp.send(DataUtility.newZeroMap()
                            .putString("identity", identity)
                            .putString("resume-token", token)
                            .putLong("sequence", 0));
            var response = resumed.await("resumed");
            require(!token.equals(response.getString("resume-token")), "Recovery token was not rotated");
            var snapshot = response.getZeroMap("snapshot");
            require(snapshot.getLong("revision") == revision && snapshot.getBoolean("ready"),
                    "Recovery did not preserve authoritative state");
            resumed.tcp.send(command(CapabilityProtocol.STATE));
            require(resumed.await("state").getLong("revision") == revision,
                    "Commands did not resume on the replacement session");
        }
    }

    private static ZeroMap command(short code) {
        return DataUtility.newZeroMap().putShort("op", code);
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw new IllegalStateException(reason);
        }
    }

    private static final class Connection implements AutoCloseable {

        private final TCP tcp;
        private final LinkedBlockingQueue<ZeroMap> messages = new LinkedBlockingQueue<>();

        private Connection(String host, int port) throws IOException {
            tcp = new TCP(host, port, 5000);
            tcp.receive(data -> {
                        if (data instanceof ZeroMap message) {
                            messages.add(message);
                        }
                    });
        }

        private ZeroMap await(String kind) throws InterruptedException, IOException {
            var message = messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (message == null || !kind.equals(message.getString("kind"))) {
                throw new IOException("Expected server response: " + kind);
            }
            return message;
        }

        @Override
        public void close() {
            tcp.close();
        }
    }
}
