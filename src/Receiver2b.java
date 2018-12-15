import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

/** Kendeas Theofanous s1317642 */


public class Receiver2b {

    public static void main (String args[]) throws Exception {
        // Check if correct number of args is used
        if (args.length != 3) {
            System.out.println("Use the parameters: Receiver1a <Port> <Filename> [WindowSize]");
        } else {
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            int windowSize = Integer.parseInt(args[2]);
            receiveFileFromSender(port, fileName, windowSize);
        }
    }

    private static void receiveFileFromSender(int port, String fileName, int windowSize) throws Exception {
        DatagramSocket receiverSocket = new DatagramSocket(port);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] receivedData = new byte[1027];
        byte[] ack_bytes = new byte[2];
        short rcvBase = 1;
        short packetAck;

        Map<Short, byte[]> buffer = new HashMap<Short, byte[]>();

        while (true) {
            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
            // Catches the timeout and breaks out of the loop. Needed because of the grace period
            try {
                receiverSocket.receive(packet);
            } catch (SocketTimeoutException ste) {
                break;
            }

            receivedData = packet.getData();
            InetAddress ipAddress = packet.getAddress();
            int sender_port = packet.getPort();
            // Get the acknowledgement from the received data
            ack_bytes[0] = receivedData[0];
            ack_bytes[1] = receivedData[1];
            packetAck = Tools.convertByteToShort(ack_bytes);
            // Check if packet falls in the window
            if (packetAck >= rcvBase && packetAck <= rcvBase + windowSize - 1) {
                // Send Ack
                DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                byte[] temp = Arrays.copyOfRange(receivedData, 3, packet.getLength());
                receiverSocket.send(ack_packet);

                // Check if the acknowledgement is the same as the base and buffer the data of the packet and don't write it
                if (packetAck != rcvBase) {
//                    System.out.println("Buffering packetAck: " + packetAck);
                    buffer.put(packetAck, temp);
                } else { // Otherwise, write it directly and increment the base
                    baos.write(temp, 0, temp.length);
                    rcvBase++;
                    // Check if the buffer contains the base
                    while (buffer.containsKey(rcvBase)) {
                        // Write packet's byte array to baos, remove the packet from the buffer increment the base
                        baos.write(buffer.get(rcvBase), 0, buffer.get(rcvBase).length);
                        buffer.remove(packetAck);
                        rcvBase++;
                    }
                }
            // Check if packet falls in the previous window
            } else if (packetAck >= rcvBase - windowSize && packetAck <= rcvBase - 1) {
                // Send Ack
                DatagramPacket ack_packet = new DatagramPacket(ack_bytes, ack_bytes.length, ipAddress, sender_port);
                receiverSocket.send(ack_packet);
            } else {
                // Do nothing
            }
            // Grace period of 2 seconds for receiving packets. It will exit at the end.
            receiverSocket.setSoTimeout(2000);
        }
        // Wait 2 sec to ensure all acks are sent
        System.out.println("-------- STOPPED RECEIVING -----------");
        FileOutputStream fos = new FileOutputStream(fileName);
        baos.writeTo(fos);
        fos.close();
        receiverSocket.close();
    }
}
