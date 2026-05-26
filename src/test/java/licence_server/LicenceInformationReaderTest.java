package licence_server;
import licence_server.licence.LicenceInformationReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LicenceInformationReaderTest {
    private LicenceInformationReader reader;
    @BeforeEach
    public void setUp() throws IOException {
        reader = new LicenceInformationReader("src/test/java/licence_server/test_licenses.json");
    }
    @Test
    public void getUserTest(){
        List<String> expectedUsers = new ArrayList<>(Arrays.asList("Radek","Guest","Admin","Flash"));
        Assertions.assertEquals(reader.getUsers(),expectedUsers);
    }
    @Test
    public void getIPTest(){
        List<String> expectedUsers = new ArrayList<>(Arrays.asList("10.0.0.0/24", "192.168.1.0/24"));
        Assertions.assertEquals(reader.getIPAddresses("Radek"),expectedUsers);
    }
    @Test
    public void getValidationTimeTest(){
        int expectedValidationTime = 600;
        Assertions.assertEquals(expectedValidationTime, reader.getValidationTime("Radek"));
    }
    @Test
    public void getAvailableSeats(){
        int expectedAvailableSeats = 600;
        Assertions.assertEquals(expectedAvailableSeats, reader.getValidationTime("Radek"));
    }
}
