import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     * int MAXDATASIZE : the maximum size of the Message data and
     * Packet payload
     *
     * int A : a predefined integer that represents entity A
     * int B : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     * void stopTimer(int entity):
     * Stops the timer running at "entity" [A or B]
     * void startTimer(int entity, double increment):
     * Starts a timer running at "entity" [A or B], which will expire in
     * "increment" time units, causing the interrupt handler to be
     * called. You should only call this with A.
     * void toLayer3(int callingEntity, Packet p)
     * Puts the packet "p" into the network from "callingEntity" [A or B]
     * void toLayer5(String dataSent)
     * Passes "dataSent" up to layer 5
     * double getTime()
     * Returns the current time in the simulator. Might be useful for
     * debugging.
     * int getTraceLevel()
     * Returns TraceLevel
     * void printEventList()
     * Prints the current event list to stdout. Might be useful for
     * debugging, but probably not.
     *
     *
     * Predefined Classes:
     *
     * Message: Used to encapsulate a message coming from layer 5
     * Constructor:
     * Message(String inputData):
     * creates a new Message containing "inputData"
     * Methods:
     * boolean setData(String inputData):
     * sets an existing Message's data to "inputData"
     * returns true on success, false otherwise
     * String getData():
     * returns the data contained in the message
     * Packet: Used to encapsulate a packet
     * Constructors:
     * Packet (Packet p):
     * creates a new Packet that is a copy of "p"
     * Packet (int seq, int ack, int check, String newPayload)
     * creates a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and a
     * payload of "newPayload"
     * Packet (int seq, int ack, int check)
     * chreate a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and
     * an empty payload
     * Methods:
     * boolean setSeqnum(int n)
     * sets the Packet's sequence field to "n"
     * returns true on success, false otherwise
     * boolean setAcknum(int n)
     * sets the Packet's ack field to "n"
     * returns true on success, false otherwise
     * boolean setChecksum(int n)
     * sets the Packet's checksum to "n"
     * returns true on success, false otherwise
     * boolean setPayload(String newPayload)
     * sets the Packet's payload to "newPayload"
     * returns true on success, false otherwise
     * int getSeqnum()
     * returns the contents of the Packet's sequence field
     * int getAcknum()
     * returns the contents of the Packet's ack field
     * int getChecksum()
     * returns the checksum of the Packet
     * int getPayload()
     * returns the Packet's payload
     *
     */

    /*
     * Please use the following variables in your routines.
     * int WindowSize : the window size
     * double RxmtInterval : the numRetransmit timeout
     * int LimitSeqNo : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    // Add any necessary class variables here. Remember, you cannot use
    // these variables to send messages error free! They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    private int seqNumA;
    // private int ackNoA = 0;
    private int lastSeq;// last seq received by layer5 on B
    private int expecting = 0;// next expecting seq of B

    private Queue<Packet> sendBuffer;// sender sendBuffer to store out-of-window packets
    private Queue<Packet> swnd;// used to keep track of packets in the sender window
    private Map<Integer, Packet> rcvBuffer;

    // *´:°•.°+.*•´.*:˚.°*.˚•´.°:°•.°•.*•´.*:˚.°*.˚•´.°:°•.°+.*•´.*:*//
    // * DATA ANALYSIS VARIABLES *//
    // *.•°:°.´+˚.*°.˚:*.´•*.+°.•°:´*.´•*.•°.•°:°.´:•˚°.*°.˚:*.´+°.•*//

    private int numSent = 0;
    private int numRetransmit = 0;
    private int numAck = 0;
    private int numCorrupted = 0;
    private int numLayer5B = 0;

    private double totalRTT = 0.0;
    private int RTTCount = 0;

    private double totalCommunicationTime = 0.0;
    private int communicationCount = 0;

    private double[] sentTimes;
    private double[] communicationTimes;

    // This is the constructor. Don't touch!
    public StudentNetworkSimulator(int numMessages,
            double loss,
            double corrupt,
            double avgDelay,
            int trace,
            int seed,
            int winsize,
            double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize * 2; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }

    // *´:°•.°+.*•´.*:˚.°*.˚•´.°:°•.°•.*•´.*:˚.°*.˚•´.°:°•.°+.*•´.*:*//
    // * HELPER FUNCTIONS * //
    // *.•°:°.´+˚.*°.˚:*.´•*.+°.•°:´*.´•*.•°.•°:°.´:•˚°.*°.˚:*.´+°.•*//

    protected Packet buildPacketA(Message m) {
        Packet p = new Packet(seqNumA, 0, -1, m.getData());
        p.setChecksum(computeChecksum(p));
        return p;
    }

    protected int computeChecksum(Packet p) {
        int checksum = p.getSeqnum() + p.getAcknum();
        String payload = p.getPayload();

        // calculate checksum by adding sequence number, ack number and each char of
        // payload together
        for (char c : payload.toCharArray()) {
            checksum += (int) c;
        }
        return checksum;
    }

    protected boolean validateChecksum(Packet p) {
        int realChecksum = computeChecksum(p);
        return realChecksum == p.getChecksum();
    }

    protected void increaseSeqNumAByOne() {
        seqNumA = (seqNumA + 1) % LimitSeqNo;
    }

    // Check if seq is within rwnd
    // if not, duplicated seq
    protected boolean inWindowB(int index) {
        int windowEnd = (expecting + WindowSize - 1) % LimitSeqNo;
        if (expecting <= windowEnd) { // if not wrap-around
            return index >= expecting && index <= windowEnd;
        } else { // if wrap-around
            return index >= expecting || index <= windowEnd;
        }
    }

    // check whether ack is in swnd
    protected boolean inWindowA(int index) {
        for (Packet p : swnd) {
            if (p.getSeqnum() == index)
                return true;
        }
        return false;
    }

    protected void restartTimerA() {
        stopTimer(A);
        startTimer(A, RxmtInterval);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send. It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        Packet p = buildPacketA(message);

        // if `swnd` not full, add packet to window
        if (swnd.size() < WindowSize) {
            swnd.add(p);
            toLayer3(A, p);
            restartTimerA();
            numSent++;
            sentTimes[seqNumA] = getTime();
            communicationTimes[seqNumA] = getTime();
        } else {
            sendBuffer.add(p);
        }
        increaseSeqNumAByOne();
    }

    // move the window up to a specific acknowledged packet sequence
    private void moveWindowTo(int seq) {
        int num;
        // move window up to acked packet
        do {
            Packet removedPacket = swnd.poll();
            num = removedPacket.getSeqnum();

            // calculate RTT for ack packet
            if (num == seq && sentTimes[num] != -1) {
                totalRTT += getTime() - sentTimes[num];
                RTTCount++;
            }
            sentTimes[num] = -1;
            totalCommunicationTime += getTime() - communicationTimes[num];
            communicationCount++;
        } while (num != seq);
    }

    // retransmit 1st uack packet
    private void handleDuplicateAck() {
        Packet firstPacket = swnd.peek();
        if (firstPacket != null) {
            System.out.printf("|aInput|: Got duplicated ACK, retransmit first packet in the window, seq:%d, ack:%d%n",
                    firstPacket.getSeqnum(), firstPacket.getAcknum());
            sentTimes[firstPacket.getSeqnum()] = getTime();
            toLayer3(A, firstPacket);
            restartTimerA();
            numRetransmit++;
        }
    }

    // move packets from send buffer to swnd, if swnd is not full
    private void addPacketsToWindow() {
        while (swnd.size() < WindowSize && !sendBuffer.isEmpty()) {
            Packet p = sendBuffer.poll();
            swnd.add(p);
            toLayer3(A, p);
            restartTimerA();
            sentTimes[p.getSeqnum()] = getTime();
            numSent++;
        }
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        System.out.println(
                "|aInput|: Get ACK from B, packet is: seqnum:" + packet.getSeqnum() + ", ack:" + packet.getAcknum()
                        + ", checksum:" + packet.getAcknum() + ", payload:" + packet.getPayload());

        // if no packets in swnd, skip
        if (swnd.isEmpty()) {
            stopTimer(A);
            return;
        }

        // if corruption, skip
        if (!validateChecksum(packet)) {
            System.out.println(
                    "|aInput|: Got corrupted packet");
            numCorrupted++;
            return;
        }

        int seq = packet.getSeqnum();

        if (inWindowA(seq)) { // if acked packet in `swnd`
            moveWindowTo(seq);
        } else {
            handleDuplicateAck();
        }

        addPacketsToWindow();

        // If empty swnd, stop A's timer
        if (swnd.isEmpty()) {
            stopTimer(A);
        } 
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt() {
        System.out.println("|aTimerInterrupt|: timeout");
        System.out.println("|aTimerInterrupt|: retransmit the first packet in swnd due to RTO");
        toLayer3(A, swnd.peek());// resend the first one in the window
        restartTimerA();
        numRetransmit++;
        sentTimes[swnd.peek().getSeqnum()] = -1;
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        seqNumA = FirstSeqNo;
        sendBuffer = new LinkedList<Packet>();
        swnd = new LinkedList<Packet>();
        sentTimes = new double[LimitSeqNo];
        Arrays.fill(sentTimes, -1);
        communicationTimes = new double[LimitSeqNo];
        Arrays.fill(communicationTimes, -1);
    }

    protected void sendAckB() {
        Packet p = new Packet(lastSeq, 1, lastSeq + 1);
        toLayer3(B, p);
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side. "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        System.out.println("|bInput|: Get " + packet.getPayload());

        // if corrupted (checksum), do nothing
        if (!validateChecksum(packet)) {
            System.out.println("|bInput|: Got corrupted packet");
            numCorrupted++;
            return;
        }

        // if duplicated, drop and re-ack
        int seq = packet.getSeqnum();
        if (!inWindowB(seq)) {
            System.out.println("|bInput|: Got duplicated packet");
            sendAckB();
            numAck++;
            return;
        }

        // add to rcv buffer
        rcvBuffer.put(packet.getSeqnum(), packet);

        // check whether the buffer is in order
        // if true, dump every ordered packet to layer 5
        if (seq == expecting) {
            while (rcvBuffer.containsKey(expecting)) {
                Packet p = rcvBuffer.remove(expecting);
                toLayer5(p.getPayload());
                numLayer5B++;

                lastSeq = expecting;
                expecting = (expecting + 1) % LimitSeqNo;
            }
            sendAckB();
            numAck++;
        }
        // otherwise ack last received packet sequence
        else {
            System.out.println("|bInput|: Expecting pkt" + expecting + ", got pkt" + seq);
            sendAckB();
            numAck++;
        }
    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        rcvBuffer = new HashMap<>();
        lastSeq = -1;
    }

    // Use to print final statistics
    protected void Simulation_done() {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO
        // NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + numSent);
        System.out.println("Number of retransmissions by A:" + numRetransmit);
        System.out.println("Number of data packets delivered to layer 5 at B:" + numLayer5B);
        System.out.println("Number of ACK packets sent by B:" + numAck);
        System.out.println("Number of corrupted packets:" + numCorrupted);
        System.out.println("Ratio of lost packets:"
                + (double) (numRetransmit - numCorrupted) / (double) (numSent + numRetransmit + numAck));
        System.out.println("Ratio of corrupted packets:"
                + (double) ((double) numCorrupted / (numSent + numCorrupted + numAck)));
        System.out.println("Average RTT:" + totalRTT / RTTCount);
        System.out.println("Average communication time:" + totalCommunicationTime / numSent);
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        System.out.println("All RTT:" + totalRTT);
        System.out.println("Counter RTT:" + RTTCount);
        System.out.println("Total time to communicate:" + totalCommunicationTime);
        System.out.println("Counter for time to communicate:" + communicationCount);
        // EXAMPLE GIVEN BELOW
        // System.out.println("Example statistic you want to check e.g. number of ACK
        // packets received by A :" + "<YourVariableHere>");
    }

}