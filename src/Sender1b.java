/** Kendeas Theofanous s1317642 */

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 *  rdt 3.0 sender
 *
 */

public class Sender1b {

    public static void main (String args[]) throws Exception {
        if (args.length != 4) {
            System.out.println("Use the parameters: localhost <Port> <Filename> [RetryTimeout]");
            System.exit(0);
        } else {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int retryTimeout = Integer.parseInt(args[3]);
            sendFileToReceiver(host, port, fileName, retryTimeout);
        }
    }

    private static void sendFileToReceiver(String host, int port, String fileName, int retryTimeout) throws Exception {
        InetAddress ipAddress = InetAddress.getByName(host);
        DatagramSocket senderSocket = new DatagramSocket();
        File file = new File(fileName);
        byte[] message = new byte[1027]; // 1KB + 3B for message sequence number and byte flag // size of message
        byte[] image_message = new byte[1024]; // 1KB of image
        short sequence_number = 0;
        int retransmissions = 0;
        int last_packet_retransmissions = 0;

        FileInputStream fis = new FileInputStream(file);
        int packet_number = Tools.getPacketsAmount(fis, image_message.length);  // Find the number of packets to be sent

        // Find out the start of sending the packets
        long start_time = System.currentTimeMillis();
        long end_time; // Initialize end time

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

            boolean received_ack = false;
            while(!received_ack) {
                // Wait to receive acknowledgement
                byte[] ack = new byte[2];
                DatagramPacket ack_packet = new DatagramPacket(ack, ack.length);
                senderSocket.setSoTimeout(retryTimeout);
                try {
                    senderSocket.receive(ack_packet);
                    ack = ack_packet.getData();
                    if (ack[0] == message[0] && ack[1] == message[1]) {
                        received_ack = true;
                    }
                } catch(SocketTimeoutException ste) {
                    senderSocket.send(packet);
                    retransmissions += 1;
                    if (i == packet_number) {
                        if (last_packet_retransmissions == 20) {
                            break;
                        }
                        last_packet_retransmissions += 1;
                    }
                }
            }
            // Re-initialize the message and image_message to ensure correctness and alternate the sequence_number
            message = new byte[1027];
            image_message = new byte[1024];
            if (sequence_number == 0) sequence_number = 1;
            else sequence_number = 0;

//            System.out.println("Sequence number after iteration: " + sequence_number);
            // Check if it's the last byte and check the time
        }
        senderSocket.close();
        end_time = System.currentTimeMillis();

        double transfer_time = (end_time - start_time) / 1000d;
        double throughput = ((double) file.length()/ 1024d) / transfer_time;


        System.out.println("Retransmissions: " + retransmissions);
        System.out.println("Time elapsed: " + transfer_time);
        System.out.println("File size in KB: " + file.length());
        System.out.println("Average Throughput: " + throughput);

    }

}
