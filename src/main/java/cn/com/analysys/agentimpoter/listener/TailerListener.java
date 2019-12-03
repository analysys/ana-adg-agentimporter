package cn.com.analysys.agentimpoter.listener;

public interface TailerListener {
	
    void init(Tailer tailer);

    void fileNotFound(String fileName);

    void fileRotated(String fileName, long position);

    void handle(String line);

    void handle(Exception ex);
    
}
