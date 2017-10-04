package generic.mongo.microservices.config;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import generic.mongo.microservices.model.RequestObject;
import generic.mongo.microservices.util.LogUtils;

@Component
public class GhanaHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private static final Logger LOGGER = LogUtils.loggerForThisClass();
	
	/*@Resource
	CollectionObjectController collectionObjectController;

	@Resource
	SearchController searchController;*/

	@Override
	public boolean supportsParameter(MethodParameter methodParameter) {
		return methodParameter.getParameterType().equals(RequestObject.class);
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
		//String apiKey = nativeWebRequest.getHeader("X-API-Key");
		String apiKey = "";
		if (apiKey != null) {
			/**
			 * We need to create database bases on apiKey provided to user
			 * ghanadb - system specific collection
			 * api_registry will be in ghanadb
			 * api_registry will have mapping of apiKey & it's unique id
			 * We will append unique id to the db name so that that database will be unique for each user :)	
			 */
			String apiId = "";//checkIsApiKeyExist(apiKey);
			/*
			 * if (!API_KEY_LIST.contains(apiKey)) { throw new InvalidApiKeyException(); }
			 */
			HttpServletRequest httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
			Map<String, String> map = (Map<String, String>) httpServletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			String db = map.get("db") + apiId;
			String collection = map.get("collection");
			
			Map<String, String> headerParamMap = getHeadersInfo(httpServletRequest);
			String user = headerParamMap.get("user");
			String admin = headerParamMap.get("admin");
			
			Boolean async = false;
			if(map.get("async") != null && map.get("async").equalsIgnoreCase("true"))
				async = true;

			/*
			 * String bar = nativeWebRequest.getParameter("db"); String foo =
			 * nativeWebRequest.getParameter("collection");
			 */
			return new RequestObject(db, collection, apiKey, async, user, admin);
		}
		else {
			return WebArgumentResolver.UNRESOLVED;
		}
	}

	private Map<String, String> getHeadersInfo(HttpServletRequest request) {

		Map<String, String> map = new HashMap<String, String>();

		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			map.put(key, value);
		}

		return map;
	  }
	
	/*private String checkIsApiKeyExist(String apiKey) {
		String apiId;
		Document doc = new Document();
		doc.put("apiKey", apiKey);
		doc = searchController.findOne("ghanadb", "api_registry", doc);

		if (doc != null) {
			apiId = doc.getString("apiId");
		}
		else {
			apiId = collectionObjectController.insertNew("ghanadb", "api_registry", doc);
		}
		return apiId;
	}*/
}
