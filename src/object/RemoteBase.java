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
/* * Created on 24-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package object;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeNotOwned;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;

import java.io.IOException;
import java.util.Hashtable;

import object.variables.CumulativeVariable;
import object.variables.ExclusiveVariable;
import object.variables.PublicVariable;
import object.variables.ViewableVariable;

import exceptions.BadClassException;



import manager.ClassLookup;
import manager.LocalManager;

/**
 * @author Rob Minson
 *
 */
public class RemoteBase {
	
	private ClassLookup classLookup;
	private ObjectLookup objectLookup;	
	
	/* These two lookups map either from class handles to proxy lists or from
	 * instance handles to proxy lists.
	 * 
	 * The former is used when I want to add an instance to the correct ProxyList
	 * the latter is used when I want to update an existing instance.
	 */
	private Hashtable<Integer, ProxyList> proxyListsByClass = 
												new Hashtable<Integer, ProxyList>();
	private Hashtable<Integer, ProxyList> proxyListsByInstance = 
												new Hashtable<Integer, ProxyList>();
	/* These lookups allow us to keep track of discovered instances and updates
	 * being made to them before we know what local class should be instantiated 
	 * for the given class handle.
	 * 
	 * After a call to getProxyList the shells for the requested class will be
	 * turned in to RemoteObject subclass instances.
	 * 
	 * The mapping is from class handles to lists of ObjectShells
	 */
	private Hashtable<Integer, ShellList> shellListsByClass = 
												new Hashtable<Integer, ShellList>();
	private Hashtable<Integer, ShellList> shellListsByInstance = 
												new Hashtable<Integer, ShellList>();
	
	RemoteBase(ObjectLookup lookup, ClassLookup cl) {	
		this.objectLookup = lookup;
		this.classLookup = cl;		
	}	
	
	public void discover(int instHandle, int classHandle) 
								throws 	FederateInternalError, 
										BadClassException, 
										RTIinternalError {
		/* find out if we can create a new RemoteObject or whether we need to keep
		 * this as a shell for the time being (ie. does a ProxyList exist for the
		 * given class?)
		 */
		ProxyList pl = proxyListsByClass.get(classHandle);
		if (pl != null) { /* OK to create RemoteObject */
			try {
				pl.addInstance(instHandle);
			}
			catch (Exception e) {
				e.printStackTrace(); 
					//FederateInternalError is incredibly stupid and throws 
					//the message away, so print the stack here
				throw new FederateInternalError(e.getMessage());
			}
			proxyListsByInstance.put(instHandle, pl);
		}
		else { /* have to create a Shell for the time being */
			ShellList sl = shellListsByClass.get(classHandle);
			if (sl == null) {
				sl = new ShellList();
				shellListsByClass.put(classHandle, sl);
			}
			sl.add(instHandle);
			shellListsByInstance.put(instHandle, sl);
		}
	}
	
	public void update(int instHandle, ReflectedAttributes13 attrs) 
										throws 	FederateInternalError, 
												RTIinternalError, 
												IOException {
		DEV_TOOLS.print("<RemoteBase::update> processing update to " + instHandle + "...");
		DEV_TOOLS.indent();
		
		ProxyList pl = proxyListsByInstance.get(instHandle);
		if (pl != null) {
			DEV_TOOLS.print("updated proxy");
			for (int i = 0; i < attrs.size(); i++)
				pl.update(instHandle, attrs.getHandle(i), attrs.getValue(i));
		}
		else {
			ShellList sl = shellListsByInstance.get(instHandle);
			if (sl != null) {
				DEV_TOOLS.print("update shell");
				for (int i = 0; i < attrs.size(); i++)
					sl.update(instHandle, attrs.getHandle(i), attrs.getValue(i));
			}
		}
		
		DEV_TOOLS.undent();
	}
	
	public void delete(int instHandle) {
		DEV_TOOLS.print("<RemoteBase::delete> instancle " + instHandle);		
		if (hasProxy(instHandle)) {
			removeProxy(instHandle);
		}
		else if (hasShell(instHandle)){
			removeShell(instHandle);
		}
	}
	
	protected void removeShell(int instance) {
		ShellList sl = shellListsByInstance.get(instance);
		if (sl != null)
			sl.remove(instance);
		shellListsByInstance.remove(instance);
	}
	
	protected void removeProxy(int instance) {
		ProxyList pl = proxyListsByInstance.get(instance);
		if (pl != null)
			pl.removeObject(instance);
		proxyListsByInstance.remove(instance);
	}
	
	ProxyList getProxyList(Class remoteClass, Class localClass)
			throws FederateInternalError, RTIinternalError,
			InstantiationException, IllegalAccessException {
		
		DEV_TOOLS.print("<RemoteBase::getProxyList> remote class " + remoteClass + " local class " + localClass);
		DEV_TOOLS.indent();
		
		int remoteHandle = classLookup.getClassHandle(remoteClass);
		if (proxyListsByClass.containsKey(remoteHandle))
			return proxyListsByClass.get(remoteHandle);
		
		ProxyList pl = new ProxyList(classLookup.getClassHandle(remoteClass),
				localClass, classLookup, objectLookup);
		proxyListsByClass.put(remoteHandle, pl);
		
		/* add any instances of this class currently held as shells */
		ShellList sl = shellListsByClass.get(remoteHandle);
		if (sl != null) {
			DEV_TOOLS.print("shifting " + sl.size() + " shells");
			for (ObjectShell shell : sl) {
				pl.addInstance(shell);
				shellListsByInstance.remove(shell.getHandle());
				proxyListsByInstance.put(shell.getHandle(), pl);
			}
			shellListsByClass.remove(remoteHandle);
		}
		else
			DEV_TOOLS.print("no shells to shift");
		
		DEV_TOOLS.undent();
		
		return pl;
	}

	boolean hasInstance(int instanceHandle) {
		return 	hasProxy(instanceHandle) || hasShell(instanceHandle);
	}
	
	boolean hasProxy(int instanceHandle) {
		return proxyListsByInstance.containsKey(instanceHandle);
	}
	
	boolean hasShell(int instanceHandle) {
		return shellListsByInstance.containsKey(instanceHandle);
	}
	
	RemoteObject getProxy(int instanceHandle) {
		return proxyListsByInstance.get(instanceHandle).getProxy(instanceHandle);
	}

	/**
	 * Resets ownership data for all variables to the 'start of tick' defaults 
	 * depending on the semantics of the variable:
	 * 
	 * In practice this means divesting ownership of all types other than
	 * 'ViewableVariable'
	 * 
	 * @see ExclusiveVariable
	 * @see CumulativeVariable
	 * @see ViewableVariable
	 */
	void resetAll() throws RTIexception {
		for (ProxyList pl : proxyListsByClass.values()) {
			for (RemoteObject ro : pl.getProxies())
				resetObject(ro);
			/* piggy-back a refresh request since this gets called regularly */
			pl.refreshIncompleteProxies();
		}
	}

	private void resetObject(RemoteObject obj) 
		throws RTIexception {
	
		for (int i = 0; i < obj.getPublicVariables().length; i++) {
			PublicVariable nextVar = obj.getVariable(obj.getPublicVariables()[i]);
			initOwnership(nextVar);
		}			
	}

	private void initOwnership(PublicVariable var) 
		throws RTIexception {
					
//		System.out.println("<RemoteBase.initOwnership> resetting var " + var.getHandle() + " currently set to " + var.getOwnership());
		
		if (var instanceof ExclusiveVariable || var instanceof CumulativeVariable) {
			if (var.getOwnership() != PublicVariable.OwnershipState.AMBIGUOUS) {
				try {
					AttributeHandleSet handles = LocalManager.getHandleSet(1);
					handles.add(var.getHandle());
					int instHandle = var.getOwnersHandle();
					LocalManager.getRTI().unconditionalAttributeOwnershipDivestiture(	instHandle, 
																						handles);
				}
				catch (AttributeNotOwned e) {
//					System.out.println("<RemoteBase.initOwnership> tried to divest ownership of unowned variable " + var.getHandle());
				}
				var.setOwnership(PublicVariable.OwnershipState.AMBIGUOUS);
			}
		}
		else //meaning the variable is an instance of Viewable variable...
			var.setOwnership(PublicVariable.OwnershipState.NOT_OWNED);
	}
}
