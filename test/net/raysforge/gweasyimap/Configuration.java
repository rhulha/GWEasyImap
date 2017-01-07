package net.raysforge.gweasyimap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class Configuration {

	public String imap_protocol;
	public String imap_server;
	public String imap_port;
	public String imap_tapp_name;
	public String imap_tapp_key;

	public String smtp_server;
	public String smtp_user;
	public String smtp_pw;
	public String smtp_from;
	public String smtp_subject;
	public String smtp_body;

	public Configuration(InputStream is) {
		Properties props = new Properties();
		try {
			props.load(is);
			is.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		Field[] fields = getClass().getFields();
		for (Field field : fields) {
			try {
				//System.out.println("test " + field.getName());
				field.set(this, props.getProperty(field.getName()));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		
		 
	}

}
