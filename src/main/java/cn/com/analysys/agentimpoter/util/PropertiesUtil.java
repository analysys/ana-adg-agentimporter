package cn.com.analysys.agentimpoter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
	private static Properties properties = null;
	public static final String CONFIGURE = "conf.properties";
	
    static {
		properties = new Properties();
        try {
        	InputStream inner = PropertiesUtil.class.getClassLoader().getResourceAsStream(CONFIGURE);
            properties.load(inner);
        	String outpath = System.getProperty("user.dir") + File.separator + "../conf" + File.separator;
        	if(outpath != null && new File(outpath).exists()){
        		InputStream out = new FileInputStream(new File(outpath.concat(CONFIGURE)));
                properties.load(out);
        	}
        } catch (Exception e) {
        	LoggerUtil.error(e.getMessage(), e);
        }
	}

    public static String getString(String key) {
        return getProperties().getProperty(key);
    }

    public static boolean getBoolean(String key) {
        String value = getProperties().getProperty(key);
        if(value == null || value.trim().length() == 0)
        	return false;
        boolean result = false;
        try {
            result = Boolean.valueOf(value.trim());
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    public static String getString(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }
    
	public static Properties getProperties() {
		return properties;
	}
    
}
