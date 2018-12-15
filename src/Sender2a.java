/** Kendeas Theofanous s1317642 */


import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Go-Back-N protocol
 *
 */

public class Sender2a {
    public static void main (String args[]) throws Exception {
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
        short next_seqnum = 1; // Packet sequence number
        List<DatagramPacket> window_packets = new ArrayList<DatagramPacket>(windowSize);

        FileInputStream fis = new FileInputStream(file);
        int packet_number = Tools.getPacketsAmount(fis, image_message.length);  // Find the number of packets to be sent

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
            for  (int j = 0; j < counter; j++) {
                message[3+j] = image_message[j];
            }

            // Prepare packet to be sent
            DatagramPacket packet = new DatagramPacket(message, counter+3, ipAddress, port);

            // Check if sequence number is in the window
            if (next_seqnum <= base + windowSize) {
                // Send packet and add it to List of window packets
                senderSocket.send(packet);
                window_packets.add(packet);

                if (base == next_seqnum) {
                    // FSM Start timer
                    senderSocket.setSoTimeout(retryTimeout);
                }
                // Re-initialize the message and image_message to ensure correctness and increment the sequence number
                message = new byte[1027];
                image_message = new byte[1024];
                next_seqnum++;

                // When sequence number reaches the end of window, or if it's the last iteration of loop start receiving
                if (next_seqnum == base + windowSize  || i == packet_number) {
                    boolean receivedLastAck = false;
                    while (!receivedLastAck) {
                        byte[] ack = new byte[2];
                        DatagramPacket ack_packet = new DatagramPacket(ack, ack.length);
                        try {
                            senderSocket.receive(ack_packet);
                            ack = ack_packet.getData();
                            base = (short) (Tools.convertByteToShort(ack) + 1);
                            // If base == next_seqnum then add the packet to acked_packets and exit while loop
                            if (base == next_seqnum) {
                                // FSM Stop timer
                                receivedLastAck = true;
                            } else {
                                 // FSM Start timer
                                senderSocket.setSoTimeout(retryTimeout);
                            }
                        } catch (SocketTimeoutException ste) {
                            // Resend whole window if the first packet of window was lost or it's the last iteration of loop
                            if (next_seqnum - base == windowSize || ((i == packet_number) && (next_seqnum - base >= 0))) {
                                for (int j = base-1; j < window_packets.size(); j++) {
                                    senderSocket.send(window_packets.get(j));
                                }
                                // Otherwise, break out of receiving and the window slides to next acknowledged packet
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            // Else Refuse data
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
