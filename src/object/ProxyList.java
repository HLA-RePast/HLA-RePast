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
package object;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import io.DEV_TOOLS;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import object.variables.PublicVariable;



import manager.ClassLookup;
import manager.LocalManager;

/**
 * ProxyLists are lists of reflected proxy objects. These objects are created
 * by the engine when it is notified of the addition of a new object by a 
 * remote federate.
 * 
 * Each list represents all instances of one class of {@link RemoteObject}.
 * 
 * The contents of the list (accessed via {@link #getProxies()}) grow and 
 * shrink in response to addition and deletion events incoming from the 
 * federation. These events can be listened for by the local mode using the
 * {@link #registerInstanceListener(InstanceListener)} method.
 * 
 * Under no circumstances should the list returned by {@link #getProxies()} 
 * ever be directly modified. If it is, the behaviour is undefined :)
 *  
 * @see LocalManager#getProxies(Class, Class)
 * @author Rob Minson
 *
 */
public class ProxyList {
	//for instantiating RemoteObject instances
	private Class localClass;
	private int remoteClass;
	private ClassLookup clLookup;
	private ObjectLookup obLookup;
	private List<RemoteObject> visibleProxies = new ArrayList<RemoteObject>();
	private Hashtable<Integer, RemoteObject> updateMap = 
												new Hashtable<Integer, RemoteObject>();
	private Hashtable<RemoteObject, Integer> incompleteProxies = 
												new Hashtable<RemoteObject, Integer>();
	
	
	//////////// CONSTRUCTOR, ONLY CALLED BY INTERNAL ENGINE ///////////////
	
	ProxyList(int remoteClass, Class localClass, ClassLookup cl, ObjectLookup ol) {
		this.localClass = localClass;
		this.remoteClass = remoteClass;
		this.clLookup = cl;
		this.obLookup = ol;
	}
	
	

	//////////// OBJECT DISCOVERY AND VARIABLE INITIALISATION //////////////
	
	protected void initVariable(PublicVariable pv, 
								String vname, 
								int owner, 
								ObjectShell initVals) 
									throws FederateInternalError, RTIinternalError {
		int attrHandle = clLookup.getVariableHandle(remoteClass, vname); 
		pv.setLookup(obLookup);
		pv.setHandle(attrHandle);
		pv.setOwnersHandle(owner);
		if (initVals != null) {
			byte[] initVal = initVals.getAttribute(attrHandle);
			if (initVal != null) {
				pv.update(initVal);
			}
		}
	}
	
	/**
	 * This method is used when no variable values have been received for
	 * the object yet. A proxy is created and variables are initialised, but no 
	 * attempt is made to find out their values and the proxy remains hidden
	 * until values are reflected for all of them.
	 * 
	 * @param instanceHandle
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws FederateInternalError
	 * @throws RTIinternalError
	 */
	void addInstance(int instanceHandle) throws InstantiationException,
			IllegalAccessException, FederateInternalError, RTIinternalError {
		addInstance(instanceHandle, null);
	}
	
	/**
	 * This method is used when an proxy needs to be created for which 
	 * attribute updates have already been received. The values are cached
	 * in an {@link ObjectShell} and are used to provide initial values
	 * for the proxy's variables.
	 * 
	 * @param shell
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws FederateInternalError
	 * @throws RTIinternalError
	 */
	void addInstance(ObjectShell shell) 
						throws 	InstantiationException, 
								IllegalAccessException, 
								FederateInternalError, 
								RTIinternalError {
		addInstance(shell.getHandle(), shell);
	}
	
	private void addInstance(int handle, ObjectShell shell) 
						throws 	InstantiationException, 
								IllegalAccessException, 
								FederateInternalError, 
								RTIinternalError {
		DEV_TOOLS.print("<ProxyList::addInstance> instance " + handle);
		DEV_TOOLS.indent();
		if (shell != null)
			DEV_TOOLS.print("using shell");
		
		RemoteObject ob = (RemoteObject)localClass.newInstance();
		for (String s : ob.getPublicVariables()) {
			PublicVariable pv = ob.getVariable(s);
			initVariable(pv, s, handle, shell);
		}
		updateMap.put(handle, ob);
		if (isComplete(ob)) {
			visibleProxies.add(ob);
			fireInstanceAdded(ob);
			DEV_TOOLS.print("proxy complete, added to visibles");
		}
		else {
			incompleteProxies.put(ob, handle);
			DEV_TOOLS.print("proxy incomplete");
		}
		DEV_TOOLS.undent();
	}
	
	RemoteObject getProxy(int instanceHandle) {
		return updateMap.get(instanceHandle);
	}	
	
	//////////////////// OBJECT DELETION ////////////////////
	
	void removeObject(int instance) {
		RemoteObject ob = updateMap.get(instance);
		if (ob == null)
			return;
		else {
			visibleProxies.remove(ob);
			updateMap.remove(instance);
			incompleteProxies.remove(ob);
			fireInstanceRemoved(ob);
		}
	}	
	
	//////////////////// VARIABLE UPDATES ///////////////////
	
	void update(int instance, int handle, byte[] value) throws FederateInternalError, RTIinternalError {
		DEV_TOOLS.print("<ProxyList::update> to " + instance);
		DEV_TOOLS.indent();
		RemoteObject ob = updateMap.get(instance);
		ob.getVariable(clLookup.getVariableName(remoteClass, handle)).update(value);
		if (!visibleProxies.contains(ob)) {
			if (isComplete(ob)) {
				DEV_TOOLS.print("shifted proxy to visible list");
				visibleProxies.add(ob);
				incompleteProxies.remove(ob);
				fireInstanceAdded(ob);
			}
			else
				DEV_TOOLS.print("proxy still missing variables");
		}
		else {
			DEV_TOOLS.print("proxy already in visible list");
		}
		DEV_TOOLS.undent();
	}
	
	private boolean isComplete(RemoteObject ob) {
		for (String s : ob.getPublicVariables())
			if (!ob.getVariable(s).hasValue())
				return false;
		return true;
	}
	
		////////////////// UPDATE PULLING /////////////////

	void refreshIncompleteProxies() throws RTIexception {
		for (RemoteObject ob : incompleteProxies.keySet()) {
			String[] vars = ob.getPublicVariables();
			AttributeHandleSet ahs = LocalManager.getHandleSet(vars.length);
			for (String s : vars) {
				ahs.add(clLookup.getVariableHandle(remoteClass, s));
			}
			DEV_TOOLS.print("<ProxyList::refreshIncompleteProxies> requesting for object " + incompleteProxies.get(ob));
			LocalManager.getRTI().requestObjectAttributeValueUpdate(
					incompleteProxies.get(ob), ahs);
		}
	}
	
	
	/////////////// MODEL INTERFACE /////////////////////
	
	/**
	 * Obtain a list of {@link RemoteObject}s. This list contains all objects
	 * for which we have a full list of attribute values. If an object of the
	 * given class has been added to the federation, but has not yet received 
	 * updates to all of its {@link PublicVariable}s, it will not appear in
	 * this list.
	 */
	public List<RemoteObject> getProxies() {
		return visibleProxies;
	}	
	
		/////////////// OBJECT TRACKING EVENT GENERATORS ////////////////
	
	private List<InstanceListener> listeners = new ArrayList<InstanceListener>();
	/**
	 * Register to receive notification of {@link RemoteObject} instances being 
	 * added to or removed from this ProxyList
	 * 
	 * @param inst the event handler.
	 */
	public void registerInstanceListener(InstanceListener inst) {		
		this.listeners.add(inst);
	}

	protected void fireInstanceAdded(RemoteObject obj) {	
		for (int i = 0; i < listeners.size(); i++) {
			((InstanceListener)listeners.get(i)).instanceAdded(obj);
		}
	}

	protected void fireInstanceRemoved(RemoteObject obj) {	
		for (int i = 0; i < listeners.size(); i++) {
			((InstanceListener)listeners.get(i)).instanceRemoved(obj);
		}
	}
}
