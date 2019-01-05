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
package object;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeNotOwned;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import hla.rti13.java1.SuppliedAttributes;
import io.Bytes;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;

import java.io.IOException;
import java.util.List;

import object.variables.CumulativeVariable;
import object.variables.ExclusiveVariable;
import object.variables.PublicVariable;
import rtidep.RTIDep;

import exceptions.ObjectDeletedException;
import exceptions.ObjectNotFoundException;



import manager.ClassLookup;
import manager.LocalManager;

/**
 * This class stores references to the locally instantiated
 * objects which will take part in the outside federation.
 * 
 * The class provides access to local objects from outside
 * for eg. to update values or to delete objects.
 * 
 * The LocalBase of a Federate also serves to track the object
 * in the local JVM. The LocalBase itself only stores weak
 * references to objects, so if no other references exist to 
 * the object, it will be garbage collected. The LocalBase
 * can detect this and this can be used to perform automatic
 * deletion of locally registered objects. (Note this only 
 * occurs if lazy deletion is on at this federate.)
 * 
 * @see LocalManager#registerPublicObject(PublicObject)
 * @see LocalManager#setLazyDeletion(boolean)
 * 
 * @author Rob Minson
 *
 */
class LocalBase {

	private ReferenceTable instances;
	
	private ClassLookup cl;
	
	/**
	 * Create a LocalBase with a 'back reference' to the given 
	 * set of class/handle associations as represented by the 
	 * given ClassLookup
	 * 
	 * @param cl the {@link ClassLookup} configured for this 
	 * federation.
	 */
	public LocalBase(ClassLookup cl) {
		
		this.instances = new ReferenceTable();	
		this.cl = cl;
	}

	boolean hasInstance(int handle) {
	
		try {
			instances.getObject(handle);
			return true;
		} catch (ObjectDeletedException e) {
			return false;
		} catch (ObjectNotFoundException e) {
			return false;
		}		
	}

	void registerObject(PublicObject ob) 
		throws RTIexception, IOException {
	
		if (!cl.hasClass(ob.getClass()))
			throw new IllegalArgumentException("Cannot register instance of " + ob.getClass().getName());
		
		/* setup the instance's internal variables */
		int classHandle = cl.getClassHandle(ob.getClass());
		int instHandle = LocalManager.getRTI().registerObjectInstance(classHandle);		
		ob.setHandle(instHandle);
		
		/* register a weak reference to the instance in the reference table */
		instances.register(ob);
		
		/* register and do an initial update for all attributes of object */
		SuppliedAttributes attrs = 
						LocalManager.getSuppliedAttributes(ob.getPublicVariables().length);
		for (String vname : ob.getPublicVariables()) {
			PublicVariable var = ob.getVariable(vname);
			int vhandle = cl.getVariableHandle(classHandle, vname);
			var.setHandle(vhandle);
			var.setOwnersHandle(instHandle);			
			attrs.add(var.getHandle(), var.getBytes());
			DEV_TOOLS.print("<LocalBase::registerObject> updating var " + var.getHandle() + " of object " + ob.getHandle() + " with time " + LocalManager.getManager().getTick());
		}
		byte[] time = RTIDep.getLogTime(LocalManager.getManager().getTick());
		LocalManager.getRTI().updateAttributeValues(ob.getHandle(), 
													attrs, 
													time,
													"");
		
		DEV_TOOLS.print("<LocalBase::registerObject> registered and updated object " + ob.getHandle() + " successfully");
		
		/* divest ownership as default state */
		resetObject(ob);
	}

	void updateInstance(int objectHandle, ReflectedAttributes13 attrs) 
		throws IOException, ObjectNotFoundException, ObjectDeletedException, FederateInternalError, RTIinternalError{
			
		PublicObject ob = (PublicObject)instances.getObject(objectHandle);
		for (int i = 0; i < attrs.size(); i++) {
			String varName = cl.getVariableName(cl.getClassHandle(ob.getClass()), attrs.getHandle(i));			
			PublicVariable var = ob.getVariable(varName);
			var.update(attrs.getValue(i));
		}
	}	

	public PublicObject getObject(int instanceHandle) 
		throws ObjectNotFoundException, ObjectDeletedException {
	
		return instances.getObject(instanceHandle);
	}

	void cleanReferences() throws RTIexception {
	
		List deletions = instances.getDeadReferences();
		DEV_TOOLS.print("<LocalBase.cleanReferences(): clearning " + deletions.size() + " references");
		for (int i = 0; i < deletions.size(); i++) {
			LookupReference ref = (LookupReference)deletions.get(i);
			int handle = ref.getID();			
			DEV_TOOLS.print("<LocalBase.cleanReferences()> deleting object " + handle);
			try {
				deleteObject(handle);
			} catch (ObjectNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	void deleteObject(PublicObject obj) throws 	RTIexception, 
															ObjectNotFoundException {		instances.deregister(obj);
		int handle = obj.getHandle();
		double time = LocalManager.getManager().getTick();
		LocalManager.getRTI().deleteObjectInstance(handle, RTIDep.getLogTime(time), "");
	}
	
	void deleteObject(int handle) throws 	RTIexception, 
														ObjectNotFoundException {		
		instances.deregister(handle);
		double time = LocalManager.getManager().getTick();
		LocalManager.getRTI().deleteObjectInstance(handle, RTIDep.getLogTime(time), "");
	}	
	
	//The LocalBase resets with these criteria
	//ExclusiveVariable --> divestsownership, resets to AMBIGUOUS
	//CumulativeVariable --> divestsownership, resets to AMBIGUOUS	
	//NB (the only ones which should be divested anyway are a subset of the set of ExclusvieVariables)
	void resetAll() throws RTIexception {
		for (int handle : instances.allHandles()) {
			try {
				PublicObject ob = instances.getObject(handle);
				if (ob != null)
					resetObject(ob);
			}
			catch (ObjectNotFoundException e) {}
			catch (ObjectDeletedException e) {}
		}
	}

	private void resetObject(PublicObject obj) throws RTIexception {
	
		String[] publicVars = cl.getVarNames(obj.getClass());
		for (int i = 0; i < publicVars.length; i++) {
			PublicVariable nextVar = obj.getVariable(publicVars[i]);
			initOwnership(nextVar);
		}			
	}

	private void initOwnership(PublicVariable var) throws RTIexception {
					
//		System.out.println("<LocalBase.initOwnership> resetting var " + var.getHandle() + " currently set to " + var.getOwnership());
		
		if (var instanceof ExclusiveVariable || var instanceof CumulativeVariable) {
			try {
				AttributeHandleSet handles = LocalManager.getHandleSet(1);
				handles.add(var.getHandle());
				LocalManager.getRTI().unconditionalAttributeOwnershipDivestiture(
										var.getOwnersHandle(), handles);
			}
			catch (AttributeNotOwned e) {
//				System.out.println("<LocalBase.initOwnership> tried to divest ownership of unowned variable " + var.getHandle());
			}
			var.setOwnership(PublicVariable.OwnershipState.AMBIGUOUS);
		}
		else
			var.setOwnership(PublicVariable.OwnershipState.OWNED);
	}
}
