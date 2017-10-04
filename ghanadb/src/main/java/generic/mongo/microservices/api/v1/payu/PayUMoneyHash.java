package generic.mongo.microservices.api.v1.payu;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;

public class PayUMoneyHash {
	
	private Integer error;
	
	public Map<String, String> hashCalMethod(Map<String, Object> params)
            throws ServletException, IOException {

		/*String key = "rjQUPktU";
		String salt = "e5iIg1jwi8";
        String base_url = "https://test.payu.in";*/
		
		String key = "Yp3npp0X";
		String salt = "1MRE1l4aJs";
        String base_url = "https://secure.payu.in/_payment";
        		
        String action1 = "";
        error = 0;
        String hashString = "";
        
        Random rand = new Random();
        String rndm = Integer.toString(rand.nextInt()) + (System.currentTimeMillis() / 1000L);
        String hash = hashCal("SHA-512", hashString);
        
        String txnid = hashCal("SHA-256", rndm).substring(0, 20);
        params.put("txnid", txnid);
        txnid = hashCal("SHA-256", rndm).substring(0, 20);
        
        params.put("key", key);
        
        String otherPostParamSeq = "phone|surl|furl|lastname|curl|address1|address2|city|state|country|zipcode|pg";
        String hashSequence = "key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5|udf6|udf7|udf8|udf9|udf10";
        
        Map<String, String> urlParams = new HashMap<String, String>();
        
        String[] hashVarSeq = hashSequence.split("\\|");
        for (String part : hashVarSeq) {
            if (part.equals("txnid")) {
                hashString = hashString + txnid;
                urlParams.put("txnid", txnid);
            } else {
                hashString = (empty(params.get(part))) ? hashString.concat("") : hashString.concat(params.get(part).toString().trim());
                urlParams.put(part, empty(params.get(part)) ? "" : params.get(part).toString().trim());
            }
            hashString = hashString.concat("|");
        }
        hashString = hashString.concat(salt);
        action1 = base_url.concat("/_payment");
        hash = hashCal("SHA-512", hashString);
        String[] otherPostParamVarSeq = otherPostParamSeq.split("\\|");
        for (String part : otherPostParamVarSeq) {
            urlParams.put(part, empty(params.get(part)) ? "" : params.get(part).toString().trim());
        }

        urlParams.put("hash", hash);
        urlParams.put("action", action1);
        urlParams.put("hashString", hashString);
        return urlParams;
    }
	
	/**
	 * @param type
	 * @param str
	 * @return
	 */
	public String hashCal(String type, String str) {
        byte[] hashseq = str.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance(type);
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }

        } catch (NoSuchAlgorithmException nsae) {
        }
        return hexString.toString();
    }
	
	 public boolean empty(Object s) {
        if (s == null || s.toString().trim().equals("")) {
            return true;
        } else {
            return false;
        }
    }
}
