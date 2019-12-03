package cn.com.analysys.agentimpoter.listener;

import java.io.File;
import java.io.FilenameFilter;

import cn.com.analysys.agentimpoter.handler.HandlerChain;
import cn.com.analysys.agentimpoter.handler.StandardJsonHandler;
import cn.com.analysys.agentimpoter.listener.EGTailerListener;
import cn.com.analysys.agentimpoter.util.ConstantTool;
import cn.com.analysys.agentimpoter.util.LoggerUtil;
import cn.com.analysys.agentimpoter.util.PropertiesUtil;
import cn.com.analysys.agentimpoter.util.ThreadPoolUtil;

public class TailerMain {
	
	public static void main(String[] args) {
		String dataPath = PropertiesUtil.getString("ana.logfile.path");
		String upUrl = PropertiesUtil.getString("ana.logfile.upurl", "");
		if(dataPath == null || dataPath.length() == 0 || upUrl == null || upUrl.trim().length() == 0){
			System.out.println("Params ana.logfile.path/ana.logfile.upurl Must Not Be Null.");
			System.exit(1);
		}
		if(!new File(dataPath).exists()){
			System.out.println(String.format("folder %s not exist.", dataPath));
			System.exit(1);
		}
		String appId = PropertiesUtil.getString("ana.logfile.appid");
		HandlerChain.addLast(new StandardJsonHandler());
		init();
		start(appId == null || appId.trim().length() == 0 ? "" : appId);
	}
	
	public static void init() {
		ThreadPoolUtil.executeTask(new Runnable() {
			@Override
			public void run() {
				boolean run = true;
				do{
					try {
						Thread.sleep(ConstantTool.waitTimeMill);
						LoggerUtil.info(ThreadPoolUtil.monitorThreadPool());
						run = true;
					} catch (Exception e) {
						LoggerUtil.error(e.getMessage(), e);
					}
				} while(run && !ThreadPoolUtil.isStop());
			}
		});
	}
	
	public static void start(String appid) {
		start(false, appid);
	}
	
	public static void start(boolean isHand, String appid) {
		File indexFolder = new File(LoggerUtil.getIndexlogpath(appid));
		File[] files = indexFolder.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(ConstantTool.LOG_PREFIX) && name.endsWith(ConstantTool.DEFAULT_LOG_INDEX_SUFFIX);
			}
		});
		if(files != null && files.length == 1){
			File logFile = new File(LoggerUtil.getLogpath(appid).concat(files[0].getName().substring(0, files[0].getName().length() - 6)));
			Tailer tailer = new Tailer(logFile, new EGTailerListener(), ConstantTool.TAILER_FILE_DELAY, true, appid);
			ThreadPoolUtil.executeTask(tailer);
		} else {
			String filePath = LoggerUtil.getMainLogName(appid);
			if(filePath != null && filePath.trim().length() > 0){
				Tailer tailer = new Tailer(new File(filePath), new EGTailerListener(), ConstantTool.TAILER_FILE_DELAY, false, appid);
				ThreadPoolUtil.executeTask(tailer);
			} else {
				ThreadPoolUtil.closeThreadPool();
				System.exit(0);
			}
		}
	}

}
