/*
 * Created on Jan 31, 2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Copyright Kelly Wiles 2005-2009
 */

/*
 * License
 * You may use this code free of charge as long as this file contains the above copyright  notice and
 * it is for non-commercial use.  In other words if you include this code in your project and you 
 * charge money or a fee then you will need written permission from me Kelly Wiles at rkwiles@verizon.net
 * before you can sell or charge a fee for your software product.
 */

/*
 * This library reads a file with a .ini type syntax but it does not read all flavors of
 * .ini files.  This is why I use an extension of .ti instead.
 * My syntax is very simple, I have sections that contain key/value pairs.
 * The format is:
 * 
 * [Section1]
 *    key1=value1
 *    key2=100
 *    key3=true
 * 
 * A newline ends the value part of the key/value pair, keys can have spaces but I would not use them.
 * More than one section can exist but they must be named differently.
 * A section.key pair is a unique name, so Section1.key1=100 is different from Section2.key1=200
 * 
 * Examples:
 * 
 * IniFile iniFile = new IniFile("C:\\myFile.ti");
 * iniFile.addSection("Section1");  // creates section
 * or
 * iniFile.addSection("Section1", "key1", "value1"); // creates section and adds key/value
 * or 
 * iniFile.addSection("Section1");  // creates section
 * iniFile.addValuePair("Section1", key1", "value1"); // adds key/value to existing section.
 * 
 * iniFile.addValuePair("Section1", "key2", "100"); // all values are added as strings
 * iniFile.addValuePair("Section1", "key3", "true");
 * 
 * String str = iniFile.getSectionValueAsString("Section1", "key1");
 * int num = iniFile.getSectionValueAsInt("Section1", "key2");
 * double num = iniFile.getSectionValueAsDouble("Section1", "key2");
 * booelan flag = iniFile.getSectionValueAsBoolean("Section1", "key3");
 * 
 * Object[] secs = iniFile.getSectionNames();
 * for (int i = 0; i < secs.length; i++)
 *    System.out.println(secs[i]);
 *    
 * Object[] keys = iniFile.getSectionKeys(String sectionName);
 * 
 * iniFile.write(String topComment);  // save the key/value pairs.
 */
package com.rkw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to read and parse an .ti file.
 * Sections are defined by inclosing a word with [] brackets, ex. [section]
 * name and value pairs are formated like name = value, the equal sign separates name/value pairs
 * The .ti file will be looked for in jar files first then the current directory.
 */
public class IniFile {
	// Holds the .ti file in a LinkedHashMap which retains Order-of-Insertion.
	private Map<String, ValuePairMap> sections = new LinkedHashMap<String, ValuePairMap>();
	private Map<String, IndexedValuePair> idxSections = new LinkedHashMap<String, IndexedValuePair>();
	private Map<String, String> secComments = new LinkedHashMap<String, String>();
	private Map<String, String> kvComments = new LinkedHashMap<String, String>();
	private List<String> topComments = new ArrayList<String>();
	private String iniFileName = null;
	private boolean changed = false;
	private boolean noWrite = false;
	private String header = null;
	
	/**
	 * Set to true if file was found.
	 */
	public boolean fileFound = false;
	
	/**
	 * Constructor for the IniFile Class.
	 * @param fileName name of .ti type file to create or read.
	 * @param noWrite boolean if true then disable write to file.
	 */
	public IniFile(String fileName, boolean noWrite) {
		iniFileName = fileName;
		this.noWrite = noWrite;
		if (iniFileName != null)
			open(fileName);
	}
	
	/**
	 * Constructor for the IniFile Class.
	 * @param fileName name of .ti type file to create or read.
	 */
	public IniFile(String fileName) {
		iniFileName = fileName;
		this.noWrite = false;
		if (iniFileName != null)
			open(fileName);
	}
	
	/**
	 * Takes a string that has a .ini format and parses it.
	 * You can pass a null to the constructor and use this method to fill the list.
	 * @param iniData - ini formated string
	 */
	public void inputString(String fileName, String iniData) {
		String str;
		String[] arr = iniData.split("\n");
        ValuePairMap vpm = null;
        IndexedValuePair ivp = null;
        char endChar = '\0';
        boolean isIndexed = false;
        iniFileName = fileName;
        boolean isTop = true;
        
        // read each line from string.
        for (int i = 0; i < arr.length; i++) {
        	str = arr[i];
        	
        	str = str.trim();
        	
        	if (str == null || str.length() == 0)
        		continue;
        	
        	if (str.charAt(0) == '#' && isTop == true) {
        		topComments.add(str);
        		continue;
        	}
        	
        	isTop = false;
        	
        	int x = str.lastIndexOf(";");		// remove trailing comment from line.
        	if (x != -1)
        		str = str.substring(0, x);
        	
        	str = str.trim();					// remove whitespace from start and end
        	
        	if (str == null || str.length() == 0)
        		continue;
        	
        	if (str.charAt(0) == ';' || str.charAt(0) == '#') {
        		topComments.add(str);
        		continue;						// skip comments
        	}
        	
//        	System.out.println(str);
        	
        	if (str.charAt(0) == '[') {			// section found
        		isIndexed = false;
        		int e = str.indexOf(']');
        		
        		String key = str.substring(1, e);
//        		System.out.println("key " + key);
        		key = key.trim();
        		vpm = sections.get(key);
        		if( vpm == null) {
        			sections.put(key, new ValuePairMap());
        			vpm = sections.get(key);
        		}
        		
        		int j = str.indexOf(';');
        		if (j > 0) {
        			secComments.put(key, str.substring(j));
        		}
        	} else if (str.charAt(0) == '{') {			// indexed section found
        		isIndexed = true;
        		int e = str.indexOf('}');
        		String key = str.substring(1, (e - 1));
        		key = key.trim();
        		ivp = idxSections.get(key);
        		if( ivp == null) {
        			idxSections.put(key, new IndexedValuePair());
        			ivp = idxSections.get(key);
        		}
        		
        		int j = str.indexOf(';');
        		if (j > 0) {
        			secComments.put(key, str.substring(j));
        		}
        	} else {
        		x = str.indexOf('=');
        		
        		int j = str.indexOf(';');
        		
        		if (x != -1) {
        			String k = str.substring(0, x).trim();
        			if (j > 0) {
            			kvComments.put(k, str.substring(j));
            			str = str.substring(0, j).trim();		// remove comment.
            		}
        			
        			if (isIndexed == false) {
	        			String s = vpm.get(k);
	        			String v = str.substring((x + 1)).trim();
	        			
	        			if (v == null || v.length() <= 0) {
	        				vpm.add(k,  "");
	        				continue;
	        			}
	        			
	        			// if value starts with a quote then get rest of string value.
	        			if (v.charAt(0) == '"') {
	        				endChar = v.charAt(0);
	        				
	        				boolean endQuote = false;
	        				for (int z = 1; z < v.length(); z++) {
	        					if (v.charAt(z) == endChar) {
	        						endQuote = true;
	        						break;
	        					}
	        				}
	        				
	        				if (endQuote == false) {
	        					for (int kw = ++i; kw < arr.length; kw++) {
		        					str = arr[kw];
		        					
		        					for (int z = 0; z < str.length(); z++) {
		        						v += str.charAt(z);
		        						if (str.charAt(z) == endChar) {
		        							endQuote = true;
		        						}
		        					}
		        					if (endQuote == true)
		        						break;
	        					}
	        				}
	        			} else if (v.charAt(v.length() - 1) == '\\') {	// trailing backslash
	        				i++;
	        				for (; i < arr.length; i++) {
	        					str = arr[i];
	        		        	int x2 = str.lastIndexOf(";");		// remove trailing comment from line.
	        		        	if (x2 != -1)
	        		        		str = str.substring(0, x2);
	        		        	
	        		        	str = str.trim();					// remove whitespace from start and end
	        		        	
	        		        	if (str == null || str.length() == 0)
	        		        		continue;
	        		        	
	        		        	if (str.charAt(0) == ';')
	        		        		continue;						// skip comments
	        		        	
	        		        	if (str.charAt(str.length() - 1) != '\\') {
	        		        		v += " " + str;
	        		        		break;
	        		        	} else {
	        		        		v += " " + str.substring(0, str.length() - 1);
	        		        	}
	        				}
	        			}
	        			
	        			if (s == null) {
		        			// add name/value pair to hashed link list
		        			vpm.add(k, v);
	        			} else {
	        				// If key already exists then append value to existing value.
	        				// if you want a newline then place \n at end of 'desc =' value.
	        				s += v;
	        				vpm.add(k, s);
	        			}
//	        			System.out.println(str.substring(0, x).trim() + " " + str.substring((x + 1)).trim());
        			} else {
        				Integer idx = Integer.parseInt(k);
        				String s = ivp.get(idx);
	        			String v = str.substring((x + 1)).trim();
	        			
	        			if (v == null || v.length() <= 0) {
	        				ivp.add(idx,  "");
	        				continue;
	        			}
	        			
	        			// if value starts with a quote then get rest of string value.
	        			if (v.charAt(0) == '"') {
	        				endChar = v.charAt(0);
	        				
	        				boolean endQuote = false;
	        				for (int z = 1; z < v.length(); z++) {
	        					if (v.charAt(z) == endChar) {
	        						endQuote = true;
	        						break;
	        					}
	        				}
	        				
	        				if (endQuote == false) {
	        					for (int kw = ++i; kw < arr.length; kw++) {
		        					str = arr[kw];
		        					
		        					for (int z = 0; z < str.length(); z++) {
		        						v += str.charAt(z);
		        						if (str.charAt(z) == endChar) {
		        							endQuote = true;
		        						}
		        					}
		        					if (endQuote == true)
		        						break;
	        					}
	        				}
	        			}
	        			
	        			if (s == null) {
		        			// add name/value pair to hashed link list
		        			ivp.add(v);
	        			} else {
	        				// If key already exists then append value to existing value.
	        				// if you want a newline then place \n at end of 'desc =' value.
	        				s += v;
	        				ivp.add(idx, s);
	        			}
//	        			System.out.println(str.substring(0, x).trim() + " " + str.substring((x + 1)).trim());
        			}
        		}
        	}
        }
	}
	
	/**
	 * Returns the filename.
	 * @return String
	 */
	public String getFileName() {
		return iniFileName;
	}
	
	/**
	 * Used to change or supply the filename if null was passed to the constructor.
	 * @param fileName
	 */
	public void setFileName(String fileName) {
		iniFileName = fileName;
	}
	
	/**
	 * Returns the whether the list has changed or not.
	 * @return boolean
	 */
	public boolean getChangedFlag() {
		return changed;
	}
	
	/**
	 * Sets the changed flag.
	 * @param changed
	 */
	public void setChangedFlag(boolean changed) {
		this.changed = changed;
	}
	
	/**
	 * Rereads the file, discarding all changes.
	 */
	public void reread() {
		sections.clear();
		open(iniFileName);
		changed = false;
	}
	
	/**
	 * Clears all sections and key/value pairs.
	 */
	public void clearAll() {
		Iterator<String> it1 = sections.keySet().iterator();
        
    	while (it1.hasNext()) {
    		String sectionName = it1.next();
			ValuePairMap vpm = sections.get(sectionName);
			
			vpm.values.clear();
    	}
    	
    	sections.clear();
    	
    	Iterator<String> it2 = idxSections.keySet().iterator();
        
    	while (it2.hasNext()) {
    		String sectionName = it2.next();
			IndexedValuePair ivp = idxSections.get(sectionName);
			
			ivp.values.clear();
    	}
    	
    	idxSections.clear();
	}
	
	/**
	 * Open the file and parse contents.
	 * Searches jar files for the file first.
	 * @param fileName full path to file.
	 * @return false if successful.
	 */
	
	public boolean open(String fileName) {
		BufferedReader in = null;
		IndexedValuePair ivp = null;
        boolean isIndexed = false;
        boolean isTop = true;
		
		fileFound = false;
		
		try {
			// Checks to see if it is in a jar file first.
			// getResourceAsStream looks in all jar files in the Classpath.
			InputStream ins = this.getClass().getResourceAsStream("/" + fileName);
			if (ins == null) {
				// if not in a jar file than look in directory.
				File f = new File(fileName);
				if (f.exists() == true) {
					in = new BufferedReader(new FileReader(fileName));
					fileFound = true;
				} else {
					return true;
				}
			} else {
				// was found in a jar file, create a stream to it.
				InputStreamReader rd = new InputStreamReader(ins);
				in = new BufferedReader(rd);
				fileFound = true;
			}
			
	        String str;
	        char endChar = '\0';
	        ValuePairMap vpm = null;
	        boolean headerFlag = true;
	        
	        // read each line from file.
	        while ((str = in.readLine()) != null) {
	        	str = str.trim();
	        	
	        	if (str == null || str.length() == 0)
	        		continue;
	        	
	        	if (str.charAt(0) == '#' && isTop == true) {
	        		topComments.add(str);
	        		continue;
	        	}
	        	
	        	isTop = false;
	        	
	        	int x = str.lastIndexOf(";");		// remove trailing comment from line.
	        	if (x != -1)
	        		str = str.substring(0, x);
	        	
	        	str = str.trim();					// remove whitespace from start and end
	        	
	        	if (str == null || str.length() == 0)
	        		continue;
	        	
	        	if (str.charAt(0) == ';') {
	        		if (headerFlag) {
		        		if (header == null)
		        			header = str + "\n";
		        		else
		        			header += str + "\n";
	        		}
	        		continue;						// skip comments
	        	}
	        	
//	        	System.out.println(str);
	        	
	        	if (str.charAt(0) == '[') {			// section found
	        		headerFlag = false;
	        		String key = str.substring(1, (str.length() - 1));
	        		vpm = sections.get(key);
	        		if( vpm == null) {
	        			sections.put(key, new ValuePairMap());
	        			vpm = sections.get(key);
	        		}
	        	} else if (str.charAt(0) == '{') {			// indexed section found
	        		headerFlag = false;
	        		isIndexed = true;
	        		String key = str.substring(1, (str.length() - 1));
	        		ivp = idxSections.get(key);
	        		if( ivp == null) {
	        			idxSections.put(key, new IndexedValuePair());
	        			ivp = idxSections.get(key);
	        		}
	        	} else {
	        		headerFlag = false;
	        		x = str.indexOf('=');
	        		
	        		if (x != -1) {
	        			String k = str.substring(0, x).trim();
	        			if (k == null || k.length() <= 0)
	        				continue;
	        			
	        			if (isIndexed == false) {
		        			String s = vpm.get(k);
		        			String v = str.substring((x + 1)).trim();
		        			
		        			if (v == null || v.length() <= 0) {
		        				vpm.add(k,  "");
		        				continue;
		        			}
		        			
		        			// if value starts with a quote then get rest of string value.
		        			if (v.charAt(0) == '"') {
		        				endChar = v.charAt(0);
		        				
		        				boolean endQuote = false;
		        				for (int z = 1; z < v.length(); z++) {
		        					if (v.charAt(z) == endChar) {
		        						endQuote = true;
		        						break;
		        					}
		        				}
		        				
		        				if (endQuote == false) {
			        				int ch;
			        				while ((ch = in.read()) != -1) {
			        					v += ch;
			        					if (ch == endChar)
			        						break;
			        				}
		        				}
		        			}
		        			
		        			if (s == null) {
			        			// add name/value pair to hashed link list
			        			vpm.add(k, v);
		        			} else {
		        				// If key already exists then append value to existing value.
		        				// if you want a newline then place \n at end of 'desc =' value.
		        				s += v;
		        				vpm.add(k, s);
		        			}
//		        			System.out.println(str.substring(0, x).trim() + " " + str.substring((x + 1)).trim());
	        			} else {
	        				Integer idx = Integer.parseInt(k);
	        				String s = ivp.get(idx);
		        			String v = str.substring((x + 1)).trim();
		        			
		        			if (v == null || v.length() <= 0) {
		        				ivp.add(idx,  "");
		        				continue;
		        			}
		        			
		        			// if value starts with a quote then get rest of string value.
		        			if (v.charAt(0) == '"') {
		        				endChar = v.charAt(0);
		        				
		        				boolean endQuote = false;
		        				for (int z = 1; z < v.length(); z++) {
		        					if (v.charAt(z) == endChar) {
		        						endQuote = true;
		        						break;
		        					}
		        				}
		        				
		        				if (endQuote == false) {
			        				int ch;
			        				while ((ch = in.read()) != -1) {
			        					v += ch;
			        					if (ch == endChar)
			        						break;
			        				}
		        				}
		        			}
		        			
		        			if (s == null) {
			        			// add name/value pair to hashed link list
			        			ivp.add(v);
		        			} else {
		        				// If key already exists then append value to existing value.
		        				// if you want a newline then place \n at end of 'desc =' value.
		        				s += v;
		        				ivp.add(idx, s);
		        			}
//		        			System.out.println(str.substring(0, x).trim() + " " + str.substring((x + 1)).trim());
	        			}
	        		}
	        	}
	        }
	        in.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return true;
	    }
	    
	    return false;
	}
	
	/**
	 * Adds a key/value pair to an existing section or creates the section if it
	 * does not exist.
	 * @param sec section name
	 * @param key key within section
	 * @param value value of the key
	 */
	public void addSection(String sec, String key, String value) {
		if (sections.containsKey(sec) == true) {
			ValuePairMap vpm = sections.get(sec);
			vpm.add(key, value);
		} else {
			ValuePairMap vpm = new ValuePairMap();
			vpm.add(key, value);
			sections.put(sec, vpm);
		}
		
		changed = true;
	}
	
	/**
	 * Adds a indexed value to an existing indexed section or creates the section if it
	 * does not exist.
	 * @param sec section name
	 * @param value value of the index
	 */
	public void addIndexedSection(String sec, String value) {
		if (idxSections.containsKey(sec) == true) {
			IndexedValuePair ivp = idxSections.get(sec);
			ivp.add(value);
		} else {
			IndexedValuePair ivp = new IndexedValuePair();
			ivp.add(value);
			idxSections.put(sec, ivp);
		}
		
		changed = true;
	}
	
	/**
	 * Adds a new section with no key/value pairs.
	 * @param sec section name
	 * @return true if already exists, true if already exists.
	 */
	public boolean addSection(String sec) {
		if (sections.containsKey(sec) == false) {
			sections.put(sec, new ValuePairMap());
			changed = true;
		} else {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Adds a new section with no key/value pairs.
	 * @param sec section name
	 * @return true if already exists, true if already exists.
	 */
	public boolean addIndexedSection(String sec) {
		if (idxSections.containsKey(sec) == false) {
			idxSections.put(sec, new IndexedValuePair());
			changed = true;
		} else {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Renames a section.
	 * @param from from section name
	 * @param to to section name
	 * @return false if successful, true if to name exists
	 */
	public boolean renameSection(String from, String to) {
		if (sections.containsKey(to) == true)
			return true;		// section already exists.
		
		if (sections.containsKey(from) == true) {
			ValuePairMap vpm = sections.get(from);
			sections.put(to, vpm);
			sections.remove(from);
			changed = true;
		}
		
		return false;
	}
	
	/**
	 * Renames a section.
	 * @param from from section name
	 * @param to to section name
	 * @return false if successful, true if to name exists
	 */
	public boolean renameIndexedSection(String from, String to) {
		if (idxSections.containsKey(to) == true)
			return true;		// section already exists.
		
		if (idxSections.containsKey(from) == true) {
			IndexedValuePair ivp = idxSections.get(from);
			idxSections.put(to, ivp);
			idxSections.remove(from);
			changed = true;
		}
		
		return false;
	}
	
	/**
	 * Moves the key/value pair to top of the linked list.
	 * @param sec section name.
	 * @param key key name.
	 */
	public void moveToTopValuePair(String sec, String key) {
		moveToValuePair(sec, key, true);
	}
	
	/**
	 * Moves the key/value pair to bottom of linked list.
	 * @param sec section name.
	 * @param key key name.
	 */
	public void moveToBottomValuePair(String sec, String key) {
		moveToValuePair(sec, key, false);
	}
	
	private void moveToValuePair(String sec, String key, boolean top) {
		java.util.List<String> a = new LinkedList<String>();
		
		ValuePairMap vpm = sections.get(sec);
		if (vpm != null) {
			Iterator<String> it = vpm.values.keySet().iterator();
	    	while (it.hasNext()) {
		        String key2 = it.next();
		        if (key2.equals(key) == false) {
		        	a.add(key2);
		        }
	    	}
	    	
	    	if (top == true) {
		    	a.add(0, key);
	    	} else {
		    	a.add(key);
	    	}
	    	ValuePairMap vpm2 = new ValuePairMap();
	    	for (int x = 0; x < a.size(); x++) {
	    		vpm2.add(a.get(x), vpm.values.get(a.get(x)));
	    	}
	    	
	    	sections.remove(sec);
	    	
	    	vpm = new ValuePairMap();
	    	sections.put(sec, vpm);
	    	
	    	it = vpm2.values.keySet().iterator();
	    	while (it.hasNext()) {
	    		String value = vpm2.values.get(key);
				vpm.add(it.next(), value);
	    	}
	    	
			changed = true;
		}
	}
	
	/**
	 * Moves the key/value pair one place up in the linked list if it can.
	 * @param sec section name.
	 * @param key key name.
	 */
	public void moveUpValuePair(String sec, String key) {
		moveValuePair(sec, key, false);
	}
	
	/**
	 * Moves the key/value pair one place down in the linked list if it can.
	 * @param sec section name.
	 * @param key key name.
	 */
	public void moveDownValuePair(String sec, String key) {
		moveValuePair(sec, key, true);
	}
	
	private void moveValuePair(String sec, String key, boolean down) {
		java.util.List<String> a = new LinkedList<String>();
		
		ValuePairMap vpm = sections.get(sec);
		if (vpm != null) {
//			String value = vpm.values.get(key);
			
			int idx = 0;
			int i = 0;
			Iterator<String> it = vpm.values.keySet().iterator();
	    	while (it.hasNext()) {
		        String key2 = it.next();
		        if (key2.equals(key) == true) {
		        	idx = i;
		        } else {
		        	i++;
		        	a.add(key2);
		        }
	    	}
	    	
	    	if (down == true) {
		    	idx++;
		    	if (idx < vpm.values.size())
		    		a.add(idx, key);
		    	else
		    		a.add(key);
	    	} else {
	    		idx--;
		    	a.add(idx, key);
	    	}
	    	ValuePairMap vpm2 = new ValuePairMap();
	    	for (int x = 0; x < a.size(); x++) {
	    		vpm2.add(a.get(x), vpm.values.get(a.get(x)));
	    	}
	    	
	    	sections.remove(sec);
	    	
	    	vpm = new ValuePairMap();
	    	sections.put(sec, vpm);
	    	
	    	it = vpm2.values.keySet().iterator();
	    	while (it.hasNext()) {
	    		String value = vpm2.values.get(key);
				vpm.add(it.next(), value);
	    	}
	    	
			changed = true;
		}
	}
	
	/**
	 * Adds a key/value pair to the an existing section, if section does
	 * not exist the key/value is NOT added.
	 * @param sec section name
	 * @param key key within section
	 * @param value value of the key
	 * @return true if section does not exist, else false;
	 */
	public boolean addValuePair(String sec, String key, String value) {
//		System.out.println(sec + ", " + key + ", " + value);
		ValuePairMap vpm = sections.get(sec);
		if (vpm != null) {
			vpm.add(key, value);
			changed = true;
			
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Adds a key/value pair to the an existing section, if section does
	 * not exist the key/value is NOT added.
	 * @param sec section name
	 * @param value value of the key
	 * @return true if section does not exist, else false;
	 */
	public boolean addIndexedValue(String sec, String value) {
//		System.out.println(sec + ", " + key + ", " + value);
		IndexedValuePair ivp = idxSections.get(sec);
		if (ivp != null) {
			ivp.add(value);
			changed = true;
			
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Appends a key/value to an existing section, if the key/value does
	 * not exist then it is created.
	 * @param sec section name
	 * @param key key within section
	 * @param value value of the key
	 * @return true if section does not exist, else false.
	 */
	public boolean appendValuePair(String sec, String key, String value) {
		ValuePairMap vpm = sections.get(sec);
		if (vpm == null)
			return true;
		String s = vpm.get(key);
		
		if (s == null) {
			vpm.add(key, value);
		} else {
			s += " " + value;
			vpm.add(key, s);
		}
		
		changed = true;
		
		return false;
	}
	
	/**
	 * Appends a key/value to an existing section, if the key/value does
	 * not exist then it is created.
	 * @param sec section name
	 * @param key key within section
	 * @param value value of the key
	 * @return true if section does not exist, else false.
	 */
	public boolean appendIndexedValue(String sec, Integer key, String value) {
		IndexedValuePair ivp = idxSections.get(sec);
		if (ivp == null)
			return true;
		String s = ivp.get(key);
		
		if (s == null) {
			ivp.add(key, value);
		} else {
			s += " " + value;
			ivp.add(key, s);
		}
		
		changed = true;
		
		return false;
	}
	
	/**
	 * Removes an existing section and all of it's key/value pairs.
	 * @param sec section name to remove
	 * @return true if section does not exist.
	 */
	public boolean removeSection(String sec) {
		if (sections.remove(sec) == null)
			return true;
		changed = true;
		
		return false;
	}
	
	/**
	 * Removes an existing section and all of it's key/value pairs.
	 * @param sec section name to remove
	 * @return true if section does not exist.
	 */
	public boolean removeIndexedSection(String sec) {
		if (idxSections.remove(sec) == null)
			return true;
		changed = true;
		
		return false;
	}
	
	/**
	 * Removes an existing key/value pair from an existing section.
	 * @param sec section name
	 * @param key key within section
	 * @return true if section does not exist or key removal fails.
	 */
	public boolean removeValuePair(String sec, String key) {
		ValuePairMap vpm = sections.get(sec);
		if (vpm != null) {
			if (vpm.values.remove(key) == null)
				return true;
			changed = true;
			
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Removes an existing key/value pair from an existing section.
	 * @param sec section name
	 * @param key key within section
	 * @return true if section does not exist or key removal fails.
	 */
	public boolean removeIndexedValue(String sec, Integer key) {
		IndexedValuePair ivp = idxSections.get(sec);
		if (ivp != null) {
			if (ivp.values.remove(key) == null)
				return true;
			changed = true;
			
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Rename key of a ValuePair
	 * @param sec section name
	 * @param from from value key
	 * @param to to value key
	 * @return true if section does not exist.
	 */
	public boolean renameValuePair(String sec, String from, String to) {
		ValuePairMap vpm = sections.get(sec);
		if (vpm.get(from) == null)
			return true;
		String value = vpm.get(from);
		if (value == null)
			return true;
		String newValue = null;
		String[] a = value.split(",");
		if (a.length == 2)
			newValue = a[0] + "," + a[1] + "," + from;
		else
			newValue = value;
		vpm.add(to, newValue);
		vpm.remove(from);
		
		return false;
	}
	
	/**
	 * Returns the number of sections in file.
	 * @return int number of sections in a file.
	 */
	public int getSectionCount() {
		return sections.size() + idxSections.size();
	}
	
	/**
	 * returns the number of total key/values pairs by adding all sections together.
	 * @return int total key/value pair count
	 */
	public int getValueCount() {
		int count = 0;
		
		Iterator<String> it = sections.keySet().iterator();
	    while (it.hasNext()) {
	        String key = it.next();
	        
	        if (key != null)
	        	count += getSectionValueCount(key);
	    }
	    
	    it = idxSections.keySet().iterator();
	    while (it.hasNext()) {
	        String key = it.next();
	        
	        if (key != null)
	        	count += getSectionValueCount(key);
	    }
	    
	    return count;
	}
	
	/**
	 * Returns key/value pair count of a section
	 * @param sectionName section name
	 * @return int count of key/value pairs in a section.
	 */
	public int getSectionValueCount(Object sectionName) {
		int count = 0;
		
		if (sections.containsKey(sectionName)) {
			ValuePairMap vmp = sections.get(sectionName);
			count = vmp.values.size();
		}
		
		return count;
	}
	
	/**
	 * Returns index/value pair count of a section
	 * @param sectionName section name
	 * @return int count of key/value pairs in a section.
	 */
	public int getIndexedSectionValueCount(Object sectionName) {
		int count = 0;
		
		if (idxSections.containsKey(sectionName)) {
			IndexedValuePair ivp = idxSections.get(sectionName);
			count = ivp.values.size();
		}
		
		return count;
	}
	
	/**
	 * Returns the value of a key/value pair as a string.
	 * Same as getSectionValueAsString().
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @param flag whether to expand variables or not.
	 * @return String value
	 */
	public String getString(Object sectionName, Object keyName, boolean flag) {
		return getSectionValueAsString(sectionName, keyName, flag);
	}
	
	/**
	 * Returns the value of a key/value pair as a string but does not
	 * expand any variables.  Same as getSectionValueAsString().
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return String value
	 */
	public String getString(Object sectionName, Object keyName) {
		return getSectionValueAsString(sectionName, keyName, false);
	}
	
	/**
	 * Returns the value of a key/value pair as a string but does not
	 * expand any variables.
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return String value
	 */
	public String getSectionValueAsString(Object sectionName, Object keyName) {
		return getSectionValueAsString(sectionName, keyName, false);
	}
	
	/**
	 * Returns the value of a key/value pair as a string.
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @param flag whether to expand variables or not.
	 * @return String value
	 */
	public String getSectionValueAsString(Object sectionName, Object keyName, boolean flag) {
		String value = null;
		
		if (keyName instanceof String) {
			if (sections.containsKey(sectionName)) {
				ValuePairMap vpm = sections.get(sectionName);
				if (vpm != null) {
					value = vpm.values.get(keyName);
					if (flag == true)
						value = expandVariables(value);
				}
			}
		} else if (keyName instanceof Integer) {
			if (idxSections.containsKey(sectionName)) {
				IndexedValuePair ivp = idxSections.get(sectionName);
				if (ivp != null) {
					value = ivp.values.get(keyName);
					if (flag == true)
						value = expandVariables(value);
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Returns a value of a key/value pairs as an integer
	 * Same as getSectionValueAsInt()
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return int value
	 */
	public int getInt(Object sectionName, Object keyName) {
		return getSectionValueAsInt(sectionName, keyName);
	}
	
	/**
	 * Returns a value of a key/value pairs as an double
	 * Same as getSectionValueAsDouble()
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return double value
	 */
	public double getDouble(Object sectionName, Object keyName) {
		return getSectionValueAsDouble(sectionName, keyName);
	}
	
	/**
	 * Returns a value of a key/value pairs as an integer
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return int value
	 */
	public int getSectionValueAsInt(Object sectionName, Object keyName) {
		int value = -1;
		
		if (keyName instanceof String) {
			if (sections.containsKey(sectionName)) {
				ValuePairMap vpm = sections.get(sectionName);
				if (vpm != null) {
					try {
						value = Integer.parseInt(vpm.values.get(keyName));
					} catch (NumberFormatException nfe) {
						value = 0;
					}
				}
			}
		} else if (keyName instanceof Integer) {
			if (idxSections.containsKey(sectionName)) {
				IndexedValuePair ivp = idxSections.get(sectionName);
				if (ivp != null) {
					try {
						value = Integer.parseInt(ivp.values.get(keyName));
					} catch (NumberFormatException nfe) {
						value = 0;
					}
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Returns a value of a key/value pairs as an integer
	 * @param sectionName section name
	 * @param keyName key name within section
	 * @return double value
	 */
	public double getSectionValueAsDouble(Object sectionName, Object keyName) {
		double value = -1;
		
		if (keyName instanceof String) {
			if (sections.containsKey(sectionName)) {
				ValuePairMap vpm = sections.get(sectionName);
				if (vpm != null) {
					try {
						value = Double.parseDouble(vpm.values.get(keyName));
					} catch (NumberFormatException nfe) {
						value = 0;
					}
				}
			}
		} else if (keyName instanceof Double) {
			if (idxSections.containsKey(sectionName)) {
				IndexedValuePair ivp = idxSections.get(sectionName);
				if (ivp != null) {
					try {
						value = Double.parseDouble(ivp.values.get(keyName));
					} catch (NumberFormatException nfe) {
						value = 0;
					}
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Gets value as a boolean value.
	 * Same as getSectionValueAsBoolean().
	 * @param sectionName
	 * @param keyName
	 * @return false if section or key/value does not exist.
	 */
	public boolean getBoolean(Object sectionName, Object keyName) {
		boolean value = false;
		
		if (sections.containsKey(sectionName)) {
			ValuePairMap vpm = sections.get(sectionName);
			if (vpm != null) {
				value = Boolean.parseBoolean(vpm.values.get(keyName));
			}
		}
		
		return value;
	}
	
	/**
	 * Gets value as a boolean value.
	 * @param sectionName
	 * @param keyName
	 * @return false if section or key/value does not exist.
	 */
	public boolean getSectionValueAsBoolean(Object sectionName, Object keyName) {
		boolean value = false;
		
		if (keyName instanceof String) {
			if (sections.containsKey(sectionName)) {
				ValuePairMap vpm = sections.get(sectionName);
				if (vpm != null) {
					value = Boolean.parseBoolean(vpm.values.get(keyName));
				}
			}
		} else if (keyName instanceof Integer) {
			if (idxSections.containsKey(sectionName)) {
				IndexedValuePair ivp = idxSections.get(sectionName);
				if (ivp != null) {
					value = Boolean.parseBoolean(ivp.values.get(keyName));
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Return the linked list of a section.
	 * @param sectionName
	 * @return ValuePairMap
	 */
	public ValuePairMap getSectionValues(Object sectionName) {
		return sections.get(sectionName);
	}
	
	/**
	 * Return the linked list of a indexed section.
	 * @param sectionName
	 * @return IndexedValuePair
	 */
	public IndexedValuePair getIndexedSectionValues(Object sectionName) {
		return idxSections.get(sectionName);
	}
	
	/**
	 * Get an array of all section names.
	 * @return an array of section names as an array of Objects.
	 */
	public Object[] getSectionNames() {
		ArrayList<String> names = new ArrayList<String>();
		
		Iterator<String> it = sections.keySet().iterator();
	    while (it.hasNext()) {
	        String key = it.next();
	        
	        if (key != null)
	        	names.add(key);
	    }
	    
	    return names.toArray();
	}
	
	/**
	 * Get an array of all indexed section names.
	 * @return an array of section names as an array of Objects.
	 */
	public Object[] getIndexedSectionNames() {
		ArrayList<String> names = new ArrayList<String>();
		
		Iterator<String> it = idxSections.keySet().iterator();
	    while (it.hasNext()) {
	        String key = it.next();
	        
	        if (key != null)
	        	names.add(key);
	    }
	    
	    return names.toArray();
	}
	
	/**
	 * Returns an array of key names in the section.
	 * @param sectionName
	 * @return Object[]
	 */
	public Object[] getSectionKeys(Object sectionName) {
		ArrayList<String> keys = new ArrayList<String>();
		
		ValuePairMap vpm = sections.get(sectionName);
		
		if (vpm == null)
			return null;
	        	
    	Iterator<String> it = vpm.values.keySet().iterator();
    	while (it.hasNext()) {
	        String key2 = it.next();
	        
	        keys.add(key2);
    	}
	    
	    return keys.toArray();
	}
	
	/**
	 * Returns an array of indexes in the indexed section.
	 * @param sectionName
	 * @return Object[]
	 */
	public Object[] getIndexedSectionKeys(Object sectionName) {
		ArrayList<Integer> keys = new ArrayList<Integer>();
		
		IndexedValuePair ivp = idxSections.get(sectionName);
		
		if (ivp == null)
			return null;
	        	
    	Iterator<Integer> it = ivp.values.keySet().iterator();
    	while (it.hasNext()) {
	        Integer key2 = it.next();
	        
	        keys.add(key2);
    	}
	    
	    return keys.toArray();
	}
	
	/**
	 * Checks if section exists.
	 * @param sectionName
	 * @return true if section exists.
	 */
	public boolean sectionExists(Object sectionName) {
		return sections.containsKey(sectionName);
	}
	
	/**
	 * Checks if indexed section exists.
	 * @param sectionName
	 * @return true if section exists.
	 */
	public boolean indexedSectionExists(Object sectionName) {
		return idxSections.containsKey(sectionName);
	}
	
	/**
	 * Checks if a key exists in given section.
	 * @param sectionName
	 * @param key
	 * @return true if key exists.
	 */
	public boolean keyExists(Object sectionName, Object key) {
		boolean flag = false;
		
		if (sections.containsKey(sectionName)) {
			ValuePairMap vpm = sections.get(sectionName);
			if (vpm.values.containsKey(key))
				flag = true;
		}
		return flag;
	}
	
	/**
	 * Checks if a index exists in given indexed section.
	 * @param sectionName
	 * @param key
	 * @return true if key exists.
	 */
	public boolean indexedKeyExists(Object sectionName, Object key) {
		boolean flag = false;
		
		if (idxSections.containsKey(sectionName)) {
			IndexedValuePair ivp = idxSections.get(sectionName);
			if (ivp.values.containsKey(key))
				flag = true;
		}
		return flag;
	}
	
	public boolean writeFile() {
		if (header != null)
			return writeFile(header, false);
		else
			return writeFile("; No comment found.", false);
	}
	
	public boolean writeFile(boolean forceWrite) {
		if (header != null)
			return writeFile(header, forceWrite);
		else
			return writeFile("; No comment found.", forceWrite);
	}
	
	public boolean writeFile(String topSection) {
		return writeFile(topSection, false);
	}
	
	/**
	 * Writes the complete sections to the filename given when you created class.
	 * @param topSection - string comment to place at top of file.
	 * @param forceWrite - ignore 'changed' flag if true.
	 * @return true if save failed.
	 */
	public boolean writeFile(String topSection, boolean forceWrite) {
		if (iniFileName == null || noWrite == true)
			return true;
		
		if (forceWrite == false && changed == false)
			return true;
		
		changed = false;
		
		try {
	        BufferedWriter out = new BufferedWriter(new FileWriter(iniFileName));
	        
	        if (topComments.size() > 0) {
	        	for (String s : topComments)
	        		out.write(s + "\n");
	        }
	        
	        Iterator<String> it1 = sections.keySet().iterator();
	        
	    	while (it1.hasNext()) {
	    		String sectionName = it1.next();
	    		if (sectionName.endsWith("-NoWrite") == true)
	    			continue;
				ValuePairMap vpm = sections.get(sectionName);
				
				if (vpm == null)
					break;
				
				out.write("\n[" + sectionName + "]\n");
			        	
		    	Iterator<String> it2 = vpm.values.keySet().iterator();
		    	while (it2.hasNext()) {
			        String key = it2.next();
			        String value = vpm.values.get(key);
			        
			        if (value == null) {
//			        	System.out.println("key " + key);
			        	out.close();
			        	return false;
			        }
			        
		        	String[] s = value.split(System.getProperty("line.separator"));
		        	if (s.length > 0) {
			        	for (int x = 0; x < s.length; x++) {
			        		out.write("\t" + key + " = " + s[x]);
			        		if ((x + 1) < s.length)
			        			out.write("\\n\n");
			        		else
			        			out.write("\n");
			        	}
		        	} else {
		        		out.write("\t" + key + " = " + value + "\n");
		        	}
		    	}
	    	}
	    	
	    	Iterator<String> it2 = idxSections.keySet().iterator();
	        
	    	while (it2.hasNext()) {
	    		String sectionName = it2.next();
	    		if (sectionName.endsWith("-NoWrite") == true)
	    			continue;
				IndexedValuePair ivp = idxSections.get(sectionName);
				
				if (ivp == null)
					break;
				
				out.write("\n{" + sectionName + "}\n");
			        	
		    	Iterator<Integer> it3 = ivp.values.keySet().iterator();
		    	while (it3.hasNext()) {
			        Integer key = it3.next();
			        String value = ivp.values.get(key);
			        
		        	String[] s = value.split(System.getProperty("line.separator"));
		        	if (s.length > 0) {
			        	for (int x = 0; x < s.length; x++) {
			        		out.write("\t" + key + " = " + s[x]);
			        		if ((x + 1) < s.length)
			        			out.write("\\n\n");
			        		else
			        			out.write("\n");
			        	}
		        	} else {
		        		out.write("\t" + key + " = " + value + "\n");
		        	}
		    	}
	    	}
	        out.close();
	        
	        // Use this code to write file into current Eclipse project workspace.
	        // This also notifies Eclipse that a file has been added/updated.
//	        try {
//				IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		        IPath location = Path.fromOSString(iniFileName);
//		        IFile iFile = workspace.getRoot().getFileForLocation(location);
//		        iFile.refreshLocal(IResource.DEPTH_ZERO, null);
//	        } catch (CoreException ex) {
//				ex.printStackTrace();
//			}
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return true;
	    }
	    
	    return false;
	}
	
	/**
	 * Creates a string of the file to write.
	 * Does not write to file, only creates string.
	 * @param topSection
	 * @return String
	 */
	public String stringFile(String topSection) {
		String str = "";
		if (iniFileName == null)
			return null;
        
        if (topComments.size() > 0) {
        	for (String s : topComments)
        		str += s + "\n";
        }
        
        Iterator<String> it1 = sections.keySet().iterator();
        
    	while (it1.hasNext()) {
    		String sectionName = it1.next();
    		if (sectionName.endsWith("-NoWrite") == true)
    			continue;
			ValuePairMap vpm = sections.get(sectionName);
			
			if (vpm == null)
				break;
			
			str += "\n[" + sectionName + "]\n";
		        	
	    	Iterator<String> it2 = vpm.values.keySet().iterator();
	    	while (it2.hasNext()) {
		        String key = it2.next();
		        String value = vpm.values.get(key);
		        if (value == null)
		        	continue;
		        
	        	String[] s = value.split(System.getProperty("line.separator"));
	        	if (s.length > 0) {
		        	for (int x = 0; x < s.length; x++) {
		        		str += "\t" + key + " = " + s[x];
		        		if ((x + 1) < s.length)
		        			str += "\\n\n";
		        		else
		        			str += "\n";
		        	}
	        	} else {
	        		str += "\t" + key + " = " + value + "\n";
	        	}
	    	}
    	}
    	
    	Iterator<String> it2 = sections.keySet().iterator();
        
    	while (it2.hasNext()) {
    		String sectionName = it2.next();
    		if (sectionName.endsWith("-NoWrite") == true)
    			continue;
			IndexedValuePair ivp = idxSections.get(sectionName);
			
			if (ivp == null)
				break;
			
			str += "\n{" + sectionName + "}\n";
		        	
	    	Iterator<Integer> it3 = ivp.values.keySet().iterator();
	    	while (it3.hasNext()) {
		        Integer key = it3.next();
		        String value = ivp.values.get(key);
		        
	        	String[] s = value.split(System.getProperty("line.separator"));
	        	if (s.length > 0) {
		        	for (int x = 0; x < s.length; x++) {
		        		str += "\t" + key + " = " + s[x];
		        		if ((x + 1) < s.length)
		        			str += "\\n\n";
		        		else
		        			str += "\n";
		        	}
	        	} else {
	        		str += "\t" + key + " = " + value + "\n";
	        	}
	    	}
    	}
	    
	    return str;
	}
	
	/**
	 * Prints the name/value pairs for a section.
	 * @param sectionName
	 * @return true if section does not exist.
	 */
	public boolean printSection(Object sectionName) {
		
		ValuePairMap vpm = sections.get(sectionName);
		
		if (vpm == null)
			return true;
	        	
    	Iterator<String> it = vpm.values.keySet().iterator();
    	while (it.hasNext()) {
	        String key = it.next();
	        String value = vpm.values.get(key);
	        System.out.println(sectionName + "->" + key + " = " + value);
    	}
    	
    	return false;
	}
	
	/**
	 * Prints the name/value pairs for a section.
	 * @param sectionName
	 * @return true if section does not exist.
	 */
	public boolean printIndexedSection(Object sectionName) {
		
		IndexedValuePair ivp = idxSections.get(sectionName);
		
		if (ivp == null)
			return true;
	        	
    	Iterator<Integer> it = ivp.values.keySet().iterator();
    	while (it.hasNext()) {
	        Integer key = it.next();
	        String value = ivp.values.get(key);
	        System.out.println(sectionName + "->" + key + " = " + value);
    	}
    	
    	return false;
	}
	
	/**
	 * Prints all of the sections.
	 */
	@Override
	public String toString() {
		String s = null;
		
		if (iniFileName == null)
			return s;
		
		s = iniFileName;
		
		Iterator<String> it1 = sections.keySet().iterator();
    	while (it1.hasNext()) {
    		String sectionName = it1.next();
			ValuePairMap vpm = sections.get(sectionName);
			
			if (vpm == null)
				return s;
		        	
	    	Iterator<String> it2 = vpm.values.keySet().iterator();
	    	while (it2.hasNext()) {
		        String key = it2.next();
		        String value = vpm.values.get(key);
		        
		        s += "\n" + sectionName + "->" + key + " = " + value;
	    	}
    	}
    	
    	Iterator<String> it2 = sections.keySet().iterator();
    	while (it2.hasNext()) {
    		String sectionName = it2.next();
			IndexedValuePair ivp = idxSections.get(sectionName);
			
			if (ivp == null)
				return s;
		        	
	    	Iterator<Integer> it3 = ivp.values.keySet().iterator();
	    	while (it3.hasNext()) {
		        Integer key = it3.next();
		        String value = ivp.values.get(key);
		        
		        s += "\n" + sectionName + "->" + key + " = " + value;
	    	}
    	}
    	
		return s;
	}
	
	// If a string contains a variable like (var) then this method searches
	// all key/values for a match, if found then value replaces (var).
	private String expandVariables(String s) {
		String newString = null;
		
		// variables look like (var) with open and close params.
		String regEx = "\\([A-Za-z_0-9]*\\)";
		Pattern regPat = Pattern.compile(regEx);	// CASE SENSITIVE
		Matcher m = regPat.matcher(s);
		StringBuffer newBuf = new StringBuffer();
		
		while (m.find()) {
			String str = m.group();
			String var = str.substring(1, str.length() -1);
			String v = null;
			boolean found = false;
			
			Iterator<String> it = sections.keySet().iterator();
		    while (it.hasNext()) {
		        String key = it.next();
		        
		        if (key != null) {
		        	ValuePairMap vpm = sections.get(key);
		        	if (vpm != null) {
		        		if (vpm.values.containsKey(var) == true) {
			        		v = vpm.values.get(var);
			        		found = true;
			        		break;
		        		}
		        	}
		        }
		    }
		    if (found == false) {
		    	Iterator<String> it2 = idxSections.keySet().iterator();
		    	Integer vari = Integer.parseInt(var);
			    while (it2.hasNext()) {
			        String key = it2.next();
			        
			        if (key != null) {
			        	IndexedValuePair ivp = idxSections.get(key);
			        	if (ivp != null) {
			        		if (ivp.values.containsKey(vari) == true) {
				        		v = ivp.values.get(vari);
				        		found = true;
				        		break;
			        		}
			        	}
			        }
			    }
		    }
//			System.out.println("v: " + v);
			if (v != null) {
				m.appendReplacement(newBuf, v);
			} else {
				m.appendReplacement(newBuf, "");
			}
		}
		m.appendTail(newBuf);
		
		newString = newBuf.toString();
		
		return newString;
	}
}
