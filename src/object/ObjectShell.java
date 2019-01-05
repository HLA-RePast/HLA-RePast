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

import io.ReflectedAttributes13;

import java.util.Hashtable;

/**
 * Object Shells are needed to record the discovery and
 * updating of proxy objects before we know what class should
 * be used to instantiate those objects.
 * 
 * The shell simply stores the object's handle and a list
 * of &lt;attribute handle, byte value&gt; pairs. This list of
 * updates can be used when the object is instantiated for 
 * real to provide up to date attribute values without having
 * to request a refresh of the object with the federation.
 * 
 * @see RemoteObject
 * @see RemoteBase
 * @see ProxyList
 * @author Rob Minson
 *
 */
class ObjectShell {

	protected Hashtable<Integer, byte[]> attributeTable;
	protected int handle;

	ObjectShell(int handle) {	
		this.handle = handle;
		this.attributeTable = new Hashtable<Integer, byte[]>();
	}

	int getHandle() {	
		return this.handle;
	}

	void update(ReflectedAttributes13 attrs) {
		for (int i = 0; i < attrs.size(); i++)
			setAttribute(attrs.getHandle(i), attrs.getValue(i));
	}
	
	synchronized void setAttribute(int handle, byte[] value) {	
		attributeTable.put(new Integer(handle), value);
	}

	synchronized byte[] getAttribute(int handle) {
		return (byte[])attributeTable.get(handle);
	}
}
