package licence_server.licence;


import common.Records;

import java.time.Instant;
import java.util.*;


public class ActiveLicenceManager {


    private final Map<String, List<Records.LicenceData>> activeLicences;
    private final List<ActiveLicenceListener> activeLicenceListeners = new ArrayList<>();

    public ActiveLicenceManager() {
        activeLicences = new HashMap<>();
    }

    public String rentLicence(String user, Integer time, String seatIP){
        Instant expiration = Instant.now().plusSeconds(time);
        if (activeLicences.containsKey(user)){
            activeLicences.get(user).add(new Records.LicenceData(seatIP,expiration));
        }else {
            List<Records.LicenceData> licences = new ArrayList<>();
            licences.add(new Records.LicenceData(seatIP,expiration));
            activeLicences.put(user,licences);
        }
        for (ActiveLicenceListener listener : activeLicenceListeners) {
            listener.update(getActiveLicences());
        }
        return expiration.toString();
    }

    private void updateActiveLicences(){
        Instant now = Instant.now();
        for (List<Records.LicenceData> licences : activeLicences.values()) {
            licences.removeIf(licence -> licence.ExpirationDate().isBefore(now));
        }
    }

    public int getUserActiveSeats(String user){
        updateActiveLicences();
        if (activeLicences.containsKey(user)){
            return activeLicences.get(user).size();
        }else {
            return 0;
        }
    }

    public Map<String, List<Records.LicenceData>> getActiveLicences() {
        Map<String, List<Records.LicenceData>> copy = new HashMap<>();
        for (var entry : activeLicences.entrySet()) {
            copy.put(entry.getKey(),new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public void addActiveLicenceListener(ActiveLicenceListener activeLicenceListener){
        activeLicenceListeners.add(activeLicenceListener);
    }
}
