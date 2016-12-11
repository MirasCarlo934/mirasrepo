package web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import bm.CIREngine;
import bm.ComponentRepository;
import bm.TransactionEngine;
import bm.objects.cir.ArgOperator;
import bm.objects.cir.Argument;
import bm.objects.cir.Conditional;
import bm.objects.cir.ExecutionBlock;
import bm.objects.cir.Relationship;
import bm.objects.cir.Statement;
import main.TransTechSystem;
import main.components.Room;
import main.configuration.ComponentPropertyList;
import main.objects.request_response.BMRequest;
import main.objects.request_response.TransactionRequest;
import main.objects.request_response.TransactionResponse;
import tools.FileHandler;

/**
 * This MVC Controller controls the CIR Editor
 * 
 * @author cels
 */
@Controller
public class CIREWebController {
	/*
	 * Config file properties
	 */
	private static final String cirfile_prop = "cir_file";
	
	private static final String error_msg_attr = "error_msg";
	private static final String error_html = "error_page";
	private static final Logger logger = Logger.getLogger(CIREWebController.class);
	private static final String cirfile_pname = "cir_file";
	
	/*
	 * The parameters for the /addCIR request map.
	 * These are needed by the Components object in constructing their CIRE HTML
	 */
	public static final String com1_pname_param = "com1_pname";
	public static final String com1_pval_param = "com1_pval";
	public static final String com2_pname_param = "com2_pname";
	public static final String com2_pval_param = "com2_pval";
	public static final String com1_oper_param = "operator";
	
	private FileHandler propFH; //FileHandler for the cir.config file
	private FileHandler cirFH = null; //FileHandler for the cir.cirs file
	private Properties properties = new Properties();
	@Autowired
	private ComponentRepository cr;
	@Autowired
	private TransactionEngine te;
	//@Autowired
	//private ComponentPropertyList cpl;
	@Autowired
	private CIREngine ciri;
	
	public CIREWebController() {
		logger.info("Starting WebController...");
		try {
			logger.trace("Accessing cir.config file...");
			propFH = new FileHandler("configuration/cire/cire.config");
			properties.load(propFH.getFileReader());
			logger.trace("File accessed!");
		} catch (IOException e) {
			logger.fatal("Cannot find config file for CIRE! The installation files must be mishandled or corrupted!", e);
			TransTechSystem.forceShutdown();
		}
		
		try {
			cirFH = new FileHandler(properties.getProperty(cirfile_pname));
			if(!cirFH.checkExtension("cirs"))
				throw new FileNotFoundException();
		} catch (FileNotFoundException e) {
			logger.warn("CIR File not found! fileLocator.html will be posted in all request maps.");
		}
		logger.info("WebController started!");
	}
	
	//direct user interaction
	@RequestMapping("/")
	public String home() {
		logInvocation("/");
		String view;
		if(cirFH == null) { //true if CIR file is not yet specified
    		logger.info("CIR File not yet specified!");
    		view = "fileLocator";
    		logDispatch(view);
        	return view;
    	}
		
		try {
			cirFH = new FileHandler(properties.getProperty(cirfile_pname));
			if(cirFH.checkExtension("cirs"))
				view = "home";
			else 
				view = "fileLocator";
		} catch (FileNotFoundException e) {
			view = "fileLocator";
		}
		
		logDispatch(view);
		return view;
	}
	
    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
    	logInvocation("/greeting");
    	String view = "test";
    	model.addAttribute("name", name);
    	
    	logDispatch(view);
        return view;
    }
    
    @RequestMapping("/setCIRFile")
    public String setCIRFile(@RequestParam(value="path", required=true, defaultValue="none") String file, Model model) throws IOException {
    	logInvocation("/setCIRFile");
    	String view;
    	
    	logger.info("Setting new CIR file location...");
    	properties.setProperty(cirfile_prop, file);
    	properties.store(propFH.getFileWriter(), "");
    	try {
			cirFH = new FileHandler(properties.getProperty(cirfile_pname));
			if(cirFH.checkExtension("cirs"))
				view = "home";
			else {
				logger.warn("The file specified does not have a .cirs extension!");
				model.addAttribute(error_msg_attr, "The file specified does not have a .cirs extension!");
	    		view = error_html;
			}
		} catch (FileNotFoundException e) {
			logger.warn("The file specified is nonexistent!");
			model.addAttribute(error_msg_attr, "The file specified is nonexistent!");
    		view = error_html;
		}
    	
    	logDispatch(view);
    	return view;
    }
    
    //direct user interaction
    @RequestMapping("/cire")
    public String cire(Model model) throws FileNotFoundException, IOException {
    	logInvocation("/cire");
    	String view = "cire";
    	if(cirFH == null) { //true if CIR file is not yet specified
    		logger.info("CIR File not yet specified!");
    		view = "fileLocator";
    		logDispatch(view);
        	return view;
    	}
    	
    	/*
    	 * translates existing cir's
    	
    	String[] cir = new String[ciri.getCirStatements().size()];
    	for(int i = 0; i < cir.length; i++) {
    		Statement s = ciri.getCirStatements().get(i);
    		String str = s.getCondition().toString() + " ";
    		
    		for(int j = 0; j < s.getArguments().length; j++) {
    			Argument arg = s.getArguments()[j];
    			str += cr.getComponent(arg.getComID()).translateCodeBlock(arg) + " ";
    			if(!arg.getRelationshipWithNextArgument().equals(Relationship.NONE)) {
    				str += arg.getRelationshipWithNextArgument().toString() + " ";
    			}
    		}
    		str += " THEN ";
    		
    		for(int j = 0; j < s.getExecBlocks().length; j++) {
    			ExecutionBlock eb = s.getExecBlocks()[j];
    			str += cr.getComponent(eb.getComID()).translateCodeBlock(eb) + " AND ";
    		}
    		str = str.substring(0, str.length() - 4);
    		
    		cir[i] = str;
    	} */
    	
    	//model.addAttribute("props_list", cpl);
    	//model.addAttribute("coms", cr.getAllComponents());
    	//model.addAttribute("existing_cir", cir);
    	/*try {
			TransactionResponse tres = te.selectAll(new TransactionRequest(Room.obj_type, null));
			Vector<Room> rooms = new Vector<Room>(1,1);
			for(int i = 0; i < tres.getObjects().size(); i++) {
				rooms.add(new Room(tres.getObjects().get(i)));
			}
			model.addAttribute("rooms", rooms);
		} catch (SQLException e) {
			logger.error("Cannot get rooms from DB!", e);
		}*/
    	model.addAttribute("rooms", cr.getAllRooms());
    	logDispatch(view);
    	return view;
    }
    
    @RequestMapping("/addCIR")
    public String addCIR(@RequestParam Map<String, String> params, Model model) {
    	logInvocation("/addCIR");
    	String view = "cire";
    	if(cirFH == null) { //true if CIR file is not yet specified
    		logger.info("CIR File not yet specified!");
    		view = "fileLocator";
    		logDispatch(view);
        	return view;
    	}
    	
    	logger.info("Adding new CIR...");
    	if(params.containsValue("none")) {
    		logger.error("Incomplete parameters received from CIR Editor!");
    		model.addAttribute(error_msg_attr, "Incomplete input received!");
    		view = error_html;
    	} else {
    		String condition = params.get("condition");
    		String com1 = params.get("com1");
    		String com1_pname = params.get(com1_pname_param);
    		String com1_pval = params.get(com1_pval_param);
    		String com1_oper = params.get(com1_oper_param);
    		String com2 = params.get("com2");
    		String com2_pname = params.get(com2_pname_param);
    		String com2_pval = params.get(com2_pval_param);	
    		
    		ArgOperator operator = ArgOperator.translate(com1_oper);
    		Argument arg = new Argument(com1, com1_pname, com1_pval, operator, Relationship.NONE);
    		ExecutionBlock execBlock = new ExecutionBlock(com2, com2_pname, com2_pval);
    		Statement s = new Statement(Conditional.parseConditional(condition), new Argument[]{arg}, 
    				new ExecutionBlock[]{execBlock});
    		try {
    			logger.trace("Writing new CIR to file...");
				cirFH.appendToFile(s.toString());
				logger.trace("Writing successful!");
				logger.info("New CIR added!");
				ciri.update();
				view = cire(model);
			} catch (IOException e) {
				logger.error("Cannot append new CIR to file!", e);
				model.addAttribute(error_msg_attr, "Cannot add new rule due to a system error!");
				view = error_html;
			}
    	}
    	
    	logDispatch(view);
    	return view;
    }
    
    private void logInvocation(String request_map) {
    	logger.info(request_map + " request map invoked!");
    }
    
    private void logDispatch(String view) {
    	logger.info(view + ".html dispatched!");
    }
}