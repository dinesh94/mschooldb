package generic.mongo.microservices.api.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.EMailRequest;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.model.StaticCollectionNames;
import generic.mongo.microservices.util.CommonUtil;
import generic.mongo.microservices.util.LogUtils;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class PayUMoneyPostPaymentController {

	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	@Value("${spring.data.mongodb.database}")
	private String database;

	@Value("${kandapohe.payment_redirect}")
	private String paymentRedirect;

	@Resource
	private MongoClient mongoClient;
	
	@Resource
	private UserController userController;
	
	@Resource
	private CollectionObjectController collectionObjectController;
	
	@Resource
	private PayUMoneyPrePaymentController payUMoneyPrePaymentController;

	@Resource
	private EmailController emailController;
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/_payment_success")
	public void success(HttpServletRequest request, HttpServletResponse response) {
		LOGGER.debug("PayUMoneyPostPaymentController.success() START");

		Document document = getDocumentFromRequestParam(request);
		MongoCollection<Document> collection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.PAYMENT);
		String objectId = CommonUtil.getId();
		
		document.append("kp_status", "success");
		collectionObjectController.insertNew(collection, objectId, document);

		String userId = document.get("udf1").toString();
		String planName = document.get("udf2").toString();
		String txnFromPayU = document.get("txnid").toString();
		
		LOGGER.debug("PayUMoneyPostPaymentController.success() userId = " + userId + " plan = " + planName);
		
		Document plan = null;
		try {
			plan = payUMoneyPrePaymentController.getPlanDetails(planName);
		}
		catch (Exception e1) {
			LOGGER.debug("ERROR 1 - PayUMoneyPostPaymentController.updateMembershipInfo() userId = " + userId + " plan = " + planName);
			e1.printStackTrace();
		}
		
		final MongoCollection<Document> userCollection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.USER);
		Document userObject = CommonUtil.findOneObject(userCollection, userId);
		
		String userTxnId = userObject.getString("txnid");
		if(isTxnIdMatch(userTxnId, txnFromPayU)){
			// Let's remove txnid in first shot to avoid if user refresh, we will be able to catch second request & txnid will fail proceed second request.
			Document txnidDocument = new Document();
			txnidDocument.append("txnid", "");
			
			BasicDBObject searchQuery = new BasicDBObject().append(CommonString.ID, userObject.get(CommonString.ID));
			userCollection.updateOne(searchQuery, new Document("$set", new Document("txnid", "")));
			
			try {
				updateUserMembershipInfo(userObject, plan);
				
				// Send invoice
				sendInvoice(userObject, plan);
			}
			catch (IOException e1) {
				LOGGER.debug("ERROR 3 - PayUMoneyPostPaymentController.success() userId = " + userId + " plan = " + planName);
				redirect(request, response, "failure");
			}
			
			LOGGER.debug("PayUMoneyPostPaymentController.success() END");
			redirect(request, response, "success");
			
		}else{
			LOGGER.debug("PayUMoneyPostPaymentController.success() REFRESH HAS BEEN CACHED - isTxnIdMatch false");
			LOGGER.debug("ERROR 1 - PayUMoneyPostPaymentController.success() userId = " + userId + " plan = " + planName);
			redirect(request, response, "failure");
		}
	}

	private void sendInvoice(Document userObject, Document plan) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				RequestObject request = new RequestObject("kandapohe", "users", "", true);
				
				String email = userObject.get("email").toString();
				String emailSubject = "Congratulation on your subscription to kandapohe";
				String emailTemplateName = "paymentInvoice.vm";
				
				List<String> to = new ArrayList<String>();
				to.add(email);

				Map<String, String> templateValues = new HashMap<String, String>();
				templateValues.put("username", email);
				templateValues.put("email", email);
				
				templateValues.put("type", plan.getString("type"));
				templateValues.put("price", plan.getInteger("price").toString());
				templateValues.put("credits", plan.getInteger("credits").toString());
				templateValues.put("start", CommonUtil.lognToDateFormattedDateString(plan.getLong("start")));
				templateValues.put("expire", CommonUtil.lognToDateFormattedDateString(plan.getLong("expire")));

				EMailRequest eMailRequest = new EMailRequest(to, emailSubject);
				eMailRequest.setTemplateValues(templateValues);

				eMailRequest.setTemplateName(emailTemplateName);
				emailController.sendHtmlMailTemplate(request, eMailRequest, templateValues);
			
			}
		}).start();
	}

	private void redirect(HttpServletRequest request, HttpServletResponse response, String status) {
		String redirectTo = paymentRedirect + "?status"+status;
		try {
			/*RequestDispatcher dd=request.getRequestDispatcher(redirectTo);
			dd.forward(request, response);*/
			response.sendRedirect(redirectTo);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isTxnIdMatch(String userTxnId, String txnFromPayU) {
		if(userTxnId == null || txnFromPayU == null || userTxnId.isEmpty() || txnFromPayU.isEmpty()){
			return false;
		}
		return userTxnId.equals(txnFromPayU);
	}

	/**
	 * @param useId
	 * @param planName
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private void updateUserMembershipInfo(Document userObject, Document plan) throws JsonParseException, JsonMappingException, IOException {
		Document activeMemberships = userController.getActiveMemberShip(userObject);
		
		if(isCurrentMemberShipActiveWithCreditRemaining(activeMemberships)){
			updateExistingMemberShipDetails(userObject, activeMemberships, plan);
		}else{
			createNewMemberShipDetails(userObject, plan);
		}
	}

	private void updateExistingMemberShipDetails(Document userObject, Document activeMemberships, Document plan) throws JsonParseException, JsonMappingException, IOException {
		Integer credits = plan.getInteger("credits");
		Long expire = Long.parseLong(plan.getString("durationInLong"));
		Integer membershipCount = 1;
		if(activeMemberships.get("membershipCount") != null){
			try{
				membershipCount = activeMemberships.getInteger("membershipCount") + 1;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		LOGGER.debug("PayUMoneyPostPaymentController.updateExistingMemberShipDetails()");
		LOGGER.debug(" Previous credits = "+credits);
		LOGGER.debug(" Previous expire = "+expire);
		LOGGER.debug(" Previous membershipCount = "+activeMemberships.getInteger("membershipCount"));
		
		if(!userController.isMembershipDateExpire(activeMemberships)){
			 credits += activeMemberships.getInteger("credits");
			 expire += activeMemberships.getLong("expire");
		}
		
		LOGGER.debug(" New credits = "+credits);
		LOGGER.debug(" New expire = "+expire);
		LOGGER.debug(" New membershipCount = "+membershipCount);
		
		Document membership = plan;
		membership.remove(CommonString.ID);
		membership.append("membershipCount", membershipCount);
		membership.append("viewdProfile", activeMemberships.get("viewdProfile"));
		
		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		membership.append("start", start.getTimeInMillis());
		membership.append("expire", expire);
		membership.append("active", true);
		membership.append("credits", credits);
		
		List<Document> membershipArr = new ArrayList<>();
		membershipArr.add(membership);
		
		final MongoCollection<Document> userCollection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.USER);
		BasicDBObject searchQuery = new BasicDBObject().append(CommonString.ID, userObject.get(CommonString.ID));
		userCollection.updateOne(searchQuery, new Document("$set", new Document("membership", membershipArr)));
	}

	/**
	 * @param userObject
	 * @param plan
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void createNewMemberShipDetails(Document userObject, Document plan) throws JsonParseException, JsonMappingException, IOException {
		Document membership = plan;
		
		membership.remove(CommonString.ID);
		
		Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		membership.append("start", start.getTimeInMillis());
		
		Long end = start.getTimeInMillis() + Long.parseLong(plan.getString("durationInLong"));
		membership.append("expire", end);
		membership.append("active", true);
		membership.append("viewdProfile", new ArrayList<String>());
		membership.append("membershipCount", 1);
		
		List<Document> membershipArr = new ArrayList<>();
		membershipArr.add(membership);

		final MongoCollection<Document> userCollection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.USER);
		BasicDBObject searchQuery = new BasicDBObject().append(CommonString.ID, userObject.get(CommonString.ID));
		userCollection.updateOne(searchQuery, new Document("$set", new Document("membership", membershipArr)));


		/*MongoCollection<Document> collection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.USER);
		Bson filter = Filters.eq("_id", userObject.get(CommonString.ID));
		Bson updates = Updates.set("membership", membershipArr);
		collection.findOneAndUpdate(filter, updates);*/
		
		//collection.update(userObject, userObject);
		
		//collectionObjectController.update(database, StaticCollectionNames.USER, userObject, document);
	}

	private boolean isCurrentMemberShipActiveWithCreditRemaining(Document activeMemberships) {
		if(activeMemberships != null){
			boolean doIhaveCredits = userController.isMembershipCreditRemaining(activeMemberships) ;
			boolean isMembershipDateExpire = userController.isMembershipDateExpire(activeMemberships);
			
			if(doIhaveCredits && !isMembershipDateExpire)
				return true;
		}
		
		return false;
	}
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/_payment_failure")
	public void fail(HttpServletRequest request, HttpServletResponse response) {
		LOGGER.debug("PayUMoneyPostPaymentController.fail()");

		Document document = getDocumentFromRequestParam(request);
		MongoCollection<Document> collection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.PAYMENT);
		
		document.append("kp_status", "failure");
		String objectId = CommonUtil.getId();
		collectionObjectController.insertNew(collection, objectId, document);

		try {
			String redirectTo = paymentRedirect;
			redirectTo += "?status=failure";
			response.sendRedirect(redirectTo);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/_payment_cancel")
	public void cancel(HttpServletRequest request, HttpServletResponse response) {
		LOGGER.debug("PayUMoneyPostPaymentController.cancel()");

		Document document = getDocumentFromRequestParam(request);
		MongoCollection<Document> collection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.PAYMENT);
		String objectId = CommonUtil.getId();
		document.append("kp_status", "cancel");
		collectionObjectController.insertNew(collection, objectId, document);

		try {
			
			String redirectTo = paymentRedirect;
			redirectTo += "?status=cancel";
			response.sendRedirect(redirectTo);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Document getDocumentFromRequestParam(HttpServletRequest request) {
		Document document = new Document();
		Enumeration paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			String paramValue = request.getParameter(paramName);

			if (paramValue != null && !paramValue.isEmpty())
				document.append(paramName, paramValue);

		}
		return document;
	}
}
