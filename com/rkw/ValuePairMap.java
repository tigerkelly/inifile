/*
 * Created on Jan 31, 2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Copyright Kelly Wiles 2005-2009
 */
package com.rkw;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * All name/value pairs are stored in this class.
 * Used by the iniFile class.
 */
public class ValuePairMap {
	// Holds the name/value pairs in a LinkedHashMap which retains Order-of-Insertion.
	public Map<String, String> values = new LinkedHashMap<String, String>();
	
	public ValuePairMap() {
		
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
