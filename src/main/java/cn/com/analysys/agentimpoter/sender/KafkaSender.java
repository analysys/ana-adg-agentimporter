package cn.com.analysys.agentimpoter.sender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.com.analysys.agentimpoter.util.ConstantTool;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class KafkaSender implements Sender{
    
	@Override
	public String send(String msg) throws Exception {
		List<Map<String, String>> splitMapList = splitJsons(msg);
		for(Map<String, String> jsonMap : splitMapList){
			String xwhat = jsonMap.get("xwhat");
			boolean isProfile = sendToProfile(xwhat);
			String topicName = getTopicName(xwhat, jsonMap.get("appid"));
			KeyedMessage<byte[], byte[]> message = new KeyedMessage<byte[], byte[]>(topicName, (isProfile ? xwhat.getBytes() : "".getBytes()), msg.getBytes());
			getProducer().send(message);
		}
		return null;
	}
	
	private List<Map<String, String>> splitJsons(String fullJson){
		List<Map<String, String>> dataMapList = new ArrayList<Map<String, String>>();
		JSONArray jArray = JSON.parseArray(fullJson);
		if(jArray != null && jArray.size() >= 1){
			for(int i=0, len=jArray.size();i<len; i++){
				Map<String, String> map = new HashMap<String, String>();
				JSONObject jObject = null;
				try {
					jObject = jArray.getJSONObject(i);
				} catch (Exception e) {
					continue;
				}
				map.put("xwhat", jObject.getString("xwhat"));
				map.put("appid", jObject.getString("appid"));
				JSONArray jnArray = new JSONArray();
				jnArray.add(jObject);
				map.put("json", JSON.toJSONString(jnArray, SerializerFeature.WriteMapNullValue));
				dataMapList.add(map);
			}
		}
		return dataMapList;
	}
	
	private String getTopicName(String xWhat, String appid) {
		if(xWhat != null && sendToProfile(xWhat)){
			return ConstantTool.PROFILE_TOPIC_NAME.concat("_").concat(appid == null ? "" : appid);
		} else {
			return ConstantTool.EVENT_TOPIC_NAME.concat("_").concat(appid == null ? "" : appid);
		}
	}
	
	private boolean sendToProfile(String xwhat){
		if(xwhat == null)
			return false;
		if(ConstantTool.PROFILE.contains(",")){
			for(String pf : ConstantTool.PROFILE.split(",")){
				if(xwhat.toLowerCase().equals(pf)){
					return true;
				}
			}
		} else {
			return xwhat.toLowerCase().startsWith(ConstantTool.PROFILE);
		}
		return false;
	}
	
    private Producer<byte[], byte[]> getProducer() {
    	return SingletonClassInstance.getProducer();
    }
    
    static class SingletonClassInstance {
    	private static Object obj = new Object();
    	private static Producer<byte[], byte[]> producer = null;
    	
		public static Producer<byte[], byte[]> getProducer(){
			if(producer == null){
				synchronized (obj) {
					if(producer == null){
						Properties pro = new Properties();
						pro.put("metadata.broker.list", "127.0.0.1:9092");
						pro.put("serializer.class", "kafka.serializer.DefaultEncoder");
						pro.put("partitioner.class", "cn.com.analysys.agentimpoter.sender.AnaKafkaPartation");
						pro.put("request.required.acks", "1");
						pro.put("compression.codec", "2");
						pro.put("request.timeout.ms", "10000");
						pro.put("topic.metadata.refresh.interval.ms", "-1");
						pro.put("queue.buffering.max.ms", "10000");
						pro.put("queue.buffering.max.messages", "20000");
						pro.put("producer.type", "async");
						pro.put("batch.num.messages", "2000");
						pro.put("queue.enqueue.timeout.ms", "-1");
						ProducerConfig config = new ProducerConfig(pro);
						producer = new Producer<byte[], byte[]>(config);
					}
				}
			}
			return producer;
		}
	}
    
	@Override
	public void close() {
		if(getProducer() != null)
			getProducer().close();
	}
}
