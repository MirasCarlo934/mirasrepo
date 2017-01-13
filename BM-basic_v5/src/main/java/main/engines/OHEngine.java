package main.engines;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;

import main.ComponentRepository;
import main.engines.requests.EngineRequest;
import main.engines.requests.OHEngine.OHEngineRequest;
import main.engines.requests.OHEngine.OHRequestType;
import tools.FileHandler;

public class OHEngine extends Engine {
	private ComponentRepository cr;
	private String bm_props_filepath;
	private Properties bm_props = new Properties();
	private FileHandler items;
	private FileHandler sitemap;

	public OHEngine(String bm_props_filepath, ComponentRepository cr) {
		super("OHEngine", OHEngine.class.toString());
		this.bm_props_filepath = bm_props_filepath;
		this.cr = cr;
		try {
			LOG.info("Connecting to bm.properties...");
			FileHandler fh = new FileHandler(bm_props_filepath);
			bm_props.load(fh.getFileReader());
			LOG.debug("Connected to bm.properties!");
		} catch (FileNotFoundException e) {
			LOG.error("Cannot open bm.properties!", e);
			e.printStackTrace();
		} catch (IOException e) {
			LOG.error("Cannot open bm.properties!", e);
			e.printStackTrace();
		}
	}

	@Override
	protected Object processRequest(EngineRequest er) {
		OHEngineRequest oher = (OHEngineRequest) er;
		
		if(oher.getType() == OHRequestType.start) {
			startOH();
			return oher;
		}
		else if(oher.getType() == OHRequestType.stop) {
			stopOH();
			return oher;
		}
		else if(oher.getType() == OHRequestType.update) {
			updateSitemap();
			//LOG.debug("HERE!");
			return oher;
		}
		else {
			return null;
		}
	}
	
	private void startOH() {
		LOG.info("Starting OpenHAB...");
		try {
			//String[] execs = {"cmd", "/c", "start", "cd C:\\Applications\\openhab", "start.bat"};
			Runtime.getRuntime().exec("cmd /c \"cd C:\\Applications\\openhab && start.bat\"");
			connectToFiles();
			LOG.debug("OpenHAB started!");
		} catch (IOException e) {
			LOG.error("Cannot start OpenHAB!", e);
			e.printStackTrace();
		}
	}
	
	private void stopOH() {
		LOG.info("Stopping OpenHAB...");
		try {
			Runtime.getRuntime().exec("cmd /c \"taskkill /im java.exe /f\"");
		} catch (IOException e) {
			LOG.error("Cannot stpp OpenHAB!", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Opens a FileHandler connected to the .items and .sitemap files in OpenHAB directory
	 */
	private void connectToFiles() {
		LOG.info("Connecting to .items and .sitemap files in OpenHAB directory...");
		String items_filepath = bm_props.getProperty("oh.location") + "/configurations/items/"
				+ bm_props.getProperty("oh.items_filename");
		String sitemap_filepath = bm_props.getProperty("oh.location") + "/configurations/sitemaps/"
				+ bm_props.getProperty("oh.sitemap_filename");
		try {
			//LOG.fatal(items_filepath);
			//LOG.fatal(sitemap_filepath);
			items = new FileHandler(items_filepath);
			sitemap = new FileHandler(sitemap_filepath);
			LOG.debug("Connected to. items and .sitemap files!");
		} catch (FileNotFoundException e) {
			LOG.error("Cannot open .items and .sitemap files!", e);
			//e.printStackTrace();
		} /*catch (IOException e) {
			LOG.error("Cannot read .items and .sitemap files!", e);
			e.printStackTrace();
		}*/
	}
	
	/**
	 * Updates the contents of the .sitemap file in OpenHAB.
	 */
	private void updateSitemap() {
		LOG.debug("Updating .sitemap file...");
		String sitemap_name = bm_props.getProperty("oh.sitemap_name");
		String str = "sitemap myhome label=\"" + sitemap_name + "\"{ \n"
				+ "Frame {}}";
		try {
			sitemap.writeToFile(str);
		} catch (IOException e) {
			LOG.error("Cannot write to .sitemap file!", e);
			e.printStackTrace();
		}
	}
}
