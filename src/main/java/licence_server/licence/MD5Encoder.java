package licence_server.licence;

import java.util.Arrays;
import java.util.HexFormat;

public class MD5Encoder {
    private final static int INT_BITS = 32;
    private final static int BYTE_BITS = 8;
    private final static int MD5_WINDOW_BITS = 512;

    private int bufferA;
    private int bufferB;
    private int bufferC;
    private int bufferD;

    int[] startBufferStates = new int[4];

    private final int[] constants = new int[64];
    int[] currentMessageBuffer;
    byte[] message;

    public MD5Encoder() {
        Arrays.setAll(constants, i -> (int)(long)(Math.pow(2,32)*Math.abs(Math.sin(i+1))));
    }

    private int helperFunction(int step,int x,int y,int z){
        return
            switch(step){
                case 0 -> (x&y)|(~x&z);
                case 1 -> (x&z)|(y&~z);
                case 2 -> x^y^z;
                case 3 -> y^(x|~z);
                default -> throw new IllegalArgumentException("step must be between 1 and 4");
            };
    }

    public byte[] encode(byte[] data){
        bufferA = 0x67452301;
        bufferB = 0xEFCDAB89;
        bufferC = 0x98BADCFE;
        bufferD = 0x10325476;
        message = addPadding(data);
        for (int i = 0; i < message.length/(MD5_WINDOW_BITS /BYTE_BITS); i++){
            currentMessageBuffer = byteArrayToIntArray(Arrays.copyOfRange(message,i*(MD5_WINDOW_BITS /BYTE_BITS),(i+1)*(MD5_WINDOW_BITS /BYTE_BITS)));
            modifyBuffers();
        }
        return mergeBuffers();
    }

    private byte[] mergeBuffers(){
        byte[] result = new byte[INT_BITS/BYTE_BITS*4];
        return intArrayToByteArray(new int[]{bufferA, bufferB, bufferC, bufferD});
    }

    private void modifyBuffers(){
        int a = 0;
        char[] bufferOrder = {'A','D','C','B'};
        int[] jSteps = {1,5,3,7};
        int[] jStarts = {0,1,5,0};
        int[][] ps = {{7,12,17,22},{5,9,14,20},{4,11,16,23},{6,10,15,21}};

        setStartBufferStates();
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 4; j++){
                for (int k = 0; k < 4; k++){
                    modifyBuffer(bufferOrder[k],(jStarts[i]+(4*j+k)*jSteps[i]) % 16,ps[i][k],a++,i);
                }
            }
        }
        addStartBufferStates();
    }
    private void setStartBufferStates(){
        startBufferStates[0] = bufferA;
        startBufferStates[1] = bufferB;
        startBufferStates[2] = bufferC;
        startBufferStates[3] = bufferD;
    }

    private void addStartBufferStates(){
        bufferA = (int)((long)startBufferStates[0] + bufferA);
        bufferB = (int)((long)startBufferStates[1] + bufferB);
        bufferC = (int)((long)startBufferStates[2] + bufferC);
        bufferD = (int)((long)startBufferStates[3] + bufferD);
    }

    private void modifyBuffer(char buffer,int j,int p, int i,int step){
        if (buffer == 'A'){
            bufferA = bufferB + Integer.rotateLeft(bufferA + helperFunction(step,bufferB,bufferC,bufferD) + currentMessageBuffer[j] + constants[i],p);
        }else if (buffer == 'B'){
            bufferB = bufferC + Integer.rotateLeft(bufferB + helperFunction(step,bufferC,bufferD,bufferA) + currentMessageBuffer[j] + constants[i],p);
        }else if (buffer == 'C'){
            bufferC = bufferD + Integer.rotateLeft(bufferC + helperFunction(step,bufferD,bufferA,bufferB) + currentMessageBuffer[j] + constants[i],p);
        }else if (buffer == 'D'){
            bufferD = bufferA + Integer.rotateLeft(bufferD + helperFunction(step,bufferA,bufferB,bufferC) + currentMessageBuffer[j] + constants[i],p);
        }
    }

    private byte[] addPadding(byte[] data){
        int dataLen = data.length;
        int finalLength = (calculateTargetLength(dataLen * BYTE_BITS) + 64) / BYTE_BITS;
        byte[] dataWithPadding = new byte[finalLength];
        if (finalLength != dataLen) {
            for (int i = 0; i < finalLength; i++) {
                dataWithPadding[i] = i < data.length ?  data[i] : 0;
            }
        }
        dataWithPadding[data.length] = (byte)(1<<7);
        byte[] dataLength = longToLittleEndian(8L * data.length);
        for (int i = 0; i < 8; i++) {
            dataWithPadding[finalLength+i-8] = dataLength[i];
        }
        return dataWithPadding;
    }

    private int calculateTargetLength(int bits){
        int remainder = bits % 512;
        int paddingLength = (512-64)-remainder;
        if (paddingLength < 0){
            paddingLength += 512;
        }
        return bits + paddingLength;
    }

    private byte[] intArrayToByteArray(int[] intArray){
        byte[] byteArray = new byte[intArray.length * INT_BITS/BYTE_BITS];
        for (int i = 0; i < intArray.length; i++){
            byte[] littleEndianInt = intToLittleEndian(intArray[i]);
            byteArray[i*4] = littleEndianInt[0];
            byteArray[i*4+1] = littleEndianInt[1];
            byteArray[i*4+2] = littleEndianInt[2];
            byteArray[i*4+3] = littleEndianInt[3];
        }
        return byteArray;
    }

    private int[] byteArrayToIntArray(byte[] byteArray){
        int bytesInInt = 4;
        int[] intArray = new int[byteArray.length/bytesInInt];
        for (int i = 0; i < intArray.length; i++){
            intArray[i] = 0;
            for (int j = 0; j < bytesInInt; j++){
                intArray[i] |= (byteArray[i*bytesInInt + (bytesInInt-1) -j]& 0xff);
                intArray[i] <<= j==3 ? 0 : BYTE_BITS;
            }
        }
        return intArray;
    }

    private byte[] intToLittleEndian(int value){
        byte[] byteArray = new byte[4];
        byteArray[3] = (byte)(value >> 24);
        byteArray[2] = (byte)(value >> 16);
        byteArray[1] = (byte)(value >> 8);
        byteArray[0] = (byte)value;
        return byteArray;
    }

    private byte[] longToLittleEndian(long value){
        byte[] byteArray = new byte[8];
        byteArray[7] = (byte)(value >> 56);
        byteArray[6] = (byte)(value >> 48);
        byteArray[5] = (byte)(value >> 40);
        byteArray[4] = (byte)(value >> 32);
        byteArray[3] = (byte)(value >> 24);
        byteArray[2] = (byte)(value >> 16);
        byteArray[1] = (byte)(value >> 8);
        byteArray[0] = (byte)value;
        return byteArray;
    }

    public String bytestoHexString(byte[] bytes){
        return HexFormat.of().formatHex(bytes);
    }
}
