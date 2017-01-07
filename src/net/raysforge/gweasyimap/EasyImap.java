package net.raysforge.gweasyimap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchException;
import javax.mail.util.SharedFileInputStream;

import com.sun.mail.iap.*;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.util.MailLogger;

import net.raysforge.commons.*;

// TODO: replace GW "Mailbox" with IMAP "INBOX".

public class EasyImap {
	static Logger logger = Logger.getLogger(EasyImap.class.getName());

	private String protocol;
	private final String server;
	private final int port;
	private final String tappName;
	private final String tappKey;
	private final String user;


	public EasyImap(String protocol, String server, int port, String tappName, String tappKey, String user) {
		this.protocol = protocol;
		this.server = server;
		this.port = port;
		this.tappName = tappName;
		this.tappKey = tappKey;
		this.user = user;
	}

	public EasyIMAPProtocol ip;

	private boolean rawSocketMode=false;

	public void connect() throws IOException, ProtocolException {
		Properties props = new Properties();
		props.setProperty("mail.imap.connectiontimeout", "4000");
		props.setProperty("mail.imap.timeout", "4000");
		ip = new EasyIMAPProtocol(protocol, server, port, props, false, new MailLogger("test", "prefix", false, System.out));
		Argument a = new Argument();
		a.writeAtom("XGWTRUSTEDAPP");
		ip.writeCommand("AUTHENTICATE", a);
		Response response = ip.readResponse();
		if (!response.isContinuation())
			throw new ProtocolException(response);
		ip.getOutputStream().write(TrustedApp.getCommandAndBase64ofNameAndKey(tappName, tappKey));
		ip.getOutputStream().flush();
		response = ip.readResponse();
		if (!response.isOK())
			throw new ProtocolException(response);

		// ip.handleResult(response)

		ip.login(user, "");
	}

	private int[] issueSearch(String msgID) throws ProtocolException, SearchException, IOException {
		Argument args = new Argument();
		args.writeAtom("X-GWMessageID \"" + msgID + "\"");

		Response[] r;

		r = ip.command("SEARCH", args);

		Response response = r[r.length - 1];
		int[] matches = null;

		// Grab all SEARCH responses
		if (response.isOK()) { // command succesful
			Vector<Integer> v = new Vector<Integer>();
			int num;
			for (int i = 0, len = r.length; i < len; i++) {
				if (!(r[i] instanceof IMAPResponse))
					continue;

				IMAPResponse ir = (IMAPResponse) r[i];
				// There *will* be one SEARCH response.
				if (ir.keyEquals("SEARCH")) {
					while ((num = ir.readNumber()) != -1)
						v.addElement(new Integer(num));
					r[i] = null;
				}
			}

			// Copy the vector into 'matches'
			int vsize = v.size();
			matches = new int[vsize];
			for (int i = 0; i < vsize; i++)
				matches[i] = ((Integer) v.elementAt(i)).intValue();
		}

		// dispatch remaining untagged responses
		ip.notifyResponseHandlers(r);
		ip.handleResult(response);
		return matches;
	}

	// folder = IMAPFolder, i.e.: "Cabinet/Test".
	// msgID = SOAP MsgID ( not MessageID)
	public File getMessage(String folder, String msgID) throws IOException, ProtocolException, SearchException {
		if (ip == null || !ip.isAuthenticated())
			connect();

		ip.examine(folder);

		int[] searchResult = issueSearch(msgID);
		if (searchResult.length != 1)
			throw new SearchException("Message not found.");
		int msgNR = searchResult[0];

		File rawMsg = null;
		String tmpDir = System.getProperty("java.io.tmpdir");
		rawMsg = File.createTempFile("imap", ".eml", new File(tmpDir));
		OutputStream rawMsgOS = new BufferedOutputStream(new FileOutputStream(rawMsg));

		if (rawSocketMode) {
			// RAW Socket mode, very fast
			Argument arg = new Argument();
			arg.writeAtom(msgNR + " (BODY[])");
			ip.writeCommand("FETCH", arg);
			InputStream inputStream = ip.getMNInputStream();

			//        String intro = new String(inputStream.readResponse().getBytes());
			String intro = StreamUtils.readOneLineOfInputStream(inputStream, Charset.forName("ISO-8895-1"));

			if (intro.indexOf("BAD") == -1) {
				long size = Long.parseLong(StringUtils.substring(intro, '{', '}'));
				Streamer s = new Streamer();
				s.closeInputStream = false;
				s.closeOutputStream = false;
				s.bufferSize = IntUtils.const1MB;
				s.maxBytesToRead = size;
				s.copy(inputStream, rawMsgOS);
				String end1 = StreamUtils.readOneLineOfInputStream(inputStream, Charset.forName("ISO-8895-1")); // '('
				String end2 = StreamUtils.readOneLineOfInputStream(inputStream, Charset.forName("ISO-8895-1")); // 'a8
				// OK
				// FETCH
				// completed'
				logger.fine(end1 + end2);
			} else {
				rawMsgOS.close();
				rawMsgOS = null;
			}
		} else {

			// Standard IMAP partial fetch way, but 3 times slower...
			int c = 0;
			while (true) {
				BODY body = ip.fetchBody(msgNR, null, c * IntUtils.const1MB, IntUtils.const1MB);
				ByteArray byteArray = body.getByteArray();
				if (byteArray.getCount() == 0)
					break;
				logger.fine("byteArray.getCount: " + byteArray.getCount());
				rawMsgOS.write(byteArray.getBytes(), byteArray.getStart(), byteArray.getCount());
				c++;
			}
		}
		StreamUtils.close(rawMsgOS);
		return rawMsg;
	}

	public static InputStream searchMimeMessageForAttachment(MimeContent msg, String fileName) throws IOException, MessagingException {
		Object content = msg.getContent();
		if (content instanceof MimeMessage) {
			logger.fine("mimemsg");
			return searchMimeMessageForAttachment(new MimeContent((MimeMessage) content), fileName);
		} else if (content instanceof MimeMultipart) {
			logger.fine("multip");
			MimeMultipart mmp = (MimeMultipart) content;
			int count = mmp.getCount();
			for (int i = 0; i < count; i++) {
				BodyPart bodyPart = mmp.getBodyPart(i);
				InputStream stream = searchMimeMessageForAttachment(new MimeContent(bodyPart), fileName);
				if (stream != null)
					return stream;
			}
			return null;
		} else if (content instanceof InputStream) {
			logger.fine("stream");
			if (fileName.equals(msg.getFileName())) {
				InputStream ins = (InputStream) content;
				return ins;
			} else {
				return null;
				// throw new MessagingException("unknown content type: " +
				// content.getClass().getName());
			}
		} else if (content instanceof String) {
			System.out.println("content instanceof String");
			if (fileName.equals(msg.getFileName())) {
				logger.fine("string");
				String str = (String) content;
				logger.fine(str);
				return new ByteArrayInputStream(str.getBytes());
			}
			return null;
		} else {
			throw new MessagingException("unknown content type: " + content.getClass().getName());
		}
	}

	public static InputStream getAttachment(File f, String fileName) throws MessagingException, IOException {
		Session session = Session.getDefaultInstance(new Properties(), null);
		MimeMessage mm = new MimeMessage(session, new SharedFileInputStream(f.getPath()));
		InputStream searchMimeMessageForAttachment = searchMimeMessageForAttachment(new MimeContent(mm), fileName);
		if (searchMimeMessageForAttachment == null)
			throw new SearchException("attachment not found in IAMP mail.");
		return searchMimeMessageForAttachment;
	}

	public InputStream getAttachment(String folder, String msgID, String attName) throws IOException, ProtocolException, MessagingException {
		File rawMsg = getMessage(folder, msgID);
		return getAttachment(rawMsg, attName);
	}

	public void select(String folder) throws ProtocolException {
		ip.select(folder);
	}

	public int[] searchNew() throws SearchException, ProtocolException {
		Flags f = new Flags(Flag.SEEN);
		return ip.search(new FlagTerm(f, false));
	}

	public boolean logout() {
		try {
			ip.logout();
			return false;
		} catch (ProtocolException e) {
			e.printStackTrace();
			return true;
		}
	}

	/*
	public static void main(String[] args)
	{
	    String k = "2F1486E1044C00008651DE00F400EB002F1486E2044C00008651DE00F400EB00";
	    EasyImap imap = new EasyImap("demo.de", "demo", k, "demo");
	
	    try
	    {
	        StopWatch sw = new StopWatch();
	        InputStream stream = imap.getAttachment("INBOX",  TestIDs.bigEmail, TestIDs.testEmailAttachmentName1);
	        logger.debug("imap.getAttachmen took (ms): " + sw.stop());
	        StreamUtils.streamCopy(stream, new FileOutputStream("C:\\test.zip"));
	        logger.debug("streamCopy took (ms): " + sw.stop());
	    } catch (Exception e)
	    {
	        e.printStackTrace();
	    }
        imap.logout();
	}
	*/
}
