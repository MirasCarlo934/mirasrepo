package main;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

import mqtt.MQTTHandler;

public class BusinessMachine {
	private String coreConfig = "cfg/core-config.xml";
	public static ApplicationContext context;
	private static final Logger LOG = Logger.getLogger("BM_LOG.main");
	//private static final Logger logger = Logger.getLogger(BusinessMachine.class);
	
	public BusinessMachine() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		BusinessMachine bm = new BusinessMachine();
		try {
			bm.startBM();
			//SpringApplication.run(BusinessMachine.class, args);
			MQTTHandler mh = (MQTTHandler) context.getBean("MQTTHandler");
			mh.connectToMQTT();
		} catch (Exception e) {
			LOG.info("BM threw exception");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	 /**
     * This is the method used to start the Business Machine.
     * It reads the other config files and loads their context.
     * 
     * @throws Exception
     */
    public void startBM() throws Exception {
    	LOG.info("Starting the BM...");
        // Read the core configuration file, this is where the other config files are configured
   		ApplicationContext ctx = new ClassPathXmlApplicationContext(coreConfig);
   		
        //get the other configuration files
        String[] cfgFiles = ((ConfigLoader) ctx.getBean("config")).getConfig();
    	
        // Read the configuration file
        context = new ClassPathXmlApplicationContext(cfgFiles);
        LOG.info("Config file " + coreConfig +" loaded.");
        
        LOG.info("BusinessMachine started!");
    }
}