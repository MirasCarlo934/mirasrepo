/**
 * CIRInterpreter documentation
 * 		CIR, short for Component Interaction Rules, is a file with the type .cir which contains the rules for component interaction encoded
 * 	in CIRS (CIR Script). The CIRInterpreter serves as the interpreter for the CIRS. The CIRInterpreter reads the .cir file and translates
 * 	the script into a Vector of Statements that can be used by various Modules, specifically the InstructionModule, to handle component
 * 	interaction.
 */
package bm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Vector;

import org.apache.log4j.Logger;

import bm.objects.cir.*;
import bm.objects.cir.exceptions.*;
import main.TransTechSystem;

/**
 * The CIREngine handles the I/O between the BM and the CIR file. In addition to that, the CIREngine also interprets the
 * CIR file for the BM to understand and use, particularly for the POOPModule.
 * 
 * @author Carlo
 */
public class CIREngine {
	private static final Logger logger = Logger.getLogger(CIREngine.class);
	private Vector<Statement> cirStatements = new Vector<Statement>(1,1);
	private ComponentRepository cr;

	/**
	 * Instantiates the CIRInterpreter. Filename must include the complete file path of the .cir file.
	 * @param filename
	 
	public CIRInterpreter(String filename) {
		this.filename = filename;
		interpret();
	}*/
	
	public CIREngine(ComponentRepository componentRepository) {
		cr = componentRepository;
		//update();
	}
	
	/**
	 * Updates the CIR statement records of the CIREngine by reading from the CIR file and interpreting its contents.
	 */
	public void update() {
		try {
			interpret(TransTechSystem.config.getInstructionPropsConfig().getRulesFileLocation());
		} catch (IOException e) {
			logger.fatal("Could not interpret CIR file!", e);
			TransTechSystem.forceShutdown();
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
				logger.error("Syntax error in line " + i + " in the CIR file!", e);
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
				logger.warn("A statement is found to contain an invalid CID!");
				s = null;
				b = false;
				break;
			}
		}
		if(b) {
			for(int i = 0; i < s.getExecBlocks().length; i++) {
				ExecutionBlock arg = s.getExecBlocks()[i];
				if(!cr.containsComponent(arg.getComID())) {
					logger.warn("A statement is found to contain an invalid CID!");
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
			if(arg.split(":")[1].split("=").length > 1) {
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
			} else if(arg.split(":")[1].split(">=").length > 1) {
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
			} else {
				throw new CIRSSyntaxException("Invalid/nonexistent conditional operator in arguments block!");
			}
			a = new Argument(com_id, prop, propval, operator, relationshipWithNextArgument);
		}	
		
		return a;
	}

	/**
	 * Gets all the CIR statements from the CIR file interpreted by this CIRInterpreter.
	 * 
	 * @return the cirStatements
	 */
	public Vector<Statement> getCirStatements() {
		return cirStatements;
	}
}
