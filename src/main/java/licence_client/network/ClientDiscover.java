package licence_client.network;

import common.MulticastData;
import common.Records;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CompletableFuture;

public class ClientDiscover implements Runnable {
    private volatile boolean running = true;
    private final static int retryLimit = 10;
    private final CompletableFuture<Records.ServerInfo> result;

    public ClientDiscover(CompletableFuture<Records.ServerInfo> resultFuture) {
        result = resultFuture;
    }

    @Override
    public void run() {
        try (
            MulticastSocket socket = new MulticastSocket(null)
        ){
            setupSocket(socket);
            while (running) {
                sendRequest(socket);
                for (int i = 0; i < retryLimit; i++) {
                    if (running) {
                        try {
                            Records.ServerInfo info = receiveAnswer(socket);
                            if (info != null) {
                                result.complete(info);
                                return;
                            }
                        } catch (SocketTimeoutException _) {}
                    }
                }
            }
        }catch (IOException e){
            result.completeExceptionally(e);
        }
    }

    private Records.ServerInfo receiveAnswer(MulticastSocket socket) throws IOException {
        byte[] bufferReceive = new byte[64];
        DatagramPacket packetReceive = new DatagramPacket(bufferReceive, bufferReceive.length);
        socket.receive(packetReceive);
        String address = packetReceive.getAddress().getHostAddress();
        String response = new String(packetReceive.getData(), 0, packetReceive.getLength());
//        System.out.println("Received response from server: " + response);
        if (response.startsWith(MulticastData.discoveryResponsePrefix)){
            try {
               return new Records.ServerInfo(address, Integer.parseInt(response.split(":")[1]));
            }catch (NumberFormatException e){
                return null;
            }
        }
        return null;
    }

    private void sendRequest(MulticastSocket socket) throws IOException {
        byte[] bufferSend = MulticastData.discoveryRequest.getBytes();
        InetAddress group = InetAddress.getByName(MulticastData.multicastIP);
        DatagramPacket packetSend = new DatagramPacket(bufferSend, bufferSend.length,group,MulticastData.multicastPort);
        socket.send(packetSend);
    }

    private void setupSocket(MulticastSocket socket) throws IOException {
        InetAddress group = InetAddress.getByName(MulticastData.multicastIP);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(MulticastData.multicastPort));
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        socket.joinGroup(new InetSocketAddress(group, MulticastData.multicastPort),networkInterface);
        socket.setSoTimeout(2000);
    }

    public void stop(){
        running = false;
    }
}
