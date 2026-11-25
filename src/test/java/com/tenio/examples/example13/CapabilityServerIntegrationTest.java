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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercises actual bootstrap scanning, TCP routing, optional services and the sample client. */
class CapabilityServerIntegrationTest {
  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  Path working;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void runsCapabilitiesWithTheRealWireProtocol(boolean mixedHandlers) throws Exception {
    var configuration = working.resolve("configuration.xml");
    var xml = Files.readString(Path.of("configuration.example13.xml"));
    if (mixedHandlers) {
      xml =
          xml.replace(
              "default,monitor,history,performance",
              "default,monitor,history,performance,mixed-example");
    }
    Files.writeString(configuration, xml);
    var log = working.resolve("server.log");
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    // Surefire exposes the complete test classpath, also used by the isolated server JVM.
    var classpath =
        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    var server =
        new ProcessBuilder(
                java,
                "-Djava.awt.headless=true",
                "-cp",
                classpath,
                TestServerCapabilities.class.getName(),
                configuration.toString())
            .directory(working.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    try {
      await(
          Duration.ofSeconds(20),
          () -> {
            assertTrue(server.isAlive(), () -> read(log));
            return read(log).contains("[STATE] RUNNING");
          });
      TestClientCapabilities.exercise("localhost", 8032);
      await(
          Duration.ofSeconds(15),
          () -> {
            var directory = working.resolve("logs/performance");
            if (!Files.isDirectory(directory)) {
              return false;
            }
            try (var files = Files.list(directory)) {
              return files.anyMatch(file -> file.toString().endsWith(".tsv") && size(file) > 0);
            }
          });
      var output = read(log);
      assertTrue(output.contains("CLIENT MESSAGE ROUTING") && output.contains("COMMAND:"), output);
      assertEquals(mixedHandlers, output.contains("CLIENT MESSAGE ROUTING WARNING"), output);
      if (mixedHandlers) {
        assertTrue(output.contains("Both handlers are registered"), output);
        assertTrue(output.contains("will not receive gameplay messages"), output);
        assertFalse(output.contains("MIXED_HANDLER_WAS_INVOKED"), output);
      }
      assertFalse(output.contains("Exception in thread"), output);
    } finally {
      server.destroy();
      if (!server.waitFor(15, TimeUnit.SECONDS)) {
        server.destroyForcibly();
        server.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  private static String read(Path file) {
    try {
      return Files.exists(file) ? Files.readString(file) : "";
    } catch (Exception failure) {
      throw new IllegalStateException(failure);
    }
  }

  private static long size(Path file) {
    try {
      return Files.size(file);
    } catch (Exception failure) {
      return 0;
    }
  }

  private static void await(Duration timeout, Check check) throws Exception {
    long deadline = System.nanoTime() + timeout.toNanos();
    do {
      if (check.ready()) {
        return;
      }
      Thread.sleep(100);
    } while (System.nanoTime() < deadline);
    fail("Timed out waiting for the capability server or performance export");
  }

  private interface Check {
    boolean ready() throws Exception;
  }
}
