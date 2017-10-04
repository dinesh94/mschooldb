package generic.test.classes.email;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import generic.mongo.microservices.util.LogUtils;

import java.util.Properties;

/**
 * Created by anirudh on 28/10/14.
 */
public class JavaGmailSSLExample {

	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	public static void main(String[] args) {

		final String username = "dinesh94@gmail.com";
		final String password = "";

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("dinesh94@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse("dinesh94@gmail.com"));
			message.setSubject("Test JCG Example");
			message.setText("Hi," +
					"This is a Test mail for JCG Example!");

			Transport.send(message);

			LOGGER.debug("Mail sent succesfully!");

		}
		catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
