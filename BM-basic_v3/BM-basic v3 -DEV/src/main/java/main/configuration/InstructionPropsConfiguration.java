package main.configuration;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import bm.CIREngine;

public class InstructionPropsConfiguration {
	private static final Logger logger = Logger.getLogger("InstructionPropsConfiguration");
	private ComponentPropertyList componentPropertyList;
	private String rulesFileLocation;
	
	/*
	 * Specifications for openhab
	 * 	1. 'dir' = dir of openhab installation
	 * 	2. 'site_name' = name of the sitemap
	 * 	3. 'file_name' = name of files to be generated
	 */
	private HashMap<String, String> openhabSpecs = new HashMap<String, String>(3);
			
	public InstructionPropsConfiguration(UserConfig uconf) {
		//try {
			setRulesFileLocation(
					//new File(CIRInterpreter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent()
					//+ "/resources/cir.cirs");
					"resources/cir.cirs");
		/*} catch (URISyntaxException e) {
			logger.fatal("Cannot find CIR file", e);
			System.exit(0);
		}*/
		
		if(uconf.getProperty("openhab") != null) {
			getOpenhabSpecs().put("dir", uconf.getProperty("openhab"));
			getOpenhabSpecs().put("site_name", uconf.getProperty("openhab_sitemap"));
			if(uconf.getProperty("openhab_filename") != null) {
				getOpenhabSpecs().put("file_name", uconf.getProperty("openhab_filename"));
			}
		}
		else {
			logger.fatal("Openhab location not specified in properties file!");
			System.exit(0);
		}
	}

	/**
	 * @return the componentPropertyList
	 */
	public ComponentPropertyList getComponentPropertyList() {
		return componentPropertyList;
	}

	/**
	 * @param componentPropertyList the componentPropertyList to set
	 */
	public void setComponentPropertyList(ComponentPropertyList componentPropertyList) {
		this.componentPropertyList = componentPropertyList;
	}

	/**
	 * @return the rulesFileLocation
	 */
	public String getRulesFileLocation() {
		return rulesFileLocation;
	}

	/**
	 * @param rulesFileLocation the rulesFileLocation to set
	 */
	public void setRulesFileLocation(String rulesFileLocation) {
		this.rulesFileLocation = rulesFileLocation;
	}

	/**
	 * @return the openhabSpecs
	 */
	public HashMap<String, String> getOpenhabSpecs() {
		return openhabSpecs;
	}

	/**
	 * @param openhabSpecs the openhabSpecs to set
	 */
	public void setOpenhabSpecs(HashMap<String, String> openhabSpecs) {
		this.openhabSpecs = openhabSpecs;
	}
}
