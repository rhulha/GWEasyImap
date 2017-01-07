package net.raysforge.gweasyimap;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.search.FlagTerm;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

public class TestEasyIMAPStore {

	public static void main(String[] args) throws MessagingException, IOException {
		Configuration cfg = new Configuration(new FileInputStream("test.conf"));

		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props);

		EasyIMAPStore is = new EasyIMAPStore(session, cfg.imap_protocol, cfg.imap_server, Integer.parseInt(cfg.imap_port), cfg.imap_tapp_name, cfg.imap_tapp_key, "check");


		IMAPFolder folder = (IMAPFolder) is.getFolder("INBOX");

		int newMessageCount = folder.getNewMessageCount();

		if (newMessageCount > 0) {
			folder.open(Folder.READ_ONLY);
			
			Flags f = new Flags(Flag.RECENT);
			Message[] messages = folder.search(new FlagTerm(f, true));
			
			for (Message message : messages) {
				
				String messageID = ((IMAPMessage)message).getMessageID();
				System.out.println(messageID);
				
				System.out.println(message.getRecipients(RecipientType.TO)[0]);
				System.out.println(message.getSubject());
			}
			
			
		}


		is.close();
	}

}
