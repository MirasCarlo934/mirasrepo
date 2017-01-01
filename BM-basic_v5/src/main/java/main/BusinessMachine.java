package main;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

import mqtt.MQTTHandler;
import tools.FileHandler;

public class BusinessMachine {
	private String coreConfig = "cfg/core-config.xml";
	public static ApplicationContext context;
	private static final Logger LOG = Logger.getLogger("BM_LOG.main");
	private static String bm_props_file = "/home/pi/test/BM/properties/bm.properties";
	
	public BusinessMachine() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		//initialization phase
		LOG.info("Initializing BM...");
		boolean b = true;
		for(int i = 0; i < args.length; i++) {
			String[] s = args[i].split(":");
			if(s.length < 2 || s.length > 2) {
				LOG.fatal("Improper arguments!");
			} else {
				String arg = s[0];
				String val = s[1];
				if(arg.equals("cir.fileloc")) {
					try {
						FileHandler fh = new FileHandler(bm_props_file);
						Properties p = new Properties();
						p.load(fh.getFileReader());
						p.setProperty("cir.file_location", val);
						fh.saveProperties(p, null);
					} catch(IOException e) {
						LOG.fatal("Cannot load properties file!");
						e.printStackTrace();
						b = false;
					}
				} else if (arg.equals("bm.props.fileloc")) {
					try {
						bm_props_file = val;
						FileHandler fh = new FileHandler(val);
						Properties p = new Properties();
						p.load(fh.getFileReader());
						p.setProperty("bm.properties.filepath", val);
						fh.saveProperties(p, null);
					} catch(IOException e) {
						LOG.fatal("Cannot load properties file!");
						e.printStackTrace();
						b = false;
					}
				}
			}
		}
		
		//startup phase
		if(b) {
			BusinessMachine bm = new BusinessMachine();
			try {
				bm.setupSpring();
				//SpringApplication.run(BusinessMachine.class, args);
				MQTTHandler mh = (MQTTHandler) context.getBean("MQTTHandler");
				mh.connectToMQTT();
				LOG.info("BusinessMachine started!");
			} catch (Exception e) {
				LOG.info("BM threw exception");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		else {
			LOG.fatal("BM initialization process failed!");
		}
	}
	
	 /**
     * This is the method used to setup the Spring environment and object dependencies.
     * It reads the other config files and loads their context.
     * 
     * @throws Exception
     */
    public void setupSpring() throws Exception {
    	LOG.info("Starting the BM...");
        // Read the core configuration file, this is where the other config files are configured
   		ApplicationContext ctx = new ClassPathXmlApplicationContext(coreConfig);
   		
        //get the other configuration files
        String[] cfgFiles = ((ConfigLoader) ctx.getBean("config")).getConfig();
    	
        // Read the configuration file
        context = new ClassPathXmlApplicationContext(cfgFiles);
        LOG.info("Config file " + coreConfig +" loaded.");
    }
}