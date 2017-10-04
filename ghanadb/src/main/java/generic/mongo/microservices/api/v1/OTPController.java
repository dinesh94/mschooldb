package generic.mongo.microservices.api.v1;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.MongoClient;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.MobileOTP;
import generic.mongo.microservices.model.RequestObject;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class OTPController {

	@Resource
	CollectionObjectController collectionObjectController;

	@Resource
	private SearchController searchController;
	
	@Resource
	private MongoClient mongoClient;
	
	String twofactorApiKey = "5e9e5150-3b8b-11e6-9522-00163ef91450";
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/send-otp")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> sent(@ApiIgnore RequestObject request,
			@RequestBody MobileOTP mobileOTP) throws UnirestException, JsonParseException, JsonMappingException, IOException {

		boolean mobileAlreadyExist = checkMobileNoExists(request.getDbName(), mobileOTP.getMobileNumber(), mobileOTP.getId());
		Document doc = new Document();

		if (!mobileAlreadyExist) {
			Random rnd = new Random();
			int n = 100000 + rnd.nextInt(900000);

			String url = "http://2factor.in/API/V1/" + twofactorApiKey + "/SMS/" + mobileOTP.getMobileNumber() + "/" + n + "/KP-PHONE-REG";
			HttpResponse<JsonNode> response = Unirest.get(url).asJson();

			collectionObjectController.getObject(request, mobileOTP.getId());
			String jsonString = "{'_id':'" + mobileOTP.getId() + "', 'otp':'" + n + "', 'tmpPhone':'" + mobileOTP.getMobileNumber() + "' }";
			collectionObjectController.saveObject(request, jsonString);
			doc.append("AlreadyExist", false);
		}
		else {
			doc.append("AlreadyExist", true);
		}
		return new ResponseEntity<>(doc, HttpStatus.OK);
	}
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/verify-otp")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> verify(@ApiIgnore RequestObject request,
			@RequestBody MobileOTP mobileOTP) throws UnirestException, JsonParseException, JsonMappingException, IOException {

		ResponseEntity<?> response = collectionObjectController.getObject(request, mobileOTP.getId());
		Document document = (Document) response.getBody();
		
		if(document.get("otp") != null && document.get("otp").toString().equals(mobileOTP.getOtp())){
			String jsonString = "{'_id':'"+ mobileOTP.getId()+"', 'mobile_no_verified':'true', 'phone':'"+document.get("tmpPhone")+"'}";
			collectionObjectController.saveObject(request, jsonString);
			return new ResponseEntity<>(HttpStatus.OK);
			
		}

		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}
	
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/mobile-exist")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> mobileExist(@ApiIgnore RequestObject request,
			@RequestBody MobileOTP mobileOTP) throws UnirestException, JsonParseException, JsonMappingException, IOException {

		boolean mobileAlreadyExist = checkMobileNoExists(request.getDbName(), mobileOTP.getMobileNumber(), mobileOTP.getId());
		Document doc = new Document();

		if (!mobileAlreadyExist) 
			doc.append("AlreadyExist", false);
		else 
			doc.append("AlreadyExist", true);

		return new ResponseEntity<>(doc, HttpStatus.OK);

		
	}
	private boolean checkMobileNoExists(String dbName,String mobileNo,String id){

		Document userMobileDoc = new Document();
		userMobileDoc.append(CommonString.USER_COLLECTION_PHONE, mobileNo);
		
		boolean isMobileAlreadyRegister = false;
		List<Document> results = searchController.find(dbName, CommonString.COLLECTION_USER, userMobileDoc);
		if(results != null){
			for(Document doc : results){
				if(doc != null ){
					if(doc.getString(CommonString.ID).equals(id)){
						isMobileAlreadyRegister = false;
						break;
					}else{
						isMobileAlreadyRegister = true;
					}
				}
			}
		}
		return isMobileAlreadyRegister;
	}
}
