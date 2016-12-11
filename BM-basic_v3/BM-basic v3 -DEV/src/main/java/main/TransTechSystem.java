package main;

import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.*;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import bm.*;
import main.configuration.Configuration;
import main.objects.DB.DBTableConfig;
import mqtt.MQTTHandler;
import tools.*;

@SpringBootApplication
@ImportResource("spring-config.xml")
public class TransTechSystem {
	private static final Logger logger = Logger.getLogger(TransTechSystem.class);
	private static ApplicationContext system;
	private boolean started = false; //true if BM has already started
	private static TransTechSystem tts;
	@Autowired
	public static Configuration config;
	@Autowired
	private TrafficController tc;
	@Autowired
	private MQTTHandler mh;
	@Autowired
	private ComponentRepository cr;
	@Autowired
	private CIREngine cireng;
	@Autowired
	private OpenhabHandlingEngine ohe;
	
    public static void main(String[] args) {
    	System.setProperty("spring.devtools.restart.enabled", "false");
    	System.setProperty("server.port", "8182");
    	tts = new TransTechSystem();
    	
    	ApplicationContext configXML = new ClassPathXmlApplicationContext("system-config.xml");
    	config = (Configuration) configXML.getBean("sysconfig");
    	((ConfigurableApplicationContext)configXML).close();
    	
    	system = SpringApplication.run(TransTechSystem.class, args);
    	tts.tc = ((TrafficController) system.getBean("DerbyTrafficController"));
    	tts.mh = ((MQTTHandler) system.getBean("BM-MQTTHandler"));
    	tts.cireng = ((CIREngine) system.getBean("CIRInterpreter"));
    	tts.cr = (ComponentRepository) system.getBean("ComponentRepository");
    	tts.ohe = (OpenhabHandlingEngine) system.getBean("OpenhabEngine");
    	
    	tts.handleArgs(args);
    	
    	if(!tts.start()) {
    		logger.fatal("Cannot start BusinessMachine!");
        	tts.exit();
    	}
    }
    
    /**
     * Starts the server system.
     * 
     */
    public boolean start() {
    	boolean successful = true;
    	logger.info("Starting BusinessMachine...");
    	
    	//setting up MQTTHandler
    	logger.info("Setting up MQTTHandler...");
    	try {
			mh.start(config.getConnectionConfig().getMqttURL());
			mh.subscribe(config.getMqttTopicConfig().getBMTopic(), 1);
			mh.subscribe(config.getMqttTopicConfig().getAdminTopic(), 1);
		} catch (MqttException e) {
			logger.fatal("Cannot setup MQTTHandler!");
			logger.trace(e);
			successful = false;
		}
    	
    	//setting up TrafficController
    	logger.info("Setting up TrafficController...");
    	try {
			tc.createConnection(config.getConnectionConfig().getDbURL(), config.getConnectionConfig().getDbUser(), 
					config.getConnectionConfig().getDbPass());
		} catch (SQLException e) {
			logger.fatal("Cannot setup " + tc.getName() + "!");
			logger.trace(e);
			successful = false;
		}
    	
    	//populates DBTableConfigs for relevant info needed by the TransactionEngine of this BusinessMachine
    	logger.info("Populating SQL Table configurations...");
    	HashMap<String, DBTableConfig> populatedConfigs = new HashMap<String, DBTableConfig>(1);
    	Object[] tables = config.getDatabaseConfig().getDbTableConfigs().values().toArray();
    	for(int i = 0; i < tables.length; i++) {
    		DBTableConfig table = (DBTableConfig) tables[i];
    		table.populateConfig(tc);
    		populatedConfigs.put(table.getObjectName().toLowerCase(), table);
    	}
    	TransTechSystem.config.getDatabaseConfig().setDbTableConfigs(populatedConfigs);
    	logger.info("Population finished!");
    	
    	//setting up ComponentRepository
    	logger.info("Setting up ComponentRepository...");
    	cr.update();
    	//cr.updateDatabase();
    	logger.info("ComponentRepository setup successful!");
    	
    	//setting up CIREngine
    	logger.info("Setting up CIREngine...");
    	cireng.update();
    	logger.info("CIREngine setup successful!");
    	
    	//setup OpenHab files
    	logger.info("Updating OpenHab...");
    	try {
			ohe.updateOpenhabRecords(cr.getAllRooms(), cr.getAllComponents());
			logger.info("OpenHab updated!");
		} catch (SQLException | MqttException e) {
			logger.fatal("Cannot start OpenhabHandlingEngine!", e);
			System.exit(0);
		}
    	
    	if(successful) {
    		logger.info("BusinessMachine started!");
    		started = true;
    	}
    	else logger.fatal("BusinessMachine failed to start!");
    	
    	return successful;
    }
    
    /**
     * Shuts down existing server system
     */
    public void exit() {
    	logger.info("Shutting down system...");
    	if(started) {
    		logger.info("Shutting down... performing shutdown procedures");
        	//disconnecting to MQTTBroker
        	try {
        		logger.info("Exitting MQTTClient...");
    			mh.disconnect();
    		} catch (MqttException e) {
    			logger.warn("Cannot disconnect MQTTClient!");
    			logger.trace(e);
    		}
        	
        	//disconnecting to Derby
        	try {
        		logger.info("Exitting TrafficController...");
    			tc.closeConnection();
    		} catch (SQLException e) {
    			logger.warn("Cannot disconnect TrafficController!");
    			logger.trace(e);
    		}
    	}
    	SpringApplication.exit(system, new ExitCodeGenerator());
    	logger.info("System shutdown complete!");
    	System.exit(0);
    }
    
    public static void forceShutdown() {
    	logger.info("Forcing BusinessMachine shutdown...");
    	tts.exit();
    }
    
    public void printArgs(String[] args) {
		for(int i = 0; i < args.length; i++) {
			logger.info(args[i]);
		}
	}
	
	public void handleArgs(String[] args) {
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(arg.equals("setup")) {
				logger.info("Setting up BusinessMachine for this system...");
				try {
					tc.createConnection("jdbc:derby://localhost:1527/DB", "APP", "APP");
					
					//this part is reached when a connection is successful
					logger.info("BusinessMachine is already set up! Cancelling setup procedure.");
				} catch (SQLException e) {
					logger.info("Creating new database for Derby...");
					try {
						tc.createConnection("jdbc:derby://localhost:1527/DB;create=true", "APP", "APP");
						logger.info("Database created! Setting up database...");
						logger.debug("Creating tables...");
						tc.executeQuery("CREATE TABLE APP.COMPONENTS ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "TOPIC VARCHAR(40) NOT NULL,"
								+ "MAC VARCHAR(20) NOT NULL,"
								+ "FUNCTN VARCHAR(10) NOT NULL,"
								+ "NAME VARCHAR(50) NOT NULL,"
								+ "ROOM VARCHAR(10) NOT NULL,"
								+ "PRIMARY KEY (SSID))");
						tc.executeQuery("CREATE TABLE APP.ROOMS ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "NAME VARCHAR(50) NOT NULL,"
								+ "PRIMARY KEY (SSID))");
						tc.executeQuery("CREATE TABLE APP.COMCAT ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "NAME VARCHAR(50) NOT NULL,"
								+ "DESCRIPTION VARCHAR(140) NOT NULL,"
								+ "PRIMARY KEY (SSID))");
						tc.executeQuery("CREATE TABLE APP.PROPCAT ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "NAME VARCHAR(50) NOT NULL,"
								+ "DESCRIPTION VARCHAR(140) NOT NULL,"
								+ "MODE VARCHAR(2) NOT NULL,"
								+ "TYPE VARCHAR(10),"
								+ "PRIMARY KEY (SSID))");
						tc.executeQuery("CREATE TABLE APP.PVALCAT ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "NAME VARCHAR(25) NOT NULL,"
								+ "MINIM INTEGER DEFAULT 0 NOT NULL,"
								+ "MAXIM INTEGER NOT NULL,"
								+ "DESCRIPTION VARCHAR(140),"
								+ "OH_ITEM VARCHAR(25) DEFAULT 'switch' NOT NULL,"
								+ "PRIMARY KEY (SSID))");
						tc.executeQuery("CREATE TABLE APP.COMPROPLIST ("
								+ "COM_TYPE VARCHAR(10) NOT NULL,"
								+ "PROP_TYPE VARCHAR(10) NOT NULL,"
								+ "QTY INTEGER NOT NULL)");
						tc.executeQuery("CREATE TABLE APP.COMP_PROPERTIES ("
								+ "SSID VARCHAR(10) NOT NULL,"
								+ "COM_ID VARCHAR(10) NOT NULL,"
								+ "PROP_NAME VARCHAR(25) NOT NULL,"
								+ "PROP_VALUE VARCHAR(50) DEFAULT '0' NOT NULL,"
								+ "PRIMARY KEY (SSID))");
						
						logger.debug("Creating table relationships...");
						tc.executeQuery("ALTER TABLE APP.COMPONENTS "
								+ "ADD FOREIGN KEY (FUNCTN) "
								+ "REFERENCES APP.COMCAT (SSID)");
						tc.executeQuery("ALTER TABLE APP.COMPONENTS "
								+ "ADD FOREIGN KEY (ROOM) "
								+ "REFERENCES APP.ROOMS (SSID)");
						tc.executeQuery("ALTER TABLE APP.COMPROPLIST "
								+ "ADD FOREIGN KEY (COM_TYPE) "
								+ "REFERENCES APP.COMCAT (SSID)");
						tc.executeQuery("ALTER TABLE APP.COMP_PROPERTIES "
								+ "ADD FOREIGN KEY (COM_ID) "
								+ "REFERENCES APP.COMPONENTS (SSID)");
						tc.executeQuery("ALTER TABLE APP.PROPCAT "
								+ "ADD FOREIGN KEY (TYPE) "
								+ "REFERENCES APP.PVALCAT (SSID)");
						
						logger.debug("Populating tables...");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0000', 'system', 'Function of all system-embedded components')");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0005', 'MediaPlayer', 'Plays Videos')");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0003', 'ZenSocket', 'An ESP-controlled switch.')");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0001', 'ZenLight', 'Light Bulb with daylight white')");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0002', 'ZenLightRGB', 'Light Bulb with daylight white and RGB')");
						tc.executeQuery("INSERT INTO APP.COMCAT(SSID, NAME, DESCRIPTION) "
								+ "VALUES ('0004', 'ZenTemp2', "
								+ "'Temperature Monitoring Device with 1 Analog Input and 1 Analog Output')");
						tc.executeQuery("INSERT INTO APP.COMPROPLIST(COM_TYPE, PROP_TYPE, QTY) "
								+ "VALUES ('0001', '0002', 1)");
						tc.executeQuery("INSERT INTO APP.COMPROPLIST(COM_TYPE, PROP_TYPE, QTY) "
								+ "VALUES ('0002', '0004', 4)");
						tc.executeQuery("INSERT INTO APP.COMPROPLIST(COM_TYPE, PROP_TYPE, QTY) "
								+ "VALUES ('0004', '0003', 1)");
						tc.executeQuery("INSERT INTO APP.COMPROPLIST(COM_TYPE, PROP_TYPE, QTY) "
								+ "VALUES ('0004', '0004', 1)");
						tc.executeQuery("INSERT INTO APP.COMPROPLIST(COM_TYPE, PROP_TYPE, QTY) "
								+ "VALUES ('0003', '0002', 1)");
						tc.executeQuery("INSERT INTO APP.PVALCAT(SSID, NAME, MINIM, MAXIM, DESCRIPTION, OH_ITEM) "
								+ "VALUES ('D', 'Digital', 0, 1, 'able to recognize on/off functions only', 'switch')");
						tc.executeQuery("INSERT INTO APP.PVALCAT(SSID, NAME, MINIM, MAXIM, DESCRIPTION, OH_ITEM) "
								+ "VALUES ('A1', 'Analog', 0, 1023, "
								+ "'able to recognize 0-1023 values (has an in/decrement of 93)', 'rollershutter')");
						tc.executeQuery("INSERT INTO APP.PVALCAT(SSID, NAME, MINIM, MAXIM, DESCRIPTION, OH_ITEM) "
								+ "VALUES ('A2', 'Percentile', 0, 100, "
								+ "'able to recognize 0-100 values', 'rollershutter')");
						tc.executeQuery("INSERT INTO APP.PROPCAT(SSID, NAME, DESCRIPTION, MODE, TYPE) "
								+ "VALUES ('0001', 'DigitalInput', "
								+ "'Accepts Digital Inputs, used for sensors', 'I', 'D')");
						tc.executeQuery("INSERT INTO APP.PROPCAT(SSID, NAME, DESCRIPTION, MODE, TYPE) "
								+ "VALUES ('0002', 'DigitalOutput', "
								+ "'Sends digital commands, used for controllers', 'O', 'D')");
						tc.executeQuery("INSERT INTO APP.PROPCAT(SSID, NAME, DESCRIPTION, MODE, TYPE) "
								+ "VALUES ('0003', 'AnalogInput', "
								+ "'Accepts Analog Inputs, used for sensors', 'I', 'A1')");
						tc.executeQuery("INSERT INTO APP.PROPCAT(SSID, NAME, DESCRIPTION, MODE, TYPE) "
								+ "VALUES ('0004', 'AnalogOutput', "
								+ "'Sends analog commands, used for controllers', 'O', 'A1')");
						
						tc.closeConnection();
						logger.info("BusinessMachine setup complete! Starting BusinessMachine...");
					} catch (SQLException e1) {
						logger.fatal("Cannot configure Derby!", e1);
						System.exit(0);
					}
				}
				System.out.println("\n");
				break;
			}
		}
	}
}