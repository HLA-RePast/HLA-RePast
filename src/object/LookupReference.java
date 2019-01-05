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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import models.BasicTestObject;

/**
 * A class which allows us to keep HLA information about objects which
 * have been garbage collected by the local JVM.
 * 
 * A weak reference holds information on an object but does not prevent
 * that object from being finalized by the JVM garbage collector.
 * 
 * A LookupReference adds RTI handle information to this. This is used
 * by HLA_RePast to issue deleteObjectInstance events when an object
 * dissapears from the local model.
 * 
 * @author Rob Minson
 */
public class LookupReference extends WeakReference<PublicObject> {

	private int ID;

	public LookupReference(PublicObject ob) {
		super(ob, new ReferenceQueue<PublicObject>());
		this.ID = ob.getHandle();
	}
	
	public boolean isGCd() {
		return isEnqueued();
	}

	public int getID() {	
		return this.ID;
	}
	
	public static void main(String[] args) {
		PublicObject po = new BasicTestObject();
		po.setHandle(1);
		LookupReference lr = new LookupReference(po);
		po = null;
		System.gc();
		if (lr.isGCd())
			System.out.println("object was garbage collected");
		else
			System.out.println("object was not garbage collected");
	}
}
