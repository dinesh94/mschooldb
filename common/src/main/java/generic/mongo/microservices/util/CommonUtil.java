package generic.mongo.microservices.util;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class CommonUtil {

	public static String getId() {
		return ObjectId.get().toHexString();
	}

	public static Document findOneObject(MongoCollection<Document> collection, String objectId) {
		List<Document> result = findObjects(collection, objectId);
		if (result != null && !result.isEmpty())
			return result.get(0);
		return null;
	}

	public static List<Document> findObjects(MongoCollection<Document> collection, String objectId) {
		BasicDBObject query = new BasicDBObject();
		query.put("_id", objectId);
		FindIterable<Document> iterable = collection.find(query);

		final List<Document> result = new ArrayList<>();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				result.add(document);
			}
		});
		return result;
	}

	/**
	 * @param templateAsString
	 * @return
	 * @throws ParseException
	 */
	public static Template getTemplate(final String templateAsString) throws ParseException {
		final RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
		final StringReader reader = new StringReader(templateAsString);
		final SimpleNode node = runtimeServices.parse(reader, "Template name");
		final Template template = new Template();
		template.setRuntimeServices(runtimeServices);
		template.setData(node);
		template.initDocument();
		template.setEncoding("UTF-8");
		return template;

	}

	/**
	 * @param list
	 * @return
	 */
	public static Object convertToBasicDBObject(List<?> list) {
		List<BasicDBObject> basicDBObjects = new ArrayList<BasicDBObject>();
		for (Object object : list) {
			BasicDBObject basicDBObject = new BasicDBObject();
			basicDBObject.append("value", object.toString());
			basicDBObjects.add(basicDBObject);
		}

		return basicDBObjects;
	}

	/**
	 * @param long1
	 * @return
	 */
	public static String lognToDateFormattedDateString(Long long1) {
		String dateText = "";
		try {
			Date date = new Date(long1);
			SimpleDateFormat df2 = new SimpleDateFormat("dd-MMM-yyyy");
			dateText = df2.format(date);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return dateText;
	}
}
