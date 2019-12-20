package cn.com.analysys.agentimpoter.handler;

import cn.com.analysys.agentimpoter.sender.Sender;
import cn.com.analysys.agentimpoter.util.LoggerUtil;

public class LogHandler {
	private String lineTxt;
	
	public LogHandler(String lineTxt){
		this.lineTxt = lineTxt;
	}
	
	public LogHandler doChain() {
		for(Handler handler : HandlerChain.getHandlers()){
			lineTxt = handler.handle(lineTxt);
		}
		return this;
	}
	
	public String handle(){
		for(Sender sender : HandlerChain.getSenders()){
		try {
				sender.send(lineTxt);
		} catch (Exception e) {
			LoggerUtil.error(e.getMessage(), e);
		}
		}
		return null;
	}
}