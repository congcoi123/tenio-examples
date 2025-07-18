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
import com.tenio.core.handler.event.EventSystemMonitoring;
import com.tenio.examples.client.ServerUtility;

@EventHandler
public final class SystemMonitoringHandler extends AbstractHandler
    implements EventSystemMonitoring {

  @Override
  public void handle(double cpuUsage, long totalMemory, long usedMemory, long freeMemory,
                     int countRunningThreads) {
    if (isInfoEnabled()) {
      var info = String.format(
          "cpuUsage: %.2f%%; totalMemory: %.3fMB; usedMemory: %.3fMB; freeMemory: %.3fMB; runningThreads: %d",
          (float) cpuUsage * 100, ServerUtility.convertBytesToMB(totalMemory),
          ServerUtility.convertBytesToMB(usedMemory), ServerUtility.convertBytesToMB(freeMemory),
          countRunningThreads);

      info("SYSTEM MONITORING", info);
    }
  }
}
