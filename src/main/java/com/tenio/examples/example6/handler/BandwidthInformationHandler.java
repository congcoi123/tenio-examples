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

import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventFetchedBandwidthInfo;

@EventHandler
public final class BandwidthInformationHandler extends AbstractHandler
    implements EventFetchedBandwidthInfo {

  @Override
  public void handle(long readBytes, long readPackets, long readDroppedPackets, long writtenBytes,
                     long writtenPackets, long writtenDroppedPacketsByPolicy,
                     long writtenDroppedPacketsByFull) {
    if (isInfoEnabled()) {
      var info = String.format(
          "readBytes: %d, readPackets: %d, readDroppedPackets: %d, writtenBytes: %d, writtenPackets: %d, writtenDroppedPacketsByPolicy: %d, writtenDroppedPacketsByFull: %d, writtenDroppedPackets: %d",
          readBytes, readPackets, readDroppedPackets, writtenBytes, writtenPackets,
          writtenDroppedPacketsByPolicy,
          writtenDroppedPacketsByFull, writtenDroppedPacketsByPolicy + writtenDroppedPacketsByFull);

      info("BANDWIDTH INFO", info);
    }
  }
}
