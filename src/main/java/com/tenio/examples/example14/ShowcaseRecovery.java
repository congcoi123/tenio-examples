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
import com.tenio.common.data.DataUtility;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.entity.Player;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.recovery.RecoveryManager;
import com.tenio.core.recovery.RecoveryPolicy;

import java.util.function.Supplier;

/**
 * Rotated-token snapshot recovery using the same application mutation lock as room commands.
 */
@Component
public final class ShowcaseRecovery extends AbstractHandler implements RecoveryPolicy {

    @Autowired
    private ShowcaseWorld world;

    /**
     * Creates the framework-instantiated ShowcaseRecovery component.
     */
    public ShowcaseRecovery() {
    }

    /**
     * {@inheritDoc}
     */
    public Credentials credentials(Session session, DataCollection message) {
        var request = ShowcaseWire.request(message);
        if (!request.containsKey("resume-token")) {
            return null;
        }
        return new Credentials(
                request.getString("identity"),
                request.getString("resume-token"),
                ShowcaseWire.number(request, "sequence")
        );
    }

    /**
     * {@inheritDoc}
     */
    public Snapshot snapshot(Player player, long cursor) {
        return new Snapshot(cursor, world.snapshot(player));
    }

    /**
     * {@inheritDoc}
     */
    public void recover(Player player, Session session, Supplier<RecoveryManager.Result> capture) {
        synchronized (world.getMutationMonitor()) {
            resumed(player, session, capture.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resumed(Player player, Session session, RecoveryManager.Result result) {
        var snapshot = (ZeroMap) DataUtility.binariesToCollection(result.snapshot().type(), result.snapshot().bytes());
        var responseData = map().putString("kind", "resumed")
                        .putString("resume-token", result.token())
                        .putLong("sequence", result.sequence())
                        .putString("udp-token", world.rotateUdpToken(player))
                        .putInteger("udp-port", api().getUdpPort())
                        .putZeroMap("snapshot", snapshot);
        response().setContent(ShowcaseWire.response(responseData, player))
                .setRecipientSession(session)
                .write();
    }
}
