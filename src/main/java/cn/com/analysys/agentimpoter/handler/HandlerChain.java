package cn.com.analysys.agentimpoter.handler;

import java.util.LinkedList;

public class HandlerChain {
	
	private static final LinkedList<Handler> handlers = new LinkedList<Handler>();
	
	public synchronized static void addLast(Handler handler){
		handlers.add(handler);
	}
	
	public static Handler poll(){
		return handlers.poll();
	}

	public static LinkedList<Handler> getHandlers() {
		return handlers;
	}
}