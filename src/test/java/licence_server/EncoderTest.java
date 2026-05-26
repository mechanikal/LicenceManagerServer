package licence_server;
import licence_server.licence.MD5Encoder;
import org.junit.jupiter.api.Test;

public class EncoderTest {
    @Test
    public void testEmptyStringHash(){
        String testMessage = "";
        String expectedHash = "d41d8cd98f00b204e9800998ecf8427e";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testaStringHash(){
        String testMessage = "a";
        String expectedHash = "0cc175b9c0f1b6a831c399e269772661";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testAbcStringHash(){
        String testMessage = "abc";
        String expectedHash = "900150983cd24fb0d6963f7d28e17f72";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testMdStringHash(){
        String testMessage = "message digest";
        String expectedHash = "f96b697d7cb7938d525a2f31aaf161d0";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testAlphabetStringHash(){
        String testMessage = "abcdefghijklmnopqrstuvwxyz";
        String expectedHash = "c3fcd3d76192e4007dfb496cca67e13b";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testAlphabetAndDigitsStringHash(){
        String testMessage = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String expectedHash = "d174ab98d277d9f5a5611c2c9f419d9f";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }
    @Test
    public void testDigitsLongStringHash(){
        String testMessage = "12345678901234567890123456789012345678901234567890123456789012345678901234567890";
        String expectedHash = "57edf4a22be3c955ac49da2e2107b67a";

        MD5Encoder encoder = new MD5Encoder();
        byte [] msgBytes = testMessage.getBytes();
        byte [] hashBytes = encoder.encode(msgBytes);
        String hashString = encoder.bytestoHexString(hashBytes);
        assert hashString.equals(expectedHash);
    }

}
