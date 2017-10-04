package generic.mongo.microservices.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import generic.mongo.microservices.constant.CommonString;

public class DistrictToNativeDistrict {
	
	public static Document doPatch(MongoCollection<Document> collection, Document existingDocument, Document docFromUI) throws JsonParseException, JsonMappingException, IOException {
		Document mergedDoc = merge(existingDocument, docFromUI);
		mergedDoc.put(CommonString.ID, existingDocument.get(CommonString.ID).toString());

		mergedDoc.replace(CommonString.UPDATED_ON, System.currentTimeMillis());
		mergedDoc.replace(CommonString.UPDATED_BY, mergedDoc.get(CommonString.ID));

		collection.findOneAndReplace(existingDocument, mergedDoc);

		return mergedDoc;
	}
	
	@SuppressWarnings("unchecked")
	private static Document merge(Document existingDocument, Document docFromUI) throws JsonParseException, JsonMappingException, IOException {
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
	
	public static void main(String[] args) {

		MongoCredential credential = MongoCredential.createCredential("kandapohe", "kandapohe", "m0ng0_k@nd@p0he".toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress("ec2-35-160-105-209.us-west-2.compute.amazonaws.com", 26101), Arrays.asList(credential));

		FindIterable<Document> iterable;

		MongoCollection<Document> collection = mongoClient.getDatabase("kandapohe").getCollection("user");
		iterable = collection.find();

		final List<Document> result = new ArrayList<>();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				if (document.containsKey("district")) {
					String district = document.get("district").toString();
					document.put("familyOriginCity", district);

					try {
						doPatch(collection, document, document);
					}
					catch (IOException e) {
						e.printStackTrace();
					}

					collection.replaceOne(new Document("_id", document.get("_id")), document);
					//collection.findOneAndReplace(document, document);
				}
			}
		});

	}

}
