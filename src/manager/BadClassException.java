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
package manager;


/**
 * An exception indicating some problem with a {@link Class} 
 * object passed to the HLA_RePast engine.
 * 
 * This generally happens is the class is badly formed (eg.
 * has no default constructor) or if the class is used to
 * instantiate proxies for remote objects but the class was
 * not registered with the local database during instantiation
 * of the LocalManager.
 * 
 * @see LocalManager#createManager(Class[], int, String, String)
 * @see LocalManager#getRemoteObjects(Class, Class) 
 * 
 * @author Rob
 */
public class BadClassException extends RuntimeException {

	public BadClassException(String s) {
		super(s);
	}

	public BadClassException(String s, Throwable t) {
		super(s, t);
	}
}
