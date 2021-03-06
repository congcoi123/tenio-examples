/*
The MIT License

Copyright (c) 2016-2021 kong <congcoi123@gmail.com>

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

package com.tenio.examples.example7.handler;

import com.tenio.common.bootstrap.annotation.Component;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.result.PlayerJoinedRoomResult;
import com.tenio.core.extension.AbstractExtension;
import com.tenio.core.extension.events.EventPlayerJoinedRoomResult;
import com.tenio.examples.example7.constant.Example7Constant;
import com.tenio.examples.server.SharedEventKey;

@Component
public final class PlayerJoinedRoomHandler extends AbstractExtension
    implements EventPlayerJoinedRoomResult {

  @Override
  public void handle(Player player, Room room, PlayerJoinedRoomResult result) {
    if (result == PlayerJoinedRoomResult.SUCCESS) {
      var players = room.getReadonlyPlayersList();
      var iterator = players.iterator();

      var pack = array();
      if (iterator.hasNext()) {
        var rplayer = iterator.next();

        var parray = array();
        parray.addString(rplayer.getName());
        parray.addInteger((int) rplayer.getProperty(Example7Constant.PLAYER_POSITION_X));
        parray.addInteger((int) rplayer.getProperty(Example7Constant.PLAYER_POSITION_Y));

        pack.addZeroArray(parray);
      }

      var message = object().putZeroArray(SharedEventKey.KEY_PLAYER_POSITION, pack);

      response().setRecipientPlayers(players).setContent(message.toBinary()).write();
    }
  }
}
