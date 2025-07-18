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

package com.tenio.examples.example1.handler;

import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.result.PlayerLoginResult;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventPlayerLoginResult;
import com.tenio.examples.server.SharedEventKey;

@EventHandler
public final class PlayerLoginHandler extends AbstractHandler
    implements EventPlayerLoginResult<Player> {

  @Override
  public void handle(Player player, PlayerLoginResult result) {
    if (result == PlayerLoginResult.SUCCESS) {
      var request = msgmap().putString(SharedEventKey.KEY_PLAYER_LOGIN,
          String.format("Welcome to server: %s", player.getIdentity()));

      response().setContent(request.toBinary()).setRecipientPlayer(player).write();
    }
  }
}
