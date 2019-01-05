/*

Copyright 2008, Rob Minson (rzm@cs.bham.ac.uk)

School of Computer Science
University of Birmingham
Edgbaston
B152TT
United Kingdom

This file is part of HLA_RePast.

    HLA_RePast is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HLA_RePast is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HLA_RePast.  If not, see <http://www.gnu.org/licenses/>.

*/
/*
 * Created on 24-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package manager;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;
import io.DEV_TOOLS;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Hashtable;

import object.PublicObject;
import rtidep.Threads;

/**
 * Translates class names and class.attribute names to integer handles
 * as defined by the RTI for this federation execution.
 * 
 * This could be done by calling the RTI every time, but since handles
 * never change through a federation, the result is cached instead..
 * 
 * The datastructure must be loaded with classes, each one of which 
 * must be associated with a name and a list of variable names which 
 * match the info in the .fed file used for this simulation.
 * 
 * Once {@link #resolveNames(RTIambassador)} is called the ClassLookup
 * is now useable.
 * 
 * @author Rob Minson
 */
public class ClassLookup {

	private Hashtable<Integer, ClassTranslator> handleToClass = 
		new Hashtable<Integer, ClassTranslator>();
	private Hashtable<Class, ClassTranslator> classToHandle = 
		new Hashtable<Class, ClassTranslator>();
	
	private boolean isResolved;

	/**
	*   Construct an emtpy, un-resolved ClassLookup
	*/
	public ClassLookup() {
	
		super();
		this.isResolved = false;
	}
	
	public ClassLookup(Class[] classes) throws RTIexception {
		
		this();
		for (int i = 0; i < classes.length; i++) {
			this.addClass(classes[i]);
		}
		this.resolveNames();
	}

	/**
	*   Adds a Class to the Lookup along with the HLAName for the Class
	*   as it is recorded in a .fed file which will be used to run the 
	*   federation for which this ClassLookup is valid
	*   @param c the Class to add
	*   @param HLAName the name of the class in the .fed file
	*/
	public void addClass(Class c, String HLAName, String[] varNames) {
		ClassTranslator translator = new ClassTranslator(c, HLAName, varNames);
		classToHandle.put(c, translator);
	}
	
	/**
	 * This is a convenience method which associates the given class with the
	 * class and variable names derived from reflection.
	 * 
	 * Specficially, this method calls {@link PublicObject#getPublicVariables()} on
	 * a new instance of the given class. If the class has no default constructor,
	 * then this method will crash and the {@link #addClass(Class, String, String[])} 
	 * version of this method should be used instead.
	 * 
	 * @param c The class to add to this ClassLookup, using the actual name of this
	 * class and the result of {@link PublicObject#getPublicVariables()} as the 
	 * information in the .fed file for this federation.
	 */
	public void addClass(Class c) {		
		this.addClass(c, c.getSimpleName(), extractVars(c));
	}
	
	static String[] extractVars(Class c) {		
		try {
			Object exampleInstance = c.newInstance();
			Method extractedMethod = c.getMethod("getPublicVariables");
			return (String[])extractedMethod.invoke(exampleInstance);
		}
		catch (Exception e) {
			DEV_TOOLS.showException(e);
			return null;
		}		
	}

	/**
	*   Resolves the .fed recorded names to HLA class handle integers. This
	*   means that the value held for each Class key will change from a String
	*   to an Integer.<b> Do not call this method from the RTI callback thread</b> 
	*   unless your RTI is re-entrant.
	*   
	*   @throws RTIexception if an error occurs with the RTI. In almost all cases
	*   the exception will be a result of one of the HLANames not having been 
	*   recorded in the ClassLookup correctly
	*/
	public void resolveNames() throws RTIexception {	
		/* resolve the translator's attribute handles then associate 
		 * the translator with its class handle
		 */
		for (Class c : classToHandle.keySet()) {
			ClassTranslator trans = classToHandle.get(c);
			trans.resolve();
			handleToClass.put(trans.getHandle(), trans);
		}
		this.isResolved = true;		
	}

	/**
	*   Tells whether or not the .fed names have been resolved to RTI class handles
	*   in this ClassLookup
	*   @return true if an int-type handle can be retrieved from this ClassLookup
	*   or whether it will still return a String
	*/
	public boolean isResolved() {	
		return this.isResolved;
	}

	/**
	*   Gets the handle of this class as it is recorded by the RTI
	*   @param c the Class to find out about
	*   @return the handle of the class that can be used in communication with
	*   the RTIambassador given as the argument to 
	*   {@link #resolveNames() resolveNames()}
	*   @throws NullPointerException if c is null or if c has not been recorded
	*   in this ClassLookup
	*   @throws ClassCastException if this ClassLookup has not been resolved
	*   (i.e. if {@link #isResolved()} returns false)
	*/
	public int getClassHandle(Class c) {
		return classToHandle.get(c).classHandle;
	}

	/**
	 * True if the given class has been locally registered with this lookup.
	 * 
	 * @see ClassLookup#addClass(Class)
	 * @param c the class to query
	 * @return true if the class has been locally registered
	 */
	public boolean hasClass(Class c) {	
		return classToHandle.containsKey(c);
	}

	/**
	 * Get all classes registered with this lookup
	 * @return the classes
	 */
	public Collection<Class> getClasses() {
		return classToHandle.keySet();
	}

	/**
	 * Get the variables associated with this class as registered
	 * with this lookup.
	 * @param c the class to query
	 * @return a list of the 'public' variables of this class, as far
	 * as the RTI is concerned
	 */
	public String[] getVarNames(Class c) {
		return classToHandle.get(c).getVarNames();
	}

	/**
	 * Get the {@link Class} object corresponding to the given handle
	 * assigned to it by the RTI.
	 * @param handle the RTI-assigned handle
	 * @return the java Class associated with the handle in the local JVM
	 */
	public Class getClassFor(int handle) {
		return handleToClass.get(handle).getClass();
	}

	/**
	 * Get the handle assigned to the given public variable of the 
	 * given class.
	 * @param classHandle the RTI-assinged class handle (see 
	 * 	{@link #getClassHandle(Class)})
	 * @param varName the String name of the variable in question (see
	 * 	{@link PublicObject#getPublicVariables()})
	 * @return the RTI-assigned handle for the given variable.
	 */
	public int getVariableHandle(int classHandle, String varName) {
		ClassTranslator trans = handleToClass.get(classHandle);
		if (trans == null)
			throw new IllegalArgumentException("class " + classHandle + " not registered");
		else
			return trans.getHandle(varName);
	}

	/**
	 * Get the local String name associated with the given variable.
	 * @param classHandle the RTI-assinged class handle (see 
	 * 	{@link #getClassHandle(Class)})
	 * @param varHandle the RTI-assigned handle for the given variable (see
	 *  {@link #getVariableHandle(int, String)})
	 * @return the local name of the variable (see 
	 * 	{@link PublicObject#getPublicVariables()})
	 */
	public String getVariableName(int classHandle, int varHandle) {	
		ClassTranslator trans = handleToClass.get(classHandle);
		if (trans == null)
			throw new IllegalArgumentException("class " + classHandle + " not registered");
		else
			return trans.getName(varHandle);
	}

	public String toString() {
	
		String returnString = new String("");
		for (Class c : classToHandle.keySet()) {
			ClassTranslator box = classToHandle.get(c);
			returnString = new String(returnString + box.getName());
			if (isResolved) {
				returnString = new String(returnString + "/" + box.getHandle());
			}
			returnString = new String(returnString + "\n");
		}
		return returnString;
	}

	/*
	 * This class encapsulates the handle->local name translation for the
	 * class and attribute handles of a single class.
	 * 
	 * It is initialised with a class, a corresponding 'HLA name' and a 
	 * list of the public variables associated with it in the federation.
	 * 
	 * It is then 'resolved' against an RTI to obtain the corresponding
	 * handles for these things.
	 */
	private class ClassTranslator implements Serializable {
	
		private String HLAName;
		private Class theClass;
		private int classHandle;
	
		private Hashtable<String, Integer> nameToHandle;
		private Hashtable<Integer, String> handleToName;
		private String[] varNames;
	
		ClassTranslator(Class c, String HLAName, String[] varNames) {		
			this.theClass = c;
			this.HLAName = HLAName;
			this.varNames = varNames;
		
			nameToHandle = new Hashtable<String, Integer>();
			handleToName = new Hashtable<Integer, String>();
		}
	
		void resolve() throws RTIexception {		
			resolveClass(LocalManager.getRTI());
			resolveVariables(LocalManager.getRTI());
			notifyFed(LocalManager.getRTI());
			DEV_TOOLS.print("<ClassLookup::resolve> done");
		}
	
		void resolveClass(RTIambassador amb) throws RTIexception {		
			this.classHandle = amb.getObjectClassHandle(HLAName);
		}
	
		void resolveVariables(RTIambassador amb) throws RTIexception {		
			for (String s : varNames) {
				int handle = amb.getAttributeHandle(s, classHandle);
				nameToHandle.put(s, handle);
				handleToName.put(handle, s);
			}
		}
		
		/*
		 * Subscribes and publishes the object class
		 * REMEMBER: the java bindings throw a 'ObjectClassNotPublished'
		 * exception if the AttributeHandleSet supplied is accidentally 
		 * empty, ensure the handles are going in to the set properly and 
		 * that the correct variable is being supplied to the method.
		 */				
		void notifyFed(RTIambassador amb) throws RTIexception {						
			AttributeHandleSet handles = LocalManager.getHandleSet(varNames.length);
			for (String var : varNames) {
				handles.add(this.getHandle(var));
			}
			amb.subscribeObjectClassAttributes(classHandle, handles);
			Threads.RTI_LONG_TICK();
			amb.publishObjectClass(classHandle, handles);
			Threads.RTI_LONG_TICK();
		}
	
		Class getContainedClass() {
		
			return theClass;
		}
	
		int getHandle() {
		
			return classHandle;
		}
	
		String getName() {
		
			return HLAName;
		}
	
		String[] getVarNames() {
		
			return varNames;
		}
	
		int getHandle(String varName) {		
			return nameToHandle.get(varName);
		}
	
		String getName(int varHandle) {
			return handleToName.get(varHandle);
		}
	}
}
