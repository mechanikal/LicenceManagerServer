package licence_server.network;

import licence_server.licence.ActiveLicenceListener;
import licence_server.licence.ActiveLicenceManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ServerCore {
    ServerDiscover serverDiscover;
    ServerMLS serverMLS;
    Thread threadDiscover;
    Thread threadMLS;
    private List<String> activeLicences;
    private List<String> discoveryRequests;

    public ServerCore(int mlsPort, ActiveLicenceListener activeLicenceListener, BlockingQueue<String> discoveryRequestQueue) throws UnknownHostException {
        serverMLS = new ServerMLS(mlsPort,"src/main/resources/licenses.json",activeLicenceListener);
        serverDiscover = new ServerDiscover(mlsPort,discoveryRequestQueue);
    }

    public void run(){
        threadDiscover = new Thread(serverDiscover);
        threadMLS = new Thread(serverMLS);
        threadDiscover.start();
        threadMLS.start();
    }

    public void stop(){
        serverMLS.stop();
        serverDiscover.stop();
        try {
            threadMLS.join();
        }catch (InterruptedException _){}
        try {
            threadDiscover.join();
        }catch (InterruptedException _){}
    }
}
