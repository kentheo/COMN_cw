/** Kendeas Theofanous s1317642 */

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Tools {

    public static byte[] convertShortToByte(short number) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(number);
        return bb.array();
    }

    public static int getPacketsAmount(FileInputStream fis, int packet_size) throws IOException {
        int file_size = fis.available();
        return (int) Math.ceil(file_size / packet_size);
    }

    public static short convertByteToShort(byte[] byte_array) {
        final ByteBuffer bb = ByteBuffer.wrap(byte_array);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getShort();
    }
}
