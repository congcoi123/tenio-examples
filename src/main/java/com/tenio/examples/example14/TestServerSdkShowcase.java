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

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.core.ApplicationLauncher;
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.Bootstrap;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.bootstrap.annotation.Scheduled;
import com.tenio.core.bootstrap.annotation.Setting;
import com.tenio.core.command.client.ClientCommandPolicy;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.result.AccessDatagramChannelResult;
import com.tenio.core.entity.define.result.ConnectionEstablishedResult;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventAccessDatagramChannelRequestValidation;
import com.tenio.core.handler.event.EventAccessDatagramChannelRequestValidationResult;
import com.tenio.core.handler.event.EventConnectionEstablishedResult;
import com.tenio.core.handler.event.EventPlayerLogin;
import com.tenio.core.network.entity.session.Session;
import com.tenio.examples.server.AbstractExampleConfiguration;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server counterpart to tenio-client-tools ShowcaseClient; no SDK dependency is needed here.
 */
@Bootstrap
public final class TestServerSdkShowcase {

    private TestServerSdkShowcase() {
    }

    /**
     * Starts the Java 21 showcase server. Client runtimes can use Java 7.
     *
     * @param args optional example XML configuration path
     */
    public static void main(String[] args) {
        ApplicationLauncher.run(TestServerSdkShowcase.class, args);
    }

    /**
     * Reads the example XML and scans only the example-14 application package.
     */
    @Setting
    public static final class Configuration extends AbstractExampleConfiguration {
        /**
         * Creates the framework-instantiated Configuration component.
         */
        public Configuration() {
        }
    }

    /**
     * Extracts the example short operation code from either supported payload format.
     */
    @Component
    public static final class Commands implements ClientCommandPolicy {
        /**
         * Creates the framework-instantiated Commands component.
         */
        public Commands() {
        }

        /**
         * {@inheritDoc}
         */
        public CommandRequest applyPolicy(DataCollection message) {
            var request = ShowcaseWire.request(message);
            if (!request.containsKey("op")) {
                return null;
            }
            return new CommandRequest(request.getShort("op"), request);
        }
    }

    /**
     * Demonstrates a scheduler-owned job without an application-created executor.
     */
    @Component
    public static final class TickJob {

        private static final long INTERVAL_SECONDS = 1;

        @Autowired
        private ShowcaseWorld world;

        /**
         * Creates the framework-instantiated TickJob component.
         */
        public TickJob() {
        }

        /**
         * Advances a demonstration counter on the framework scheduler thread.
         */
        @Scheduled(
                intervalSeconds = INTERVAL_SECONDS,
                initialDelaySeconds = INTERVAL_SECONDS,
                threadName = "SDK_SHOWCASE_TICK")
        public void sample() {
            world.incrementTick();
        }
    }

    /**
     * Handles explicit login and validates token-bound UDP registration.
     */
    @EventHandler
    public static final class Connections extends AbstractHandler
            implements EventConnectionEstablishedResult<DataCollection>,
            EventPlayerLogin<Player>,
            EventAccessDatagramChannelRequestValidation<DataCollection>,
            EventAccessDatagramChannelRequestValidationResult<Player> {

        private final Map<Long, DataType> formats = new ConcurrentHashMap<>();
        private final SecureRandom random = new SecureRandom();

        @Autowired
        private ShowcaseWorld world;

        /**
         * Creates the framework-instantiated Connections component.
         */
        public Connections() {
        }

        /**
         * {@inheritDoc}
         */
        public void onConnectionEstablishedResult(Session session, DataCollection message, ConnectionEstablishedResult result) {
            if (result != ConnectionEstablishedResult.SUCCESS) {
                return;
            }
            var request = ShowcaseWire.request(message);
            String identity = request.getString("name");
            if (identity == null || identity.isBlank() || identity.length() > 64) {
                throw new IllegalArgumentException("Invalid name");
            }
            // Local demonstration identity only; a deployed application must authenticate this handshake.
            formats.put(session.getId(), message.getType());
            try {
                api().login(identity, session);
            } finally {
                formats.remove(session.getId());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void onPlayerLogin(Player player) {
            synchronized (world.getMutationMonitor()) {
                player.setProperty("sdk-format", formats.get(player.getSession().orElseThrow().getId()));
                String udpToken = world.rotateUdpToken(player);
                world.enroll(player);
                String recoveryToken = api().getRecoveryManager().issue(player);
                var welcome = world.snapshot(player)
                        .putString("kind", "welcome")
                        .putString("resume-token", recoveryToken)
                        .putString("udp-token", udpToken)
                        .putInteger("udp-port", api().getUdpPort())
                        .putLong("sequence", 0);
                response().setContent(ShowcaseWire.response(welcome, player))
                        .setRecipientPlayer(player)
                        .write();
            }
        }

        /**
         * {@inheritDoc}
         */
        public Optional<Player> onAccessDatagramChannelRequestValidation(DataCollection message) {
            var request = ShowcaseWire.request(message);
            String identity = request.getString("identity"), provided = request.getString("udp-token");
            if (identity == null || provided == null) {
                return Optional.empty();
            }
            return api()
                    .getPlayerByIdentity(identity)
                    .filter(
                            player -> {
                                Object secret = player.getProperty("sdk-udp-token");
                                return secret instanceof String expected
                                        && MessageDigest.isEqual(
                                        expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                        provided.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            }
                    );
        }

        /**
         * {@inheritDoc}
         */
        public void onAccessDatagramChannelRequestValidationResult(Player player, int convey, AccessDatagramChannelResult result) {
            if (result == AccessDatagramChannelResult.SUCCESS) {
                response().setContent(ShowcaseWire.response(map().putString("kind", "udp-ready").putInteger("convey", convey), player))
                        .setRecipientPlayer(player)
                        .write();
            }
        }
    }
}
