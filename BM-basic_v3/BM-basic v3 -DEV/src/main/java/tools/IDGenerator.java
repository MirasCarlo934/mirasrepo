package tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;

import main.TransTechSystem;

public class IDGenerator {
	private static final Logger logger = Logger.getLogger("IDGenerator");
	private Vector<String> existingIDs = new Vector<String>(1,1);
	
	/**
	 * Generates a String with mixed characters consisting of numbers and letters that is used as an ID in various system processes.
	 * Mixed character ID's are used almost exclusively as ID's of requests and components. <br><br>
	 * 
	 * <i><b>By convention,</b> all mixedchar IDs start with a letter.</i>
	 * 
	 * @param length of the String ID to be generated.
	 * @return The generated String mixed character ID with the specified length.
	 */
	public String generateMixedCharID(int length) {
		String id = "";
		Random r = new Random();
		do {
			char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
			id = id + alphabet[r.nextInt(25)]; //starting char will ALWAYS be a letter.
			for(int i = 1; i < length; i++) {
				if(r.nextBoolean()) { //if true then next id char will be a number, letter otherwise
					id = id + String.valueOf(r.nextInt(9));
				} else {
					id = id + alphabet[r.nextInt(25)]; //random from 0 to 25
				}
			}
		} while(existingIDs.contains(id));
		existingIDs.add(id);
		return id;
	}
	
	/**
	 * Generates a String with mixed characters consisting of numbers and letters that is used as an ID in various system processes.
	 * Mixed character ID's are used almost exclusively as ID's of requests and components. Mixed character ID's are also used as ID
	 * of database entries in a table which accommodate lots of persistence of objects. An issue with these tables is the case of ID
	 * uniqueness. Mixed-character ID's have more possible combinations than Integer ID's, which is ideal for tables with massive numbers
	 * of entries.
	 *
	 * Because this type of ID is used in database entries, this method will generate a unique ID based on the existing ID's that are 
	 * already in the database.
	 * 
	 * @param length
	 * @param existingIDs
	 * @return
	 * @throws SQLException
	 */
	public String generateMixedCharID(int length, String[] existingIDs) throws SQLException {
		String id = "";
		Random r = new Random();
		boolean b = false; //true if ID generated is already unique
		do {
			char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
			id = id + alphabet[r.nextInt(25)];
			//generates the ID
			for(int i = 1; i < length; i++) {
				if(r.nextBoolean()) { //if true then next id char will be a number, letter otherwise
					id = id + String.valueOf(r.nextInt(9));
				} else {
					id = id + alphabet[r.nextInt(25)]; //random from 0 to 25
				}
			}
			
			//compares the generated ID to the existing ID's
			if(existingIDs != null) { //checks if there are no existing IDs
				boolean c = true; //true if ID no longer has a duplicate in the existingIDs array
				for(int i = 0; i < existingIDs.length; i++) {
					if(id.equals(existingIDs[i])) {
						c = false;
						break;
					}
				}
				
				if(c) {
					b = true;
					break;
				}
			}
		} while(!b);
		return id;
		
		/*Vector<String> ids = new Vector<String>(1,1); //container of all existing IDs
		
		//gets existing ids from ResultSet
		if(existingIDs == null) { //generic ID generation, no existing IDs given
			b = true;
		} else {
			while(existingIDs.next()) {
				ids.add(existingIDs.getString(Application.configuration.getDatabaseConfig().getSsidColName()));
			}
		}
		
		do {
			//generates the ID
			for(int i = 0; i < length; i++) {
				if(r.nextBoolean()) { //if true then next id char will be a number, letter otherwise
					id = id + String.valueOf(r.nextInt(9));
				} else {
					char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
					id = id + alphabet[r.nextInt(25)]; //random from 0 to 25
				}
			}
			
			//compares the generated ID to the existing ID's
			boolean c = true; //true if ID no longer has a duplicate in the existingIDs array
			for(int i = 0; i < ids.size(); i++) {
				if(id.equals(ids.get(i))) {
					c = false;
					break;
				}
			}
			
			if(c) {
				b = true;
				break;
			}
		} while(!b);
		return id;*/
	}
	
	/**
	 * Generates a String with exclusively numerical characters that is used as an ID in various system processes.
	 * This type of ID is used almost exclusively as ID's of different database entries. Because this type of ID is used in 
	 * database entries, this method will generate a unique ID based on the existing ID's that are already in the database.
	 * 
	 * @param length
	 * @param existingIDs SQL ResultSet containing all the existing SSIDs of the table
	 * @return
	 * @throws SQLException 
	 */
	public String generateIntID(int length, String[] existingIDs) throws SQLException {
		String id = "";
		boolean b = false; //true if ID generated is already unique
		//Vector<String> ids = new Vector<String>(1,1); //container of all existing IDs
		
		do {
			//generates the ID
			Random r = new Random();
			for(int i = 0; i < length; i++) {
				id = id + String.valueOf(r.nextInt(9));
			}
			
			//compares the generated ID to the existing ID's
			boolean c = true; //true if ID no longer has a duplicate in the existingIDs array
			for(int i = 0; i < existingIDs.length; i++) {
				if(id.equals(existingIDs[i])) {
					c = false;
					break;
				}
			}
			
			if(c) {
				b = true;
				break;
			}
		} while(!b);
		return id;
	}
	
	public String generateIntID(int length) throws SQLException {
		String id = "";
		boolean b = false; //true if ID generated is already unique
		//Vector<String> ids = new Vector<String>(1,1); //container of all existing IDs
		
		//generates the ID
		Random r = new Random();
		for(int i = 0; i < length; i++) {
			id = id + String.valueOf(r.nextInt(9));
		}
		
		return id;
	}
		
		//gets existing ids from ResultSet
		/*if(existingIDs == null) { //generic ID generation, no existing IDs given
			b = true;
		} else {
			while(existingIDs.next()) {
				ids.add(existingIDs.getString(Application.configuration.getDatabaseConfig().getSsidColName()));
			}
		}*/
		
		/*do {
			//generates the ID
			Random r = new Random();
			for(int i = 0; i < length; i++) {
				id = id + String.valueOf(r.nextInt(9));
			}
			
			//compares the generated ID to the existing ID's
			boolean c = true; //true if ID no longer has a duplicate in the existingIDs array
			for(int i = 0; i < ids.size(); i++) {
				if(id.equals(ids.get(i))) {
					c = false;
					break;
				}
			}
			
			if(c) {
				b = true;
				break;
			}
		} while(!b);
		return id;
	}*/
}
