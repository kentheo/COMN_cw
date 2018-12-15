/** Kendeas Theofanous s1317642 */

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;



public class Receiver2a {
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
        short expected_seqnum = 1;
        byte[] expected_seqnum_bytes;
        short correction = 0;
        byte[] correction_bytes;

        while (true) {
            expected_seqnum_bytes = Tools.convertShortToByte(expected_seqnum);
            correction_bytes = Tools.convertShortToByte(correction);
            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
            receiverSocket.receive(packet);

            receivedData = packet.getData();
            InetAddress ipAddress = packet.getAddress();
            int sender_port = packet.getPort();

            ack_bytes[0] = receivedData[0];
            ack_bytes[1] = receivedData[1];

            // Check the eof flag. i.e. 3rd byte != 0 and if the ack == expected seq
            if (receivedData[2] != 0 && hasSeqNum(ack_bytes, expected_seqnum_bytes)) {
                byte[] properTemp = Arrays.copyOfRange(receivedData, 3, packet.getLength());
                baos.write(properTemp, 0, properTemp.length);
                DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                receiverSocket.send(ack_packet);
                break;
            } else {
                // Check if the received ack bytes are equal to the expected sequence number bytes
                if (hasSeqNum(ack_bytes, expected_seqnum_bytes)) {
                    // Write the data
                    byte[] temp = Arrays.copyOfRange(receivedData, 3, receivedData.length);
                    baos.write(temp, 0, temp.length);
                    // Send acknowledgement
                    DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                    receiverSocket.send(ack_packet);

                    // Re-initialize the receivedData and increment expected sequence number
                    receivedData = new byte[1027];
                    correction = expected_seqnum;  // Correction variable needed to send that as the Default ack
                    expected_seqnum++;
                } else { // Otherwise, do the Default of the FSM and send an ack packet
                    DatagramPacket ack_packet = new DatagramPacket(correction_bytes, correction_bytes.length, ipAddress, sender_port);
                    receiverSocket.send(ack_packet);
                }
            }
        }
        System.out.println("-------- STOPPED RECEIVING -----------");
        FileOutputStream fos = new FileOutputStream(fileName);
        baos.writeTo(fos);
        fos.close();
        receiverSocket.close();
    }

    private static boolean hasSeqNum(byte[] received_ack, byte[] expected_seqnum) {
        return received_ack[0] == expected_seqnum[0] && received_ack[1] == expected_seqnum[1];
    }

}
