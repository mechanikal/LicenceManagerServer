package licence_server;
import com.fasterxml.jackson.databind.ObjectMapper;
import licence_server.licence.ActiveLicenceManager;
import licence_server.licence.LicenceInformationReader;
import licence_server.licence.MD5Encoder;
import common.Records;
import licence_server.network.UserRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class UserRequestHandlerTest {

    UserRequestHandler requestHandler;

    private String keyFor(String user) {
        MD5Encoder encoder = new MD5Encoder();
        return encoder.bytestoHexString(encoder.encode(user.getBytes()));
    }
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void beforeEach() throws IOException{
        ActiveLicenceManager activeLicenceManager = new ActiveLicenceManager();
        LicenceInformationReader reader = new LicenceInformationReader("src/test/java/licence_server/test_licenses.json");
        requestHandler = new UserRequestHandler(reader, activeLicenceManager);
    }
    @Test
    void invalidRequestTest(){
        Records.ServerResponse response = requestHandler.handleInvalidRequest();
        assertFalse(response.success());
        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) response;

        assertEquals("unknown", failure.LicenceUsername());
        assertEquals("request invalid", failure.Description());
    }
    @Test
    void validRequestTest(){
        Records.UserRequest request = new Records.UserRequest("Radek", keyFor("Radek"));
        Records.ServerResponse response = requestHandler.handleRequest(request,"192.168.1.10");

        assertTrue(response.success());

        Records.ServerSuccessResponse success = (Records.ServerSuccessResponse) response;
        assertEquals("Radek", success.LicenceUsername());
        assertNotNull(success.Expired());
        assertFalse(success.Expired().isBlank());
    }
    @Test
    void unknownUserRequestTest(){
        Records.UserRequest request = new Records.UserRequest("UnknownUser",keyFor("UnknownUser"));
        Records.ServerResponse response = requestHandler.handleRequest(request, "192.168.1.10");

        assertFalse(response.success());

        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) response;

        assertEquals("UnknownUser", failure.LicenceUsername());
        assertEquals("User not found", failure.Description());
    }
    @Test
    void invalidLicenceKeyTest(){
        Records.UserRequest request = new Records.UserRequest("Radek","invalid-key");

        Records.ServerResponse response = requestHandler.handleRequest(request,"192.168.1.10");

        assertFalse(response.success());

        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) response;

        assertEquals("Radek", failure.LicenceUsername());
        assertEquals("Invalid licence key", failure.Description());
    }
    @Test
    void noAvailableSeatsTest(){
        Records.UserRequest request = new Records.UserRequest("Admin",keyFor("Admin"));

        Records.ServerResponse response = requestHandler.handleRequest(request,"192.168.1.10");

        assertFalse(response.success());

        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) response;

        assertEquals("Admin", failure.LicenceUsername());
        assertEquals("no available licence seats", failure.Description());
    }
    @Test
    void unknownIPTest(){
        Records.UserRequest request = new Records.UserRequest("Radek",keyFor("Radek"));

        Records.ServerResponse response = requestHandler.handleRequest(request,"172.16.0.10");

        assertFalse(response.success());

        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) response;

        assertEquals("Radek", failure.LicenceUsername());
        assertEquals("IP address not recognized", failure.Description());
    }
    @Test
    void allSeatsOccupiedTest(){
        Records.UserRequest request = new Records.UserRequest("Flash", keyFor("Flash"));

        Records.ServerResponse first = requestHandler.handleRequest(request, "127.0.0.1");
        Records.ServerResponse second = requestHandler.handleRequest(request, "127.0.0.1");
        Records.ServerResponse third = requestHandler.handleRequest(request, "127.0.0.1");

        assertTrue(first.success());
        assertTrue(second.success());
        assertFalse(third.success());

        Records.ServerFailureResponse failure = (Records.ServerFailureResponse) third;

        assertEquals("Flash", failure.LicenceUsername());
        assertEquals("no available licence seats",failure.Description()
        );
    }
    @Test
    void licenceExpiredTest() throws InterruptedException{
        Records.UserRequest request = new Records.UserRequest("Flash",keyFor("Flash"));

        Records.ServerResponse first = requestHandler.handleRequest(request, "127.0.0.1");

        Records.ServerResponse second = requestHandler.handleRequest(request, "127.0.0.1");

        assertTrue(first.success());
        assertTrue(second.success());

        Thread.sleep(3000); // wait until previous licence expires
        Records.ServerResponse third = requestHandler.handleRequest(request, "127.0.0.1");
        assertTrue(third.success());

        Records.ServerSuccessResponse success = (Records.ServerSuccessResponse) third;

        assertEquals("Flash", success.LicenceUsername());
        assertNotNull(success.Expired());
        assertFalse(success.Expired().isBlank());
    }
}
