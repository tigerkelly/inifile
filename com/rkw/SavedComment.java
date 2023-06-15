package com.rkw;

import java.util.LinkedHashMap;
import java.util.Map;

public class SavedComment {

	// Holds the name/value pairs in a LinkedHashMap which retains Order-of-Insertion.
	public Map<String, String> values = new LinkedHashMap<String, String>();
	
	public SavedComment() {
		
	}
	
	// add a name/value pair to the list
	public void add(String n, String v) {
		values.put(n, v);
	}
	
	// add a name/value pair to the list
	public void remove(String n) {
		values.remove(n);
	}
	
	// Retrieve value by key.
	public String get(String k) {
		return values.get(k);
	}
}
