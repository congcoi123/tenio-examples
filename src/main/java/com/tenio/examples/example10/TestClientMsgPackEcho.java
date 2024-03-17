/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.examples.example10;

/**
 * This class shows how a client communicates with the server:<br>
 * 1. Create connections.<br>
 * 2. Send a login request.<br>
 * 3. Receive a response for login success and send a UDP connection
 * request.<br>
 * 4. Receive a response for allowed UDP connection.<br>
 * 5. Send messages via UDP connection and get these echoes from the server.<br>
 * 6. Close connections.
 */
public final class TestClientMsgPackEcho {
//
//  private static final int SOCKET_PORT = 8032;
//  private final TCP tcp;
//  private final Session session;
//  private final String playerName;
//  private final NetworkWriterStatistic networkWriterStatistic;
//  private UDP udp;
//
//  public TestClientMsgPackEcho() {
//    playerName = ClientUtility.generateRandomString(5);
//
//    // create a new TCP object and listen for this port
//    tcp = new TCP(SOCKET_PORT);
//    tcp.receive(this);
//    session = tcp.getSession();
//    session.setName(playerName);
//
//    networkWriterStatistic = NetworkWriterStatistic.newInstance();
//
//    // send a login request
//    var request =
//        DataUtility.newMsgMap().putString(SharedEventKey.KEY_PLAYER_LOGIN, playerName);
//    tcp.send(request);
//
//    System.out.println("Login Request -> " + request);
//  }
//
//  /**
//   * The entry point.
//   */
//  public static void main(String[] args) {
//    new TestClientMsgPackEcho();
//  }
//
//  @Override
//  public void sessionRead(Session session, DataCollection message) {
//    System.out.println("[KCP SESSION READ] " + message);
//  }
//
//  @Override
//  public void sessionException(Session session, Exception exception) {
//    System.out.println("[KCP EXCEPTION] " + exception.getMessage());
//  }
//
//  @Override
//  public void setSessionManager(SessionManager sessionManager) {
//    // do nothing
//  }
//
//  @Override
//  public void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic) {
//    // do nothing
//  }
//
//  @Override
//  public DataType getDataType() {
//    return DataType.MSG_PACK;
//  }
//
//  @Override
//  public void setDataType(DataType dataType) {
//    // do nothing
//  }
//
//  @Override
//  public void channelActiveIn(Session session) {
//    System.out.println("[KCP ACTIVATED] " + session.toString());
//  }
//
//  @Override
//  public void channelInactiveIn(Session session) {
//    System.out.println("[KCP INACTIVATED] " + session.toString());
//  }
//
//  @Override
//  public void onReceivedTCP(byte[] binaries) {
//    var parcel = (MsgPackMap) DataUtility.binaryToCollection(DataType.MSG_PACK, binaries);
//
//    System.err.println("[RECV FROM SERVER TCP] -> " + parcel);
//    var pack = parcel.getMsgPackArray(SharedEventKey.KEY_ALLOW_TO_ACCESS_UDP_CHANNEL);
//
//    switch (pack.getInteger(0)) {
//      case DatagramEstablishedState.ALLOW_TO_ACCESS -> {
//        // now you can send request for UDP connection request
//        var request =
//            DataUtility.newMsgMap().putString(SharedEventKey.KEY_PLAYER_LOGIN, playerName);
//        // create a new UDP object and listen for this port
//        udp = new UDP(pack.getInteger(1));
//        udp.receive(this);
//        udp.send(request);
//
//        System.out.println(
//            udp.getLocalAddress().getHostAddress() + ", " + udp.getLocalPort() + " Request a UDP " +
//                "connection -> " + request);
//      }
//      case DatagramEstablishedState.ESTABLISHED -> {
//        // the UDP connected successful, you now can send test requests
//        System.out.println("Start the conversation ...");
//
//        var ukcp = initializeKcp(pack.getInteger(1));
//        kcpProcessing();
//
//        for (int i = 1; i <= 100; i++) {
//          var request = DataUtility.newMsgMap().putString(SharedEventKey.KEY_CLIENT_SERVER_ECHO,
//              String.format("Hello from client %d", i));
//          ukcp.send(request.toBinary());
//
//          try {
//            Thread.sleep(1000);
//          } catch (InterruptedException e) {
//            e.printStackTrace();
//          }
//        }
//
//        session.setUkcp(null);
//        udp.close();
//        tcp.close();
//      }
//    }
//  }
//
//  private Ukcp initializeKcp(int conv) {
//    var kcpWriter = new KcpWriterHandler(udp.getDatagramSocket(),
//        udp.getLocalAddress(), udp.getRemotePort());
//    var ukcp =
//        new Ukcp(conv, KcpConfiguration.PROFILE, session, this, kcpWriter, networkWriterStatistic);
//    ukcp.getKcpIoHandler().channelActiveIn(session);
//
//    return ukcp;
//  }
//
//  private void kcpProcessing() {
//    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
//        () -> {
//          if (session.containsKcp()) {
//            var ukcp = session.getUkcp();
//            ukcp.update();
//            ukcp.receive();
//          }
//        }, 0, 10, TimeUnit.MILLISECONDS);
//  }
//
//  @Override
//  public void onReceivedUDP(byte[] binary) {
//    session.getUkcp().input(binary);
//  }
}
