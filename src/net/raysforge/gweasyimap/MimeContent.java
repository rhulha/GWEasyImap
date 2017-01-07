package net.raysforge.gweasyimap;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


public class MimeContent
{
    private MimeMessage mm;
    private BodyPart bp;

    public MimeContent(MimeMessage mm)
    {
        this.mm = mm;
    }

    public MimeContent(BodyPart bp)
    {
        this.bp = bp;
    }

    public Object getContent() throws IOException, MessagingException
    {
        return (mm == null ? bp.getContent() : mm.getContent());
    }

    public String getFileName() throws MessagingException
    {
        return (mm == null ? bp.getFileName() : mm.getFileName());
    }
}
