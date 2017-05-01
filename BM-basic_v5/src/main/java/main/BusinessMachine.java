package main;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import components.Component;
import components.properties.Property;
import mqtt.MQTTHandler;
import tools.*;

public class BusinessMachine {
	private String coreConfig = "cfg/core-config.xml";
	public static ApplicationContext context;
	private static final Logger LOG = Logger.getLogger("main.BusinessMachine");
	private static String bm_props_file = "configuration/bm.properties";
	private static IDGenerator idg = new IDGenerator();
	private static MQTTHandler mh;
	private static ComponentRepository cr;

	public static void main(String[] args) {
		//initialization phase
		LOG.info("Starting BM...");
		BusinessMachine bm = new BusinessMachine();
		try {
			//spring initialization
			final StartupManager sm = new StartupManager();
			bm.setupSpring();
			mh = (MQTTHandler) context.getBean("MQTTHandler");
			cr = (ComponentRepository) context.getBean("Components");
			
			//updates OH property states
			sm.updateOHPropertyStates();
			LOG.info("BusinessMachine started!");
		} catch (Exception e) {
			LOG.info("BM threw exception");
			// TODO Auto-generated catch block
			e.printStackTrace();
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
        // Read the core configuration file, this is where the other config files are configured
   		ApplicationContext ctx = new ClassPathXmlApplicationContext(coreConfig);
   		
        //get the other configuration files
        String[] cfgFiles = ((ConfigLoader) ctx.getBean("config")).getConfig();
    	
        // Read the configuration file
        context = new ClassPathXmlApplicationContext(cfgFiles);
        LOG.info("Config file " + coreConfig +" loaded.");
    }
    
    private final static class StartupManager {
    	public void updateOHPropertyStates() {
			//LOG.trace("UPDATE!");
			LOG.info("Updating OH items' states...");
			Component[] coms = cr.getAllComponents();
			for(int i = 0; i < coms.length; i++) {
				Component c = coms[i];
				Property[] props = c.getProperties().values().toArray(new Property[0]);
				for(int j = 0; j < props.length; j++) {
					Property prop = props[j];
					LOG.trace("Updating property " + prop.getSSID() + " in OH...");
					mh.publish("openhab/" + c.getTopic(), prop.getSSID() + "_" 
							+ prop.getValue());
				}
			}
			LOG.debug("Update complete!");
		}
    }
}