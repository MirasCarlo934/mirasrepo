package tools;

import java.util.Vector;

public class Gate extends Vector {
	
	public Object release() {
		return remove(0);
	}
	
	public void push(Object o) {
		addElement(o);
	}
}
