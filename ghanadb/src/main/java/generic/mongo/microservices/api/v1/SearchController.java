/**
 * 
 */
package generic.mongo.microservices.api.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;
import generic.mongo.microservices.model.Condition;
import generic.mongo.microservices.model.Query;
import generic.mongo.microservices.model.RequestObject;
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
public class SearchController {
	
	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	@Resource
	private MongoClient mongoClient;
	
	@Resource
	private CollectionObjectController collectionObjectController;

	private ExecutorService executor = Executors.newFixedThreadPool(10);
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/search")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public synchronized ResponseEntity<?> search(
			@ApiIgnore RequestObject request,
			@RequestParam(required = false, value = "likesearch") Boolean likesearch,
			@RequestBody Query query) {

		final List<Document> result = searchByQuery(request, likesearch, query);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * @param request
	 * @param likesearch
	 * @param query
	 * @return
	 */
	private List<Document> searchByQuery(RequestObject request, Boolean likesearch, Query query) {
		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());

		final List<Document> result = new ArrayList<>();
		FindIterable<Document> iterable;

		BasicDBObject joinQuery = new BasicDBObject();
		List<BasicDBObject> orConditions = new ArrayList<BasicDBObject>();
		List<BasicDBObject> andConditions = new ArrayList<BasicDBObject>();

		List<Condition> conditions = query.getCondition();
		for (Condition condition : conditions) {
			BasicDBObject aCondition = new BasicDBObject();
			BasicDBList valuesToSearch = new BasicDBList();
			for (String value : condition.getValues()) {
				//docIds.add(Integer.parseInt(value));
				if (likesearch != null && likesearch) {
					Pattern regex = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
					valuesToSearch.add(regex);
				}
				else {
					valuesToSearch.add(value);
				}
			}

			DBObject inClause = new BasicDBObject("$in", valuesToSearch);
			aCondition.put(condition.getSearchpath(), inClause);

			if (condition.getIsOr())
				orConditions.add(aCondition);
			else
				andConditions.add(aCondition);
		}

		if (!orConditions.isEmpty())
			joinQuery.put("$or", orConditions);
		if (!andConditions.isEmpty())
			joinQuery.put("$and", andConditions);

		LOGGER.debug("Query = " + joinQuery);

		if (joinQuery.isEmpty())
			iterable = collection.find().sort(new BasicDBObject(query.getSortOn(), query.isSortAscending() ? 1 : -1));
		else
			iterable = collection.find(joinQuery).sort(new BasicDBObject(query.getSortOn(), query.isSortAscending() ? 1 : -1	));

		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});
		return result;
	}

	@RequestMapping(method = { RequestMethod.GET }, value = "/searchwords")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public synchronized ResponseEntity<?> search(
			@ApiIgnore RequestObject request,
			@RequestParam("keywords") List<String> keywords,
			@RequestParam(required = false, value = "likesearch") Boolean likesearch,
			@RequestParam(required = false, value = "searchFields") List<String> searchPaths) {

		MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
		FindIterable<Document> iterable;

		StringBuilder stringBuilder = new StringBuilder();
		for (String keyword : keywords) {
			stringBuilder.append(keyword + " ");
		}

		String keywordString = stringBuilder.toString();

		BasicDBObject query = new BasicDBObject();
		BasicDBObject joinQuery = new BasicDBObject();
		List<BasicDBObject> orConditions = new ArrayList<BasicDBObject>();

		if (searchPaths != null && !searchPaths.isEmpty()) {
			BasicDBObject aCondition = new BasicDBObject();
			BasicDBList valuesToSearch = new BasicDBList();
			for (String searchField : searchPaths) {
				//docIds.add(Integer.parseInt(value));
				for (String keyword : keywords) {
					if (likesearch != null && likesearch) {
						valuesToSearch.add(Pattern.compile(keyword, Pattern.CASE_INSENSITIVE));
					}
					else {
						valuesToSearch.add(keyword);
					}
				}

				DBObject inClause = new BasicDBObject("$in", valuesToSearch);
				aCondition.put(searchField, inClause);
				orConditions.add(aCondition);
			}
		}

		if (searchPaths != null && !searchPaths.isEmpty()) {
			joinQuery.put("$or", orConditions);
			query = joinQuery;
		}
		else {
			BasicDBObject search;
			if (likesearch != null && likesearch) {
				search = new BasicDBObject("$text", new BasicDBObject("$search", Pattern.compile(keywordString, Pattern.CASE_INSENSITIVE)));
			}
			else {
				search = new BasicDBObject("$text", new BasicDBObject("$search", keywordString));
			}
			query = search;
		}

		LOGGER.debug("Query = " + query);

		final List<Document> result = new ArrayList<>();
		if (!query.isEmpty()) {
			iterable = collection.find(query);
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					result.add(document);
				}
			});

		}
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@RequestMapping(method = { RequestMethod.POST }, value = "/searchAndDelete")
	@ApiImplicitParams({ @ApiImplicitParam(name = "db", value = "Database Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "collection", value = "Collection Name", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "user", value = "User ID", required = true, dataType = "string", paramType = "path", defaultValue = ""),
			@ApiImplicitParam(name = "X-API-Key", value = "API Key", required = true, dataType = "string", paramType = "header", defaultValue = "ghana")
	})
	public synchronized ResponseEntity<?> searchAndDelete(
			@ApiIgnore RequestObject request,
			@RequestParam(required = false, value = "likesearch") Boolean likesearch,
			@RequestBody Query query) {

		Future<Long> future = executor.submit(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				MongoCollection<Document> collection = mongoClient.getDatabase(request.getDbName()).getCollection(request.getCollectionName());
				Long deleteCount = 0l;
				final List<Document> result = searchByQuery(request, likesearch, query);
				for (Document document : result) {
					deleteCount += collectionObjectController.doDelete(collection, document.get(CommonString.ID).toString());
				}
				return deleteCount;
			}
		});
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public Document findOne(String dbName, String collectionName, Document doc) {
		MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
		final List<Document> result = new ArrayList<>();
		FindIterable<Document> iterable = collection.find(doc);
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});

		return !result.isEmpty() ? result.get(0) : null;
	}
	
	public List<Document> find(String dbName, String collectionName, Document doc) {
		MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(collectionName);
		final List<Document> result = new ArrayList<>();
		FindIterable<Document> iterable = collection.find(doc);
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});

		return !result.isEmpty() ? result : null;
	}
}
