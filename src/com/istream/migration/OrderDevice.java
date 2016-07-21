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

public class OrderDevice {
	
final static Logger logger = Logger.getLogger(Activation.class);
	
	final static String locale = "en";
	final static String dateFormat = "dd MMMM yyyy";
	final static Boolean isNewplan = true;
	
	private static CSVReader reader;
	private static HttpClient httpClient;
	private static String encodePassword;
	private static String tenantIdentifier;
	private static HttpPost devicePostRequest;
	private static String result;
	private static  String deviceurl;
	
	public OrderDevice(final Properties prop, final HttpClient client) {
		
		httpClient = client;
		tenantIdentifier = prop.getProperty("tenantIdentfier");
		String credentials = prop.getProperty("username").trim() + ":" +  prop.getProperty("password").trim();
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		devicePostRequest = setRequiredPostUrl(prop.getProperty("deviceUrl").trim());
		deviceurl =prop.getProperty("deviceUrl").trim();
	}

	
	private HttpPost setRequiredPostUrl(final String postUrl) {
		
	    HttpPost postRequest = new HttpPost(postUrl);
		postRequest.addHeader("Authorization", "Basic " + encodePassword);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("X-Obs-Platform-TenantId", tenantIdentifier);
		
		return postRequest;
	}

	static JSONObject salejson = new JSONObject();
	static JSONObject allocatejson = new JSONObject();
	static JSONArray sale = new JSONArray();
	static JSONArray serialNumber = new JSONArray();
	static JSONArray allocate = new JSONArray();
	static JSONObject allocatejson1 = new JSONObject();
	static SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
	static Date nextBilldate =null;
	static String chargeCode = "OTC";
	static Date disconnectionDate = null;
	static Long officeid = 1L; 
	
	public synchronized void readOrderActvation(final String fileName, char delimiter) throws FileNotFoundException, IOException {
		
		reader = new CSVReader(new FileReader(fileName), delimiter);
		
		String[] currentLineData=reader.readNext();
		
		while((currentLineData = reader.readNext()) != null){
			
			try{
				
				salejson.put("dateFormat", dateFormat);
				salejson.put("locale", locale);
				salejson.put("itemId", currentLineData[2]);
				salejson.put("chargeCode", "OTC");
				salejson.put("saleDate", currentLineData[3]);
				salejson.put("unitPrice", currentLineData[4]);
			    salejson.put("totalPrice", currentLineData[5]);
				salejson.put("discountId", currentLineData[6]);
				salejson.put("saleType", "NEWSALE");
				salejson.put("quantity", 1);
				salejson.put("officeId",Long.valueOf(officeid));
				
				allocatejson.put("itemMasterId", Integer.parseInt(currentLineData[2]));
				allocatejson.put("serialNumber", currentLineData[7]);
				allocatejson.put("status","allocated");
				allocatejson.put("isNewHw","Y");
				serialNumber.add(allocatejson);
				salejson.put("serialNumber",serialNumber);
		        
				sale.add(salejson);
				
				allocatejson1.put("itemMasterId",Integer.parseInt(currentLineData[1]));
				
				allocate.add(allocatejson1);
				
				devicePostRequest.setURI(new URI(deviceurl+"/"+currentLineData[1]+ "?devicesaleTpye=NEWSALE"));
				devicePostRequest.setEntity(new StringEntity(salejson.toString()));
				HttpResponse response = httpClient.execute(devicePostRequest);
				result = EntityUtils.toString(response.getEntity());
				
				if (response.getStatusLine().getStatusCode() != 200) {
					org.json.JSONObject obj = new org.json.JSONObject(result);
					logger.error("CNO:" + currentLineData[0] + "  HTTP error code : "+ response.getStatusLine().getStatusCode()
							+ "Error Message :" + obj.getString("defaultUserMessage"));
					
				}else{
					String orderId = Util.getStringFromJson("resourceIdentifier",result);
					logger.info("Order created with id : " + orderId + " For Client:"+ currentLineData[1] +"CNO "+currentLineData[0]);
				}
				
			}catch(Exception e){
				logger.error(e.getMessage());
			}
			sale.clear();allocate.clear();
			allocatejson.clear();
			allocatejson1.clear();
			serialNumber.clear();
		}
		
	}

}
