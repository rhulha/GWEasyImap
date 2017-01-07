package net.raysforge.gweasyimap;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.IMAPProtocol;

public class EasyIMAPStore extends IMAPStore {

	private String tappName;
	private String tappKey;
	private String username;

	public EasyIMAPStore(String protocol, String server, int port, String tappName, String tappKey, String username) throws MessagingException {
		this(Session.getDefaultInstance(new Properties()), protocol, server, port, tappName, tappKey, username);
	}

	public EasyIMAPStore(Session session, String protocol, String server, int port, String tappName, String tappKey, String username) throws MessagingException {
		super(session, new URLName(protocol, server, port, null, username, ""), protocol, protocol.equalsIgnoreCase("imaps"));
		this.tappName = tappName;
		this.tappKey = tappKey;
		this.username = username;
		connect();
	}

	@Override
	protected void preLogin(IMAPProtocol ip) throws ProtocolException {
		try {
			Argument a = new Argument();
			a.writeAtom("XGWTRUSTEDAPP");
			ip.writeCommand("AUTHENTICATE", a);
			Response response = ip.readResponse();
			if (!response.isContinuation())
				throw new ProtocolException(response);

			Method method;
			try {
				method = ip.getClass().getSuperclass().getDeclaredMethod("getOutputStream");
				method.setAccessible(true);
				OutputStream os = (OutputStream) method.invoke(ip);
				byte[] cmd = TrustedApp.getCommandAndBase64ofNameAndKey(tappName, tappKey);
				System.out.println(new String(cmd));
				os.write(cmd);
				os.flush();
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
			
			response = ip.readResponse();
			if (!response.isOK())
				throw new ProtocolException(response);
			
			ip.login(username, "");
			
		} catch (IOException e) {
			throw new ProtocolException(e.getMessage(), e);
		}
	}

}
