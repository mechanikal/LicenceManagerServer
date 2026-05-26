package licence_server.network;

import common.MulticastData;

import java.io.*;
import java.net.*;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerDiscover implements Runnable {
    private volatile boolean running = true;
    private final MulticastSocket socket;
    private final int mlsPort;
    private final BlockingQueue<String> queue;

    public ServerDiscover(int serverMLSport, BlockingQueue<String> discoverQueue){
        try {
            InetAddress group = InetAddress.getByName(MulticastData.multicastIP);
            socket = new MulticastSocket(MulticastData.multicastPort);
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.joinGroup(new InetSocketAddress(group, MulticastData.multicastPort),networkInterface);
            socket.setSoTimeout(1000);
            mlsPort = serverMLSport;
            queue = discoverQueue;
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (running) {
            handleClient();
        }
    }

    public void stop(){
        running = false;
        if  (socket != null) {
            socket.close();
        }
    }

    private void handleClient(){
        byte[] buffer = new byte[64];
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String request = new String(
                    packet.getData(), 0, packet.getLength());
            if (request.equals(MulticastData.discoveryRequest)) {
                queue.put(packet.getSocketAddress().toString());
                byte[] responseData = (MulticastData.discoveryResponsePrefix + mlsPort).getBytes();
                InetAddress group = InetAddress.getByName(MulticastData.multicastIP);
                DatagramPacket responsePacket = new DatagramPacket(
                        responseData,
                        responseData.length,
                        group,
                        MulticastData.multicastPort
                );

                socket.send(responsePacket);
            }
        }catch (SocketTimeoutException _){}
        catch (IOException e){
            if (running) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
