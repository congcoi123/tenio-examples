/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

package com.tenio.examples.example4.handler;

import com.tenio.core.bootstrap.annotation.AutowiredAcceptNull;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.data.ServerMessage;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventReceivedMessageFromPlayer;
import com.tenio.engine.heartbeat.HeartBeatManager;
import com.tenio.examples.example4.constant.ServerEventKey;
import com.tenio.examples.server.ExampleMessage;
import com.tenio.examples.server.SharedEventKey;

@Component
public final class ReceivedMessageFromPlayerHandler extends AbstractHandler
    implements EventReceivedMessageFromPlayer {

  @AutowiredAcceptNull
  private HeartBeatManager heartBeatManager;

  @Override
  public void handle(Player player, ServerMessage message) {

    var data = (ZeroMap) message.getData();
    if (data.containsKey(SharedEventKey.KEY_PLAYER_REQUEST_NEIGHBOURS)) {
      var request = ExampleMessage.newInstance();
      request.putContent(ServerEventKey.KEY_PLAYER_NAME, player.getName());
      request.putContent(ServerEventKey.KEY_PLAYER_REQUEST,
          data.getString(SharedEventKey.KEY_PLAYER_REQUEST_NEIGHBOURS));

      heartBeatManager.sendMessage("world", request);
    }
  }
}
