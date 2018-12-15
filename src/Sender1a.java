/** Kendeas Theofanous s1317642 */

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1a {

    public static void main (String args[]) throws Exception {

        // Check if correct number of args is used
        if (args.length != 3) {
            System.out.println("Use the parameters: localhost <Port> <Filename>");
            System.exit(0);
        } else {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            sendFileToReceiver(host, port, fileName);
        }
    }

    private static void sendFileToReceiver(String host, int port, String fileName) throws Exception {
        InetAddress ipAddress = InetAddress.getByName(host);
        DatagramSocket senderSocket = new DatagramSocket();
        File file = new File(fileName);
        byte[] message = new byte[1027]; // 1KB + 3B for message sequence number and byte flag // size of message
        byte[] image_message = new byte[1024]; // 1KB of image
        short sequence_number = 0;

        FileInputStream fis = new FileInputStream(file);
        int packet_number = Tools.getPacketsAmount(fis, image_message.length);  // Find the number of packets to be sent

        for (int i = 0; i <= packet_number; i++) {
            int counter = fis.read(image_message, 0, image_message.length);
            // Sequence number converted to a 2-byte array
            byte[] sequence_byteArray = Tools.convertShortToByte(sequence_number);
            // Set the 2 first bytes of the message to the sequence number
            message[0] = sequence_byteArray[0];
            message[1] = sequence_byteArray[1];
            message[2] = 0; // flag always set to 0

            // Check if it's the last byte and set the flag to 1
            if (i == packet_number) {
                message[2] = 1;
            }

            // Get the image bytes
            for  (int j = 0; j < counter; j++) {
                message[3+j] = image_message[j];
            }

            // Packet ready to be sent
            DatagramPacket packet = new DatagramPacket(message, message.length, ipAddress, port);
            senderSocket.send(packet);

            Thread.sleep(10); // 10ms gap after each packet transmission
            sequence_number++;

            // Re-initialize the message and image_message to ensure correctness
            message = new byte[1027];
            image_message = new byte[1024];
        }
        senderSocket.close();
    }

}
