package generic.mongo.microservices.api.v1;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.CreditResponse;
import generic.mongo.microservices.model.EMailRequest;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.CommonUtil;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class UserController {

	@Resource
	private CollectionObjectController collectionObjectController;

	@Resource
	private SearchController searchController;

	@Resource
	private EmailController emailController;

	@Value("${kandapohe.server}")
	private String kandapoheServer;

	@Resource
	MongoClient mongoClient;

	@RequestMapping(method = { RequestMethod.GET }, value = "/canIViewThisProfile/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public synchronized ResponseEntity<?> canIViewThisProfile(
			@ApiIgnore RequestObject request,
			@PathVariable("id") String userId,
			@RequestParam("otherUserId") String otherUserId) {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		Document user = CommonUtil.findOneObject(collection, userId);

		if (isPhysicallyChallenged(user)) {
			Document otherUser = CommonUtil.findOneObject(collection, otherUserId);
			if (isPhysicallyChallenged(otherUser)) {
				CreditResponse creditResponse = new CreditResponse(HttpStatus.OK);
				/** Allow handicap user to see handicap profile details **/
				creditResponse.setIsAlreadyUsedForProfile(true);
				creditResponse.setUserData(otherUser);
				return new ResponseEntity<>(creditResponse, HttpStatus.OK);
			}
		}

		Document activeMemberships = getActiveMemberShip(user);
		if (activeMemberships == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		else {
			List<String> viewdProfile = getProfilesViewdDuringMembership(activeMemberships);

			CreditResponse creditResponse = new CreditResponse(HttpStatus.OK);
			creditResponse.setActiveMemberShip(activeMemberships);

			if (isCreditAlreadySpentOnThisProfile(otherUserId, viewdProfile)) {
				creditResponse.setIsAlreadyUsedForProfile(true);
				Document otherUser = CommonUtil.findOneObject(collection, otherUserId);
				creditResponse.setUserData(otherUser);
			}
			else {
				creditResponse.setIsAlreadyUsedForProfile(false);

				boolean doIhaveCredits = isMembershipCreditRemaining(activeMemberships);
				boolean isMembershipDateExpire = isMembershipDateExpire(activeMemberships);

				if (!doIhaveCredits && isMembershipDateExpire) {
					creditResponse.setIsCreditExpire(true);
					creditResponse.setIsMembershipDateExpire(true);
				}

				else if (!doIhaveCredits) {
					creditResponse.setIsCreditExpire(true);
					creditResponse.setIsMembershipDateExpire(false);
				}

				else if (isMembershipDateExpire) {
					creditResponse.setIsCreditExpire(false);
					creditResponse.setIsMembershipDateExpire(true);
				}
			}

			return new ResponseEntity<>(creditResponse, HttpStatus.OK);
		}
	}

	/**
	 * @param user
	 * @return
	 */
	private boolean isPhysicallyChallenged(Document user) {
		return user.getString("physicality") != null && user.getString("physicality").equalsIgnoreCase("Physically Challenged");
	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/detailsOnCredit")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public synchronized ResponseEntity<?> detailsOnCredit(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) {

		Document requestBody = Document.parse(jsonString);
		String userId = requestBody.getString("userId");
		String otherUserId = requestBody.getString("otherUserId");

		CreditResponse creditResponse = new CreditResponse(HttpStatus.BAD_REQUEST);

		Document otherUser = null;

		if (userId == null || otherUserId == null) {
			return new ResponseEntity<>("userId == " + userId + " & otherUserId = " + otherUserId, HttpStatus.BAD_REQUEST);
		}

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		Document user = CommonUtil.findOneObject(collection, userId);

		Document activeMemberships = getActiveMemberShip(user);

		BasicDBList newMemberships = new BasicDBList();

		creditResponse.setActiveMemberShip(activeMemberships);

		List<String> viewdProfile = getProfilesViewdDuringMembership(activeMemberships);

		if (isCreditAlreadySpentOnThisProfile(otherUserId, viewdProfile)) {
			otherUser = CommonUtil.findOneObject(collection, otherUserId);
			creditResponse.setUserData(otherUser);
			creditResponse.setHttpStatus(HttpStatus.OK);
		}
		else {
			boolean doIhaveCredits = isMembershipCreditRemaining(activeMemberships);
			boolean isMembershipDateExpire = isMembershipDateExpire(activeMemberships);

			creditResponse.setIsCreditExpire(!doIhaveCredits);
			creditResponse.setIsMembershipDateExpire(isMembershipDateExpire);

			if (doIhaveCredits && !isMembershipDateExpire) {
				viewdProfile.add(otherUserId);
				activeMemberships.replace("viewdProfile", viewdProfile);

				otherUser = CommonUtil.findOneObject(collection, otherUserId);
				creditResponse.setUserData(otherUser);
				creditResponse.setHttpStatus(HttpStatus.OK);

				newMemberships.add(activeMemberships);
				BasicDBObject searchQuery = new BasicDBObject().append(CommonString.ID, userId);

				user.put("membership", newMemberships);
				collection.replaceOne(searchQuery, user);
			}
		}

		return new ResponseEntity<>(creditResponse, HttpStatus.OK);
	}

	public boolean isCreditAlreadySpentOnThisProfile(String otherUserId, List<String> viewdProfile) {
		return viewdProfile.contains(otherUserId);
	}

	public List<String> getProfilesViewdDuringMembership(Document activeMemberships) {
		List<String> viewdProfile = new ArrayList<>();
		if (activeMemberships != null && activeMemberships.containsKey("viewdProfile")) {
			viewdProfile = ((List<String>) activeMemberships.get("viewdProfile"));
		}
		return viewdProfile;
	}

	/**
	 * @param user
	 * @return
	 */
	public Document getActiveMemberShip(Document user) {
		try {
			List<Document> memberships = (List<Document>) user.get("membership");
			if (memberships != null && !memberships.isEmpty()) {
				for (Document document : memberships) {
					Boolean active = document.getBoolean("active");
					if (active != null && active == true) {
						return document;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean isMembershipDateExpire(Document activeMemberships) {
		String expiryDateInLong = activeMemberships.get("expire").toString();
		Calendar expiryDate = Calendar.getInstance();
		expiryDate.setTimeInMillis(new Long(expiryDateInLong));
		long estimatedTime = expiryDate.getTimeInMillis() - System.currentTimeMillis();

		if (estimatedTime < 0) {
			return true;
		}

		return false;
	}

	public boolean isMembershipCreditRemaining(Document activeMemberships) {
		boolean result = false;
		List<String> viewdProfile = getProfilesViewdDuringMembership(activeMemberships);
		Object object = activeMemberships.get("credits");
		if (viewdProfile != null && object != null) {
			try {
				Integer intCredits = Integer.parseInt(object.toString());
				int creditRemaining = intCredits - viewdProfile.size();
				if (creditRemaining > 0) {
					result = true;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/projectionview")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> view(@ApiIgnore RequestObject request,
			@RequestBody List<String> projectionParam,
			@RequestParam(value = "skipVerifiedCheck", required = false) Boolean skipVerifiedCheck) {

		if (projectionParam.isEmpty()) {
			projectionParam = new ArrayList<>();
		}
		projectionParam.add("verified");

		Set<String> projectionDistinct = new HashSet<>(projectionParam);

		List<Document> result = new ArrayList<>();
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());

		//Bson projection = fields(include(projectionParam), excludeId());
		Bson projection = fields(include(new ArrayList<String>(projectionDistinct)));

		FindIterable<Document> iterable;
		iterable = collection.find().projection(projection);

		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				if (skipVerifiedCheck != null && skipVerifiedCheck == true) {
					result.add(document);
				}
				else {
					Object verified = document.get("verified");
					if (verified != null && verified.toString().equalsIgnoreCase("true"))
						result.add(document);
				}
			}
		});

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/register")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> createUser(
			@ApiIgnore RequestObject request,
			@RequestParam(value = "email", required = true) String email,
			@RequestParam(value = "emailConfirmation", required = false) boolean emailConfirmation,
			@RequestParam(value = "emailTemplateName", required = false, defaultValue = "confirmEmailRegistered.vm") String emailTemplateName,
			@RequestParam(value = "emailSubject", required = false, defaultValue = CommonString.CREATE_USER_CONFIRM_EMAIL_SUBJECT) String emailSubject,
			@RequestBody String jsonString,
			@RequestHeader Map<String, String> headers) throws Exception {

		Document findEmailDoc = new Document();
		findEmailDoc.append(CommonString.LOGINID, email);
		findEmailDoc = searchController.findOne(request.getDbName(), request.getCollectionName(), findEmailDoc);

		if (findEmailDoc == null) {
			String token = CommonUtil.getId();

			Document doc = Document.parse(jsonString);
			doc.append(CommonString.LOGINID, email);
			doc.append(CommonString.EMAIL_VALIDATED, false);
			doc.append(CommonString.EMAIL_VERIFICATION_TOKEN, token);

			String objectId = collectionObjectController.insertNew(request.getDbName(), request.getCollectionName(), doc);
			doc.append(CommonString.ID, objectId);

			new Thread(new Runnable() {
				@Override
				public void run() {
					if (emailConfirmation) {
						List<String> to = new ArrayList<String>();
						to.add(email);

						Map<String, String> templateValues = new HashMap<String, String>();
						templateValues.put("username", doc.getString("first_name"));
						templateValues.put("email", email);
						templateValues.put("token", token);
						templateValues.put(CommonString.ID, objectId);
						templateValues.put("kandapoheServer", kandapoheServer);

						templateValues.put(CommonString.DB, request.getDbName());
						templateValues.put(CommonString.COLLECTION, request.getCollectionName());

						EMailRequest eMailRequest = new EMailRequest(to, emailSubject);
						eMailRequest.setTemplateValues(templateValues);

						eMailRequest.setTemplateName(emailTemplateName);
						emailController.sendHtmlMailTemplate(request, eMailRequest, headers);
					}
				}
			}).start();

			return new ResponseEntity<>(doc, HttpStatus.OK);
		}
		else {
			throw new Exception("User already exist");
		}

	}

	@RequestMapping(method = RequestMethod.GET, value = "/confirm-registration/{token}")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> confirmRegistration(
			@ApiIgnore RequestObject request,
			@PathVariable(value = "token") String token) throws Exception {

		Document findDocByToken = new Document();
		findDocByToken.append(CommonString.EMAIL_VERIFICATION_TOKEN, token);
		findDocByToken = searchController.findOne(request.getDbName(), request.getCollectionName(), findDocByToken);

		if (findDocByToken != null) {
			Document updatedDocument = Document.parse(findDocByToken.toJson());
			updatedDocument.append(CommonString.EMAIL_VALIDATED, true);
			collectionObjectController.update(request.getDbName(), request.getCollectionName(), findDocByToken, updatedDocument);
			return new ResponseEntity<>(HttpStatus.OK);
		}
		else {
			throw new Exception("Invalid confirmation link");
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/forgot-password")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> forgotPassword(
			@ApiIgnore RequestObject request,
			@RequestParam(value = "email") String email,
			@RequestHeader Map<String, String> headers) throws Exception {

		final Document doc = new Document();
		doc.append(CommonString.LOGINID, email);
		Document searchedDoc = searchController.findOne(request.getDbName(), request.getCollectionName(), doc);
		if (searchedDoc != null) {

			new Thread(new Runnable() {

				@Override
				public void run() {
					List<String> to = new ArrayList<String>();
					to.add(email);

					Map<String, String> templateValues = new HashMap<String, String>();
					templateValues.put("username", email);
					String password;
					if (searchedDoc.get(CommonString.PASSWORD) != null) {
						password = searchedDoc.get(CommonString.PASSWORD).toString();
					}
					else {
						password = CommonUtil.getId();
						Document docWithPassword = Document.parse(searchedDoc.toJson());
						docWithPassword.append(CommonString.PASSWORD, password);
						try {
							collectionObjectController.update(request.getDbName(), request.getCollectionName(), searchedDoc, docWithPassword);
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					templateValues.put("password", password);
					templateValues.put(CommonString.DB, request.getDbName());
					templateValues.put(CommonString.COLLECTION, request.getCollectionName());

					EMailRequest eMailRequest = new EMailRequest(to, CommonString.FORGOT_PASSWORD_SUBJECT);
					eMailRequest.setTemplateValues(templateValues);

					eMailRequest.setTemplateName("forgetPassword.vm");
					emailController.sendHtmlMailTemplate(request, eMailRequest, headers);
				}
			}).start();

			return new ResponseEntity<>(HttpStatus.OK);
		}
		else {
			throw new Exception("User does not exit");
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/change-user-email")
	@ApiImplicitParams({ @ApiImplicitParam(name = CommonString.DB, value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = CommonString.COLLECTION, value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana"),
			@ApiImplicitParam(name = "id", value = "id", required = true, dataType = "string"),
			@ApiImplicitParam(name = "changeEmail", value = "changeEmail", required = true, dataType = "string"),
			@ApiImplicitParam(name = "emailTemplateName", value = "emailTemplateName", required = false, dataType = "string", defaultValue = "confirmEmailRegistered.vm"),
			@ApiImplicitParam(name = "authdomain", value = "authdomain", required = false, dataType = "string", defaultValue = "kandapohe.com"),
			@ApiImplicitParam(name = "emailSubject", value = "emailSubject", required = false, dataType = "string", defaultValue = CommonString.CREATE_USER_CONFIRM_EMAIL_SUBJECT),
	})
	public ResponseEntity<?> confirmUserEmail(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) throws Exception {

		Document params = Document.parse(jsonString);

		String id = params.getString("id");
		String changeEmail = params.getString("changeEmail");
		String emailTemplateName = params.getString("emailTemplateName") != null ? params.getString("emailTemplateName") : "confirmEmailRegistered.vm";
		String authdomain = params.getString("authdomain") != null ? params.getString("authdomain") : "kandapohe.com";
		String emailSubject = params.getString("emailSubject") != null ? params.getString("emailSubject") : CommonString.CREATE_USER_CONFIRM_EMAIL_SUBJECT;

		Document findEmailDoc = new Document();
		findEmailDoc.append(CommonString.LOGINID, changeEmail);
		findEmailDoc = searchController.findOne(request.getDbName(), request.getCollectionName(), findEmailDoc);
		if (findEmailDoc != null)
			throw new Exception("Email ID is already registered with us.");

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		List<Document> documents = CommonUtil.findObjects(collection, id);
		Document existingDoc = new Document();
		if (documents != null)
			existingDoc = documents.get(0);

		if (existingDoc != null) {
			String token = CommonUtil.getId();

			Document updatedDoc = new Document();
			String objectId = existingDoc.get(CommonString.ID).toString();
			updatedDoc.append(CommonString.ID, objectId);
			updatedDoc.append(CommonString.EMAIL_VERIFICATION_TOKEN, token);
			updatedDoc.append("changeEmail", changeEmail);

			collectionObjectController.update(request.getDbName(), request.getCollectionName(), existingDoc, updatedDoc);

			new Thread(new Runnable() {
				@Override
				public void run() {
					List<String> to = new ArrayList<String>();
					to.add(changeEmail);

					Map<String, String> templateValues = new HashMap<String, String>();
					templateValues.put("authdomain", authdomain);
					templateValues.put("username", changeEmail);
					templateValues.put("email", changeEmail);
					templateValues.put("token", token);
					templateValues.put("flow", "changeEmail");
					templateValues.put(CommonString.ID, objectId);
					templateValues.put("kandapoheServer", kandapoheServer);

					templateValues.put(CommonString.DB, request.getDbName());
					templateValues.put(CommonString.COLLECTION, request.getCollectionName());

					EMailRequest eMailRequest = new EMailRequest(to, emailSubject);
					eMailRequest.setTemplateValues(templateValues);

					eMailRequest.setTemplateName(emailTemplateName);
					emailController.sendHtmlMailTemplate(request, eMailRequest, templateValues);
				}
			}).start();

			return new ResponseEntity<>(jsonString, HttpStatus.OK);
		}
		else {
			throw new Exception("Server error try again later");
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/show-password")
	public ResponseEntity<?> showPassword(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) {

		Document params = Document.parse(jsonString);

		String userId = params.getString("userId");
		String secret = params.getString("secret");

		String password = "";
		if ("kpadmin".equalsIgnoreCase(secret)) {
			try {
				MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
				Document user = CommonUtil.findOneObject(collection, userId);

				password = user.getString("password");
			}
			catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		params.append("password", password);
		
		return new ResponseEntity<>(params, HttpStatus.OK);
	}

}
