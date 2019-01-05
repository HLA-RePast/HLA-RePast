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
package object;

import hla.rti13.java1.ArrayIndexOutOfBounds;
import hla.rti13.java1.AttributeAlreadyBeingAcquired;
import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.FederateOwnsAttributes;
import hla.rti13.java1.ObjectNotKnown;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import hla.rti13.java1.SuppliedAttributes;
import io.Bytes;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;

import java.io.IOException;
import java.util.Hashtable;

import object.variables.CumulativeVariable;
import object.variables.ExclusiveVariable;
import object.variables.PublicVariable;
import object.variables.ViewableVariable;
import rtidep.RTIDep;

import exceptions.BadClassException;
import exceptions.ObjectDeletedException;
import exceptions.ObjectNotFoundException;
import exceptions.VariableException;


import logging.Logger;
import logging.StopWatch;
import manager.ClassLookup;
import manager.DistributedSchedule;
import manager.LocalManager;

/**
 * This gives an interface on to the local and remote object 
 * databases.
 * 
 * The database is divided in to locally registered 
 * {@link PublicObject} and locally discovered {@link RemoteObject}
 * instances.
 * 
 * Individual {@link PublicVariable} instances hold back-references
 * to an ObjectLookup which provides them access to methods 
 * implementing attribute updates invoked on the RTI.
 * 
 * The ObjectLookup for a federate also coordinates the ownership
 * management for doing conflict resolution.
 * 
 * @author Rob Minson
 */
public class ObjectLookup {

	private ClassLookup clLookup;	
	
	private LocalBase local;
	private RemoteBase remotes;
	
	private Logger logger;
	private boolean loggingOn;
	
	/**
	 * Creates an ObjectLookup with a ClassLookup configured for this
	 * federation.
	 * 
	 * @param lookup the ClassLookup correctly initialised with handle
	 * 	information for this federation.
	 * @param logger
	 * @throws RTIexception
	 */
	public ObjectLookup(ClassLookup lookup, Logger logger) throws RTIexception {
	
		this.clLookup = lookup;
					
		if (!clLookup.isResolved())
			clLookup.resolveNames();	
	
		remotes = new RemoteBase(this, clLookup);		
		local = new LocalBase(clLookup);
		
		this.logger = logger;
		loggingOn = false;
	}	



	///////////////////////////////////UTILITY////////////////////////////////////

	AttributeHandleSet getHandleSet(int size) 
		throws RTIexception {
	
		return LocalManager.getHandleSet(size);
	}

	static SuppliedAttributes getSuppAttrs(int size) 
		throws 	RTIexception {
	
		return LocalManager.getSuppliedAttributes(size);
	}

	double getTick() {	
		return LocalManager.getManager().getTick();
	}
	
	private void print(String s) {		
		System.err.println(s + " <object lookup>");
	}
	
	
	
	
	////////////////////OBJECT CREATION/DISCOVERY////////////////////////
	
	public void registerPublicObject(PublicObject obj) 
			throws RTIexception, IOException {
		
		DistributedSchedule.hlaTimer.start();
		for (String vname : obj.getPublicVariables())
			obj.getVariable(vname).setLookup(this);
		local.registerObject(obj);
		DistributedSchedule.hlaTimer.stop();
	}

	public void discoverRemoteObject(int instanceHandle, int classHandle) 
			throws IOException, RTIinternalError, FederateInternalError {		
		DEV_TOOLS.print("<ObjectLookup::discoverRemoteObject> instance " + instanceHandle + " of class " + classHandle);
		remotes.discover(instanceHandle, classHandle);
	}

	
	/* remembers what global classes local remotes have been instantiated as,
	 * useful when we need to use the class lookup to determine RTI handles 
	 * based on local reflection operations. 
	 */
	private Hashtable<Class, Class> instantiationMap = new Hashtable<Class, Class>();
	
	/**
	 * Obtains a list of proxies of objects registered by other federates
	 * 
	 * @see ReflectedList
	 * @see RemoteObject
	 * 
	 * @param globalClass The Class that the {@link LocalManager} for this
	 * 	federate was configured with.
	 * @param localClass The Class of objects that we would like the 
	 * 	{@link ReflectedList} to be populated with. This does not have to be
	 * 	the same as the globalClass argument, but in most cases it will be.
	 * 	sometimes it might be useful to create a subclass that behaves slightly
	 * 	differently but usually not.
	 * @return a {@link ProxyList} of {@link RemoteObject} instances whose
	 * 	population will dynamically change as objects of the given class are 
	 * 	added and removed from this federation.
	 * @throws BadClassException
	 * @throws IOException
	 */
	public ProxyList getProxyList(Class globalClass, Class localClass)
			throws FederateInternalError, RTIinternalError,
			InstantiationException, IllegalAccessException {
		if (!instantiationMap.containsKey(localClass))
			instantiationMap.put(localClass, globalClass);
		return remotes.getProxyList(globalClass, localClass);
	}
	
	
	
	
	
	
	
	
	
	////////////////////////////////////UPDATE RESOLUTION//////////////////////////////
			
	public void resolveUpdate(int objectHandle, ReflectedAttributes13 attrs, double time) 
			throws ObjectNotFoundException, ObjectDeletedException, 
			IOException, RTIinternalError, FederateInternalError{
	
		DEV_TOOLS.print("<ObjectLookup::resolveUpdate> instance " + objectHandle + " at time " + time);
		
		if (remotes.hasInstance(objectHandle))
			remotes.update(objectHandle, attrs);
		else
			local.updateInstance(objectHandle, attrs);
		
		if (loggingOn) {
			try {
				LocalManager manager = LocalManager.getManager();
				logger.open();
				logger.logUpdate(manager.getSessionID(), manager.getFederateID(), objectHandle, attrs.getHandle(0), time, Bytes.intValue(attrs.getValue(0)) + "", Logger.REMOTE);
				logger.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}

	public void internalUpdate(PublicVariable var) throws 	IllegalStateException, 
													RTIexception, 
													IOException {
		DEV_TOOLS.print("<ObjectLookup::internalUpdate> sending update to object " + var.getOwnersHandle() + ", var " + var.getHandle());
		DistributedSchedule.hlaTimer.start();
	
		if (var.getOwnership() != PublicVariable.OwnershipState.OWNED) {
			throw new IllegalStateException("variable is not " + 
											"owned by this federate");
		}
	
		SuppliedAttributes attrs = LocalManager.getSuppliedAttributes(1);
		attrs.add(var.getHandle(), var.getBytes());
		byte[] time = RTIDep.getLogTime(LocalManager.getManager().getTick());
		LocalManager.getRTI().updateAttributeValues(var.getOwnersHandle(), attrs, time, "");
		if (loggingOn && logger != null) {
			try {
				LocalManager manager = LocalManager.getManager();
				logger.open();
				logger.logUpdate(manager.getSessionID(), manager.getFederateID(), var.getOwnersHandle(), var.getHandle(), manager.getTick(), "" + Bytes.intValue(var.getBytes()), Logger.LOCAL);		
				logger.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		DistributedSchedule.hlaTimer.stop();
	}	
	
	public void sendUpdate(PublicVariable var, byte[] value) 
														throws RTIexception {
		DEV_TOOLS.print("<ObjectLookup::sendUpdate> sending update to object " + var.getOwnersHandle() + ", var " + var.getHandle());
		SuppliedAttributes attrs = LocalManager.getSuppliedAttributes(1);
		attrs.add(var.getHandle(), value);
		byte[] time = RTIDep.getLogTime(LocalManager.getManager().getTick());
		LocalManager.getRTI().updateAttributeValues(var.getOwnersHandle(), attrs, time, "");
	}
	
	public void loggingOn(boolean on) {
		
		loggingOn = on;
	}

	

	
	
	
	
	
	//////////////////////////////////OBJECT DELETION/////////////////////////////////
	
	public void cleanPublicObjects() throws RTIexception {
		local.cleanReferences();
	}

	public void localDelete(PublicObject obj) 
								throws RTIexception, ObjectNotFoundException {
		local.deleteObject(obj);
	}
	
	public void removeInstance(int objectHandle) {
		DEV_TOOLS.print("<ObjectLookup::removeInstance> instance " + objectHandle);
		remotes.delete(objectHandle);
	}
	
	////////////////////////////OBJECT REGISTRATION HELPER /////////////
	
	public void refreshObject(int handle) 
		throws 	RTIexception, 
						IOException, 
						ObjectDeletedException, 
						ObjectNotFoundException {
		
		if (local.hasInstance(handle)) {
			PublicObject o = local.getObject(handle);
			o.refresh();
		}			
	}




	////////////////////////////CONFLICT RESOLUTION//////////////////////////
	public boolean checkOwnership(PublicVariable var) throws VariableException {
	
		DistributedSchedule.hlaTimer.start();
	
		boolean answer = false;
		try {
			if (var instanceof ExclusiveVariable) {
				answer = oneTimeCheck(var);
			}
			else if (var instanceof CumulativeVariable) {
				answer = persistentCheck(var);
			}
			else if (var instanceof ViewableVariable) {
				if (remotes.hasInstance(var.getOwnersHandle()))
					answer = false;
				else if (local.hasInstance(var.getOwnersHandle()))
					answer = true;
				else 
					answer = false;
			}
			else
				answer = false;
		}
		catch (RTIexception e) {
			DistributedSchedule.hlaTimer.stop();
			throw new VariableException("Terminal error with the RTI", e);
		}
		catch (ObjectNotFoundException e) {
			DistributedSchedule.hlaTimer.stop();
			throw new VariableException("Object could not be located in local " + 
										"database", 
										e);
		}
		catch (ObjectDeletedException e) {
			answer = false;
		}
		
		DistributedSchedule.hlaTimer.stop();
		return answer;
	}

	public void requestOwnership(PublicVariable var) throws RTIexception {		
		AttributeHandleSet handles = LocalManager.getHandleSet(1);
		handles.add(var.getHandle());
		try {
			int ownHand = var.getOwnersHandle();
			LocalManager.getRTI().attributeOwnershipAcquisitionIfAvailable(
											ownHand, handles);	
		}
		catch (FederateOwnsAttributes e) {
			var.setOwnership(PublicVariable.OwnershipState.OWNED);
		}
	}
	
	private boolean oneTimeCheck(PublicVariable var) 
		throws RTIexception, ObjectNotFoundException, ObjectDeletedException {

		AttributeHandleSet handles = LocalManager.getHandleSet(1);
		handles.add(var.getHandle());
		try {
			int ownHand = var.getOwnersHandle();
			LocalManager.getRTI().attributeOwnershipAcquisitionIfAvailable(
											ownHand, handles);	
		}
		catch (FederateOwnsAttributes e) {
			var.setOwnership(PublicVariable.OwnershipState.OWNED);
			return true;
		}
		return var.waitForOwnershipResolution();
	}

	private boolean persistentCheck(PublicVariable var) 
		throws RTIexception, ObjectNotFoundException, ObjectDeletedException {

		AttributeHandleSet handles = LocalManager.getHandleSet(1);
		handles.add(var.getHandle());
		do {
			try {
				int ownHand = var.getOwnersHandle();
				LocalManager.getRTI().attributeOwnershipAcquisitionIfAvailable(
												ownHand, handles);				
			}
			catch (FederateOwnsAttributes e) {
				var.setOwnership(PublicVariable.OwnershipState.OWNED);
				return true;
			}
			catch (AttributeAlreadyBeingAcquired e) {}
			catch (ObjectNotKnown e) {
				throw new ObjectDeletedException();
			}
		} while (!var.waitForOwnershipResolution());
		if (!var.waitForOwnershipResolution())
			DEV_TOOLS.print("<ObjectLookup::persistentCheck> attribute is not owned but we think it is");
		return true;
	}

	public void returnOwnership(PublicVariable var) throws RTIexception {

		DistributedSchedule.hlaTimer.start();
		AttributeHandleSet handles = LocalManager.getHandleSet(1);		
		handles.add(var.getHandle());
		int ownHand = var.getOwnersHandle();
		LocalManager.getRTI().unconditionalAttributeOwnershipDivestiture(ownHand, handles);
		var.setOwnership(PublicVariable.OwnershipState.AMBIGUOUS);
		DistributedSchedule.hlaTimer.stop();
	}

	public void resetOwnership() throws RTIexception {

		DEV_TOOLS.print("resetting all ownerships...");
		DEV_TOOLS.indent();		
		StopWatch t = new StopWatch(); t.start();
		
		remotes.resetAll();
		local.resetAll();
		
		t.stop();
		DEV_TOOLS.undent();
		DEV_TOOLS.print("took " + t.getTotal() + " millis");		
	}
	
	
	public void notifyOwnership(int objectHandle, AttributeHandleSet handles) 
		throws ObjectNotFoundException, ObjectDeletedException {				
		try {		
			if (remotes.hasProxy(objectHandle)) {
				RemoteObject ob = remotes.getProxy(objectHandle);
				Class globalClass = instantiationMap.get(ob.getClass());
				int gcHandle = clLookup.getClassHandle(globalClass);
				for (int i = 0; i < handles.size(); i++) {
					String varName = 
						clLookup.getVariableName(gcHandle, handles.getHandle(i));
					ob.getVariable(varName).setOwnership(PublicVariable.OwnershipState.OWNED);
				}
			}
			else if (local.hasInstance(objectHandle)) {		
				PublicObject ob = local.getObject(objectHandle);
				int cHandle = clLookup.getClassHandle(ob.getClass());
				for (int i = 0; i < handles.size(); i++) {
					String varName =
						clLookup.getVariableName(cHandle, handles.getHandle(i));
					ob.getVariable(varName).setOwnership(PublicVariable.OwnershipState.OWNED);
				}
			}
			else
				DEV_TOOLS.print("<ObjectLookup::notifyOwnership> recieved ownership notification for an unknown object !");
		}
		catch (ArrayIndexOutOfBounds e) {
			e.printStackTrace();
		}	
	}

	public void notifyOwnershipFailure(int objectHandle, AttributeHandleSet handles) 
		throws ObjectNotFoundException, ObjectDeletedException {
		
		
		try {		
			if (remotes.hasProxy(objectHandle)) {
				RemoteObject ob = remotes.getProxy(objectHandle);
				Class globalClass = instantiationMap.get(ob.getClass());
				int gcHandle = clLookup.getClassHandle(globalClass);
				for (int i = 0; i < handles.size(); i++) {
					String varName = 
						clLookup.getVariableName(gcHandle, handles.getHandle(i));
					ob.getVariable(varName).setOwnership(PublicVariable.OwnershipState.NOT_OWNED);
				}
			}
			else if (local.hasInstance(objectHandle)) {		
				PublicObject ob = local.getObject(objectHandle);
				int cHandle = clLookup.getClassHandle(ob.getClass());
				for (int i = 0; i < handles.size(); i++) {
					String varName =
						clLookup.getVariableName(cHandle, handles.getHandle(i));
					ob.getVariable(varName).setOwnership(PublicVariable.OwnershipState.NOT_OWNED);
				}
			}
			else
				DEV_TOOLS.print("<ObjectLookup::notifyOwnershipFailure> received ownership failure for an unkown object");
		}
		catch (ArrayIndexOutOfBounds e) {
			e.printStackTrace();
		}
		
		DEV_TOOLS.print("<ObjectLookup::notifyOwnershipFailure> for object " + objectHandle);
	}
}
