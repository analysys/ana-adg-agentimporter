package cn.com.analysys.agentimpoter.sender;

public interface Sender {
	
    public String send(String msg) throws Exception;
    
    public void close();
}
