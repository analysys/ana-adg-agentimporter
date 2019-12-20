package cn.com.analysys.agentimpoter.handler;

import java.util.LinkedList;

import cn.com.analysys.agentimpoter.sender.Sender;

public class HandlerChain {
	private static final LinkedList<Handler> handlers = new LinkedList<Handler>();
	private static final LinkedList<Sender> senders = new LinkedList<Sender>();
	
	public synchronized static void addLastHandler(Handler handler){
		handlers.add(handler);
	}
	
	public synchronized static void addLastSender(Sender sender){
		senders.add(sender);
	}
	
	public static Handler poll(){
		return handlers.poll();
	}

	public static LinkedList<Handler> getHandlers() {
		return handlers;
	}

	public static LinkedList<Sender> getSenders() {
		return senders;
	}
}