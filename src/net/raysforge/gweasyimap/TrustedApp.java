package net.raysforge.gweasyimap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import net.raysforge.commons.Codecs;

public class TrustedApp {

	public static String getBase64ofNameAndKey(String tappName, String tappKey) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = null;
		try {
			ps = new PrintStream(baos, true, "ISO-8859-15");
			ps.print(tappName);
			ps.write(0);
			ps.print(tappKey);
			ps.flush();
			return Codecs.toBase64(baos.toByteArray());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] getCommandAndBase64ofNameAndKey(String tappName, String tappKey) {
		try {
			return ("XGWTRUSTEDAPP " + getBase64ofNameAndKey(tappName, tappKey) + "\n").getBytes("ISO-8859-15");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
