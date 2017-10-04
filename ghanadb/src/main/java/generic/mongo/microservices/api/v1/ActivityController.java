package generic.mongo.microservices.api.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.CommonUtil;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class ActivityController {

	@Resource
	MongoClient mongoClient;

	@Resource
	private CollectionObjectController collectionObjectController;

	private ExecutorService executor = Executors.newFixedThreadPool(10);
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/activity")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana"),
			@ApiImplicitParam(name = "user_id", required = true, dataType = "string"),
			@ApiImplicitParam(name = "to_user_id", required = true, dataType = "string"),
			@ApiImplicitParam(name = "email", required = true, dataType = "string")
	})
	public synchronized ResponseEntity<?> like(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) {
		
		
		Document requestBody = Document.parse(jsonString);
		String user_id = requestBody.getString("user_id");
		String to_user_id = requestBody.getString("to_user_id");
		Boolean email = requestBody.getBoolean("email");
		
		if(user_id == null || to_user_id == null){
			return new ResponseEntity<>("user_id == "+user_id+" & to_user_id = "+to_user_id, HttpStatus.BAD_REQUEST);
		}
		
		Future<Long> future = executor.submit(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
				
				BasicDBObject andQuery = new BasicDBObject();
				List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
				obj.add(new BasicDBObject("user_id", user_id));
				obj.add(new BasicDBObject("to_user_id", to_user_id));
				andQuery.put("$and", obj);

				final List<Document> result = new ArrayList<>();
				FindIterable<Document> iterable = collection.find(andQuery);
				iterable.forEach(new Block<Document>() {
					@Override
					public void apply(final Document document) {
						result.add(document);
					}
				});
				
				String objectId;
				if(result.isEmpty()){ // Not likes
					objectId = CommonUtil.getId()+"_"+request.getCollectionName();
					Document doc = new Document();
					doc.append("user_id", user_id);
					doc.append("to_user_id", to_user_id);
					doc.append("utcepoch", System.currentTimeMillis());
					collectionObjectController.insertNew(collection, objectId.toString(), doc);
					
					sendNotification(request.getDbName(), objectId, user_id, to_user_id, email, request.getCollectionName());
				}else{
					// Already like so dislike
					for (Document document : iterable) {
						objectId = document.get(CommonString.ID).toString();
						collectionObjectController.doDelete(collection, objectId);
						removeNotification(request.getDbName(), objectId);
					}
				}
				return null;
			}
		});
		return new ResponseEntity<>(HttpStatus.OK);
	}
 

	private void removeNotification(String db, String objectId) {
		MongoCollection<Document> collection = mongoClient.getDatabase(db).getCollection("notifications");
		collectionObjectController.doDelete(collection, objectId);
	}

	
	private void sendNotification(String db, String objectId, String user_id, String to_user_id, Boolean email, String task) {
		MongoCollection<Document> collection = mongoClient.getDatabase(db).getCollection("notifications");
		
		Document doc = new Document();
		doc.append("from_user_id", user_id);
		doc.append("user_id", to_user_id);
		doc.append("email", email);
		doc.append("task", task);
		doc.append("utcepoch", System.currentTimeMillis());
		collectionObjectController.insertNew(collection, objectId, doc);
	}
}
