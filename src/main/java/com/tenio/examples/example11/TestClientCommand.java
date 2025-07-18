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

package com.tenio.examples.example11;

import com.tenio.common.data.DataType;
import com.tenio.common.data.DataUtility;
import com.tenio.examples.client.ClientUtility;
import com.tenio.examples.client.SocketListener;
import com.tenio.examples.client.TCP;
import com.tenio.examples.server.SharedEventKey;

/**
 * This class shows how a client communicates with the server:<br>
 * 1. Create connections.<br>
 * 2. Send a login request.<br>
 * 3. Receive messages via TCP connection from the server.<br>
 */
public final class TestClientCommand implements SocketListener {

  private static final int SOCKET_PORT = 8032;
  private final TCP tcp;

  public TestClientCommand() {
    // create a new TCP object and listen for this port
    tcp = new TCP(SOCKET_PORT);
    tcp.receive(this);

    String name = ClientUtility.generateRandomString(5);

    // send a login request
    var request = DataUtility.newZeroMap();
    request.putString(SharedEventKey.KEY_PLAYER_LOGIN, name);
    tcp.send(request);

    System.err.println("Login Request -> " + request);
  }

  /**
   * The entry point
   */
  public static void main(String[] args) {
    new TestClientCommand();
  }

  @Override
  public void onReceivedTCP(byte[] binaries) {
    var parcel = DataUtility.binaryToCollection(DataType.ZERO, binaries);

    System.out.println("[RECV FROM SERVER TCP] -> " + parcel.toString());

    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      exception.printStackTrace();
    }

    var request = DataUtility.newZeroMap();
    request.putString(SharedEventKey.KEY_CLIENT_SERVER_ECHO, "Hello from client");
    tcp.send(request);

    System.err.println("[SENT TO SERVER] -> " + request);
  }
}
