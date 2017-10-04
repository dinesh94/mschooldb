package generic.mongo.microservices.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMailRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5612154635418161902L;

	private List<String> mailTo = new ArrayList<String>();

	private List<String> mailCc = new ArrayList<String>();

	private List<String> mailBcc = new ArrayList<String>();

	private String mailSubject = "";

	private String templateName = "";

	private Map<String, String> templateValues = new HashMap<String, String>();

	private String mailBody;
	
	private String userType;
	
	public EMailRequest() {
	}

	public EMailRequest(List<String> mailTo, String mailSubject) {
		super();
		this.mailTo = mailTo;
		this.mailSubject = mailSubject;
	}

	public List<String> getMailTo() {
		return mailTo;
	}

	public void setMailTo(List<String> mailTo) {
		this.mailTo = mailTo;
	}

	public List<String> getMailCc() {
		return mailCc;
	}

	public void setMailCc(List<String> mailCc) {
		this.mailCc = mailCc;
	}

	public List<String> getMailBcc() {
		return mailBcc;
	}

	public void setMailBcc(List<String> mailBcc) {
		this.mailBcc = mailBcc;
	}

	public String getMailSubject() {
		return mailSubject;
	}

	public void setMailSubject(String mailSubject) {
		this.mailSubject = mailSubject;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public Map<String, String> getTemplateValues() {
		return templateValues;
	}

	public void setTemplateValues(Map<String, String> templateValues) {
		this.templateValues = templateValues;
	}

	public String getMailBody() {
		return mailBody;
	}

	public void setMailBody(String mailBody) {
		this.mailBody = mailBody;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	
	
}
