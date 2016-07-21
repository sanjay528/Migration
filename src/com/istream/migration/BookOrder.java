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
import net.sf.json.JSONObject;

public class BookOrder {
	
	
final static Logger logger = Logger.getLogger(Activation.class);
	
	final static String locale = "en";
	final static String dateFormat = "dd MMMM yyyy";
	final static Boolean isNewplan = true;
	
	private static CSVReader reader;
	private static HttpClient httpClient;
	private static String encodePassword;
	private static String tenantIdentifier;
	private static HttpPost orderPostRequest;
	private static String result;
	private static  String orderUrl;
	
	public BookOrder(final Properties prop, final HttpClient client) {
		
		httpClient = client;
		tenantIdentifier = prop.getProperty("tenantIdentfier");
		String credentials = prop.getProperty("username").trim() + ":" +  prop.getProperty("password").trim();
		encodePassword = new String(Base64.encodeBase64(credentials.getBytes()));
		orderPostRequest = setRequiredPostUrl(prop.getProperty("orderUrl").trim());
		orderUrl =prop.getProperty("orderUrl").trim();
	}

	
	private HttpPost setRequiredPostUrl(final String postUrl) {
		
	    HttpPost postRequest = new HttpPost(postUrl);
		postRequest.addHeader("Authorization", "Basic " + encodePassword);
		postRequest.addHeader("Content-Type", "application/json");
		postRequest.addHeader("X-Obs-Platform-TenantId", tenantIdentifier);
		
		return postRequest;
	}

	static JSONObject orderJson = new JSONObject();
	static SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
	static Date nextBilldate =null;
	static Date disconnectionDate = null;
	
	public synchronized void readOrderActvation(final String fileName, char delimiter) throws FileNotFoundException, IOException {
		
		reader = new CSVReader(new FileReader(fileName), delimiter);
		
		String[] currentLineData=reader.readNext();
		
		while((currentLineData = reader.readNext()) != null){
			
			try{
				orderJson.put("planCode", currentLineData[2]);
				orderJson.put("dateFormat", dateFormat);
				orderJson.put("locale", locale);
				orderJson.put("start_date", currentLineData[3]);
				orderJson.put("contractPeriod",currentLineData[4]);
				orderJson.put("paytermCode", currentLineData[5]);
				orderJson.put("isNewplan", isNewplan);
				orderJson.put("billAlign", currentLineData[6]);
	            Date startDate = formatter.parse(currentLineData[3]) ;
			
				if(!currentLineData[7].isEmpty()){
					nextBilldate = formatter.parse(currentLineData[7]) ;
					if(nextBilldate != null &&(startDate.equals(nextBilldate) || startDate.before(nextBilldate))){
						orderJson.put("nextBillDate", currentLineData[7]);
					}
				}
				if(!currentLineData[8].isEmpty()){
					disconnectionDate = formatter.parse(currentLineData[8]) ;
					if(disconnectionDate != null && startDate.before(disconnectionDate)){
						orderJson.put("disconnectionDate", currentLineData[8]);
					}
				}
				
				orderPostRequest.setURI(new URI(orderUrl+"/"+currentLineData[1]));
				orderPostRequest.setEntity(new StringEntity(orderJson.toString()));
				HttpResponse response = httpClient.execute(orderPostRequest);
				result = EntityUtils.toString(response.getEntity());
				
				if (response.getStatusLine().getStatusCode() != 200) {
					org.json.JSONObject obj = new org.json.JSONObject(result);
					logger.error("CNO:" + currentLineData[0] + "  HTTP error code : "+ response.getStatusLine().getStatusCode()
							+ "Error Message :" + obj.getString("defaultUserMessage"));
					
				}else{
					String orderId = Util.getStringFromJson("resourceIdentifier",result);
					logger.info("Order created with id : " + orderId + " For Client:"+ currentLineData[1] +"CNO "+currentLineData[0]);
				}
				
				orderJson.clear();
				
			}catch(Exception e){
				logger.error(e.getMessage());
			}
		}
		
	}

}
