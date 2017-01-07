package net.raysforge.gweasyimap;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage.RecipientType;

public class TestEasyIMAPStore {

	public static void main(String[] args) throws MessagingException, IOException {
		Configuration cfg = new Configuration(new FileInputStream("test.conf"));
		
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);
        
		EasyIMAPStore is = new EasyIMAPStore(session, new URLName("imap://check@localhost:143"), "imap", false, cfg.imap_tapp_name, cfg.imap_tapp_key, "check");
		
		is.connect("check", "");
		
		Folder folder = is.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		
		Message[] messages = folder.getMessages();
		
		for (Message message : messages) {
			System.out.println(message.getRecipients(RecipientType.TO)[0]);
			System.out.println(message.getSubject());
		}
		
		is.close();
	}

}
