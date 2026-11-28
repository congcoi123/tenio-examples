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
import com.tenio.core.ApplicationLauncher;
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.Bootstrap;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.bootstrap.annotation.Setting;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.result.ConnectionEstablishedResult;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventConnectionEstablishedResult;
import com.tenio.core.handler.event.EventPlayerLogin;
import com.tenio.core.network.entity.session.Session;
import com.tenio.examples.server.AbstractExampleConfiguration;
import com.tenio.examples.server.SharedEventKey;
import com.tenio.examples.server.ZeroMapCommandPolicy;

/**
 * Run with configuration.example13.xml; the matching client exercises framework services over
 * localhost TCP.
 */
@Bootstrap
public final class TestServerCapabilities {

    public static void main(String[] args) {
        ApplicationLauncher.run(TestServerCapabilities.class, args);
    }

    @Setting
    public static final class Configuration extends AbstractExampleConfiguration {
    }

    @Component
    public static final class Commands extends ZeroMapCommandPolicy {
    }

    @EventHandler
    public static final class Login extends AbstractHandler implements EventConnectionEstablishedResult<ZeroMap>, EventPlayerLogin<Player> {

        @Autowired
        private CapabilityFeatures features;

        @Override
        public void onConnectionEstablishedResult(Session session, ZeroMap message, ConnectionEstablishedResult result) {
            if (result == ConnectionEstablishedResult.SUCCESS) {
                // Local demonstration only. A deployed application must authenticate this identity.
                api().login(message.getString(SharedEventKey.KEY_PLAYER_LOGIN), session);
            }
        }

        @Override
        public void onPlayerLogin(Player player) {
            synchronized (features.room()) {
                features.enroll(player);
                var token = api().getRecoveryManager().issue(player);
                var content = features.snapshot(player)
                                .putString("kind", "welcome")
                                .putString("resume-token", token)
                                .putLong("sequence", 0);
                response().setContent(content).setRecipientPlayer(player).write();
            }
        }
    }
}
