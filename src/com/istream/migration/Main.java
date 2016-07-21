package com.istream.migration;

import java.io.FileInputStream;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

public class Main {

	static Logger logger = Logger.getLogger(Main.class);
	static Properties prop = new Properties();
	static HttpClient httpClient = new DefaultHttpClient();
	public static String methodName;
	public static String fileName;
	public static String destination;

	public static void main(String[] args) throws Exception {
		try {

			prop.load(new FileInputStream("Migrate.properties"));
			if (args.length > 0) {
				methodName = args[0];
				fileName = args[1];
			}
			httpClient = wrapClient(httpClient);
			
			switch (methodName.trim()) {
			case TransactionsConstants.ACTIVATION:
				
				Activation activation = new Activation(prop,httpClient);
				activation.readActivation(fileName, ',');
				break;

			case TransactionsConstants.ORDERS:
				
				BookOrder bookOrder = new BookOrder(prop,httpClient);
				bookOrder.readOrderActvation(fileName, ',');
				break;
				
			case TransactionsConstants.DEVICE:
				
				OrderDevice orderDevice = new OrderDevice(prop,httpClient);
				orderDevice.readOrderActvation(fileName, ',');
				break;
          case TransactionsConstants.USAGECHARGE:
				
				UsageCharge charges = new UsageCharge(prop,httpClient);
				charges.readOrderActvation(fileName, ',');
				break;	
				

			default:
				logger.error("Invalid Method Name");
				System.exit(0);
				break;
			}

		} catch (Exception exception) {
			System.out.println("failure : throwing " + exception.getClass().getSimpleName());
		}
	}
	public static HttpClient wrapClient(HttpClient base) {

		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				@SuppressWarnings("unused")
				public void checkClientTrusted(X509Certificate[] xcs,String string) throws CertificateException {
				}

				@SuppressWarnings("unused")
				public void checkServerTrusted(X509Certificate[] xcs,String string) throws CertificateException {
				}

				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {

				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
		    sr.register(new Scheme("https", ssf,443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			return null;
		}
	}
}
