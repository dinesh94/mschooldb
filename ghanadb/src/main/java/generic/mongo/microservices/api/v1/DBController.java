/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCursor;
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
@RequestMapping("api/v1/dbs")
public class DBController {

	@Resource
	MongoClient mongoClient;

	@RequestMapping(method = { RequestMethod.GET })
	@ApiImplicitParams({
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> dbs() {
		List<String> dbs = new ArrayList<>();
		MongoCursor<String> dbsCursor = mongoClient.listDatabaseNames().iterator();
		while (dbsCursor.hasNext()) {
			dbs.add(dbsCursor.next());
		}

		return new ResponseEntity<>(dbs, HttpStatus.OK);
	}

	@RequestMapping(method = { RequestMethod.GET }, value = "/{db}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public ResponseEntity<?> db(@ApiIgnore RequestObject request) {
		MongoDatabase mongoDatabase = mongoClient.getDatabase(request.getDbName());
		List<String> collections = new ArrayList<>();
		for (String name : mongoDatabase.listCollectionNames()) {
			collections.add(name);
		}
		return new ResponseEntity<>(collections, HttpStatus.OK);
	}
}
