public class Packet
{
    private int seqnum;
    private int acknum;
    private int checksum;
    private String payload;

    // sack fields added here, in order to make array operation easier, I directly made it public.
    public int[] sack = {1000,1000,1000,1000,1000};

    private double sendTime = 0.0;

    private double primarySendTime = 0.0;

    public Packet(Packet p)
    {
        if (p == null) {
            seqnum = 0;
            acknum = 0;
            checksum = 0;
            payload = "";
        } else {
            seqnum = p.getSeqnum();
            acknum = p.getAcknum();
            checksum = p.getChecksum();
            payload = new String(p.getPayload());
            sack = p.sack;
        }
    }

    public Packet(int seq, int ack, int check, String newPayload)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        if (newPayload == null)
        {
            payload = "";
        }
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = null;
        }
        else
        {
            payload = new String(newPayload);
        }
    }

    public Packet(int seq, int ack, int check)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
    }

    public Packet(int seq, int ack, int check, String newPayload, int[] _sack)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
        sack = _sack;
    }


    public boolean setSeqnum(int n)
    {
        seqnum = n;
        return true;
    }

    public boolean setAcknum(int n)
    {
        acknum = n;
        return true;
    }

    public boolean setChecksum(int n)
    {
        checksum = n;
        return true;
    }

    public boolean setPayload(String newPayload)
    {
        if (newPayload == null)
        {
            payload = "";
            return false;
        }
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = "";
            return false;
        }
        else
        {
            payload = new String(newPayload);
            return true;
        }
    }

    public int getSeqnum()
    {
        return seqnum;
    }

    public int getAcknum()
    {
        return acknum;
    }

    public int getChecksum()
    {
        return checksum;
    }

    public void setSendTime(double t) {
        sendTime = t;
        if (primarySendTime == 0.0) primarySendTime = t;
    }

    public double getSendTime(){ return sendTime;}

    public double getPrimarySendTime() { return primarySendTime;}

    public String getPayload()
    {
        return payload;
    }

    public String toString()
    {
        return("seqnum: " + seqnum + "  acknum: " + acknum + "  checksum: " +
                checksum + "  payload: " + payload);
    }

}