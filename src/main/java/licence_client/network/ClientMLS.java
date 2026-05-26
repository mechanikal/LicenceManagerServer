package licence_client.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Records;

import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ClientMLS implements Runnable {
    String username;
    String key;
    String serverIP;
    int serverPort;
    private final CompletableFuture<Records.ServerResponse> response;

    public ClientMLS(String licenceUsername, String licenceKey, Records.ServerInfo serverInfo, CompletableFuture<Records.ServerResponse> resultFuture) {
        username = licenceUsername;
        key = licenceKey;
        serverIP = serverInfo.ipAddress();
        serverPort = serverInfo.port();
        response = resultFuture;
    }

    @Override
    public void run() {
        try (
                Socket socketMLS = new Socket(serverIP, serverPort);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socketMLS.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketMLS.getOutputStream()))
        ) {
            ObjectMapper mapper = new ObjectMapper();
            Records.UserRequest request = new Records.UserRequest(username, key);
            String requestString = mapper.writeValueAsString(request);
            writer.write(requestString);
            writer.newLine();
            writer.flush();
            String responseString = reader.readLine();
            Records.ServerResponse serverResponse;
            if (resolveResponseType(responseString).equals("success")) {
                serverResponse = mapper.readValue(responseString, Records.ServerSuccessResponse.class);
            }else{
                serverResponse = mapper.readValue(responseString, Records.ServerFailureResponse.class);
            }
            response.complete(serverResponse);
        }catch (IOException e){
//            System.out.println("IOException"+e.getMessage());
            response.completeExceptionally(e);
        }
    }
    private String resolveResponseType(String response){
        if (response.contains("\"Licence\":true")){
            return "success";
        }
        return "failure";
    }
    public void stop(){}
}
