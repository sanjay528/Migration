package com.istream.migration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;



public class Activation {
	
	final static Logger logger = Logger.getLogger(Activation.class);
	
	final static String locale = "en";
	final static String dateFormat = "dd MMMM yyyy";
	final static Boolean isNewplan = true;
    static String adjustmentUrl;
	static HttpClient httpClient;
	private static String encodePassword;
	private static String tenantIdentifier;
	private static HttpPost activationPostRequest  ;
	private static HttpPost adjustmentPostRequest;
	static String result;
	private static CSVReader reader;
	
	public Activation(final Properties prop, final HttpClient client) {
		
		httpClient =client;
		tenantIdentifier = prop.getProperty("tenantIdentfier");
		String credentials = prop.getProperty("username").trim() + ":" +  prop.getProperty("password").trim();
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		activationPostRequest = setRequiredPostUrl((prop.getProperty("activationProcessUrl").trim()));
		adjustmentPostRequest = setRequiredPostUrl((prop.getProperty("adjustmenturl").trim()));
		adjustmentUrl =prop.getProperty("adjustmenturl").trim();
	}

	
	private HttpPost setRequiredPostUrl(final String postUrl) {
		
	    HttpPost postRequest = new HttpPost(postUrl);
		postRequest.addHeader("Authorization", "Basic " + encodePassword);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("X-Obs-Platform-TenantId", tenantIdentifier);
		return postRequest;
	}

	static JSONObject activation = new JSONObject();
	static JSONArray client = new JSONArray();
	static JSONArray sale = new JSONArray();
	static JSONArray allocate = new JSONArray();
	static JSONArray order = new JSONArray();
	static JSONArray serialNumber = new JSONArray();
	static JSONObject clientjson = new JSONObject();
	static JSONObject salejson = new JSONObject();
	static JSONObject allocatejson = new JSONObject();
	static JSONObject allocatejson1 = new JSONObject();
	static JSONObject orderjson = new JSONObject();
	static JSONObject adjustmentJson = new JSONObject();
	static SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
	static Date nextBilldate =null;
	static Date disconnectionDate = null;
	
	
	public synchronized void readActivation(final String fileName, char delimiter) throws FileNotFoundException, IOException {
		
		reader = new CSVReader(new FileReader(fileName), delimiter);
		
		String[] currentLineData=reader.readNext();
		
		while((currentLineData = reader.readNext()) != null){
		
			try{
				//int len = currentLineData.length;
				String officeId=currentLineData[4];
				clientjson.put("activationDate", currentLineData[1]);
				clientjson.put("firstname", currentLineData[2]);
				clientjson.put("lastname", currentLineData[3]);
				clientjson.put("officeId",Long.valueOf(officeId) );
				clientjson.put("clientCategory", currentLineData[5]);
				clientjson.put("active", currentLineData[6]);
				clientjson.put("addressNo", currentLineData[7]);//propertycode-enable
				clientjson.put("street", currentLineData[8]);//property street
				clientjson.put("city", currentLineData[9]);
				clientjson.put("state", currentLineData[10]);//property state
				clientjson.put("country", currentLineData[11]);//property country
				clientjson.put("zipCode", currentLineData[12]);//property zipcode
				clientjson.put("phone", currentLineData[13]);
				clientjson.put("email", currentLineData[14]);
				clientjson.put("locale", locale);
				clientjson.put("dateFormat", dateFormat);
				clientjson.put("flag","false");
				clientjson.put("entryType",currentLineData[15]);
				
				client.add(clientjson);
				clientjson.clear();
				
				salejson.put("dateFormat", dateFormat);
				salejson.put("locale", locale);
				salejson.put("itemId", currentLineData[16]);
				salejson.put("chargeCode", "OTC");
				salejson.put("saleDate", currentLineData[17]);
				salejson.put("unitPrice", currentLineData[18]);
			    salejson.put("totalPrice", currentLineData[19]);
				salejson.put("discountId", currentLineData[20]);
				salejson.put("saleType", currentLineData[22]);
				salejson.put("quantity", 1);
				salejson.put("officeId",Long.valueOf(officeId));
				
				allocatejson.put("itemMasterId", Integer.parseInt(currentLineData[16]));
				allocatejson.put("serialNumber", currentLineData[21]);
				allocatejson.put("status","allocated");
				allocatejson.put("isNewHw","Y");
				serialNumber.add(allocatejson);
				salejson.put("serialNumber",serialNumber);
		        
				sale.add(salejson);
				
				allocatejson1.put("itemMasterId",Integer.parseInt(currentLineData[16]));
				
				allocate.add(allocatejson1);
				allocatejson.clear();
				salejson.clear();
				allocatejson1.clear();
				
				orderjson.put("planCode", currentLineData[23]);
				orderjson.put("dateFormat", dateFormat);
				orderjson.put("locale", locale);
				orderjson.put("start_date", currentLineData[24]);
				orderjson.put("contractPeriod",currentLineData[25]);
				orderjson.put("paytermCode", currentLineData[26]);
				orderjson.put("isNewplan", isNewplan);
				orderjson.put("billAlign", currentLineData[27]);

	            Date startDate = formatter.parse(currentLineData[24]) ;
				
				if(!currentLineData[28].isEmpty()){
					nextBilldate = formatter.parse(currentLineData[28]) ;
					if(nextBilldate != null && disconnectionDate == null && (startDate.equals(nextBilldate) || startDate.before(nextBilldate))){
						orderjson.put("nextBillDate", currentLineData[28]);
					}
				}
				if(!currentLineData[29].isEmpty()){
					disconnectionDate = formatter.parse(currentLineData[29]) ;
					if(disconnectionDate != null && nextBilldate == null && startDate.before(disconnectionDate)){
						orderjson.put("disconnectionDate", currentLineData[29]);
					}
				}
				
				order.add(orderjson);
				orderjson.clear();
				
				activation.put("client",client);
				activation.put("sale",sale);
				activation.put("allocate",allocate);
				activation.put("bookorder", order);
				
				adjustmentJson.put("adjustment_type",currentLineData[30]);
				adjustmentJson.put("adjustment_code",currentLineData[31]);
				adjustmentJson.put("amount_paid",currentLineData[32]);
				adjustmentJson.put("locale",locale);
				adjustmentJson.put("dateFormat", dateFormat);
				adjustmentJson.put("adjustment_date",currentLineData[33]);
				sendingActivationPost(activation.toString(), adjustmentJson,currentLineData[0]);
				
				 //sendingActivationPost(activation.toString(),currentLineData[0]);
				
				activation.remove("client");
				activation.remove("sale");
				activation.remove("allocate");
				activation.remove("bookorder");
				client.clear();allocate.clear();
				sale.clear();order.clear();
				serialNumber.clear();
				//adjustmentJson.clear();
				
			}catch(Exception e){
				System.err.println(e.getMessage());
			}
			
		}//end of while loop	
	}//end of readActivation	


	// Customer activation and those balance adjustment done
	private static String sendingActivationPost(final String activationJsonData,final JSONObject adjustmentJsonData, String cno) {


		try {
			activationPostRequest.setEntity(new StringEntity(activationJsonData));// Replacing actvation Json
			HttpResponse response = httpClient.execute(activationPostRequest);
			result = EntityUtils.toString(response.getEntity());

			if (response.getStatusLine().getStatusCode() != 200) {
				org.json.JSONObject obj = new org.json.JSONObject(result);
				logger.error("CNO:" + cno + "  HTTP error code : "+ response.getStatusLine().getStatusCode()
						+ "Error Message :" + obj.getString("defaultUserMessage"));
			}
			else{
				
				String clientId = Util.getStringFromJson("resourceIdentifier",result);
				logger.info("Created clientId : " + clientId + " For CNO:"+ cno);
				
				if (clientId != null) {
					adjustmentPostRequest.setURI(new URI(adjustmentUrl + "/" + clientId));// append clientId here
					adjustmentPostRequest.setEntity(new StringEntity(adjustmentJsonData.toString()));// Replacing Json
					HttpResponse adjustmentResponse = httpClient.execute(adjustmentPostRequest);
					result = EntityUtils.toString(adjustmentResponse.getEntity());
					
					if (adjustmentResponse.getStatusLine().getStatusCode() != 200) {
						org.json.JSONObject obj = new org.json.JSONObject(result);
						logger.error("Adjustment not created for CNO:"+ cno+ "  HTTP error code : "+ adjustmentResponse.getStatusLine().getStatusCode() 
								+ "Error Message :"+ obj.getString("defaultUserMessage"));
					} else {
						logger.info("Adjustment was done for clientId : "+ clientId + " with amount "+ adjustmentJsonData.getString("amount_paid"));
					}
				}
			}
		} catch (Exception exception) {
			logger.error(exception.getMessage());
		}

		return result;
	}
}