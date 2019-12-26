package cn.com.analysys.agentimpoter.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class LoggerUtil {
	private static final String logPath;
	private static final String indexLogPath;
	private static Logger logger = Logger.getRootLogger();
	
	static {
		String tmpLogPath = PropertiesUtil.getString("ana.logfile.path", "");
		if(!tmpLogPath.endsWith(ConstantTool.FOLDER_SPLIT)){
			tmpLogPath = tmpLogPath.concat(ConstantTool.FOLDER_SPLIT);
		}
		logPath = tmpLogPath;
		
		String curDir = System.getProperty("user.dir", "");
		String logDir = ConstantTool.DEFAULT_LOG_DIR;
		if(curDir != null && curDir.trim().length() > 0){
			if(!curDir.endsWith(ConstantTool.FOLDER_SPLIT)){
				curDir = curDir.concat(ConstantTool.FOLDER_SPLIT);
			}
			logDir = curDir.concat("../logs");
		}
		String indexPath = PropertiesUtil.getString("ana.agent.log.path", logDir);
		if(!indexPath.endsWith(ConstantTool.FOLDER_SPLIT)){
			indexPath = indexPath.concat(ConstantTool.FOLDER_SPLIT);
		}
		indexLogPath = indexPath;
		if(!new File(indexLogPath).exists()){
			new File(indexLogPath).mkdirs();
		}
		FileAppender fileAppender = (FileAppender) logger.getAppender("importerAppender");
		fileAppender.addFilter(new Filter() {
			@Override
			public int decide(LoggingEvent event) {
				if(event.getLoggerName().startsWith("log_position_")){
					return Filter.DENY;
				}
				return Filter.NEUTRAL;
			}
		});
		fileAppender.setFile(indexLogPath.concat("import.log"));
		fileAppender.activateOptions();
	}
	
    public static Logger getPositionLog(String fileName, String appid) {
		Logger logger = Logger.getLogger("log_position_".concat(fileName).concat(appid==null?"":appid.trim()));
        logger.removeAllAppenders();
        logger.setLevel(Level.INFO);
        logger.setAdditivity(true);

        RollingFileAppender appender = new RollingFileAppender();
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern(ConstantTool.INDEX_PATTERN);
        appender.setLayout(layout);

        appender.setFile(getLogPositionIndexName(fileName, appid));
        appender.setMaxBackupIndex(0);
        appender.setMaxFileSize(ConstantTool.INDEX_SIZE);
        appender.setEncoding(ConstantTool.CODEC);
        appender.setAppend(true);
        appender.activateOptions();

        logger.addAppender(appender);
        return logger;
    }
    
    public static long getPosition(String fileName, String appid) {
    	long position = ConstantTool.TAILER_FILE_INDEX_POSITION;
    	String countFile = getLogPositionIndexName(fileName, appid);
		if(countFile != null && countFile.trim().length() > 0){
			File file = new File(countFile);
			if(file.exists()){
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(file));
					String tempString = null;
					while ((tempString = reader.readLine()) != null) {
						position = Long.valueOf(tempString);
					}
					reader.close();
				} catch (IOException e) {
					error(e.getMessage(), e);
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e1) {
						}
					}
				}
			}
		}
        return position;
    }
    
    /**
     * 获取索引位置文件名
     * @param fileName
     * @return
     */
    public static String getLogPositionIndexName(String fileName, String appid) {
    	if(appid != null && appid.trim().length() > 0){
    		return indexLogPath.concat(appid).concat(ConstantTool.FOLDER_SPLIT).concat(fileName).concat(ConstantTool.DEFAULT_LOG_INDEX_SUFFIX);
    	} else {
    		return indexLogPath.concat(fileName).concat(ConstantTool.DEFAULT_LOG_INDEX_SUFFIX);
    	}
    }
    
    public static String getMainLogName(String appid) {
    	File indexFolder = new File(getLogpath(appid));
    	File[] files = indexFolder.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				boolean matchNameFile = name.startsWith(ConstantTool.LOG_PREFIX);
				return new File(dir, name).isFile() && matchNameFile;
			}
		});
    	if(files != null && files.length > 0){
			sortFile(files);
			return files[0].getAbsolutePath();
		}
		return "";
    }
    
    /**
     * 删除索引位置文件
     * @param fileName
     * @return
     */
    public static void delLogPositionIndexFile(String fileName, String appid) {
    	String indexFile = getLogPositionIndexName(fileName, appid);
    	boolean ret = new File(indexFile).delete();
		if(!ret)
			warn(String.format("Index Position File %s delete err.", indexFile));
    }
    
    /**
     * 获取滚动后生成的文件名
     * @param fileName
     * @return
     */
    public static String getNextLogName(String fileName, String appid) {
    	String dateHour = fileName.substring(fileName.lastIndexOf(ConstantTool.DEFAULT_LOG_NAME_SPLIT) + 1, fileName.lastIndexOf("."));
    	File indexFolder = new File(getLogpath(appid));
		File[] files = indexFolder.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				if(!new File(dir, name).isFile())
					return false;
				boolean matchNameFile = name.startsWith(ConstantTool.LOG_PREFIX);
				if(!matchNameFile)
					return false;
				boolean nextTime = false;
				String fileHour = name.substring(name.lastIndexOf(ConstantTool.DEFAULT_LOG_NAME_SPLIT) + 1, name.lastIndexOf("."));
		    	nextTime = Long.valueOf(dateHour) < Long.valueOf(fileHour);
				return matchNameFile && nextTime;
			}
		});
		if(files != null && files.length > 0){
			sortFile(files);
			return files[0].getAbsolutePath();
		}
    	return "";
    }
    
    private static void sortFile(File[] files){
    	if(files.length > 1){
			try {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						long f1Date = Long.valueOf(f1.getName().substring(f1.getName().lastIndexOf(ConstantTool.DEFAULT_LOG_NAME_SPLIT) + 1, f1.getName().lastIndexOf(".")));
						long f2Date = Long.valueOf(f2.getName().substring(f2.getName().lastIndexOf(ConstantTool.DEFAULT_LOG_NAME_SPLIT) + 1, f2.getName().lastIndexOf(".")));
						return (int)(f1Date - f2Date);
					}
				});
			} catch (Exception e) {}
		}
    }
    
    public static String getLogpath(String appid) {
    	if(appid != null && appid.trim().length() > 0){
    		return logPath.concat(appid).concat(ConstantTool.FOLDER_SPLIT);
		}
		return logPath;
	}

	public static String getIndexlogpath(String appid) {
		if(appid != null && appid.trim().length() > 0){
    		return indexLogPath.concat(appid).concat(ConstantTool.FOLDER_SPLIT);
		}
		return indexLogPath;
	}
	
	public static void info(String message){
		logger.info(message);
	}
	
	public static void warn(String message){
		logger.warn(message);
	}
	
	public static void error(String message){
		logger.error(message);
	}
	
	public static void error(String message, Exception e){
		logger.error(message, e);
	}
}
