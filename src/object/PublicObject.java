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

import hla.rti13.java1.RTIexception;

import java.io.IOException;

import object.variables.PublicVariable;

import exceptions.ObjectNotFoundException;


import manager.LocalManager;

/**
 * PublicObject is the primary class which should be subclassed
 * to represent globally shared objects in an HLA_RePast simulation.
 * 
 * All subclassing implementations of this class should ensure
 * the two abstract methods behave in the obvious (map-like) way. This
 * allows the instantiation and synchronisation of local and remote
 * objects with fairly low overhead and fairly high transparency.
 * 
 * PublicObject instances can be registered with the LocalManager, 
 * this will lead to them being reflected by all other federates 
 * throughout the federation as {@link RemoteObject} proxy instances.
 * 
 * Locally registered PublicObjects are deleted from the federation 
 * (leading to the deletion of corresponding proxies) in two situations:
 * 
 * <ol>
 * 	<li>{@link #delete()} or 
 * 	{@link LocalManager#deletePublicObject(PublicObject)} is called</li>
 * 	<li>Lazy deletion is being used (see 
 * 	{@link LocalManager#setLazyDeletion(boolean)}) and the object is 
 * 	garbage collected by the local JVM</li>
 * </ol>
 * 
 * An HLA_RePast model can use one of the two approaches or a mixture 
 * depending on whatever fits best in to the structure of the code.
 * Always ensure previously {@link #delete()}ed objects are not
 * updated, refreshed, etc.
 * 
 * @see RemoteObject
 * @see LocalManager#registerPublicObject(PublicObject)
 * @see LocalManager#deletePublicObject(PublicObject)
 * @see LocalManager#setLazyDeletion(boolean)
 * @see PublicVariable
 * 
 * @author Rob Minson
 */
public abstract class PublicObject {

	private int handle;
	
	public abstract String[] getPublicVariables();

	public abstract PublicVariable getVariable(String varName);
	
	void setHandle(int handle) {
	
		this.handle = handle;
	}

	int getHandle() {
	
		return this.handle;
	}
	
	public void delete() throws RTIexception, ObjectNotFoundException {
		LocalManager.getManager().deletePublicObject(this);
	}
	
	public void refresh() throws IOException, RTIexception {		
		String[] vars = this.getPublicVariables();
		for (int i = 0; i < vars.length; i++)
			getVariable(vars[i]).refresh();
	}
}
