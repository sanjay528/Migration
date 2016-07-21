package com.istream.migration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import au.com.bytecode.opencsv.CSVReader;
import net.sf.json.JSONObject;

public class UsageCharge {
	
final static Logger logger = Logger.getLogger(UsageCharge.class);
	
	final static String locale = "en";
	final static String dateFormat = "dd MMMM yyyy";
	static HttpClient httpClient;
	private static String encodePassword;
	private static String tenantIdentifier;
	private static HttpPost usageChargesPostRequest  ;
	static String result;
	private static CSVReader reader;
	
	public UsageCharge(Properties prop, HttpClient client) {
		
		httpClient =client;
		tenantIdentifier = prop.getProperty("tenantIdentfier");
		String credentials = prop.getProperty("username").trim() + ":" +  prop.getProperty("password").trim();
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		usageChargesPostRequest = setRequiredPostUrl(prop.getProperty("usagerawUrl").trim());
		
	}
	
   private HttpPost setRequiredPostUrl(final String postUrl) {
		
	    HttpPost postRequest = new HttpPost(postUrl);
		postRequest.addHeader("Authorization", "Basic " + encodePassword);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("X-Obs-Platform-TenantId", tenantIdentifier);
		return postRequest;
	}
   
   static JSONObject usageJson = new JSONObject();

	public void readOrderActvation(String fileName, char delimiter) throws FileNotFoundException, IOException, JSONException  {

		
		reader = new CSVReader(new FileReader(fileName), delimiter);
		
		String[] currentLineData=reader.readNext();
		
		while((currentLineData = reader.readNext()) != null){
			
			 try {
				 usageJson.put("clientId", currentLineData[1]);
				 usageJson.put("number", currentLineData[2]);
				 usageJson.put("time", currentLineData[3]);
				 usageJson.put("destinationNumber",currentLineData[4]);
				 usageJson.put("destinationLocation", currentLineData[5]);
				 usageJson.put("duration", currentLineData[6]);
				 usageJson.put("cost", currentLineData[7]);
				 usageJson.put("locale", locale);
	            
				 usageChargesPostRequest.setEntity(new StringEntity(usageJson.toString()));
				 HttpResponse response = httpClient.execute(usageChargesPostRequest);
				 result = EntityUtils.toString(response.getEntity());
				 org.json.JSONObject obj = new org.json.JSONObject(result);
				
				 if (response.getStatusLine().getStatusCode() != 200) {
					
					 logger.error("CNO:" + currentLineData[0] + "  HTTP error code : "+ response.getStatusLine().getStatusCode()
							 + "Error Message :" + obj.getString("defaultUserMessage"));
					
				}else{
					
					logger.info("Order created with id : "+obj.getDouble("resourceId")+" For Client:"+ currentLineData[1] +"CNO "+currentLineData[0]);
				}
				
				 usageJson.clear();
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
            
	   }

		
	}
}
