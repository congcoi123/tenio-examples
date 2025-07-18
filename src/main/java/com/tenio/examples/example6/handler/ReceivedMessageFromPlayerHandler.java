/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

package com.tenio.examples.example6.handler;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.common.utility.MathUtility;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.entity.Player;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventReceivedMessageFromPlayer;
import com.tenio.examples.server.SharedEventKey;
import java.util.ArrayList;
import java.util.List;

@EventHandler
public final class ReceivedMessageFromPlayerHandler extends AbstractHandler
    implements EventReceivedMessageFromPlayer<Player> {

  @Override
  public void handle(Player player, DataCollection message) {
    var parcel = map().putString(SharedEventKey.KEY_PLAYER_LOGIN, player.getIdentity())
        .putString(SharedEventKey.KEY_CLIENT_SERVER_ECHO,
            String.format("Echo(%s): %s", player.getIdentity(),
                ((ZeroMap) message).getString(SharedEventKey.KEY_CLIENT_SERVER_ECHO)))
        .putIntegerArray(SharedEventKey.KEY_INTEGER_ARRAY, getSortRandomNumberArray());

    response().setContent(parcel.toBinary()).setRecipientPlayer(player).write();
  }

  private List<Integer> getSortRandomNumberArray() {
    var array = new ArrayList<Integer>();
    for (int i = 0; i < 10; i++) {
      // storing random integers in an array
      array.add(MathUtility.randInt(0, 100));
    }

    return new ArrayList<>(array);
  }
}
