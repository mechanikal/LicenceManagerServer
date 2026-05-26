package licence_server.licence;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LicenceInformationReader {
    private record LicenceRecord(
            String LicenceUserName,
            Integer SeatCount,
            List<String> IPAddresses,
            Integer ValidationTime
    ){}
    private record LicencePayload(
            List<LicenceRecord> payload
    ){}
    private LicencePayload licencePayload;

    public LicenceInformationReader(String licenceFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        licencePayload = objectMapper.readValue(new File(licenceFilePath),LicencePayload.class);
    }

    public List<String> getUsers(){
        List<String> users = new ArrayList<>();
        for  (LicenceRecord licenceRecord : licencePayload.payload) {
            users.add(licenceRecord.LicenceUserName);
        }
        return users;
    }

    public Integer getValidationTime(String username){
        for(LicenceRecord licenceRecord : licencePayload.payload) {
            if(licenceRecord.LicenceUserName.equals(username)){
                return licenceRecord.ValidationTime;
            }
        }
        return null;
    }

    public List<String> getIPAddresses(String username){
        for(LicenceRecord licenceRecord : licencePayload.payload) {
            if (licenceRecord.LicenceUserName.equals(username)){
                return licenceRecord.IPAddresses;
            }
        }
        return null;
    }

    public Integer getAvailableSeats(String username){
        for(LicenceRecord licenceRecord : licencePayload.payload) {
            if (licenceRecord.LicenceUserName.equals(username)){
                return licenceRecord.SeatCount;
            }
        }
        return null;
    }

}
