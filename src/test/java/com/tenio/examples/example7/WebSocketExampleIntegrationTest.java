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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs example 7 in its own JVM with the JDK client and actual MessagePack messages. */
class WebSocketExampleIntegrationTest {

  @TempDir Path working;

  @Test
  void testLoginMovementPingCloseAndSecondLogin() throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    var configuration = working.resolve("configuration.xml");
    Files.writeString(
        configuration,
        Files.readString(Path.of("configuration.example7.xml"))
            .replace(">8033</Port>", ">" + port + "</Port>"));
    var log = working.resolve("server.log");
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    var classpath =
        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    var server =
        new ProcessBuilder(
                java,
                "-Djava.awt.headless=true",
                "-cp",
                classpath,
                TestServerWebsocket.class.getName(),
                configuration.toString())
            .directory(working.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    try {
      long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
      while (!Files.readString(log).contains("[STATE] RUNNING")) {
        assertTrue(server.isAlive(), () -> read(log));
        assertTrue(System.nanoTime() < deadline, () -> read(log));
        Thread.sleep(25);
      }
      var uri = "ws://localhost:" + port + "/websocket";
      TestClientWebsocket.main(new String[] {uri, "WebSocket-A"});
      TestClientWebsocket.main(new String[] {uri, "WebSocket-B"});
    } finally {
      server.destroy();
      boolean stopped = server.waitFor(5, TimeUnit.SECONDS);
      if (!stopped) {
        server.destroyForcibly();
        server.waitFor(5, TimeUnit.SECONDS);
      }
      assertTrue(stopped, () -> read(log));
    }
    var output = read(log);
    assertTrue(output.contains("[STATE] STOPPED"), output);
    assertFalse(output.contains("ClosedSelectorException"), output);
    assertFalse(output.contains("ERROR"), output);
  }

  private String read(Path log) {
    try {
      return Files.readString(log);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
