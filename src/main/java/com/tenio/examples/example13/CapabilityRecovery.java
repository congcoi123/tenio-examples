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
 * Application-owned snapshot recovery. Login enrolls the player and privately supplies the token.
 * Capture and delivery hold the same room monitor as gameplay mutations. This localhost demo
 * requires authenticated login and encrypted transport before use on a remote deployment.
 */
@Component
public final class CapabilityRecovery extends AbstractHandler implements RecoveryPolicy {
  @Autowired private CapabilityFeatures features;

  @Override
  public Credentials credentials(Session session, DataCollection message) {
    if (!(message instanceof ZeroMap request) || !request.containsKey("resume-token")) {
      return null;
    }
    return new Credentials(
        request.getString("identity"),
        request.getString("resume-token"),
        request.containsKey("sequence") ? request.getLong("sequence") : 0);
  }

  @Override
  public Snapshot snapshot(Player player, long cursor) {
    return new Snapshot(cursor, features.snapshot(player));
  }

  @Override
  public void recover(Player player, Session session, Supplier<RecoveryManager.Result> capture) {
    Object context = player.getCurrentRoom().<Object>map(room -> room).orElse(player);
    synchronized (context) {
      resumed(player, session, capture.get());
    }
  }

  @Override
  public void resumed(Player player, Session session, RecoveryManager.Result result) {
    var snapshot =
        (ZeroMap)
            DataUtility.binariesToCollection(result.snapshot().type(), result.snapshot().bytes());
    var content =
        map()
            .putString("kind", "resumed")
            .putString("resume-token", result.token())
            .putLong("sequence", result.sequence())
            .putZeroMap("snapshot", snapshot);
    response().setContent(content).setRecipientSession(session).write();
  }
}
