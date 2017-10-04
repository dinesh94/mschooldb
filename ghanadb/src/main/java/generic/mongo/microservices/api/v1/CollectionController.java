/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import generic.mongo.microservices.model.RequestObject;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Dinesh
 *
 */
@RestController
@RequestMapping("api/v1/dbs/{db}/{collection}")
public class CollectionController {

	private Map<String, List<Document>> cashedData = new HashMap<>();
	
	@Resource
	MongoClient mongoClient;

	/** OPERATES ON COLLECTION **/
	@RequestMapping(method = { RequestMethod.POST }, value = "/clearCashedData")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> clearCashedData(@ApiIgnore RequestObject request) {
		cashedData.clear();
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	/** OPERATES ON COLLECTION **/
	@RequestMapping(method = { RequestMethod.POST }, value = "/create")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> create(@ApiIgnore RequestObject request) {
		MongoDatabase mongoDatabase = mongoClient.getDatabase(request.getDbName());
		mongoDatabase.createCollection(request.getCollectionName());
		MongoCollection<Document> collection = mongoDatabase.getCollection(request.getCollectionName());
		collection.createIndex(new Document("$**", "text"));// This is required for full test search

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/update")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> update(@ApiIgnore RequestObject request) {
		// mongoClient.getDatabase(dbName).runCommand(collectionName);
		return new ResponseEntity<>("API NOT IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED);
	}

	// For security let's avoid deleting collection directly
	
	/*@RequestMapping(method = { RequestMethod.DELETE }, value = "/delete")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> delete(@ApiIgnore RequestObject request) {
		mongoClient.getDatabase(request.getDbName()).createCollection(request.getCollectionName());
		return new ResponseEntity<>(HttpStatus.OK);
	}*/

	@RequestMapping(method = { RequestMethod.GET }, value = "/view")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> view(@ApiIgnore RequestObject request,
			@RequestParam(required = false, value = "cache") Boolean cache) {

		List<Document> result = cashedData.get(request.getCollectionName());
		
		if(cache != null && cache == true && result != null && !result.isEmpty()){
			return new ResponseEntity<>(result, HttpStatus.OK);
		}else{
			final List<Document> resultfinal = new ArrayList<>();
			MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
			FindIterable<Document> iterable;
			
			iterable = collection.find();
			
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					resultfinal.add(document);
				}
			});
			
			result = resultfinal;
			if(cache != null && cache == true){
				cashedData.put(request.getCollectionName(), result);
			}
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/projection")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> viewProjection(@ApiIgnore RequestObject request,
			@RequestParam("keys") List<String> keys) {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		FindIterable<Document> iterable;

		BasicDBObject projection = new BasicDBObject("_id", 1);
		for (String key : keys) {
			projection.append(key, 1);
		}
		iterable = collection.find().projection(projection);

		final List<Document> result = new ArrayList<>();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});

		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping(method = { RequestMethod.GET }, value = "/recordcount")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> recordcount(@ApiIgnore RequestObject request) {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		return new ResponseEntity<>(collection.count(new Document("verified", true)), HttpStatus.OK);
	}
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/recordcount")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> recordcountCondition(
			@ApiIgnore RequestObject request,
			@RequestBody String jsonString) {
		
		Document condition = Document.parse(jsonString);
		
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		return new ResponseEntity<>(collection.count(condition), HttpStatus.OK);
	}

}
