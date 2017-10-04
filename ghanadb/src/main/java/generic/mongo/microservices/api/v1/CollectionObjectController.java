/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.CommonUtil;
import generic.mongo.microservices.util.LogUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Dinesh
 *
 */
@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class CollectionObjectController {
	private static final Logger LOGGER = LogUtils.loggerForThisClass();

	@Resource
	MongoClient mongoClient;

	@Resource
	UserController userController;

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "id", value = "Object Id", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> getObject(@ApiIgnore RequestObject request,
			@PathVariable("id") String idParam) {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		List<Document> documents = CommonUtil.findObjects(collection, idParam);
		Document document = new Document();
		if (documents != null)
			document = documents.get(0);

		boolean isAdmin = false;
		if (request.getAdmin() != null && request.getAdmin().equalsIgnoreCase("true")) {
			isAdmin = true;
		}

		if (request.getCollectionName().equalsIgnoreCase("user") && isAdmin == false) {
			if (request.getUser() == null) {
				return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
			}
			else if (request.getUser() != null) {
				Document loggedinUser = CommonUtil.findOneObject(collection, request.getUser());
				filterFields(document, loggedinUser);
			}
		}

		if (document.containsKey("password"))
			document.remove("password");

		return new ResponseEntity<>(document, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/projection")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "id", value = "Object Id", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> getProjectionObject(@ApiIgnore RequestObject request,
			@PathVariable("id") String idParam,
			@RequestParam("keys") List<String> keys) {

		if (keys == null) {
			keys = new ArrayList<String>();
		}
		keys.add("_id");

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		List<Document> documents = CommonUtil.findObjects(collection, idParam);
		Document document = new Document();
		if (documents != null) {
			for (String key : keys) {
				document.append(key, documents.get(0).get(key));
			}
		}
		return new ResponseEntity<>(document, HttpStatus.OK);
	}

	/**
	 * POST WILL CREATE A NEW OBJECT IN THE DATABSE, WITH GENERATED ID
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 **/
	@RequestMapping(method = RequestMethod.POST)
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> saveObject(@ApiIgnore RequestObject request, @RequestBody String jsonString) throws JsonParseException, JsonMappingException, IOException {

		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<Document> future = executor.submit(new Callable<Document>() {
			@Override
			public Document call() throws Exception {
				MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());

				Document doc = Document.parse(jsonString);
				Object objectId = doc.get(CommonString.ID);

				if (objectId != null) {
					Document existingDocument = CommonUtil.findOneObject(collection, objectId.toString());
					if (existingDocument != null) {
						/* if object exist in database with given id --> Merge */
						doc = doPatch(collection, existingDocument, doc);
					}
					else {
						insertNew(collection, objectId.toString(), doc);
					}
				}
				else {
					/* Use has not provided id --> Create new entry */
					objectId = CommonUtil.getId();
					insertNew(collection, objectId.toString(), doc);
				}

				return doc;
			}
		});

		if (request.getAsync())
			return new ResponseEntity<>(HttpStatus.OK);
		else {
			Document result = null;
			try {
				result = future.get();
			}
			catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "id", value = "Object Id", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> deleteObject(@ApiIgnore RequestObject request, @PathVariable("id") String idParam) {
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		Long deleteCount = doDelete(collection, idParam);
		return new ResponseEntity<>(deleteCount, HttpStatus.OK);
	}

	/**
	 * PUT WILL REPLACE WHOLE OBJECT, WHICH IS ALREADY PRESENT IN THE DATABASE
	 **/
	/**
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 **/
	@RequestMapping(method = { RequestMethod.PUT }, value = "/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "id", value = "Object Id", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> put(@ApiIgnore RequestObject request, @PathVariable("id") String idParam, @RequestBody String jsonString) throws JsonParseException, JsonMappingException, IOException {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		doDelete(collection, idParam);

		Document doc = Document.parse(jsonString);
		doc.put(CommonString.ID, idParam);

		insertNew(collection, idParam, doc);

		return new ResponseEntity<>(doc, HttpStatus.OK);
	}

	/**
	 * PATHC WILL MERGE EXISTING OBJECT, IF OBJECT DOES NOT EXIST IT WILL PERFORM PUT
	 * 
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 **/
	@RequestMapping(method = { RequestMethod.PATCH }, value = "/{id}")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "id", value = "Object Id", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> patch(@ApiIgnore RequestObject request, @PathVariable("id") String idParam, @RequestBody String jsonString) throws JsonParseException, JsonMappingException, IOException {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		Document existingDocument = CommonUtil.findOneObject(collection, idParam);
		Document doc = Document.parse(jsonString);
		if (existingDocument != null) {
			// Merge both the document, override existing properties & add newly added properties
			doc = doPatch(collection, existingDocument, doc);
		}
		else {
			insertNew(collection, idParam, doc);
		}

		return new ResponseEntity<>(doc, HttpStatus.OK);
	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/bulksaveupdate")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> bulkFindAndModifyElseCreate(@ApiIgnore RequestObject request, @RequestBody String jsonString) throws JsonParseException, JsonMappingException, IOException {

		final MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());

		final List<Document> documents = new ArrayList<Document>();

		JsonParser jsonParser = new JsonParser();
		JsonArray jsonArray;
		try {
			jsonArray = (JsonArray) jsonParser.parse(jsonString);
		}
		catch (Exception e1) {
			jsonArray = new JsonArray();
			jsonArray.add(jsonParser.parse(jsonString));
			LOGGER.debug("CollectionObjectController.bulkFindAndModifyElseCreate() " + e1.getMessage());
		}
		Iterator<JsonElement> iterator = jsonArray.iterator();
		iterator.forEachRemaining(new Consumer<JsonElement>() {
			@Override
			public void accept(JsonElement jsonElement) {

				Document doc = Document.parse(jsonElement.getAsJsonObject().toString());
				String _id = doc.get(CommonString.ID) != null ? doc.get(CommonString.ID).toString() : null;
				if (_id != null) {
					Document existingDocument = CommonUtil.findOneObject(collection, _id);
					if (existingDocument != null) {
						// Merge both the document, override existing properties & add newly added properties
						try {
							doc = doPatch(collection, existingDocument, doc);
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
					else {
						insertNew(collection, _id, doc);
					}
				}
				else {
					String objectId = CommonUtil.getId();
					insertNew(collection, objectId.toString(), doc);
				}

				documents.add(doc);
			}

		});
		return new ResponseEntity<>(documents, HttpStatus.OK);
	}

	public void insertNew(MongoCollection<Document> collection, String objectId, Document doc) {
		doc.append(CommonString.ID, objectId);
		if (!doc.containsKey(CommonString.CREATED_ON)) {
			doc.append(CommonString.CREATED_ON, System.currentTimeMillis());
		}
		if (!doc.containsKey(CommonString.CREATED_BY)) {
			doc.append(CommonString.CREATED_BY, objectId);
		}
		if (!doc.containsKey(CommonString.ACCESS_LEVEL)) {
			doc.append(CommonString.ACCESS_LEVEL, CommonString.AccessLevel.DEFAULT.value());
		}
		doc.append(CommonString.UPDATED_ON, System.currentTimeMillis());
		doc.append(CommonString.UPDATED_BY, objectId);

		collection.insertOne(doc);
	}

	private Document doPatch(MongoCollection<Document> collection, Document existingDocument, Document docFromUI) throws JsonParseException, JsonMappingException, IOException {
		Document mergedDoc = merge(existingDocument, docFromUI);
		mergedDoc.put(CommonString.ID, existingDocument.get(CommonString.ID).toString());

		mergedDoc.replace(CommonString.UPDATED_ON, System.currentTimeMillis());
		mergedDoc.replace(CommonString.UPDATED_BY, mergedDoc.get(CommonString.ID));

		collection.findOneAndReplace(existingDocument, mergedDoc);

		return mergedDoc;
	}

	public long doDelete(MongoCollection<Document> collection, String idParam) {
		BasicDBObject query = new BasicDBObject();
		query.put(CommonString.ID, idParam);

		DeleteResult deleteResult = collection.deleteOne(query);
		return deleteResult.getDeletedCount();
	}

	@SuppressWarnings("unchecked")
	private Document merge(Document existingDocument, Document docFromUI) throws JsonParseException, JsonMappingException, IOException {
		String mergedJson = "";
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map1 = mapper.readValue(existingDocument.toJson(), Map.class);
		Map<String, Object> map2 = mapper.readValue(docFromUI.toJson(), Map.class);
		Map<String, Object> merged = new HashMap<String, Object>(map1);
		merged.putAll(map2);
		mergedJson = mapper.writeValueAsString(merged);

		Document mergedDoc = Document.parse(mergedJson);
		return mergedDoc;
	}

	public String insertNew(String dbName, String collectionName, Document doc) {
		MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
		String objectId = CommonUtil.getId();
		insertNew(collection, objectId, doc);
		return objectId;
	}

	public void update(String dbName, String collectionName, Document existingDocument, Document updatedDocument) throws JsonParseException, JsonMappingException, IOException {
		MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
		doPatch(collection, existingDocument, updatedDocument);
	}

	private Document filterFields(Document document, Document loggedinUser) {
		
		String[] filterFields = new String[] { "password", "email", "phone", "kundli",
				"mobile_no", "social_url", "alternateEmail", "address", "zodiac_sign", "relative_surnames",
				"guardian_name", "familyOriginCity", "maternalUncle" 
				/*, "dob", "birth_time", "birth_place", "birth_time_obj", "state", "district", "city", "middle_name", */};

		Document activeMemberships = userController.getActiveMemberShip(loggedinUser);
		boolean filter = false;

		if (loggedinUser.getString(CommonString.ID).equals(document.getString(CommonString.ID))) {
			filter = false;
		}
		else if (activeMemberships == null) {
			filter = true;
		}
		else {
			List<String> viewdProfile = userController.getProfilesViewdDuringMembership(activeMemberships);
			if (viewdProfile.contains(document.getString(CommonString.ID))) {
				filter = false;
			}
			else {
				filter = true;
			}
		}

		if (filter) {
			String middle_name = document.getString("middle_name");
			if (middle_name != null && !middle_name.isEmpty())
				document.replace("middle_name", middle_name.charAt(0));

			String first_name = document.getString("first_name");
			if (first_name != null && !first_name.isEmpty())
				document.replace("first_name", first_name.charAt(0));

			for (String field : filterFields)
				document.replace(field, "--");
		}
		return document;
	}

}
