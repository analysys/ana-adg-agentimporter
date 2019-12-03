package cn.com.analysys.agentimpoter.util;

import java.io.File;

public class ConstantTool {
	public static final int TAILER_FILE_DELAY = 2000;
	public static final long TAILER_FILE_INDEX_POSITION = -99L;
	public static final GeneralRule RULE;
	public static final String LOG_PREFIX;
	public static final String SERVER_URL;
	public static final String UP = "/up";
	public static final String CODEC = "UTF-8";
	public static final String DEFAULT_LOG_DIR = "/tmp/logs";
	public static final String INDEX_PATTERN = "%m%n";
	public static final String INDEX_SIZE = "3MB";
	public static final String DEFAULT_LOG_INDEX_SUFFIX = "_index";
	public static final String DEFAULT_LOG_NAME_SPLIT = "_";
	private static final String DEFAULT_LOG_PREFIX = "datas_";
	public static final long waitTimeMill = 300000;
	public static final String FOLDER_SPLIT = File.separator;
	
	static {
		String dataSplit = PropertiesUtil.getString("ana.logfile.splittype", "hour");
		if("day".equalsIgnoreCase(dataSplit)){
			RULE = GeneralRule.DAY;
		} else {
			RULE = GeneralRule.HOUR;
		}
		LOG_PREFIX = PropertiesUtil.getString("ana.logfile.prefix", DEFAULT_LOG_PREFIX);
		SERVER_URL = PropertiesUtil.getString("ana.logfile.upurl", "");
	}
	
}
