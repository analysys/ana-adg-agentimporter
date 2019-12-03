package cn.com.analysys.agentimpoter.handler;

import cn.com.analysys.agentimpoter.util.ConstantTool;
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
		try {
			return new MessageSender(ConstantTool.SERVER_URL, null, lineTxt).send();
		} catch (Exception e) {
			LoggerUtil.error(e.getMessage(), e);
		}
		return null;
	}
}