package com.rkw;

import java.util.LinkedHashMap;
import java.util.Map;

public class IndexedValuePair {
	// Holds the name/value pairs in a LinkedHashMap which retains Order-of-Insertion.
	public Map<Integer, String> values = new LinkedHashMap<Integer, String>();
	private int idx = 0;
	
	public IndexedValuePair() {
		
	}
	
	// add a name/value pair to the list
	public void add(String v) {
		values.put(idx++, v);
	}
	
	// add a name/value pair to the list
	public void add(Integer k, String v) {
		values.put(k, v);
	}
	
	// add a name/value pair to the list
	public void remove(Integer n) {
		values.remove(n);
	}
	
	// Retrieve value by key.
	public String get(Integer n) {
		return values.get(n);
	}

	// Retrieve value by key.
	public Integer getNextIndex() {
		return idx;
	}
}
