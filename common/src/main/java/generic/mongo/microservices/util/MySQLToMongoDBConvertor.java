package generic.mongo.microservices.util;

public class MySQLToMongoDBConvertor {/*
	
	static Map<String, List<String>> masterData = new HashMap<String, List<String>>();
	
	static String[] masterTableList = {"master_day", "master_month", "master_profile_created_for", "master_marital_status",
	                        "master_aged_from", "master_religion", "master_mother_tongue", "master_cast", "master_year", "master_height",
	                        "master_blood_group", "master_zodiac_sign", "master_gothra", "master_nakshatra", "master_education", "master_employment_sector",
	                        "master_employment_area", "master_occupation", "master_annual_income", "master_income_in", "master_hobbies", "master_music",
	                        "master_sports", "master_books", "master_spoken_languages", "master_fathers_name", "mother_name"
	 };
	
	static Map<String, String> findReplace = new HashMap<String, String>();
	{
		findReplace.put("zodiac_sign", "zodiacSign");
	}

	private static void loadMasterData() {
		MongoCredential credential = MongoCredential.createCredential("kandapohe", "kandapohe", "m0ng0_k@nd@p0he".toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress("ec2-35-160-105-209.us-west-2.compute.amazonaws.com", 26101), Arrays.asList(credential));
		
		for (String string : masterTableList) {
			MongoCollection<Document> collection = mongoClient.getDatabase("kandapohe").getCollection(string);
			FindIterable<Document> iterable;

			iterable = collection.find();

			final List<Document> result = new ArrayList<>();
			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					result.add(document);
				}
			});
			
//			masterData.put(masterTableList, result);
		}
		
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		String jsonData = null;
		int count = 0;
		
		loadMasterData();

		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/kp", "root", "root");

		Statement smt = con.createStatement();
		ResultSet rs = smt.executeQuery("SELECT * FROM profiles,users WHERE profiles.user_id = users.id"); // SELECT * FROM users WHERE id not in (select user_id from profiles)

		jsonData = getJSONFromResultSet(rs, "user");
		FileWriter file = new FileWriter("d:\\teamsig\\ProfileRecords.json");
		file.write(jsonData.toString());
	}

	

	public static String getJSONFromResultSet(ResultSet rs, String keyName) {
		Map json = new HashMap();
		List list = new ArrayList();
		if (rs != null) {
			try {
				ResultSetMetaData metadata = rs.getMetaData();
				while (rs.next()) {
					Map<String, Object> columnMap = new HashMap<String, Object>();
					for (int columnIndex = 1; columnIndex <= metadata.getColumnCount(); columnIndex++) {
						if (rs.getString(metadata.getColumnName(columnIndex)) != null) {
							String columnName = metadata.getColumnLabel(columnIndex);
							
							switch (columnName) {
							case "education":
								
								break;

							default:
								break;
							}
							
							if(findReplace.containsKey(columnName)){
								columnName = findReplace.get(metadata.getColumnLabel(columnIndex));
							}
							columnMap.put(columnName, rs.getString(metadata.getColumnName(columnIndex)));
						}
						else {
							columnMap.put(metadata.getColumnLabel(columnIndex), "");
						}
						list.add(columnMap);
					}
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			json.put(keyName, list);
		}
		return JSONValue.toJSONString(json);
	}

*/}
