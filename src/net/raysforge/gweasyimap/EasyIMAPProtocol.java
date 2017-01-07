package net.raysforge.gweasyimap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.util.MailLogger;

public class EasyIMAPProtocol extends IMAPProtocol {

	public EasyIMAPProtocol(String name, String host, int port, Properties props, boolean isSSL, MailLogger logger) throws IOException, ProtocolException {
		super(name, host, port, props, isSSL, logger);
	}

	public OutputStream getOutputStream() {
		return super.getOutputStream();
	}

	public InputStream getMNInputStream() {
		// TODO: This breaks IMAP as of Javamail 1.4.1!
		return null;
		//return  super.getInputStream();
	}

}
