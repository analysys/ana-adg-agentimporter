package cn.com.analysys.agentimpoter.handler;

import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import cn.com.analysys.agentimpoter.util.ConstantTool;
import cn.com.analysys.agentimpoter.util.LoggerUtil;

public class MessageSender {
	private final CloseableHttpClient httpclient;
	private final Boolean isEncode;
	private final String serverUrl;
	private final Map<String, String> egHeaderParams;
	private final String jsonData;
    
	public MessageSender(String serverUrl, Map<String, String> egHeaderParams, String jsonData){
		this(serverUrl, egHeaderParams, jsonData, true);
	}
	
	public MessageSender(String serverUrl, Map<String, String> egHeaderParams, String jsonData, Boolean isEncode){
		String keyw = ConstantTool.UP;
		if(serverUrl.contains(keyw)){
			serverUrl = serverUrl.substring(0, serverUrl.indexOf(keyw));
		}
		this.serverUrl = serverUrl.concat(keyw);
		this.egHeaderParams = egHeaderParams;
		this.jsonData = jsonData;
		this.isEncode = isEncode;
		this.httpclient = getHttpClient();
	}
	
	public String send() throws Exception {
		CloseableHttpResponse response = null;
		try {
			HttpPost egHttpPost = new HttpPost(this.serverUrl);
			if (this.egHeaderParams != null) {
				for (Map.Entry<String, String> entry : this.egHeaderParams.entrySet()) {
					egHttpPost.addHeader(entry.getKey(), entry.getValue());
				}
			}
			StringEntity egRequest = null;
			if(isEncode){
				egRequest = new StringEntity(AnalysysEncoder.encode(AnalysysEncoder.compress(jsonData)));
			} else {
				egRequest = new StringEntity(jsonData);
				egRequest.setContentType("application/json");
			}
			egRequest.setContentEncoding("UTF-8");
	        egHttpPost.setEntity(egRequest);
	        egHttpPost.setConfig(getHttpConfig());
	        response = httpclient.execute(egHttpPost);
			int httpStatusCode = response.getStatusLine().getStatusCode();
			int minCode = 200;
			int maxCode = 300;
			String message = EntityUtils.toString(response.getEntity(), "utf-8");
			try {
				message = AnalysysEncoder.uncompress(AnalysysEncoder.decode(message));
			} catch (Exception e) {}
			printLog(message, jsonData);
			if (httpStatusCode >= minCode && httpStatusCode < maxCode) {
				if(message != null && message.contains("\"code\":200")){
					return message;
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if(response != null)
				response.close();
		}
		return null;
	}
	
	private void printLog(String message, String jsonData) {
		if(message != null && !message.contains("\"code\":200")){
			LoggerUtil.error("Data Upload Fail: " + jsonData);
		}
    }
	
    private CloseableHttpClient getHttpClient() {
    	return SingletonClassInstance.getHttpClient();
    }
    
    private RequestConfig getHttpConfig() {
    	RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(20000).setConnectTimeout(20000).build();
    	return requestConfig;
    }
    
    static class SingletonClassInstance {
		private static CloseableHttpClient httpClient = HttpClients.createDefault();
		
		public static CloseableHttpClient getHttpClient(){
			return httpClient;
		}
	}
}
