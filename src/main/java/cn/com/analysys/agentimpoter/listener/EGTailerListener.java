package cn.com.analysys.agentimpoter.listener;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.com.analysys.agentimpoter.handler.LogHandler;
import cn.com.analysys.agentimpoter.util.ConstantTool;
import cn.com.analysys.agentimpoter.util.LoggerUtil;
import cn.com.analysys.agentimpoter.util.ThreadPoolUtil;

public class EGTailerListener implements TailerListener{
	
	@Override
	public void init(Tailer tailer) {}

	@Override
	public void fileNotFound(String fileName) {
		LoggerUtil.warn(String.format("File: %s Not Exist", fileName));
	}

	@Override
	public void fileRotated(String fileName, long position) {}

	@Override
	public void handle(String line) {
		ThreadPoolUtil.counterAdd();
		if (line != null && line.trim().length() > 0) {
			try {
				new LogHandler(line).doChain().handle();
				if (ThreadPoolUtil.getCounter() % ConstantTool.logPrintLines == 0) 
					LoggerUtil.info(String.format("Total: %s Time: %s", ThreadPoolUtil.getCounter(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
			} catch (Exception e) {
				LoggerUtil.error(e.getMessage(), e);
			}
		}
	}
	
	@Override
	public void handle(Exception ex) {
		LoggerUtil.error(ex.getMessage(), ex);
	}
}
