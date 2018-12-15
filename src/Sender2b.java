/** Kendeas Theofanous s1317642 */

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Selective Repeat Protocol
 *
 */
public class Sender2b {

    public static void main(String args[]) throws Exception {
        if (args.length != 5) {
            System.out.println("Use the parameters: localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
            System.exit(0);
        } else {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int retryTimeout = Integer.parseInt(args[3]);
            int windowSize = Integer.parseInt(args[4]);
            sendFileToReceiver(host, port, fileName, retryTimeout, windowSize);
        }
    }

    private static void sendFileToReceiver(String host, int port, String fileName, int retryTimeout, int windowSize) throws Exception {
        InetAddress ipAddress = InetAddress.getByName(host);
        DatagramSocket senderSocket = new DatagramSocket();
        File file = new File(fileName);
        byte[] message = new byte[1027]; // 1KB + 3B for message sequence number and byte flag // size of message
        byte[] image_message = new byte[1024]; // 1KB of image
        short base = 1; // Initialise base same as sequence number
        short next_seqnum = 1; // This variable will be used to iterate in the window
        short ack_number;

        FileInputStream fis = new FileInputStream(file);
        int packet_number = Tools.getPacketsAmount(fis, image_message.length);  // Find the number of packets to be sent
        // Initialize a list for acks of all packets and set all values to False
        List<Short> all_acks = new ArrayList<Short>(packet_number);

        // Find out the start of sending the packets
        long start_time = System.currentTimeMillis();
        long end_time; // Define end time

        for (int i = 0; i <= packet_number; i++) {
            int counter = fis.read(image_message, 0, image_message.length);
            // Sequence number converted to a 2-byte array
            byte[] sequence_byteArray = Tools.convertShortToByte(next_seqnum);
            // Set the 2 first bytes of the message to the sequence number
            message[0] = sequence_byteArray[0];
            message[1] = sequence_byteArray[1];
            message[2] = 0; // flag always set to 0

            // Check if it's the last byte and set the flag to 1
            if (i == packet_number) {
                message[2] = 1;
            }

            // Get the image bytes
            for (int j = 0; j < counter; j++) {
                message[3 + j] = image_message[j];
            }

            // Prepare packet to be sent
            DatagramPacket packet = new DatagramPacket(message, counter + 3, ipAddress, port);

            // Check if sequence number is in the window or if it's the last iteration of loop
            if (next_seqnum <= base + windowSize || i == packet_number) {
                // Start thread action
                ThreadSender threadSender = new ThreadSender(senderSocket, packet, retryTimeout, all_acks, next_seqnum);
                Thread thd = new Thread(threadSender);
                thd.start();

                // Re-initialize the message and image_message to ensure correctness and increment the sequence number
                message = new byte[1027];
                image_message = new byte[1024];
                next_seqnum++;

                // Start receiving when sequence number reaches end of window or if it's the last iteration of loop
                if (next_seqnum == base + windowSize || i == packet_number) {
                    while (all_acks.size() < next_seqnum-1) {
                        byte[] ack = new byte[2];
                        DatagramPacket ack_packet = new DatagramPacket(ack, ack.length);
                        // Receive an acknowledgement and take its number
                        senderSocket.receive(ack_packet);
                        ack = ack_packet.getData();
                        ack_number = Tools.convertByteToShort(ack);
                        // Check if it already contains the ack and if it doesn't add it
                        if (!all_acks.contains(ack_number)) {
                            all_acks.add(ack_number);
                        }
                        // Increment base
                        base = (short) (Tools.convertByteToShort(ack) + 1);
                    }
                }
            } else {
                // Refuse data
            }
        }
        senderSocket.close();
        end_time = System.currentTimeMillis();

        double transfer_time = (end_time - start_time) / 1000d;
        double throughput = ((double) file.length()/ 1024d) / transfer_time;

        System.out.println("Time elapsed: " + transfer_time);
        System.out.println("File size in KB: " + file.length());
        System.out.println("Average Throughput: " + throughput);

    }

}