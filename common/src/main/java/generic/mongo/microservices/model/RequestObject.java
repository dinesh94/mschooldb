/**
 * 
 */
package generic.mongo.microservices.model;

import java.io.Serializable;

import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Dinesh
 *
 */
@ApiIgnore
public class RequestObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1777809864327383001L;

	private String dbName;

	private String collectionName;

	private String apiKey;

	private Boolean async;

	private String user;

	private String admin;

	public RequestObject(String dbName, String collectionName, String apiKey, Boolean async) {
		this.dbName = dbName;
		this.collectionName = collectionName;
		this.apiKey = apiKey;
		this.async = async;
	}

	public RequestObject(String dbName, String collectionName, String apiKey, Boolean async, String user, String admin) {
		super();
		this.dbName = dbName;
		this.collectionName = collectionName;
		this.apiKey = apiKey;
		this.async = async;
		this.user = user;
		this.admin = admin;
	}

	public String getAdmin() {
		return admin;
	}

	public void setAdmin(String admin) {
		this.admin = admin;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Boolean getAsync() {
		return async;
	}

	public void setAsync(Boolean async) {
		this.async = async;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
}