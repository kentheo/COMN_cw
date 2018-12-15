/** Kendeas Theofanous s1317642 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class ThreadSender implements Runnable {

    private DatagramSocket socket;
    private DatagramPacket packet;
    private Timer timer = new Timer();
    private int retryTimeout;
    private short seqNum;
    private List<Short> allAcks;

    // Constructor has these parameters in order to be able to manipulate packets and check acknowledgments
    ThreadSender(DatagramSocket socket, DatagramPacket packet, int retryTimeout, List<Short> allAcks, short seqNum) {
        this.socket = socket;
        this.packet = packet;
        this.retryTimeout = retryTimeout;
        this.seqNum = seqNum;
        this.allAcks = allAcks;
    }

    public void run() {
        // Schedule the timer for repeated fixed-delay execution
        // The delay before the task is executed and the period between successive task executions are
        // set to be the same as the retransmission timeout
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Check if the packet was already acknowledged and if it was stop the timer
                if (allAcks.contains(seqNum)) {
                    timer.cancel();
                    timer.purge();
                } else {
                    // Otherwise, send the packet
                    try {
                        socket.send(packet);
                        // Catches the case where threads try to send packets even after all packets were sent and acknowledged
                    } catch (SocketException se) {
                        timer.cancel();
                        timer.purge();
                        if (!socket.isClosed()) socket.close();
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }, retryTimeout, retryTimeout);


    }


}
