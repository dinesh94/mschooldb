package generic.mongo.microservices.model;

import org.bson.Document;
import org.springframework.http.HttpStatus;

public class CreditResponse {

	private Document userData;

	private HttpStatus httpStatus;

	private Document activeMemberShip;

	private Boolean isCreditExpire = false;

	private Boolean isMembershipDateExpire = false;

	private Boolean isCreditUsedForProfile = false;
	
	public Boolean getIsCreditUsedForProfile() {
		return isCreditUsedForProfile;
	}

	public void setIsAlreadyUsedForProfile(Boolean isAlreadyUsedForProfile) {
		this.isCreditUsedForProfile = isAlreadyUsedForProfile;
	}

	public Boolean getIsCreditExpire() {
		return isCreditExpire;
	}

	public void setIsCreditExpire(Boolean isCreditExpire) {
		this.isCreditExpire = isCreditExpire;
	}

	public Boolean getIsMembershipDateExpire() {
		return isMembershipDateExpire;
	}

	public void setIsMembershipDateExpire(Boolean isMembershipDateExpire) {
		this.isMembershipDateExpire = isMembershipDateExpire;
	}

	public CreditResponse(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	public Document getUserData() {
		return userData;
	}

	public void setUserData(Document userData) {
		this.userData = userData;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	public Document getActiveMemberShip() {
		return activeMemberShip;
	}

	public void setActiveMemberShip(Document activeMemberShip) {
		this.activeMemberShip = activeMemberShip;
	}

}
