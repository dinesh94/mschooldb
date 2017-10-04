package generic.mongo.microservices.api.v1;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.api.v1.payu.PayUMoneyHash;
import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.model.StaticCollectionNames;
import generic.mongo.microservices.util.CommonUtil;
import generic.mongo.microservices.util.LogUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class PayUMoneyPrePaymentController {

	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	@Value("${kandapohe.payment_success}")
    private String surl;
    
	@Value("${kandapohe.payment_fail}")
	private String furl;
    
	@Value("${kandapohe.payment_cancel}")
	private String curl;
    
	@Value("${spring.data.mongodb.database}")
	private String database;
	
	@Resource
	private MongoClient mongoClient;

	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	@RequestMapping(method = { RequestMethod.POST }, value = "{id}/pay/{planName}")
	public ResponseEntity<?> pay(HttpServletResponse response,
			@ApiIgnore RequestObject request,
			@PathVariable("id") String userId,
			@PathVariable("planName") String planName) {

		response.setContentType("text/html;charset=UTF-8");

		Document plan = null;
		try {
			plan = getPlanDetails(planName);
		}
		catch (Exception e1) {
			LOGGER.debug("ERROR 1 - PayUMoneyPrePaymentController.pay() userId = " + userId + " plan = " + planName);
			e1.printStackTrace();
		}
		
		if (plan != null) {
			final MongoCollection<Document> userCollection = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.USER);
			Document userObject = CommonUtil.findOneObject(userCollection, userId);

			if (userObject == null) {
				LOGGER.debug("ERROR 2 - PayUMoneyPrePaymentController.pay() userId = " + userId + " plan = " + planName);
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			else {

				try {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("phone", userObject.get("phone"));
					params.put("lastname", userObject.get("phone"));
					params.put("amount", calculateAmount(plan));
					params.put("productinfo", "kandapohe_"+planName);
					params.put("firstname", userObject.get("loginUserFirstName"));
					params.put("email", userObject.get("email"));
					params.put("udf1", userObject.get("_id"));
					params.put("udf2", plan.get("type"));
					
					params.put("surl", surl);
					params.put("furl", furl);
					params.put("curl", curl);

					Map<String, String> payUhashed = new PayUMoneyHash().hashCalMethod(params);
					String txnid = payUhashed.get("txnid");
					
					BasicDBObject searchQuery = new BasicDBObject().append(CommonString.ID, userObject.get(CommonString.ID));
					userCollection.updateOne(searchQuery, new Document("$set", new Document("txnid", txnid)));
					
					postDataToPayUMoney(payUhashed, response);
				}
				catch (ServletException | IOException e) {
					LOGGER.debug("ERROR 3 - PayUMoneyPrePaymentController.pay() userId = " + userId + " plan = " + planName);
					e.printStackTrace();
				}
				catch (Exception e) {
					LOGGER.debug("ERROR 4 - PayUMoneyPrePaymentController.pay() userId = " + userId + " plan = " + planName);
					e.printStackTrace();
				}
			}

		}
		else {
			LOGGER.debug("ERROR 5 - PayUMoneyPrePaymentController.pay() userId = " + userId + " plan = " + plan);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private int calculateAmount(Document plan) throws Exception {
		Integer price = plan.getInteger("price");
		if(plan.get("discountAmount") != null){
			Integer discountAmount = plan.getInteger("discountAmount");
			if(discountAmount.intValue() > 0 && (price.doubleValue() - discountAmount.doubleValue()) > 0){
				price = price - discountAmount; 
			}
		}
		
		if(price == null){
			throw new Exception("Invalid plan value found in server database");
		}else if(price.intValue() <= 0){
			throw new Exception("Invalid plan value found in server database");
		}
		
		return price;
	}

	/**
	 * @param planName
	 * @return
	 * @throws Exception
	 */
	public Document getPlanDetails(String planName) throws Exception {
		final MongoCollection<Document> collectionForPlan = mongoClient.getDatabase(database).getCollection(StaticCollectionNames.MEMBERSHIP_PLANS);

		final List<Document> result = new ArrayList<>();
		Pattern regexPlanName = Pattern.compile(planName, Pattern.CASE_INSENSITIVE);
		BasicDBObject searchQuery = new BasicDBObject().append("type", regexPlanName);
		FindIterable<Document> iterable = collectionForPlan.find(searchQuery);
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});
		
		if(result != null && result.isEmpty() == false){
			return result.get(0);
		}else{
			throw new Exception("No such plan exist in server database.");
		}
		
	}

	/**
	 * @param values
	 * @param response
	 * @throws IOException
	 */
	private void postDataToPayUMoney(Map<String, String> values, HttpServletResponse response) throws IOException {
		PrintWriter writer = response.getWriter();
		
		 String htmlResponse = "<html> <body>  \n"
	                + "      \n"
	                + "  \n"
	                + "  <h1> Redirecting to payment gateway.. </h1>\n"
	                + "  \n" + "<div style=\"display: none;\">"
	                + "        <form id=\"payuform\" action=\"" + values.get("action") + "\"  name=\"payuform\" method=POST >\n"
	                + "      <input type=\"hidden\" name=\"key\" value=" + values.get("key").trim() + ">"
	                + "      <input type=\"hidden\" name=\"hash\" value=" + values.get("hash").trim() + ">"
	                + "      <input type=\"hidden\" name=\"txnid\" value=" + values.get("txnid").trim() + ">"
	                + "      <table>\n"
	                + "        <tr>\n"
	                + "          <td><b>Mandatory Parameters</b></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "         <td>Amount: </td>\n"
	                + "          <td><input name=\"amount\" value=" + values.get("amount").trim() + " /></td>\n"
	                + "          <td>First Name: </td>\n"
	                + "          <td><input name=\"firstname\" id=\"firstname\" value=" + values.get("firstname").trim() + " /></td>\n"
	                + "        <tr>\n"
	                + "          <td>Email: </td>\n"
	                + "          <td><input name=\"email\" id=\"email\" value=" + values.get("email").trim() + " /></td>\n"
	                + "          <td>Phone: </td>\n"
	                + "          <td><input name=\"phone\" value=" + values.get("phone") + " ></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Product Info: </td>\n"
	                + "<td><input name=\"productinfo\" value=" + values.get("productinfo").trim() + " ></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Success URI: </td>\n"
	                + "          <td colspan=\"3\"><input name=\"surl\"  size=\"64\" value=" + values.get("surl") + "></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Failure URI: </td>\n"
	                + "          <td colspan=\"3\"><input name=\"furl\" value=" + values.get("furl") + " size=\"64\" ></td>\n"
	                + "        </tr>\n"
	                + "\n"
	                + "        <tr>\n"
	                + "          <td colspan=\"3\"><input type=\"hidden\" name=\"service_provider\" value=\"payu_paisa\" /></td>\n"
	                + "        </tr>\n"
	                + "             <tr>\n"
	                + "          <td><b>Optional Parameters</b></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Last Name: </td>\n"
	                + "          <td><input name=\"lastname\" id=\"lastname\" value=" + values.get("lastname") + " ></td>\n"
	                + "          <td>Cancel URI: </td>\n"
	                + "          <td><input name=\"curl\" value=" + values.get("curl") + " ></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Address1: </td>\n"
	                + "          <td><input name=\"address1\" value=" + values.get("address1") + " ></td>\n"
	                + "          <td>Address2: </td>\n"
	                + "          <td><input name=\"address2\" value=" + values.get("address2") + " ></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>City: </td>\n"
	                + "          <td><input name=\"city\" value=" + values.get("city") + "></td>\n"
	                + "          <td>State: </td>\n"
	                + "          <td><input name=\"state\" value=" + values.get("state") + "></td>\n"
	                + "        </tr>\n"
	                + "        <tr>\n"
	                + "          <td>Country: </td>\n"
	                + "          <td><input name=\"country\" value=" + values.get("country") + " ></td>\n"
	                + "          <td>Zipcode: </td>\n"
	                + "          <td><input name=\"zipcode\" value=" + values.get("zipcode") + " ></td>\n"
	                + "        </tr>\n"
	                + "          <td>UDF1: </td>\n"
	                + "          <td><input name=\"udf1\" value=" + values.get("udf1") + "></td>\n"
	                + "          <td>UDF2: </td>\n"
	                + "          <td><input name=\"udf2\" value=" + values.get("udf2") + "></td>\n"
	                + " <td><input name=\"hashString\" value=" + values.get("hashString") + "></td>\n"
	                + "          <td>UDF3: </td>\n"
	                + "          <td><input name=\"udf3\" value=" + values.get("udf3") + " ></td>\n"
	                + "          <td>UDF4: </td>\n"
	                + "          <td><input name=\"udf4\" value=" + values.get("udf4") + " ></td>\n"
	                + "          <td>UDF5: </td>\n"
	               + "          <td><input name=\"udf5\" value=" + values.get("udf5") + " ></td>\n"
	                 + "          <td>PG: </td>\n"
	               + "          <td><input name=\"pg\" value=" + values.get("pg") + " ></td>\n"
	                + "        <td colspan=\"4\"><input type=\"submit\" value=\"Submit\"  /></td>\n"
	                + "      \n"
	                + "    \n"
	                + "      </table>\n"
	                + "    </form>\n"
	                + " <script> "
	                + " document.getElementById(\"payuform\").submit(); "
	                + " </script> "
	                + "       </div>   "
	                + "  \n"
	                + "  </body>\n"
	                + "</html>";
	// return response
	        writer.println(htmlResponse);
	}
}
