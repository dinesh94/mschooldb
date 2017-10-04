package generic.test.classes.email;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hibernate.validator.constraints.Email;

public class TestMail {

	private int port = 465;

	private String host = "spreaditguru.com";

	private String from = "dinesh@spreaditguru.com";

	private boolean auth = true;

	private String username = "dinesh@spreaditguru.com";

	private String password = "";

	private boolean debug = true;

	public TestMail() {

		Properties props = new Properties();
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		//props.put("mail.smtp.ssl.enable", true);
		//props.put("mail.smtp.starttls.enable", true);

		Authenticator authenticator = null;
		if (auth) {
			props.put("mail.smtp.auth", true);
			authenticator = new Authenticator() {
				private PasswordAuthentication pa = new PasswordAuthentication(username, password);

				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return pa;
				}
			};
		}

		Session session = Session.getInstance(props, authenticator);
		session.setDebug(debug);

		MimeMessage message = new MimeMessage(session);
		try {
			message.setFrom(new InternetAddress(from));
			InternetAddress[] address = { new InternetAddress("dinesh94@gmail.com") };
			message.setRecipients(Message.RecipientType.TO, address);
			message.setSubject("Test");
			message.setSentDate(new Date());
			message.setText("Test");
			Transport.send(message);
		}
		catch (MessagingException ex) {
			ex.printStackTrace();
		}

	}

	public static void main(String[] args) {
		//new TestMail();
		new TestMail().one();
	}

	private void one() {
	}
}
