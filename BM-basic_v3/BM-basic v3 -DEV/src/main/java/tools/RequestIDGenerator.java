package tools;

import java.util.Random;
import java.util.Vector;

public class RequestIDGenerator extends Random {

	/**
	 * 
	 */
	private static final long serialVersionUID = -593252532412757064L;
	private Vector<Integer> existingIDs = new Vector<Integer>(1,1);
	private int max; //maximum limit for id value
	
	public RequestIDGenerator(int max) {
		this.max = max;
	}

	public int generateID() {
		int id = nextInt(max);
		while(existingIDs.contains(id)) {
			id = nextInt(max);
		}
		existingIDs.add(id);
		return id;
	}
}
