package licence_server.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.Records;
import licence_server.licence.ActiveLicenceListener;
import licence_server.licence.ActiveLicenceManager;
import licence_server.licence.LicenceInformationReader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerMLS implements Runnable {
    private final UserRequestHandler requestHandler;
    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    public ServerMLS(int port, String licencesFilename, ActiveLicenceListener activeLicenceListener){
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000);
            ActiveLicenceManager activeLicenceManager = new ActiveLicenceManager();
            activeLicenceManager.addActiveLicenceListener(activeLicenceListener);
            LicenceInformationReader licenceInformationReader = new LicenceInformationReader(licencesFilename);
            this.requestHandler = new UserRequestHandler(licenceInformationReader, activeLicenceManager);
        }catch (IOException e) {
//            System.out.println("IOException"+e.getMessage());
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
        if (serverSocket != null) {
            try {
                serverSocket.close();
            }catch (IOException e) {}
        }
    }
    private void handleClient(){
        try (
            Socket clientSocket = serverSocket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ){
            String userData =  reader.readLine();
            ObjectMapper mapper = new ObjectMapper();
            try {
                Records.UserRequest userRequest = mapper.readValue(userData, Records.UserRequest.class);
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                Records.ServerResponse response = requestHandler.handleRequest(userRequest,clientIP);
                String responseString = mapper.writeValueAsString(response);
                writer.write(responseString);
                writer.newLine();
                writer.flush();
            } catch (JsonProcessingException _){
                mapper.writeValue(writer, requestHandler.handleInvalidRequest());
            }
        }catch (SocketTimeoutException _){}
        catch (IOException e){
            if (running) {
                e.printStackTrace();
            }
        }
    }
}
