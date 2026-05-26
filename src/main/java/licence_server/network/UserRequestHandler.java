package licence_server.network;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.Records;
import licence_server.licence.ActiveLicenceManager;
import licence_server.licence.LicenceInformationReader;
import licence_server.licence.MD5Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class UserRequestHandler {
    LicenceInformationReader reader;
    ActiveLicenceManager statusManager;
    MD5Encoder keyEncoder;
    ObjectMapper mapper;

    public UserRequestHandler(LicenceInformationReader licenceInformationReader, ActiveLicenceManager activeLicenceManager) throws IOException {
        reader = licenceInformationReader;
        keyEncoder = new MD5Encoder();
        statusManager = activeLicenceManager;
        mapper = new ObjectMapper();
    }

    public synchronized Records.ServerResponse handleRequest(Records.UserRequest userRequest, String clientIP){
        String user = userRequest.LicenceUsername();
        List<String> users = reader.getUsers();
        if (users == null || !users.contains(user)){
            return new Records.ServerFailureResponse(user,false,"User not found");
        }
        String serverKey = keyEncoder.bytestoHexString(keyEncoder.encode(user.getBytes()));
        String userKey = userRequest.LicenceKey().toLowerCase().replaceAll("-","");
        if (!serverKey.equals(userKey)){
            return new Records.ServerFailureResponse(user,false,"Invalid licence key");
        }
        List<String> userIps = reader.getIPAddresses(user);
        if (!ipInPool(clientIP,userIps)){
            return new Records.ServerFailureResponse(user,false,"IP address not recognized");
        }
        Integer availableSeats = reader.getAvailableSeats(user);
        if (availableSeats == null || availableSeats - statusManager.getUserActiveSeats(user) <=0){
            return new Records.ServerFailureResponse(user,false,"no available licence seats");
        }
        String expiration =  statusManager.rentLicence(user, reader.getValidationTime(user),clientIP);
        return new Records.ServerSuccessResponse(user,true,expiration);
    }

    public Records.ServerResponse handleInvalidRequest(){
        return new Records.ServerFailureResponse("unknown",false,"request invalid");
    }

    private boolean ipInPool(String ip,List<String> pool){
        if (pool == null){
            return false;
        }
        for (String p : pool){
            if (ipFitsMask(ip,p)){
                return true;
            }
        }
        return false;
    }

    private boolean ipFitsMask(String ipAddress, String mask){
        if (mask.equals("any")){
            return true;
        }
        if (!(isIPValid(ipAddress,false) && isIPValid(mask,true))){
            return false;
        }
        int addressInt = addressToInt(ipAddress);
        int maskAddressInt = addressToInt(mask.split("/")[0]);
        int maskInt = maskStringToInt(mask.split("/")[1]);
        return (addressInt & maskInt) == (maskAddressInt & maskInt);
    }

    private int maskStringToInt(String maskString){
        int mask = Integer.parseInt(maskString);
        int bitMask = 0;
        for (int i = 0; i < 32; i++){
            if (mask > i){
                bitMask |= 1;
            }
            bitMask <<= i == 31 ? 0 : 1;
        }
        return bitMask;
    }
    private int addressToInt(String ipAddress){
        String[] parts = ipAddress.split("\\.");
        byte[] ipBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipBytes[i] = (byte)Integer.parseInt(parts[i]);
        }
        return ByteBuffer.wrap(ipBytes).getInt();
    }
    private boolean isIPValid(String ipAddress, boolean withMask){
        String address;
        if (ipAddress == null || ipAddress.isEmpty()){
            return false;
        }
        if (withMask){
            String[] parts = ipAddress.split("/");
            if (parts.length != 2){
                return false;
            }
            try {
                Integer.parseInt(parts[1]);
            }catch (NumberFormatException e){
                return false;
            }
            address = parts[0];
        }else {
            address = ipAddress;
        }
        String[] ipParts = address.split("\\.");
        if (ipParts.length != 4){
            return false;
        }
        try {
            for (int i = 0; i < 4; i++) {
                int addressPart = Integer.parseInt(ipParts[i]);
                if (addressPart < 0 || addressPart > 255){
                    return false;
                }
            }
        }catch (NumberFormatException e){
            return false;
        }
        return true;
    }
}
