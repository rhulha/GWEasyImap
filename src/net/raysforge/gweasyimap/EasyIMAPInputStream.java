package net.raysforge.gweasyimap;

import java.io.IOException;
import java.io.InputStream;

import com.sun.mail.iap.ByteArray;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPProtocol;

public class EasyIMAPInputStream extends InputStream
{
    private String section;
    private int pos;
    private int blksize;
    private int max;
    private byte buf[];
    private int bufcount;
    private int bufpos;

    private final int seqNr;
    private final IMAPProtocol ip;

    public EasyIMAPInputStream(IMAPProtocol ip, String s, int i, int blockSize, int seqNr)
    {
        this.ip = ip;
        section = s;
        max = i;
        this.seqNr = seqNr;
        pos = 0;
        blksize = blockSize; // imapmessage.getFetchBlockSize();
    }

    private void fill() throws IOException
    {
        if (max != -1 && pos >= max)
        {
            return;
        }
        BODY body = null;
        synchronized (this)
        {
            // if(msg.isExpunged())
            // throw new IOException("No content for expunged message");
            int i = seqNr;
            int k = blksize;
            if (max != -1 && pos + blksize > max)
                k = max - pos;
            try
            {
                body = ip.fetchBody(i, section, pos, k);
            } catch (ProtocolException protocolexception)
            {
                throw new IOException(protocolexception.getMessage());
            }
        }
        ByteArray bytearray;
        if (body == null || (bytearray = body.getByteArray()) == null)
            throw new IOException("No content");
        buf = bytearray.getBytes();
        bufpos = bytearray.getStart();
        int j = bytearray.getCount();
        bufcount = bufpos + j;
        pos += j;
    }

    public synchronized int read() throws IOException
    {
        if (bufpos >= bufcount)
        {
            fill();
            if (bufpos >= bufcount)
                return -1;
        }
        return buf[bufpos++] & 255;
    }

    public synchronized int read(byte abyte0[], int i, int j) throws IOException
    {
        int k = bufcount - bufpos;
        if (k <= 0)
        {
            fill();
            k = bufcount - bufpos;
            if (k <= 0)
                return -1;
        }
        int l = k >= j ? j : k;
        System.arraycopy(buf, bufpos, abyte0, i, l);
        bufpos += l;
        return l;
    }

    public int read(byte abyte0[]) throws IOException
    {
        return read(abyte0, 0, abyte0.length);
    }

    public synchronized int available() throws IOException
    {
        return bufcount - bufpos;
    }
}
