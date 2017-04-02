/**
 * CIRInterpreter documentation
 * 		CIR, short for Component Interaction Rules, is a file with the type .cir which contains the rules for component interaction encoded
 * 	in CIRS (CIR Script). The CIRInterpreter serves as the interpreter for the CIRS. The CIRInterpreter reads the .cir file and translates
 * 	the script into a Vector of Statements that can be used by various Modules, specifically the InstructionModule, to handle component
 * 	interaction.
 */
package main.engines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import cir.*;
import cir.exceptions.*;
import components.Component;
import components.properties.Property;
import json.objects.ReqPOOP;
import json.objects.ResError;
//import main.TransTechSystem;
import main.ComponentRepository;
import main.engines.requests.EngineRequest;
import main.engines.requests.CIREngine.CIREngineRequest;
import main.engines.requests.CIREngine.CIRRequestType;
import main.engines.requests.CIREngine.GetSpecificExecBlocksCIREReq;
import main.engines.requests.CIREngine.GetStatementsCIREReq;
import tools.FileHandler;

/**
 * The CIREngine handles the I/O between the BM and the CIR file. In addition to that, the CIREngine also interprets the
 * CIR file for the BM to understand and use, particularly for the POOPModule.
 * 
 * @author Carlo
 */
public class CIREngine extends AbstEngine {
	//private static final Logger LOG = Logger.getLogger(CIREngine.class);
	private Vector<Statement> cirStatements = new Vector<Statement>(1,1);
	private ComponentRepository cr;
	//private FileHandler bm_props_file;
	//private Properties bm_props = new Properties();
	private String cir_filepath;

	/**
	 * Instantiates the CIRInterpreter. Filename must include the complete file path of the .cir file.
	 * @param filename
	 
	public CIRInterpreter(String filename) {
		this.filename = filename;
		interpret();
	}*/
	
	public CIREngine(String cir_filepath, ComponentRepository componentRepository) {
		super("CIREngine", CIREngine.class.toString());
		LOG.info("CIREngine started!");
		LOG.info("JAR Filepath:" + CIREngine.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		cr = componentRepository;
		this.cir_filepath = cir_filepath;
		/*try {
			bm_props_file = new FileHandler(bm_props_filepath);
			//bm_props_file = new FileHandler("src/main/resources/cfg/bm.properties");
		} catch (FileNotFoundException e) {
			LOG.fatal("Cannot find bm.properties file! Bad filepath.");
			e.printStackTrace();
		}*/
		update();
	}
	
	@Override
	protected Object processRequest(EngineRequest er) {
		CIREngineRequest cirer = (CIREngineRequest) er;
		
		if(cirer.getType() == CIRRequestType.update) {
			update();
			return cirer;
		}
		else if(cirer.getType() == CIRRequestType.getStatements) {
			GetStatementsCIREReq get = (GetStatementsCIREReq) cirer;
			if(get.isGetAll()) {
				get.setResponse(getCIRStatements());
			} else {
				get.setResponse(getCIRStatementsWithArgComponent(get.getComponent(), get.getProperty()));
			}
			return get.getResponse();
		}
		else if(cirer.getType() == CIRRequestType.getSpecificExecBlocks) {
			GetSpecificExecBlocksCIREReq get = (GetSpecificExecBlocksCIREReq) cirer;
			get.setResponse(getSpecificExecBlocks(get.getPoop()));
			return get.getResponse();
		}
		else {
			return null;
		}
	}
	
	/**
	 * Updates the CIR statement records of the CIREngine by reading from the CIR file and interpreting its contents.
	 */
	protected void update() {
		LOG.trace("Updating CIR records...");
		try {
			//interpret(TransTechSystem.config.getInstructionPropsConfig().getRulesFileLocation());
			//bm_props.load(bm_props_file.getFileReader());
			//cir_filepath = bm_props.getProperty("cir.filepath");
			interpret(cir_filepath);
			LOG.trace("CIR records updated!");
		} catch (IOException e) {
			LOG.fatal("Could not interpret CIR file! " + cir_filepath, e);
			e.printStackTrace();
			currentRequest.setResponse(new ResError(name, "Could not interpret CIR file!"));
		}
	}
	
	/**
	 * Interprets the .cir file.
	 * 
	 * @param filename the filename of the .cir file
	 * @return a Vector containing all the statements
	 */
	//public Vector<Statement> interpret(String filename) throws FileNotFoundException, IOException {
	private void interpret(String filename) throws IOException {
		Vector<Statement> statements = new Vector<Statement>(1,1);
		
		InputStream file = new FileInputStream(filename);
		BufferedReader reader = new BufferedReader(new InputStreamReader(file));
		
		int i = 1;
		while(reader.ready()) {
			String line = reader.readLine();
			//logger.debug(line.split("THEN")[0]);
			Statement s = null;
			try {
				s = extractStatement(line);
			} catch (CIRSSyntaxException e) {
				LOG.error("Syntax error in line " + i + " in the CIR file!", e);
				e.printStackTrace();
				currentRequest.setResponse(new ResError(name, "Syntax error in line " + i 
						+ " in the CIR file! " + e.getMessage()));
			}
			if(s != null) { //only null if line sent is a whitespace
				statements.add(s);
			}
			i++;
		}	
		
		reader.close();
		cirStatements = statements;
		//return statements;
	}
	
	/**
	 * Extracts the CIRS statement from the specified line from the .cir file. The extracted statement will be added to the 
	 * Statements vector.
	 * 
	 * @param line The String where the CIRS statement will be extracted from.
	 * @return the extracted CIRS statement, or <i>null</i> if <b>line</b> is a whitespace or if
	 * 		the CIRS statement contains an invalid CID.
	 * @throws CIRSSyntaxException throws CIRSyntaxException if there is a violation in the syntax
	 */
	private Statement extractStatement(String line) throws CIRSSyntaxException {			
		String[] s1 = line.split(" "); //removes spaces from string
		String raw = ""; //contains the space-free String representation of the CIRS statement
		String cond = ""; //contains the condition of the statement
		//String[] args; //contains the arguments of the statement
		Argument[] argsblocks;
		String[] execs; //contains the execution blocks of the statement
		
		/*
		 * Creates 'raw' string
		 */
		for(int i = 0; i < s1.length; i++) {
			raw = raw + s1[i];
		}
		
		/*
		 * Checks for whitespaces and easily identifiable syntax errors
		 */
		if(raw.equals("")) { //true if line is a whitespace, ignore the line if true
			return null;
		} 
		else if (raw.substring(0, 2).equals("//")) { //signifies a comment, consider as whitespace
			return null;
		}
		else {
			if(line.contains("THEN")) { 
				if(line.split("THEN").length > 2) { //error: More than one 'THEN' conditional separator!
					throw new CIRSSyntaxException("More than one 'THEN' conditional separator!");
				}
			} else {
				throw new CIRSSyntaxException("Conditional separator 'THEN' cannot be found!");
			}
		}
		
		/*
		 * Identifies statement condition and code blocks
		 */
		String[] splitraw; //the 'raw' string split into the two main parts of the statement
		if(raw.substring(0, 2).equals("IF")) {
			cond = "IF";
			splitraw = raw.substring(2).split("THEN");
		}
		else if(raw.substring(0, 4).equals("WHILE")) {
			cond = "WHILE";
			splitraw = raw.substring(4).split("THEN");
		}
		else { //error 1: Syntax error, conditional type cannot be identified!
			throw new CIRSSyntaxException("Conditional type cannot be identified!");
		}
		
		/*
		 * Retrieves args and execs blocks
		 */
		char[] args_block = splitraw[0].toCharArray();
		
		/*
		 * Parses args into CodeBlocks
		 */
		Vector<Argument> args_vec = new Vector<Argument>(1,1);
		String single_arg_str = "";
		for(int i = 0; i < args_block.length; i++) {
			char c = args_block[i];
			if(c == 'A') {
				if(args_block[i + 1] == 'N') {
					if(args_block[i + 2] == 'D') {
						args_vec.add(parseArgumentBlockString(single_arg_str, Relationship.AND));
						single_arg_str = "";
						i = i + 2;
					}else {
						single_arg_str += c;
					}
				}else {
					single_arg_str += c;
				}
			}
			else if(c == 'O') {
				if(args_block[i + 1] == 'R') {
					args_vec.add(parseArgumentBlockString(single_arg_str, Relationship.OR));
					single_arg_str = "";
					i = i + 1;
				}
				else {
					single_arg_str += c;
				}
			}
			else {
				single_arg_str += c;
			}
		}
		args_vec.add(parseArgumentBlockString(single_arg_str, Relationship.NONE)); //adds final arg block
		argsblocks = new Argument[args_vec.size()];
		args_vec.toArray(argsblocks);
		
		/*
		 * Parses execs into CodeBlocks
		 */
		if(splitraw[1].contains("OR")) { //error: Syntax error, Execution block cannot contain 'OR' separator!
			throw new CIRSSyntaxException("Execution block cannot contain 'OR' separator!");
		} else {
			execs = splitraw[1].split("AND");
		}
		ExecutionBlock[] execsblocks = new ExecutionBlock[execs.length];
		for(int i = 0; i < execs.length; i++) {
			String exec = execs[i];
			execsblocks[i] = parseExecutionBlock(exec);
		}
		
		Statement s = new Statement(Conditional.parseConditional(cond), argsblocks, execsblocks);
		/*
		 * Checks if the statement contains a CID that does not exist
		 */
		boolean b = true;
		for(int i = 0; i < s.getArguments().length; i++) {
			Argument arg = s.getArguments()[i];
			if(!cr.containsComponent(arg.getComID())) {
				LOG.warn("A statement is found to contain an invalid CID!");
				s = null;
				b = false;
				break;
			}
		}
		if(b) {
			for(int i = 0; i < s.getExecBlocks().length; i++) {
				ExecutionBlock arg = s.getExecBlocks()[i];
				if(!cr.containsComponent(arg.getComID())) {
					LOG.warn("A statement is found to contain an invalid CID!");
					s = null;
					break;
				}
			}
		}
		return s;
	}
	
	/**
	 * Parses a String of an execution block into an ExecutionBlock.
	 * 
	 * @param block The execution block String
	 * @return
	 * @throws CIRSSyntaxException 
	 */
	private ExecutionBlock parseExecutionBlock(String block) throws CIRSSyntaxException {
		String exec = block;
		for(int i = 0; i < exec.split(":").length; i++) {
			//System.out.println(exec.split(":")[i]);
		}
		if(exec.split(":").length > 2) { //error 2: Syntax error, Improper usage of ':' in execution block!
			throw new CIRSSyntaxException("Improper usage of ':' in execution block!");
		} else if(exec.split("=").length > 2) { //error 3: Syntax error, Improper usage of '=' in execution block!
			throw new CIRSSyntaxException("Improper usage of '=' in execution block!");
		}
		else {
			String com_id = exec.split(":")[0];
			String prop = exec.split(":")[1].split("=")[0];
			String propval = exec.split(":")[1].split("=")[1];
			return new ExecutionBlock(com_id, prop, propval);
		}
	}
	
	/**
	 * Parses a String of an argument block into a CodeBlock.
	 * 
	 * @param block The argument block String.
	 * @param relationshipWithNextArgument The relationship of this argument block with the next argument block
	 * @return
	 * @throws CIRSSyntaxException
	 */
	private Argument parseArgumentBlockString(String block, Relationship relationshipWithNextArgument) throws CIRSSyntaxException {
		Argument a;
		String arg = block;
		/*
		 * Checks if single argument block contains more than one operator
		 */
		if(arg.split(":").length > 2) { //error 2: Syntax error, Improper usage of ':' in arguments block!
			throw new CIRSSyntaxException("Improper usage of ':' in arguments block!");
		} else if(arg.split("=").length > 2) { //error 3: Syntax error, Improper usage of '=' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '=' in arguments block!");
		} else if(arg.split(">=").length > 2) { //error 3: Syntax error, Improper usage of '>=' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '>=' in arguments block!");
		} else if(arg.split("<=").length > 2) { //error 3: Syntax error, Improper usage of '<=' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '<=' in arguments block!");
		} else if(arg.split("!=").length > 2) { //error 3: Syntax error, Improper usage of '!=' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '!=' in arguments block!");
		} else if(arg.split(">").length > 2) { //error 3: Syntax error, Improper usage of '>' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '>' in arguments block!");
		} else if(arg.split("<").length > 2) { //error 3: Syntax error, Improper usage of '<' in arguments block!
			throw new CIRSSyntaxException("Improper usage of '<' in arguments block!");
		}
		else {
			String com_id = arg.split(":")[0];
			ArgOperator operator = null;
			String prop = null;
			String propval = null;
			if(arg.split(":")[1].split(">=").length > 1) {
				prop = arg.split(":")[1].split(">=")[0];
				propval = arg.split(":")[1].split(">=")[1];
				operator = ArgOperator.GREATEREQUALS;
			} else if(arg.split(":")[1].split("<=").length > 1) {
				prop = arg.split(":")[1].split("<=")[0];
				propval = arg.split(":")[1].split("<=")[1];
				operator = ArgOperator.LESSEQUALS;
			} else if(arg.split(":")[1].split("!=").length > 1) {
				prop = arg.split(":")[1].split("!=")[0];
				propval = arg.split(":")[1].split("!=")[1];
				operator = ArgOperator.INEQUAL;
			} else if(arg.split(":")[1].split("=").length > 1) {
				prop = arg.split(":")[1].split("=")[0];
				propval = arg.split(":")[1].split("=")[1];
				operator = ArgOperator.EQUALS;
			} else if(arg.split(":")[1].split(">").length > 1) {
				prop = arg.split(":")[1].split(">")[0];
				propval = arg.split(":")[1].split(">")[1];
				operator = ArgOperator.GREATER;
			} else if(arg.split(":")[1].split("<").length > 1) {
				prop = arg.split(":")[1].split("<")[0];
				propval = arg.split(":")[1].split("<")[1];
				operator = ArgOperator.LESS;
			} else {
				throw new CIRSSyntaxException("Invalid/nonexistent conditional operator in arguments block!");
			}
			a = new Argument(com_id, prop, propval, operator, relationshipWithNextArgument);
		}	
		
		return a;
	}
	
	/**
	 * Returns a Vector containing all CIR statements that have the specified component and property in
	 * its argument block. <br><br>
	 * 
	 * <b>NOTE:</b> This does not support CIR entanglement.
	 * @param c The Component object
	 * @param p The Property object
	 * @return The Vector containing all the CIR statements found
	 */
	protected Vector<Statement> getCIRStatementsWithArgComponent(Component c, Property p) {
		LOG.debug("Retrieving CIR for component " + c.getSSID() + " with property " + p.getSystemName());
		Vector<Statement> collection = cirStatements;
		Vector<Statement> statements = new Vector<Statement>(1,1);
		
		for(int i = 0; i < collection.size(); i++) {
			Statement rule = collection.get(i);
			if(rule.containsComponentInArguments(c, p)) {
				statements.add(rule);
			}
		}
		LOG.trace(statements.size() + " retrieved!");
		return statements;
	}
	
	/**
	 * Returns a Vector containing all CIR statements that have the specified component and property in
	 * its argument block. <br><br>
	 * 
	 * The returned CIR statements are not only for the specified component property itself, 
	 * but also for the other component properties that will be affected once these statements
	 * are executed. For more information, see <i>Entangled CIR Statements</i> in the CIR
	 * document.
	 * 
	 * @param c The Component object
	 * @param p The Property object
	 * @return The Vector containing all the CIR statements found
	 
	protected Vector<Statement> getCIRStatementsWithArgComponent(Component c, Property p) {
		LOG.debug("Retrieving CIR for component " + c.getSSID() + " with property " + p.getSystemName());
		Vector<Statement> collection = cirStatements;
		Vector<String> affectedComs = new Vector<String>(1,1); //contains all the SSID of the components affected and their corresponding properties
		Vector<Statement> statements = new Vector<Statement>(1,1);
		affectedComs.add(c.getSSID() + ":" + p.getSSID());
		 
		/*
		 * START FIX HERE! Current patch compares statements size to collection size,
		 * may cause problems when there are multiple rules as some rules can be repeated
		 
		LOG.fatal(collection.size());
		while(!affectedComs.isEmpty() && statements.size() <= collection.size()) {
			String comSSID = affectedComs.get(0).split(":")[0];
			String propSSID = affectedComs.get(0).split(":")[1];
			affectedComs.remove(0);
			for(int i = 0; i < collection.size(); i++) {
				Statement rule = collection.get(i);
				Argument[] args = rule.getArguments();
				for(int j = 0; j < args.length; j++) {
					Argument arg = args[j];
					if(arg.getComID().equals(comSSID) && arg.getPropName().equals(propSSID)) {
						statements.add(rule);
						for(int l = 0; l < rule.getExecBlocks().length; l++) {
							String affectedComSSID = rule.getExecBlocks()[l].getComID();
							String affectedPropSSID = rule.getExecBlocks()[l].getPropName();
							if(!affectedComs.contains(affectedComSSID + ":" + affectedPropSSID)) {
								affectedComs.add(affectedComSSID + ":" + affectedPropSSID);
							}
						}
					}
				}
			}
		}
		LOG.trace(statements.size() + " retrieved!");
		return statements;
	}*/

	/**
	 * Gets all the CIR statements from the CIR file interpreted by this CIRInterpreter.
	 * 
	 * @return the cirStatements
	 */
	protected Vector<Statement> getCIRStatements() {
		return cirStatements;
	}
	
	/**
	 * Returns the execution blocks of the rules that were followed as determined by the
	 * POOP request that was received. <i>This method is exclusively called for by the 
	 * POOPModule</i>
	 * 
	 * @param poop The POOP request received. This is supplied by the POOPModule
	 * @return A Vector containing all the execution blocks of the rules that were followed, 
	 * 		ResError object if the process encountered an error
	 */
	protected Object getSpecificExecBlocks(ReqPOOP poop) {
		Component com = cr.getComponent(poop.cid);
		Property prop = com.getProperty(poop.propSSID);
		Vector<Statement> rules = getCIRStatementsWithArgComponent(com, prop);
		Vector<ExecutionBlock> execsRetrieved = new Vector<ExecutionBlock>(1,1);
		//LOG.fatal("Rules amount:" + rules.size());
		
		for(int i = 0; i < rules.size(); i++) {
			Statement rule = rules.get(i);
			Argument[] args = rule.getArguments();
			ExecutionBlock[] execs = rule.getExecBlocks();
			boolean rule_result = false; //TRUE if rule will be used, false otherwise
			//LOG.fatal("Rule " + i + ":" + rule.toString());
			
			for(int j = 0; j < args.length; j++) {
				rule_result = false;
				Argument arg = args[j];
				Component arg_com = cr.getComponent(arg.getComID());
				int pval = arg_com.getProperty(arg.getPropSSID()).getValue();
				rule_result = compareValueWithArgument(arg, pval);
				
				if(!rule_result) { //rule will NOT be used
					break;
				}
			}
			if(rule_result) { //rule will be used
				for(int j = 0; j < execs.length; j++) {
					execsRetrieved.add(execs[j]);
				}
			}
		}
		return execsRetrieved;
	}
	
	/**
	 * Checks if the specified Property value meets the specified Argument's condition
	 * 
	 * @param arg The Argument where the Property value will be compared with
	 * @param pval The Property value that will be compared with the Argument
	 * @return <b>True</b> if the specified Property value meets the specified Argument's 
	 * 		condition, <b>false</b> otherwise.
	 */
	private boolean compareValueWithArgument(Argument arg, int pval) {
		boolean b = false;
		
		if(arg.getOperator().equals(ArgOperator.EQUALS)) {
			if(arg.getPropValue().equals(String.valueOf(pval))) {
				b = true;
			}
		}
		else if(arg.getOperator().equals(ArgOperator.GREATER)) {
			if(Float.parseFloat(arg.getPropValue()) < pval) {
				b = true;
			}
		}
		else if(arg.getOperator().equals(ArgOperator.LESS)) {
			if(Float.parseFloat(arg.getPropValue()) > pval) {
				b = true;
			}
		}
		else if(arg.getOperator().equals(ArgOperator.GREATEREQUALS)) {
			if(Float.parseFloat(arg.getPropValue()) <= pval) {
				b = true;
			}
		}
		else if(arg.getOperator().equals(ArgOperator.LESSEQUALS)) {
			if(Float.parseFloat(arg.getPropValue()) >= pval) {
				b = true;
			}
		}
		else if(arg.getOperator().equals(ArgOperator.INEQUAL)) {
			if(Float.parseFloat(arg.getPropValue()) != pval) {
				b = true;
			}
		}
		
		return b;
	}
}
