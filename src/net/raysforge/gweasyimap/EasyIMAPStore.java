package net.raysforge.gweasyimap;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

	public EasyIMAPStore(Session session, URLName url, String name, boolean isSSL, String tappName, String tappKey, String username) {
		super(session, url, name, isSSL);
		this.tappName = tappName;
		this.tappKey = tappKey;
		this.username = username;
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
