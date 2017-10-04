package generic.test.classes.email;

import java.io.StringWriter;

import javax.annotation.Resource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import generic.mongo.microservices.model.EMailRequest;

@Component
public class Mailer {

	@Value("${email.from}")
	private String from;

	@Resource
	private JavaMailSender javaMailService;

	@Resource
	private VelocityEngine velocityEngine;

	public void setMailSender(JavaMailSender javaMailService) {
		this.javaMailService = javaMailService;
	}

	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	public void sendMail(EMailRequest mail, VelocityContext velocityContext) {
		SimpleMailMessage message = new SimpleMailMessage();

		message.setTo(mail.getMailTo().toArray(new String[mail.getMailTo().size()]));
		message.setFrom(from);
		message.setSubject(mail.getMailSubject());

		Template template = velocityEngine.getTemplate("./templates/" + mail.getTemplateName());

		StringWriter stringWriter = new StringWriter();

		template.merge(velocityContext, stringWriter);

		message.setText(stringWriter.toString());

		javaMailService.send(message);
	}

	public void sendHtmlMail(final EMailRequest mail, VelocityContext velocityContext) {

		Template template = velocityEngine.getTemplate("./templates/" + mail.getTemplateName());

		final StringWriter stringWriter = new StringWriter();

		template.merge(velocityContext, stringWriter);

		MimeMessagePreparator preparator = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {

				String mailto = mail.getMailTo().toArray(new String[mail.getMailTo().size()])[0];
				String subject = mail.getMailSubject();
				/*
				 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(mailto));
				 * mimeMessage.setFrom(new InternetAddress(from)); mimeMessage.setText(stringWriter.toString());
				 */
				// use the true flag to indicate you need a multipart message
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
				helper.setTo(mailto);
				helper.setFrom(new InternetAddress(from));
				helper.setSubject(subject);
				// use the true flag to indicate the text included is HTML
				helper.setText(stringWriter.toString(), true);
			}
		};

		javaMailService.send(preparator);
	}

}