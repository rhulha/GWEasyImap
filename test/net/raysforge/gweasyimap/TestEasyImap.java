package net.raysforge.gweasyimap;


import java.io.FileInputStream;
import java.io.IOException;

import javax.mail.search.SearchException;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;

public class TestEasyImap {

	public static void main(String[] args) throws IOException, ProtocolException, SearchException {
		Configuration cfg = new Configuration(new FileInputStream("test.conf"));
		
		EasyImap imap = new EasyImap(cfg.imap_protocol, cfg.imap_server, Integer.parseInt(cfg.imap_port), cfg.imap_tapp_name, cfg.imap_tapp_key, "info");
		imap.connect();
		imap.select("INBOX");
		int[] msgs = imap.searchNew();
		for (int msg : msgs) {
			Response[] fetch = imap.ip.fetch(msg, "ENVELOPE");
			for (Response response : fetch) {
				System.out.println(response.readString());
			}

		}
	}

}
