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
package exceptions;

import manager.LocalManager;
import object.ProxyList;


/**
 * Thrown in many situations when an interaction is attempted
 * locally which refers to an object which has been deleted
 * by an external (or possibly even internal) event.
 * 
 * This primarily happens since there is no way of forcing
 * objects to be removed from the local JVM in response to 
 * this occuring in the federation at large.
 * 
 * If this exception has been thrown it is probably because
 * the local model is not keeping objects organised in a 
 * sensible way, but is instead passing references to them
 * around and using these references without checking that 
 * they still actually exist in (for example) an instance of
 * {@link ProxyList} obtained via 
 * {@link LocalManager#getProxies(Class, Class)}.
 * 
 * @author Rob Minson
 *
 */
public class ObjectDeletedException extends Exception {

	public ObjectDeletedException() {
		super("The requested object has been deleted");
	}
}
