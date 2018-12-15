/** Kendeas Theofanous s1317642 */


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Receiver1a {

    //  The receiver should write the received data into a file
    //  You can check the correctness of your implementation by comparing the original file with the received (and saved) file

    public static void main (String args[]) throws Exception {
        // Check if correct number of args is used
        if (args.length != 2) {
            System.out.println("Use the parameters: Receiver1a <Port> <Filename>");
        } else {
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            receiveFileFromSender(port, fileName);
        }
    }

    private static void receiveFileFromSender(int port, String fileName) throws Exception {
        DatagramSocket receiverSocket = new DatagramSocket(port);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] receivedData = new byte[1027];

        while (true) {
            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
            receiverSocket.receive(packet);

            receivedData = packet.getData();

            // Check the eof flag. i.e. 3rd byte != 0
            if (receivedData[2] != 0) {
                // Find where the zeros end and copy that array as the receivedData
                int packetSize = 1023;
                for (int i = 1023; i >= 0; i--) {
                    if (receivedData[i] != 0) {
                        packetSize = i+1;
                        break;
                    }
                }
                byte[] properTemp = Arrays.copyOfRange(receivedData, 3, packetSize);
                baos.write(properTemp, 0, properTemp.length);
                break;
            } else {
                byte[] temp = Arrays.copyOfRange(receivedData, 3, receivedData.length);
                baos.write(temp, 0, temp.length);

                // Re-initialize the receivedData
                receivedData = new byte[1027];
            }
        }

        FileOutputStream fos = new FileOutputStream(fileName);
        baos.writeTo(fos);
        fos.close();
        receiverSocket.close();
        System.out.println("File was successfully received!");
    }

}
