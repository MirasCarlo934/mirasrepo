package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

import main.engines.OHEngine;
import main.engines.requests.CIREngine.UpdateCIREReq;
import main.engines.requests.OHEngine.StartOHEReq;
import mqtt.MQTTHandler;
import tools.FileHandler;
import tools.IDGenerator;

public class BusinessMachine {
	private String coreConfig = "cfg/core-config.xml";
	public static ApplicationContext context;
	private static final Logger LOG = Logger.getLogger("BM_LOG.main");
	private static String bm_props_file = "src/main/resources/cfg/bm.properties";
	private static IDGenerator idg = new IDGenerator();
	
	public BusinessMachine() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		//initialization phase
		LOG.info("Initializing BM...");
		boolean b = true;
		Properties p = new Properties();
		FileHandler fh = null;
		try {
			fh = new FileHandler(bm_props_file);
			p.load(fh.getFileReader());
		} catch (FileNotFoundException e1) {
			LOG.warn("bm.properties file not found, filepath must be set in the args");
		} catch (IOException e) {
			LOG.warn("bm.properties file not found, filepath must be set in the args");
		}
		
		for(int i = 0; i < args.length; i++) {
			String[] s = args[i].split(":");
			if(s.length < 2 || s.length > 2) {
				LOG.error("Improper arguments!");
			} else {
				String arg = s[0];
				String val = s[1];
				if (arg.equals("bm.properties.filepath")) { //this MUST be specified first
					bm_props_file = val;
					try {
						fh = new FileHandler(bm_props_file);
						p.load(fh.getFileReader());
					} catch (FileNotFoundException e) {
						LOG.fatal("Cannot find bm.properties file in specified location! "
								+ "BM cannot start!");
						errorStartup();
					} catch (IOException e) {
						LOG.fatal("Cannot load bm.properties file!", e);
						e.printStackTrace();
					}
				}
				if(fh != null) { //to set the property
					p.setProperty(arg, val);
				}
				else {
					LOG.fatal("bm.properties file location not yet specified! "
							+ "BM cannot start!");
					errorStartup();
					break;
				}
			}
		}
		if(args.length > 0) fh.saveProperties(p, null);
		
		//startup phase
		if(b) {
			BusinessMachine bm = new BusinessMachine();
			try {
				bm.setupSpring();
				//SpringApplication.run(BusinessMachine.class, args);
				MQTTHandler mh = (MQTTHandler) context.getBean("MQTTHandler");
				mh.connectToMQTT();
				OHEngine ohe = (OHEngine) context.getBean("OHEngine");
				ohe.forwardRequest(new StartOHEReq(idg.generateMixedCharID(10)));
				ohe.forwardRequest(new UpdateCIREReq(idg.generateMixedCharID(10)));
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
	
	public static void errorStartup() {
		LOG.fatal("Cannot start BM! See error logs for more info.");
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
    
    /*
     * in setup phase
     * 
     * String arg = s[0];
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
	} else if (arg.equals("bm.os")) {
		try {
			FileHandler fh = new FileHandler(val);
			Properties p = new Properties();
			p.load(fh.getFileReader());
			p.setProperty("bm.os", val);
			fh.saveProperties(p, null);
		} catch(IOException e) {
			LOG.fatal("Cannot load properties file!");
			e.printStackTrace();
			b = false;
		}
	}*/
}