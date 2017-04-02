package cir;

import org.apache.log4j.Logger;

import components.Component;
import components.properties.Property;
import tools.StringTools;
import tools.StringTools.StringInjectionException;

public class Statement {
	private static final Logger logger = Logger.getLogger(Statement.class);
	private Conditional condition;
	private Argument[] arguments;
	private ExecutionBlock[] execBlocks;

	public Statement(Conditional condition, Argument[] arguments, ExecutionBlock[] execBlocks) {
		setCondition(condition);
		setArguments(arguments);
		setExecBlocks(execBlocks);
	}
	
	/**
	 * Checks if the specified Component and Property pair exists in the arguments section 
	 * of this Statement
	 * 
	 * @param c The Component to be checked for
	 * @param p The Property paired with the Component which will also be checked for
	 * @return <b>True</b> if the specified Component exists in the arguments section, 
	 * 		<b>false</b> otherwise
	 */
	public boolean containsComponentInArguments(Component c, Property p) {
		boolean b = false;
		for(int i = 0; i < arguments.length; i++) {
			Argument arg = arguments[i];
			if(arg.getComID().equals(c.getSSID())) {
				if(arg.getPropSSID().equals(p.getSSID())) {
					b = true;
					break;
				}
			}
		}
		return b;
	}

	/**
	 * @return the condition
	 */
	public Conditional getCondition() {
		return condition;
	}

	/**
	 * @param condition the condition to set
	 */
	public void setCondition(Conditional condition) {
		this.condition = condition;
	}

	/**
	 * @return the execBlocks
	 */
	public ExecutionBlock[] getExecBlocks() {
		return execBlocks;
	}

	/**
	 * @param execBlocks the execBlocks to set
	 */
	public void setExecBlocks(ExecutionBlock[] execBlocks) {
		this.execBlocks = execBlocks;
	}

	/**
	 * Returns the Arguments block in order based on groupings.
	 * 
	 * @return the arguments
	 */
	public Argument[] getArguments() {
		return arguments;
	}

	/**
	 * @param arguments the arguments to set
	 */
	public void setArguments(Argument[] arguments) {
		this.arguments = arguments;
	}
	
	/**
	 * Returns a String representation of this CIR Statement.
	 */
	public String toString() {
		String args = "";
		String execs = "";
		String str = null;
		
		for(int i = 0; i < arguments.length; i++) {
			args += arguments[i] + " ";
		}
		for(int i = 0; i < execBlocks.length; i++) {
			execs += execBlocks[i] + " AND ";
		}
		execs = execs.substring(0, execs.length() - 4);
		
		str = StringTools.injectStrings("%s %s THEN %s", new String[]{getCondition().toString(), args, execs}, "%s");
		
		return str;
	}
}
