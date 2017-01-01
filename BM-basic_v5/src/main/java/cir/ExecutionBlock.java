package cir;

import org.apache.log4j.Logger;

import tools.StringTools;
import tools.StringTools.StringInjectionException;

public class ExecutionBlock extends CodeBlock {
	private static final Logger logger = Logger.getLogger(ExecutionBlock.class);
	
	public ExecutionBlock(String comID, String comProperty, String comValue) {
		super(comID, comProperty, comValue);
	}

	@Override
	public String toString() {
		return StringTools.injectStrings("%s:%s = %s", new String[]{getComID(), getPropName(), getPropValue()}, "%s");
	}

}
