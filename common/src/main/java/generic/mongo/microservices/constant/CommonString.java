package generic.mongo.microservices.constant;

public class CommonString {
	public static final String ID = "_id";

	public static final String CREATED_ON = "_createdOn";

	public static final String CREATED_BY = "_createdBy";

	public static final String UPDATED_ON = "_updatedOn";

	public static final String UPDATED_BY = "_updatedBy";

	public static final String ACCESS_LEVEL = "_accessLevel";

	public static final String LOGINID = "email";

	public static final String EMAIL_VALIDATED = "emailValidated";

	public static final String EMAIL_VERIFICATION_TOKEN = "emailVerificationToken";

	public static final String PASSWORD = "password";

	public static final String CREATE_USER_CONFIRM_EMAIL_SUBJECT = "Verify Your Email Address";

	public static final String FORGOT_PASSWORD_SUBJECT = "Your password";

	public static final String DB = "db";

	public static final String COLLECTION = "collection";

	public static final String COLLECTION_EMAIL_TEMPLATES = "_email_templates";

	public static final String TEMPLATES_NAME = "templateName";

	public static final String EMAIL_LOGS = "_email_logs";

	public static final String JOB_NAME = "jobName";

	public static final String COLLECTION_USER = "user";

	public static final String USER_COLLECTION_PHONE = "phone";

	public static final String MEMBERSHIP_ALL = "all";

	public static final String MEMBERSHIP_FREE = "free";

	public static final String MEMBERSHIP_PREMIUM = "premium";

	public static enum AccessLevel {
		DEFAULT(1), SELF(2);
		private int accessLevel;

		AccessLevel(int accessLevel) {
			this.accessLevel = accessLevel;
		}

		public int value() {
			return accessLevel;
		};
	}
}
