package generic.test.classes.email;

import java.util.Properties;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class SpringEmailTemplateExample {
	public static void main(String[] args) {

		SimpleMailMessage message = new SimpleMailMessage();

		message.setSubject("Subject - Send Email using Spring Velocity Template");

		message.setTo("dinesh94@gmail.com");
		message.setFrom("noreply@kandapohe.com");
		message.setText("This is test email");

		/*String host = "mail.spreaditguru.com";
		int port = 25;
		String username = "no-reply@spreaditguru.com";
		String password = "Gorkf34tSHzl";*/
		
		String host = "mail.kandapohe.com";
		int port = 26;
		String username = "noreply@kandapohe.com";
		String password = "n@rep!y@kpnew";

		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
		javaMailSender.setHost(host);
		javaMailSender.setPort(port);
		javaMailSender.setUsername(username);
		javaMailSender.setPassword(password);

		Properties props = new Properties();
		javaMailSender.setJavaMailProperties(props);

		javaMailSender.send(message);

	}
}
