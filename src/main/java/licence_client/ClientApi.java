package licence_client;

import common.Records;
import licence_client.network.ClientDiscover;
import licence_client.network.ClientMLS;

import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientApi {
    private ClientMLS mls;
    private ClientDiscover discover;
    private Thread threadDiscover;
    private Thread threadMLS;
    private CompletableFuture<Records.ServerInfo> serverInfoFuture;
    private CompletableFuture<Records.ServerResponse> licenceDataFuture;
    private volatile Records.ServerResponse currentLicenceInfo;
    private Records.ServerInfo currentServerInfo;
    private String user;
    private String key;
    private Timer refreshTimer;

    public void start(){
        serverInfoFuture = new CompletableFuture<>();
        discover = new ClientDiscover(serverInfoFuture);
        threadDiscover = new Thread(discover);
        threadDiscover.start();
    }

    public void setLicence(String licenceUser, String licenceKey){
        user = licenceUser;
        key = licenceKey;
        currentLicenceInfo = null;
    }

    public Records.ServerResponse getLicenceToken(){
        if (serverInfoFuture == null) {
            return new Records.ServerFailureResponse("unknown",false,"call start() to connect to server");
        }
        if(user == null || key == null){
            return new Records.ServerFailureResponse("unknown",false,"no licence provided");
        }
        if (currentLicenceInfo == null){
            if (currentServerInfo == null) {
                try {
                    currentServerInfo = serverInfoFuture.get(3, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    return new Records.ServerFailureResponse(user, false, e.getMessage());
                } catch (InterruptedException e) {
                    return new Records.ServerFailureResponse(user, false, "interrupted");
                } catch (TimeoutException e) {
                    return new Records.ServerFailureResponse(user, false, "server not found");
                }
            }
            licenceDataFuture = new CompletableFuture<>();
            mls = new ClientMLS(user,key,currentServerInfo,licenceDataFuture);
            threadMLS = new Thread(mls);
            threadMLS.start();
            try{
                currentLicenceInfo = licenceDataFuture.get();
                if (currentLicenceInfo.success()){
                    setupRefresher(Instant.parse(((Records.ServerSuccessResponse)(currentLicenceInfo)).Expired()));
                }
                return currentLicenceInfo;
            } catch (ExecutionException e) {
                return new Records.ServerFailureResponse(user,false,e.getMessage());
            } catch (InterruptedException e) {
                return new Records.ServerFailureResponse(user,false,"interrupted");
            }
        }else {
            if (!isLicenceActive()){
                Records.ServerResponse expiredLicence = currentLicenceInfo;
                refreshLicence();
                return expiredLicence;
            }
            return currentLicenceInfo;
        }
    }

    public void stop(){
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        if  (discover!=null){
            discover.stop();
        }
        if (mls!=null){
            mls.stop();
        }
        user = null;
        key = null;
        serverInfoFuture = null;
        currentLicenceInfo = null;
        currentServerInfo = null;
        if (threadDiscover!=null) {
            try {
                threadDiscover.join();
            } catch (InterruptedException _) {
            }
        }
        if (threadMLS!=null) {
            try {
                threadMLS.join();
            } catch (InterruptedException _) {
            }
        }
    }

    private boolean isLicenceActive(){
        Instant now = Instant.now();
        if (currentLicenceInfo == null){
            return false;
        }
        if (!currentLicenceInfo.success()){
            return false;
        }
        Instant expiration = Instant.parse(((Records.ServerSuccessResponse)currentLicenceInfo).Expired());
        return expiration.isAfter(now);
    }

    private void refreshLicence(){
        currentLicenceInfo = null;
        licenceDataFuture = new CompletableFuture<>();
        mls = new ClientMLS(user,key,currentServerInfo,licenceDataFuture);
        threadMLS = new Thread(mls);
        threadMLS.start();
        licenceDataFuture.thenAccept(licenceInfo -> {
            if (licenceInfo.success()){
                setupRefresher(Instant.parse(((Records.ServerSuccessResponse)(licenceInfo)).Expired()));
            }
            currentLicenceInfo = licenceInfo;
        });
    }

    private void setupRefresher(Instant expiration){
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        Instant now = Instant.now();
        Duration duration = Duration.between(now, expiration);
        if ((duration.compareTo(Duration.ZERO))< 0) duration = Duration.ZERO;
        refreshTimer = new Timer();
        TimerTask task = new TimerTask() {public void run() {
            refreshLicence();
        }};
        refreshTimer.schedule(task, duration.toMillis());
    }

}
