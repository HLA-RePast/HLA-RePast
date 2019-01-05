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
 */
package object.variables;

import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import io.DEV_TOOLS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import object.ObjectLookup;
import object.PublicObject;
import object.RemoteObject;
import object.VariableListener;

import manager.LocalManager;


import rtidep.Threads;

/**
 * This is the root class for all share-able variables in an
 * HLA_RePast model.
 * 
 * A {@link PublicObject} instance may be made up of a mix of 
 * PublicVariables and standard Java variables (eg. an agent's
 * position may be represented as {@link ViewablePoint} whilst
 * its rule-base (or whatever cognitive model) is represented
 * as a standard Java {@link Hashtable}. This would result in
 * changes to position being reflected to {@link RemoteObject}
 * proxies of the agent but changes in the rule-base remaining
 * purely within the local JVM.
 * 
 * An extensive collection of 
 * 
 * @author Rob Minson
 */
public abstract class PublicVariable {
	
	public enum OwnershipState {
		OWNED, 		/* asked for and got */
		NOT_OWNED, 	/* asked for and not got */
		AMBIGUOUS;	/* not asked for or waiting for answer */
	}

	private ArrayList<VariableListener> listeners = new ArrayList<VariableListener>();

	protected OwnershipState ownership = OwnershipState.AMBIGUOUS;
	protected ObjectLookup obLook;

	protected int myHandle;
	protected int ownersHandle;

	protected boolean hasValue = false;

	/**
	 * Whether or not a value has been obtained for this PublicVariable
	 * this method is primarily relevant for variables of {@link RemoteObject}
	 * instances, indicating whether valid initial values are currently
	 * held.
	 * @return
	 */
	public boolean hasValue() {	
		return hasValue;
	}

	public void setLookup(ObjectLookup obLook) {	
		this.obLook = obLook;
	}

	public void setHandle(int handle) {	
		this.myHandle = handle;
	}

	public int getHandle() {	
		return myHandle;
	}
	
	public void setOwnersHandle(int handle) {
		this.ownersHandle = handle;
	}

	public int getOwnersHandle() {	
		return ownersHandle;
	}

	/**
	 * This method is used internally by the engine to assign the values of 
	 * a newly discovered {@link RemoteObject} without triggering updates to be
	 * sent to the RTI. 
	 * 
	 * This method should NOT be called by user code and is left public as an 
	 * artefact of implementation. 
	 * 
	 * @param newValue
	 * @throws FederateInternalError
	 * @throws RTIinternalError
	 */
	public void update(byte[] newValue) throws FederateInternalError, RTIinternalError{	
		hasValue = true;
		for (int i = 0; i < listeners.size(); i++) {
		    try {
		        ((VariableListener)listeners.get(i)).variableChanged(getBytes(), newValue);
		    } catch (IOException e) {
		        throw new FederateInternalError("IOException: " + e.getMessage());
		    }
		}
	}

	public abstract byte[] getBytes() throws RTIinternalError;

	public boolean isOwned() {
		return ownership == OwnershipState.OWNED;
	}
	
	protected void requestOwnership() throws RTIexception {
		if (obLook == null)
			throw new IllegalStateException("Variable Not Yet Registered");		
		ownership = OwnershipState.AMBIGUOUS;
		obLook.requestOwnership(this);
	}
	
	protected boolean checkOwnership() throws IllegalStateException, RTIexception {	
		if (obLook == null)
			throw new IllegalStateException("Variable Not Yet Registered");
		else
			return obLook.checkOwnership(this);
	}

	public boolean waitForOwnershipResolution() {	
		while (ownership == OwnershipState.AMBIGUOUS) {
			try { Threads.RTI_LONG_TICK(); }
			catch (RTIexception e) { e.printStackTrace(); }
		}
		if (ownership == OwnershipState.OWNED) {
			return true;
		}
		else {
			return false;
		}
	}

	public OwnershipState getOwnership() {	
		return ownership;
	}

	public void setOwnership(OwnershipState ownership) {	
		this.ownership = ownership;
	}
	
	public void refresh() throws IOException, RTIexception {		
		DEV_TOOLS.print("<PublicVariable::refresh> var " + getHandle() + " at time " + LocalManager.getManager().getTick());
		if (isOwned())
			obLook.sendUpdate(this, getBytes());
//		if (obLook.checkOwnership(this))
//			obLook.internalUpdate(this);
	}

	public synchronized void addVariableListener(VariableListener listener) {
		this.listeners.add(listener);
	}

	public synchronized void removeVariableListener(VariableListener listener) {
		this.listeners.remove(listener);
	}		
}
