/**
 * 
 */
package generic.mongo.microservices.api.v1;

import javax.annotation.Resource;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoClient;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

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
public class ConstraintsController {

	@Resource
	MongoClient mongoClient;

	@RequestMapping(method = { RequestMethod.POST }, value = "/{field}/unique")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> unique(
			@ApiIgnore RequestObject request,
			@PathVariable("field") String field) {
		Document index = new Document(field, 1);
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		collection.createIndex(index, new IndexOptions().unique(true));
		return listIndexes(request);
	}

	@RequestMapping(method = { RequestMethod.POST }, value = "/listindexes")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> listIndexes(@ApiIgnore RequestObject request) {
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		ListIndexesIterable<Document> result = collection.listIndexes();
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
