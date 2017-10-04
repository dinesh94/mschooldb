package generic.mongo.microservices.util;

import org.apache.log4j.Logger;

public class LogUtils {

	public static Logger loggerForThisClass() {
		StackTraceElement myCaller = Thread.currentThread().getStackTrace()[2];
		return Logger.getLogger(myCaller.getClassName());
	}
}
