// import java.util.*;

// public class StudentNetworkSimulator extends NetworkSimulator {
//     /*
//      * Predefined Constants (static member variables):
//      *
//      * int MAXDATASIZE : the maximum size of the Message data and
//      * Packet payload
//      *
//      * int A : a predefined integer that represents entity A
//      * int B : a predefined integer that represents entity B
//      *
//      * Predefined Member Methods:
//      *
//      * void stopTimer(int entity):
//      * Stops the timer running at "entity" [A or B]
//      * void startTimer(int entity, double increment):
//      * Starts a timer running at "entity" [A or B], which will expire in
//      * "increment" time units, causing the interrupt handler to be
//      * called. You should only call this with A.
//      * void toLayer3(int callingEntity, Packet p)
//      * Puts the packet "p" into the network from "callingEntity" [A or B]
//      * void toLayer5(String dataSent)
//      * Passes "dataSent" up to layer 5
//      * double getTime()
//      * Returns the current time in the simulator. Might be useful for
//      * debugging.
//      * int getTraceLevel()
//      * Returns TraceLevel
//      * void printEventList()
//      * Prints the current event list to stdout. Might be useful for
//      * debugging, but probably not.
//      *
//      *
//      * Predefined Classes:
//      *
//      * Message: Used to encapsulate a message coming from layer 5
//      * Constructor:
//      * Message(String inputData):
//      * creates a new Message containing "inputData"
//      * Methods:
//      * boolean setData(String inputData):
//      * sets an existing Message's data to "inputData"
//      * returns true on success, false otherwise
//      * String getData():
//      * returns the data contained in the message
//      * Packet: Used to encapsulate a packet
//      * Constructors:
//      * Packet (Packet p):
//      * creates a new Packet that is a copy of "p"
//      * Packet (int seq, int ack, int check, String newPayload)
//      * creates a new Packet with a sequence field of "seq", an
//      * ack field of "ack", a checksum field of "check", and a
//      * payload of "newPayload"
//      * Packet (int seq, int ack, int check)
//      * chreate a new Packet with a sequence field of "seq", an
//      * ack field of "ack", a checksum field of "check", and
//      * an empty payload
//      * Methods:
//      * boolean setSeqnum(int n)
//      * sets the Packet's sequence field to "n"
//      * returns true on success, false otherwise
//      * boolean setAcknum(int n)
//      * sets the Packet's ack field to "n"
//      * returns true on success, false otherwise
//      * boolean setChecksum(int n)
//      * sets the Packet's checksum to "n"
//      * returns true on success, false otherwise
//      * boolean setPayload(String newPayload)
//      * sets the Packet's payload to "newPayload"
//      * returns true on success, false otherwise
//      * int getSeqnum()
//      * returns the contents of the Packet's sequence field
//      * int getAcknum()
//      * returns the contents of the Packet's ack field
//      * int getChecksum()
//      * returns the checksum of the Packet
//      * int getPayload()
//      * returns the Packet's payload
//      *
//      */

//     /*
//      * Please use the following variables in your routines.
//      * int WindowSize : the window size
//      * double RxmtInterval : the numRetransmit timeout
//      * int LimitSeqNo : when sequence number reaches this value, it wraps around
//      */

//     public static final int FirstSeqNo = 0;
//     private int WindowSize;
//     private double RmtInterval;
//     private int LimitSeqNo;

//     // Add any necessary class variables here. Remember, you cannot use
//     // these variables to send messages error free! They can only hold
//     // state information for A or B.
//     // Also add any necessary methods (e.g. checksum of a String)

//     private int sequence;

//     private LinkedList<Packet> swnd; // buffer + cur-window on the sender side

//     private int senderStartPointer = 0; // the leftmost index of sender window.

//     private LinkedList<Packet> receiverWindow; // current rwnd

//     private LinkedList<Packet> rcvBuffer; // buffer on the receiver side

//     private double rtt = 0;
//     private double communicationTime = 0;
//     private int numPacket = 0; // the number of packets need to be transmitted.
//     private int numTransmit = 0; // the times of toLayer3 called by any side.
//     private int numACK = 0; // the times of ACKs sent from B-side.
//     private int numCorrupted = 0; // the number of corrupted packets.
//     private int numReceived = 0; // the number of packets successfully transmitted between A and B
//     private int numRetransmit = 0;

//     // This is the constructor. Don't touch!
//     public StudentNetworkSimulator(int numMessages,
//             double loss,
//             double corrupt,
//             double avgDelay,
//             int trace,
//             int seed,
//             int winsize,
//             double delay) {
//         super(numMessages, loss, corrupt, avgDelay, trace, seed);
//         WindowSize = winsize;
//         LimitSeqNo = winsize * 2; // set appropriately; assumes SR here!
//         RmtInterval = delay;
//     }

//     protected int computeChecksum(Packet packet) {
//         int res = 0;
//         for (int i = 0; i < packet.getPayload().length(); i++) {
//             res += (int) packet.getPayload().charAt(i);
//         }
//         res += packet.getSeqnum() + packet.getAcknum();
//         return res;
//     }

//     // this function is used to check if a packet is corrupted during transmission
//     protected boolean validateChecksum(Packet packet) {
//         int checkSum = computeChecksum(packet);
//         return checkSum == packet.getChecksum();
//     }

//     protected Packet constructPacket(Message m) {
//         int ack = 0;
//         Packet p = new Packet(sequence, ack, -1, m.getData());
//         int checkSum = computeChecksum(p);
//         p.setChecksum(checkSum);
//         return p;
//     }

//     protected void increaseSeq() {
//         sequence = (sequence + 1) % LimitSeqNo;
//     }

//     // This routine will be called whenever the upper layer at the sender [A]
//     // has a message to send. It is the job of your protocol to insure that
//     // the data in such a message is delivered in-order, and correctly, to
//     // the receiving upper layer.
//     protected void aOutput(Message message) {
//         numPacket++;
//         Packet myPacket = constructPacket(message);
//         increaseSeq();

//         // if sender window is not full:
//         // toLayer3() & start timer
//         swnd.add(myPacket);
//         if (swnd.size() <= senderStartPointer + WindowSize - 1) {
//             myPacket.setSendTime(getTime());
//             stopTimer(A);
//             startTimer(A, RmtInterval);
//             toLayer3(A, myPacket);
//             numTransmit++;
//         }
//     }

//     // This routine will be called whenever a packet sent from the B-side
//     // (i.e. as a result of a toLayer3() being done by a B-side procedure)
//     // arrives at the A-side. "packet" is the (possibly corrupted) packet
//     // sent from the B-side.
//     protected void aInput(Packet packet) {
//         numReceived++; // successfully transmitted.
//         System.out.println(
//                 "|aInput|: Get ACK from B, packet is: seqnum: " + packet.getSeqnum() + ", ack: " + packet.getAcknum()
//                         + ", checksum: " + packet.getAcknum() + "payload: " + packet.getPayload());

//         // if corruption, return redirectly
//         if (!validateChecksum(packet)) {
//             numCorrupted++;
//             return;
//         }

//         int lastSeq = getLastSeq(packet.sack);
//         // if duplicated ACK (using window) --> retransmit the lost pkts (reset
//         // timer), return
//         if (senderStartPointer < swnd.size()) { // have to be with the window
//             // tackle retransmit
//             if (senderStartPointer == 0) { // TODO from the start
//                 while (swnd.size() > senderStartPointer
//                         && !Objects.equals(swnd.get(senderStartPointer).getPayload(), packet.getPayload())) {
//                     rtt += getTime() - swnd.get(senderStartPointer).getSendTime();
//                     communicationTime += getTime() - swnd.get(senderStartPointer).getPrimarySendTime();
//                     senderStartPointer++;
//                     System.out.println("\n|A|: senderStartPointer =" + String.valueOf(senderStartPointer));
//                 }
//                 communicationTime += getTime() - swnd.get(senderStartPointer).getPrimarySendTime();
//                 rtt += getTime() - swnd.get(senderStartPointer).getSendTime();
//                 senderStartPointer++;
//             } else {
//                 // if the packet returned is not the last one, put senderStartPointer to the
//                 // right position first.
//                 // Then the problem is reduced to "the current packet is not ACKed, retransmit
//                 // current and all holes in sacks"
//                 while (!Objects.equals(swnd.get(senderStartPointer - 1).getPayload(), packet.getPayload())) {
//                     communicationTime += getTime() - swnd.get(senderStartPointer).getPrimarySendTime();
//                     rtt += getTime() - swnd.get(senderStartPointer).getSendTime();
//                     senderStartPointer++;
//                 }

//                 // First retransmit the packet at front
//                 if (swnd.size() > senderStartPointer) {
//                     swnd.get(senderStartPointer).setSendTime(getTime());
//                     Packet temp = swnd.get(senderStartPointer);
//                     temp.setChecksum(computeChecksum(temp));
//                     toLayer3(A, temp);
//                     System.out.println(
//                             "\n|aInput|: retransmitting a packet due to a ACK from B indicating lost/corrupted\n");
//                     numTransmit++;
//                     numRetransmit++;
//                     startTimer(A, RmtInterval);
//                 }

//                 // Finding holes in sack, and retransmit it.
//                 for (int i = senderStartPointer; i < swnd.size(); i++) {
//                     int cur = swnd.get(i).getSeqnum();
//                     if (cur == lastSeq || lastSeq == -1)
//                         break;
//                     else {
//                         if (!findCurSeq(cur, packet.sack)) {
//                             // if failed to find, this cur is a hole
//                             Packet temp = swnd.get(i);
//                             temp.setSendTime(getTime());
//                             temp.setChecksum(computeChecksum(temp));
//                             toLayer3(A, swnd.get(i));
//                             startTimer(A, RmtInterval);
//                             System.out.println(
//                                     "\n|aInput|: retransmitting a packet due to a ACK from B indicates lost/corrupted\n\n");
//                             numRetransmit++;
//                             numTransmit++;
//                         }
//                     }
//                 }
//             }
//         } else {
//             return;
//         }
//         if (swnd.isEmpty()) {
//             System.out.println("\n|A|: stop the timer\n");
//             stopTimer(A);
//         }
//         // sent from the B-side
//     }

//     // check if cur is in sequnce(sack)
//     private boolean findCurSeq(int cur, int[] sequence) {
//         for (int j : sequence) {
//             if (j == cur) {
//                 return true;
//             }
//         }
//         return false;
//     }

//     // never check beyond sequence
//     private int getLastSeq(int[] sequence) {
//         int ans = -1;
//         for (int i = 0; i < sequence.length; i++) {
//             if (sequence[i] < WindowSize)
//                 ans = sequence[i];
//             else
//                 break;
//         }
//         return ans;
//     }

//     // This routine will be called when A's timer expires (thus generating a
//     // timer interrupt). You'll probably want to use this routine to control
//     // the numRetransmit of packets. See startTimer() and stopTimer(), above,
//     // for how the timer is started and stopped.
//     protected void aTimerInterrupt() {
//         // retransmit the first oustanding pkt
//         if (swnd.size() > senderStartPointer) {
//             Packet temp = swnd.get(senderStartPointer);
//             temp.setSendTime(getTime());
//             temp.setChecksum(computeChecksum(temp));
//             toLayer3(A, temp);
//             numTransmit++;
//             System.out.println("\n|A|: retransmit a packet due to RTO\n");
//             // restart timer
//             stopTimer(A);
//             startTimer(0, RmtInterval);
//         }
//     }

//     // This routine will be called once, before any of your other A-side
//     // routines are called. It can be used to do any required
//     // initialization (e.g. of member variables you add to control the state
//     // of entity A).
//     protected void aInit() {
//         sequence = FirstSeqNo;
//         swnd = new LinkedList<Packet>();
//     }

//     // This routine will be called whenever a packet sent from the B-side
//     // (i.e. as a result of a toLayer3() being done by an A-side procedure)
//     // arrives at the B-side. "packet" is the (possibly corrupted) packet
//     // sent from the A-side.
//     protected void bInput(Packet packet) {
//         numReceived++; // successfully transmitted.
//         System.out.println("|bInput|: Get " + packet.getPayload());
//         // if corruption, return directly
//         if (!validateChecksum(packet)) {
//             System.out.println("|bInput|: Checksum error, corrupted");
//             numCorrupted++;
//             return;
//         }
//         // if duplicate, send ack
//         if (checkDuplicate(packet)) {
//             sendACK();
//             return;
//         }

//         // Tcheck in-order delivery: expectSeqnum = (lastReceivedSeqnum + 1) %
//         // LimitSeqNo
//         // If in-order: seq == expectSeqnum
//         // TODO toLayer5()
//         // TODO ACK, return
//         int lastSeq = rcvBuffer.size() == 0 ? -1 : rcvBuffer.getLast().getSeqnum();
//         int seqExpected = (lastSeq + 1) % LimitSeqNo;
//         if (seqExpected == packet.getSeqnum()) { // if packet is exactly what we need now
//             System.out.println("|bInput|: Correct seqnum. Expecting pkt " + String.valueOf(seqExpected) + ", got pkt"
//                     + packet.getSeqnum());
//             receiverWindow.add(0, packet);
//             while (!receiverWindow.isEmpty() && receiverWindow.getFirst().getSeqnum() == (lastSeq + 1) % LimitSeqNo) {
//                 // if we have the next several packet needed as well, we put then to buffer
//                 // together
//                 toLayer5(receiverWindow.getFirst().getPayload());
//                 rcvBuffer.add(receiverWindow.getFirst());
//                 receiverWindow.removeFirst();
//                 lastSeq = rcvBuffer.getLast().getSeqnum();
//             }
//         } else {
//             // Only add to receiverWindow if it's not a duplicate in the buffer.
//             if (!receiverWindow.contains(packet)) {
//                 int index = 0;
//                 while (index < receiverWindow.size() &&
//                         receiverWindow.get(index).getSeqnum() < packet.getSeqnum()) {
//                     index++;
//                 }
//                 receiverWindow.add(index, packet);
//             }
//             if (receiverWindow.isEmpty())
//                 receiverWindow.add(packet);

//             if (packet.getSeqnum() > lastSeq) { //
//                 for (int i = 0; i < receiverWindow.size(); i++) {
//                     if (receiverWindow.get(i).getSeqnum() > packet.getSeqnum()) {
//                         receiverWindow.add(i, packet);
//                         break;
//                     }
//                 }
//             } else {
//                 for (int i = receiverWindow.size() - 1; i >= 0; i--) {
//                     int cur = receiverWindow.get(i).getSeqnum();
//                     if (cur > rcvBuffer.getLast().getSeqnum() || packet.getSeqnum() > cur) {
//                         receiverWindow.add(i + 1, packet);
//                         break;
//                     }

//                 }
//             }
//         }
//         sendACK();
//     }

//     private void sendACK() {
//         // ACK
//         if (rcvBuffer.isEmpty())
//             return;
//         Packet temp = new Packet(rcvBuffer.getLast());
//         for (int i = 0; i < Math.min(receiverWindow.size(), 5); i++) {
//             temp.sack[i] = receiverWindow.get(i).getSeqnum();
//         }
//         temp.setAcknum(1);
//         // temp.setPayload("");
//         temp.setChecksum(computeChecksum(temp));
//         toLayer3(1, temp);
//         numTransmit++;
//         numACK++;
//     }

//     // this function is used to check if a packet arrived at B is duplicate
//     private boolean checkDuplicate(Packet packet) {
//         int count = 0;
//         for (Packet item : receiverWindow) {
//             count++;
//             if (Objects.equals(item.getPayload(), packet.getPayload())) {
//                 System.out.println("\n\n Received duplicate packet\n\n");
//                 return true;

//             }
//         }
//         for (int i = rcvBuffer.size() - 1; i >= 0 && count < 16; i--) {
//             count++;
//             if (Objects.equals(rcvBuffer.get(i).getPayload(), packet.getPayload())) {
//                 System.out.println("\n\n Received duplicate packet\n\n");
//                 return true;
//             }
//         }
//         return false;
//     }

//     // This routine will be called once, before any of your other B-side
//     // routines are called. It can be used to do any required
//     // initialization (e.g. of member variables you add to control the state
//     // of entity B).
//     protected void bInit() {
//         receiverWindow = new LinkedList<>();
//         rcvBuffer = new LinkedList<>();
//     }

//     // Use to print final statistics
//     protected void Simulation_done() {
//         // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO
//         // NOT CHANGE THE FORMAT OF PRINTED OUTPUT
//         System.out.println("\n\n===============STATISTICS=======================");
//         System.out.println("Number of original packets transmitted by A:" + numPacket);
//         System.out.println("Number of retransmissions by A:" + String.valueOf(numRetransmit));
//         System.out.println("Number of data packets delivered to layer 5 at B:" + getToLayer5());
//         System.out.println("Number of ACK packets sent by B:" + numACK);
//         System.out.println("Number of corrupted packets:" + numCorrupted);
//         System.out.println("Ratio of lost packets:" + (double) (getToLayer3() - numReceived) / getToLayer3());
//         System.out.println("Ratio of corrupted packets:" + (double) (numCorrupted) / numTransmit);
//         System.out.println("Average totalRTT:" + rtt / getToLayer5());
//         System.out.println("Average communication time:" + communicationTime / getToLayer5());

//         System.out.println("==================================================");

//         // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
//         System.out.println("\nEXTRA:");
//         System.out.println("Throughput: " + getThoughtput());
//         System.out.println("Goodput: " + getGoodput());
//         // EXAMPLE GIVEN BELOW
//         // System.out.println("Example statistic you want to check e.g. number of ACK
//         // packets received by A :" + "<YourVariableHere>");
//     }

//     private double getThoughtput() {
//         final double PacketSize = (4 + 4 + 4 + 20) * 8;
//         double totalTransmitted = nToLayer3FromA * PacketSize;
//         return totalTransmitted / getTime();
//     }

//     private double getGoodput() {
//         double totalTransmitted = getToLayer5() * 20 * 8;
//         return totalTransmitted / getTime();
//     }

// }

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
    private int ackNoA = 0;
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
    private int layer5B = 0;

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
        Packet p = new Packet(seqNumA, ackNoA, -1, m.getData());
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

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        System.out.println(
                "|aInput|: Get ACK from B, packet is: seqnum:" + packet.getSeqnum() + ", ack:" + packet.getAcknum()
                        + ", checksum:" + packet.getAcknum() + ", payload:" + packet.getPayload());

        // if no packets in swnd, skip
        if (swnd.isEmpty())
            return;

        // if corruption, skip
        if (!validateChecksum(packet)) {
            System.out.println(
                    "|aInput|: Got corrupted packet");
            numCorrupted++;
            return;
        }

        int seq = packet.getSeqnum();

        if (inWindowA(seq)) { // if acked packet in `swnd`
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
        } else { // handle duplicate ack
            // retransmit 1st uack packet
            Packet firstPacket = swnd.peek();
            if (firstPacket != null) {
                System.out.println(
                        "|aInput|: Got duplicated ACK, retransmit the first packet in the window, seq:"
                                + String.valueOf(firstPacket.getSeqnum()) + ", ack:"
                                + String.valueOf(firstPacket.getAcknum()));
                sentTimes[firstPacket.getSeqnum()] = -1;
                toLayer3(A, firstPacket);
                restartTimerA();
                numRetransmit++;
            }
        }

        // Move packets from the send buffer to the window, if space is available
        while (swnd.size() < WindowSize && !sendBuffer.isEmpty()) {
            Packet p = sendBuffer.poll();
            swnd.add(p);
            toLayer3(A, p);
            sentTimes[p.getSeqnum()] = getTime();
            numSent++;
        }

        // Adjust the timer based on whether the window is empty
        if (swnd.isEmpty()) {
            stopTimer(A);
        } else {
            restartTimerA();
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
                layer5B++;

                lastSeq = expecting;
                expecting = (expecting + 1) % LimitSeqNo;
            }
            sendAckB();
            numAck++;
        }
        // otherwise ack last received packet sequence
        else {
            System.out.println("|bInput|: Expecting pkt"+ expecting + ", got pkt" + seq);
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
        System.out.println("Number of data packets delivered to layer 5 at B:" + layer5B);
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