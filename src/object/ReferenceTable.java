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

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import exceptions.ObjectDeletedException;
import exceptions.ObjectNotFoundException;

import models.BasicTestObject;

/**
 * A ReferenceTable is a mechanism for accessing and tracking strong
 * references to PublicObjects in the local Java Virtual Machine.
 * 
 * The purpose of this is to allow the local model to lazily perform
 * object deletion. As soon as all strong references to a locally 
 * registered PublicObject have disappeared, a call to getDeadReferences
 * will return a list containing the handle of the object. This can
 * be used to send explicit delete messages to the rest of the 
 * federation.
 * 
 * <b>NOTE</b>: it is easy to missuse this facility. For
 * example, a model keeps a list of chess pieces, removing the piece
 * from the list whenever it is captured. If this is the only 
 * existing reference to the piece, everything is fine and it will
 * become finalizeable. However, if a strong reference to the piece
 * exists in, for eg. planners, display surfaces, loggers, etc. then
 * the garbage collection will not take place and the functioning
 * of the ReferenceTable with which the object is registered will be
 * undermined.
 * 
 * For this reason it is strongly recommended that PublicObject
 * instances only exist in a single datastructure in a model if
 * lazy deletion is to be used. Any representations elsewhere should
 * clone the object or, preferrably, refer back to this single
 * datastructure when needed (eg. one might use a wrapper class
 * ChessPiceDisplayable with a reference back to a ChessPieceList
 * instance in order to graphically display pieces without creating
 * unecessary strong references to them). Complicated models in which
 * this generates lots of messy code should consider disabling lazy
 * deletion and using explicit deletion instead.
 * 
 * @see ObjectLookup#localDelete(PublicObject)
 * 
 * @author Rob Minson
 *
 */
public class ReferenceTable {

	Hashtable<Integer, LookupReference> table = new Hashtable<Integer, LookupReference>();
	private DeletionQueue delQueue = new DeletionQueue();

	/**
	 * Returns a list of LookupReferences corresponding to all locally registered
	 * PublicObjects which have been garbage collected since the method was last
	 * called. 
	 * 
	 * Typically this will be used to determine which locally registered shared 
	 * objects can now be explicitly deleted throughout the rest of the federation 
	 * (eg. via calling deleteObjectInstance(...) on the local RTIambassador)
	 * having already been implicitly deleted by the local model.
	 * 
	 * @return the list of references to finalizeable objects.
	 */
	public List<LookupReference> getDeadReferences() {		
		System.gc();		
		return delQueue.flushDeletions();
	}

	/**
	 *   Get the PublicObject specified by the given handle.
	 *   
	 *   @param ID the RTI-assigned handle of the required object
	 *   @return the corresponding PublicObject
	 *   @throws ObjectNotFoundException if the object has not been placed in the
	 *   lookup
	 *   @throws ObjectDeletedException if the object has been garbage collected 
	 *   by the local JVM prior to this call.
	 */
	public PublicObject getObject(int ID) 
		throws ObjectDeletedException, ObjectNotFoundException {
			
		LookupReference lookup = table.get(ID);
		if (lookup == null)
			throw new ObjectNotFoundException();
		else if (lookup.isGCd())			
			throw new ObjectDeletedException();
			
		return (PublicObject)lookup.get();
	}

	/**
	 *   Insert a PublicObject in to the ReferenceTable. A reference to the 
	 *   object can then be regained by a call to getObject. If a PublicObject 
	 *   with the same handle exists in the Lookup it will be overwritten by 
	 *   this object (this works fine assuming the RTI provides unique IDs).
	 *   
	 *   @param ob the PublicObject to register with this ReferenceTable
	 */
	public void register(PublicObject ob) {
		
		Integer ID = new Integer(ob.getHandle());	
		LookupReference newRef = new LookupReference(ob);
		table.put(ID, newRef);
		delQueue.add(newRef);
	}
	
	/**
	 * Removes the reference to the given object from the ReferenceTable.
	 * 
	 * This disables reference tracking for this object. It will still
	 * be possible to refer to strong references of this object elsewhere
	 * in calling code, however, if the object becomes finalizable, the 
	 * rest of the federation will not be automatically informed of its
	 * deletion.
	 * 
	 * @param ob the PublicObject which should no longer be tracked by
	 * this ReferenceTable
	 * @throws ObjectNotFoundException if the given object is not currently
	 * registered with this ReferenceTable
	 */
	public void deregister(PublicObject ob) throws ObjectNotFoundException {
		if (!table.containsKey(ob.getHandle()))
			throw new ObjectNotFoundException();
		else
			table.remove(ob.getHandle());
	}
	
	
	/**
	 * Package-accessible version of the above for removing when you only
	 * have the handle.
	 * 
	 * @param handle
	 */
	void deregister(int handle) {
		table.remove(handle);
	}
	
	public Set<Integer> allHandles() {
		return table.keySet();
	}
	
	public static void main(String[] args) {
		ReferenceTable rt = new ReferenceTable();
		PublicObject po1 = new BasicTestObject(); po1.setHandle(1);
		PublicObject po2 = new BasicTestObject(); po2.setHandle(2);
		rt.register(po1);
		rt.register(po2);
		po1 = null;
		
		List<LookupReference> dead = rt.getDeadReferences();
		for (LookupReference lr : dead)
			System.out.println(lr.getID() + " is dead");
	}
}
