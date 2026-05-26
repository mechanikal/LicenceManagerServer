package common;

import java.time.Instant;
import java.util.List;

public class Records {
    public record UserRequest(String LicenceUsername, String LicenceKey) {}

    public interface ServerResponse { public boolean success(); }
    public record ServerSuccessResponse(String LicenceUsername, boolean Licence,String Expired) implements ServerResponse {
        @Override
        public boolean success() {
            return true;
        }
    }
    public record ServerFailureResponse(String LicenceUsername, boolean Licence, String Description) implements ServerResponse {
        @Override
        public boolean success() {
            return false;
        }
    }
    public record LicenceData(String ipAddress, Instant ExpirationDate) {}
    public record ServerInfo(String ipAddress, int port) {}
}
