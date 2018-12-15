/** Kendeas Theofanous s1317642 */


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 *  rdt 2.2 receiver
 *
 */

public class Receiver1b {

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
        byte[] ack_bytes = new byte[2];
        short ack = 0;
        byte[] previous_seq = Tools.convertShortToByte((short)1); // Initialize to 0

        while (true) {
            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
            receiverSocket.receive(packet);

            receivedData = packet.getData();
            InetAddress ipAddress = packet.getAddress();
            int sender_port = packet.getPort();

            ack_bytes[0] = receivedData[0];
            ack_bytes[1] = receivedData[1];

            // Check the eof flag. i.e. 3rd byte != 0
            if (receivedData[2] != 0) {
                if (receivedData[0] != previous_seq[0] || receivedData[1] != previous_seq[1]) {
                    // Find where the zeros end and copy that array as the receivedData
                    int packetSize = 1023;
                    for (int i = 1023; i >= 0; i--) {
                        if (receivedData[i] != 0) {
                            packetSize = i + 1;
                            break;
                        }
                    }
                    byte[] properTemp = Arrays.copyOfRange(receivedData, 3, packetSize);
                    baos.write(properTemp, 0, properTemp.length);
                    DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                    receiverSocket.send(ack_packet);
                    break;
                }
            } else {
                if (receivedData[0] != previous_seq[0] || receivedData[1] != previous_seq[1]) {
                    byte[] temp = Arrays.copyOfRange(receivedData, 3, receivedData.length);
                    baos.write(temp, 0, temp.length);

                    DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                    receiverSocket.send(ack_packet);

                    previous_seq[0] = receivedData[0];
                    previous_seq[1] = receivedData[1];
                    // Re-initialize the receivedData and change the ack bytes
                    receivedData = new byte[1027];
                } else {
                    DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                    receiverSocket.send(ack_packet);
                    // Send the ack_bytes to be rejected by the sender?
                }
                if (ack == 0) ack = 1;
                else ack = 0;
            }
        }

        FileOutputStream fos = new FileOutputStream(fileName);
        baos.writeTo(fos);
        fos.close();
        receiverSocket.close();
    }

}
