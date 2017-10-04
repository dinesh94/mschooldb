/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.parser.ParseException;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.EMailRequest;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.CommonUtil;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Dinesh
 *
 */
@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class EmailController {

	@Resource
	private MongoClient mongoClient;

	@Autowired
	private JavaMailSender javaMailSender;

	@Resource
	private CollectionObjectController collectionObjectController;

	@Resource
	private SearchController searchController;

	@Resource
	private FileController fileController;

	@Resource
	private VelocityEngine velocityEngine;

	@Resource
	private UserController userController;

	
	@Value("${spring.mail.username}")
	private String emailFrom;
	
	@RequestMapping(method = { RequestMethod.DELETE }, value = "/delete-template/{id}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	
	public ResponseEntity<?> deleteTemplate(
			@ApiIgnore RequestObject request,
			@PathVariable("id") String objectId) throws IOException {
		return collectionObjectController.deleteObject(request, objectId);
	}

	@RequestMapping(method = { RequestMethod.GET }, value = "/show-template/{id}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public HttpEntity<byte[]> showTemplate(
			@ApiIgnore RequestObject request,
			@PathVariable("id") String objectId) throws IOException {

		return fileController.getFile(request, objectId);
	}

	@SuppressWarnings("deprecation")
	@RequestMapping(method = { RequestMethod.GET }, value = "/show-templates")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> showTemplates(
			@ApiIgnore RequestObject request) throws IOException {

		GridFS gridFS = new GridFS(mongoClient.getDB(request.getDbName()), CommonString.COLLECTION_EMAIL_TEMPLATES);

		DBCursor fileList = gridFS.getFileList();
		Iterator<DBObject> iterator = fileList.iterator();

		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

		iterator.forEachRemaining(new Consumer<DBObject>() {

			@Override
			public void accept(DBObject dbObject) {
				Map<String, Object> collection = new HashMap<String, Object>();
				for (String key : dbObject.keySet()) {
					collection.put(key, dbObject.get(key));
				}

				result.add(collection);
			}
		});

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@SuppressWarnings("deprecation")
	@RequestMapping(method = { RequestMethod.POST }, value = "/create-template")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public void createTemplate(
			@ApiIgnore RequestObject request,
			@RequestParam("templateName") String templateName,
			@RequestParam("file") MultipartFile templateFile) throws IOException {

		GridFSDBFile file = findFridFSDBFile(request, templateName);

		if (file == null) {
			GridFS gridFS = new GridFS(mongoClient.getDB(request.getDbName()), CommonString.COLLECTION_EMAIL_TEMPLATES);
			GridFSInputFile gfsFile = gridFS.createFile(templateFile.getInputStream());

			String fileID = CommonUtil.getId();
			gfsFile.put(CommonString.TEMPLATES_NAME, templateName);
			gfsFile.setFilename(templateFile.getOriginalFilename());
			gfsFile.setId(fileID);

			saveAndAppendCommonProperties(gfsFile);
		}
		else {
			throw new RuntimeException("Email template with name '" + templateName + "' is already exist in the database");
		}
	}

	/**
	 * @param gfsFile
	 */
	private void saveAndAppendCommonProperties(GridFSInputFile gfsFile) {
		if (gfsFile.get(CommonString.CREATED_ON) == null) {
			gfsFile.put(CommonString.CREATED_ON, System.currentTimeMillis());
		}
		if (gfsFile.get(CommonString.CREATED_BY) == null) {
			gfsFile.put(CommonString.CREATED_BY, gfsFile.getId());
		}
		if (gfsFile.get(CommonString.ACCESS_LEVEL) == null) {
			gfsFile.put(CommonString.ACCESS_LEVEL, CommonString.AccessLevel.DEFAULT.value());
		}
		gfsFile.put(CommonString.UPDATED_ON, System.currentTimeMillis());
		gfsFile.put(CommonString.UPDATED_BY, gfsFile.getId());

		gfsFile.save();
	}

	@Async
	@RequestMapping(method = { RequestMethod.POST }, value = "/send-email-use-db-template")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public void sendEmailUseDBTemplate(
			@ApiIgnore RequestObject request,
			@RequestBody EMailRequest mail,
			@RequestHeader Map<String, String> headers) throws IOException, ParseException {

		GridFSDBFile file = findFridFSDBFile(request, mail.getTemplateName());

		if (file == null)
			throw new RuntimeException("Email template with name '" + mail.getTemplateName() + "' does not exist in the database");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		file.writeTo(bos);

		mail.setTemplateName(mail.getTemplateName());

		VelocityContext velocityContext = new VelocityContext();
		addCommonHeaders(velocityContext, headers);
		
		for (Entry<String, String> templateValues : mail.getTemplateValues().entrySet()) {
			velocityContext.put(templateValues.getKey(), templateValues.getValue());
		}

		Template template = CommonUtil.getTemplate(bos.toString("UTF-8")); //velocityEngine.getTemplate("./templates/" + mail.getTemplateName());

		StringWriter stringWriter = new StringWriter();
		template.merge(velocityContext, stringWriter);

		MimeMessagePreparator preparator = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {

				String[] mailto = mail.getMailTo().toArray(new String[mail.getMailTo().size()]);
				String subject = mail.getMailSubject();
				/*
				 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(mailto));
				 * mimeMessage.setFrom(new InternetAddress(from)); mimeMessage.setText(stringWriter.toString());
				 */
				// use the true flag to indicate you need a multipart message
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
				helper.setTo(mailto);
				helper.setSubject(subject);
				helper.setFrom(emailFrom); 
				// use the true flag to indicate the text included is HTML
				helper.setText(stringWriter.toString(), true);
			}
		};

		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean emailSendStatus = true;
				String emailSendMessage = "";
				try {
					javaMailSender.send(preparator);
				}
				catch (Exception e) {
					emailSendStatus = false;
					emailSendMessage = e.getMessage();

				}
				logEmail(request, mail, emailSendStatus, emailSendMessage);
			}

		}).start();

	}

	public static <T> Stream<List<T>> batches(List<T> source, int length) {
	    if (length <= 0)
	        throw new IllegalArgumentException("length = " + length);
	    int size = source.size();
	    if (size <= 0)
	        return Stream.empty();
	    int fullChunks = (size - 1) / length;
	    return IntStream.range(0, fullChunks + 1).mapToObj(
	        n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
	}

	
	@Async
	@RequestMapping(method = { RequestMethod.POST }, value = "/send-static-email")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public void sendStaticEmail(
			@ApiIgnore RequestObject request,
			@RequestBody EMailRequest mail,
			@RequestHeader Map<String, String> headers) throws IOException, ParseException {

		if(mail.getMailBcc() != null && mail.getMailSubject() != null && !mail.getMailSubject().isEmpty() && mail.getMailBody() != null && !mail.getMailBody().isEmpty())
		{
			List<String> mailTo = mail.getMailBcc();
			List<List<String>> batchOf50 = batches(mailTo, 50).collect(Collectors.toList());

			for(List<String> batch : batchOf50)
			{
				MimeMessagePreparator preparator = new MimeMessagePreparator() {

					public void prepare(MimeMessage mimeMessage) throws Exception {

						//String mailto = mail.getMailTo().toArray(new String[mail.getMailTo().size()])[0];
						String subject = mail.getMailSubject();
						String mailBody = mail.getMailBody();

						//wrap in html body
						 String htmlBody = "<html><body>"+mailBody+"</body></html> ";
						/*
						 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(mailto));
						 * mimeMessage.setFrom(new InternetAddress(from)); mimeMessage.setText(stringWriter.toString());
						 */
						// use the true flag to indicate you need a multipart message
						MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
						//helper.setTo();
						helper.setBcc(batch.toArray(new String[batch.size()]));
						helper.setSubject(subject);
						helper.setFrom(emailFrom); 
						// use the true flag to indicate the text included is HTML
						helper.setText(htmlBody, true);
					}
				};

				new Thread(new Runnable() {
					@Override
					public void run() {
						boolean emailSendStatus = true;
						String emailSendMessage = "";
						try {
							javaMailSender.send(preparator);
						}
						catch (Exception e) {
							emailSendStatus = false;
							emailSendMessage = e.getMessage();

							e.printStackTrace();
						}
						logEmail(request, mail, emailSendStatus, emailSendMessage);
					}

				}).start();

			}
			
			//notifyAdmin()
		}
	}

	@Async
	@RequestMapping(method = { RequestMethod.POST }, value = "/query")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = CommonString.COLLECTION_EMAIL_TEMPLATES),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> query(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) throws IOException, ParseException {

		Document requestBody = Document.parse(jsonString);
		String queryString = requestBody.getString("query");
		
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());

		// {query : "{'email':/.*dinesh*/}"}
		BasicDBObject query = BasicDBObject.parse(queryString);
		FindIterable<Document> dumps = null;//collection.find(query);
		
		return new ResponseEntity<>(dumps, HttpStatus.OK);
	}
	
	/**
	 * @param request
	 * @param mail
	 * @param emailSendMessage 
	 * @param emailSendStatus 
	 */
	private void logEmail(RequestObject request, EMailRequest mail, boolean emailSendStatus, String emailSendMessage) {
		Document doc = new Document();
		doc.append("db", request.getDbName());
		doc.append("collection", request.getCollectionName());
		doc.append("apikey", request.getApiKey());
		doc.append("mailTo", CommonUtil.convertToBasicDBObject(mail.getMailTo()));
		doc.append("mailCc", CommonUtil.convertToBasicDBObject(mail.getMailCc()));
		doc.append("mailBcc", CommonUtil.convertToBasicDBObject(mail.getMailBcc()));
		doc.append("mailSubject", mail.getMailSubject());
		doc.append("templateName", mail.getTemplateName());
		doc.append("templateValues", new BasicDBObject(mail.getTemplateValues()));
		doc.append("emailSendStatus", emailSendStatus);
		doc.append("emailSendMessage", emailSendMessage);

		// Switching off email logs
		//collectionObjectController.insertNew(request.getDbName(), CommonString.EMAIL_LOGS, doc);
	}

	/**
	 * @param request
	 * @param templateName
	 * @return
	 */
	private GridFSDBFile findFridFSDBFile(RequestObject request, String templateName) {
		@SuppressWarnings("deprecation")
		GridFS gridFS = new GridFS(mongoClient.getDB(request.getDbName()), CommonString.COLLECTION_EMAIL_TEMPLATES);

		BasicDBObject query = new BasicDBObject();
		query.put(CommonString.TEMPLATES_NAME, templateName);
		GridFSDBFile imageForOutput = gridFS.findOne(query);
		return imageForOutput;
	}

	@Async
	@RequestMapping(method = { RequestMethod.POST }, value = "/send-static-email-template")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public void sendHtmlMailTemplate(final RequestObject request, final EMailRequest mail,
			@RequestHeader Map<String, String> headers) {

		Template template = velocityEngine.getTemplate("./templates/" + mail.getTemplateName());

		final StringWriter stringWriter = new StringWriter();

		VelocityContext velocityContext = new VelocityContext();
		addCommonHeaders(velocityContext, headers);

		Map<String, String> templateValues = mail.getTemplateValues();
		for (Entry<String, String> templateValue : templateValues.entrySet()) {
			velocityContext.put(templateValue.getKey(), templateValue.getValue());
		}

		template.merge(velocityContext, stringWriter);

		MimeMessagePreparator preparator = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {

				String[] mailto = mail.getMailTo().toArray(new String[mail.getMailTo().size()]);
				String subject = mail.getMailSubject();
				/*
				 * mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(mailto));
				 * mimeMessage.setFrom(new InternetAddress(from)); mimeMessage.setText(stringWriter.toString());
				 */
				// use the true flag to indicate you need a multipart message
				MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
				helper.setTo(mailto);
				helper.setSubject(subject);
				helper.setFrom(emailFrom); 
				// use the true flag to indicate the text included is HTML
				helper.setText(stringWriter.toString(), true);
			}
		};

		new Thread(new Runnable() {
			@Override
			public void run() {
				boolean emailSendStatus = true;
				String emailSendMessage = "";
				try {
					javaMailSender.send(preparator);
				}
				catch (Exception e) {
					emailSendStatus = false;
					emailSendMessage = e.getMessage();
					e.printStackTrace();
				}

				logEmail(request, mail, emailSendStatus, emailSendMessage);
			}

		}).start();

	}

	/**
	 * @param velocityContext
	 * @param headers
	 */
	private void addCommonHeaders(VelocityContext velocityContext, Map<String, String> headers) {
		for (Entry<String, String> templateValue : headers.entrySet()) {
			velocityContext.put(templateValue.getKey(), templateValue.getValue());
		}
	}
}
