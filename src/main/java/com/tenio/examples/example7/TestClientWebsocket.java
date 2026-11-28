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

package com.tenio.examples.example7;

import com.tenio.common.data.DataType;
import com.tenio.common.data.DataUtility;
import com.tenio.common.data.msgpack.element.MsgPackMap;
import com.tenio.examples.server.SharedEventKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Runs a finite WebSocket login, movement, ping and close test using the JDK transport. TenIO
 * Common supplies MessagePack serialization; no external WebSocket client is required. This Java 21
 * example is separate from the official Java client SDK.
 */
public final class TestClientWebsocket implements WebSocket.Listener {

    private static final int MAX_MESSAGE_LENGTH = 1024 * 1024;
    private static final int TIMEOUT_SECONDS = 10;

    private final ArrayBlockingQueue<Object> messages = new ArrayBlockingQueue<>(16);
    private final ByteArrayOutputStream fragments = new ByteArrayOutputStream();
    private final CompletableFuture<Void> pong = new CompletableFuture<>();
    private final CompletableFuture<Void> closed = new CompletableFuture<>();

    /**
     * Creates a listener with a bounded received queue.
     */
    public TestClientWebsocket() {
    }

    /**
     * Tests a running example 7 server and exits after the server acknowledges close.
     *
     * @param args optional WebSocket URI, followed by a player name
     * @throws Exception if connection, protocol validation or any bounded wait fails
     */
    public static void main(String[] args) throws Exception {
        var uri = URI.create(args.length > 0 ? args[0] : "ws://localhost:8033/websocket");
        var name = args.length > 1 ? args[1] : "WS-" + Long.toUnsignedString(System.nanoTime(), 36);
        var listener = new TestClientWebsocket();
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build()) {
            var socket = client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .buildAsync(uri, listener)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                var login = MsgPackMap.newInstance().putString(SharedEventKey.KEY_PLAYER_LOGIN, name);
                send(socket, login, false);
                System.out.println("Room initialized: " + listener.awaitMessage("i"));
                var move = MsgPackMap.newInstance().putIntegerArray(SharedEventKey.KEY_DATA, new int[]{150, 250});
                send(socket, move, true);
                var response = listener.awaitMessage("m");
                if (!name.equals(response.getString(SharedEventKey.KEY_USER))
                        || !Arrays.equals(new int[]{150, 250}, response.getIntegerArray(SharedEventKey.KEY_DATA))) {
                    throw new IOException("Unexpected movement response: " + response);
                }
                System.out.println("Fragmented movement echoed: " + response);
                socket.sendPing(ByteBuffer.wrap(new byte[]{42})).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                listener.pong.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                System.out.println("Ping acknowledged");
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                System.out.println("WebSocket test passed");
            } finally {
                socket.abort();
            }
        }
    }

    private static void send(WebSocket socket, MsgPackMap message, boolean fragmented) throws Exception {
        var payload = message.toBinaries();
        var packet = ByteBuffer.allocate(payload.length + 1)
                .put(DataType.MSG_PACK.getValue())
                .put(payload)
                .flip();
        if (fragmented) {
            int middle = packet.remaining() / 2;
            socket.sendBinary(packet.slice(0, middle), false).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            socket.sendBinary(packet.slice(middle, packet.remaining() - middle), true).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } else {
            socket.sendBinary(packet, true).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private MsgPackMap awaitMessage(String command) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (true) {
            var received = messages.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (received instanceof Throwable failure) {
                throw new IOException("WebSocket receive failed", failure);
            }
            if (!(received instanceof MsgPackMap message)) {
                throw new IOException("Timed out waiting for command: " + command);
            }
            if (command.equals(message.getString(SharedEventKey.KEY_COMMAND))) {
                return message;
            }
        }
    }

    @Override
    public void onOpen(WebSocket socket) {
        socket.request(1);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data, boolean last) {
        try {
            if (data.remaining() > MAX_MESSAGE_LENGTH - fragments.size()) {
                throw new IOException("WebSocket message exceeds " + MAX_MESSAGE_LENGTH + " bytes");
            }
            var bytes = new byte[data.remaining()];
            data.get(bytes);
            fragments.writeBytes(bytes);
            if (last) {
                var packet = fragments.toByteArray();
                fragments.reset();
                if (packet.length < 2 || packet[0] != DataType.MSG_PACK.getValue()) {
                    throw new IOException("Expected plain, unprefixed TenIO MessagePack");
                }
                var message = DataUtility.binariesToCollection(DataType.MSG_PACK, Arrays.copyOfRange(packet, 1, packet.length));
                if (!(message instanceof MsgPackMap) || !messages.offer(message)) {
                    throw new IOException("Unexpected message or receive queue full");
                }
            }
            socket.request(1);
        } catch (Exception exception) {
            onError(socket, exception);
            socket.abort();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        onError(socket, new IOException("Expected binary messages"));
        socket.abort();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onPong(WebSocket socket, ByteBuffer data) {
        if (data.remaining() == 1 && data.get() == 42) {
            pong.complete(null);
        } else {
            pong.completeExceptionally(new IOException("Unexpected pong payload"));
        }
        socket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        if (statusCode == WebSocket.NORMAL_CLOSURE) {
            closed.complete(null);
        } else {
            closed.completeExceptionally(new IOException("Close " + statusCode + ": " + reason));
        }
        messages.offer(new IOException("Connection closed: " + statusCode));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        messages.clear();
        messages.offer(error);
        pong.completeExceptionally(error);
        closed.completeExceptionally(error);
    }
}
