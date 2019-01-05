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

import object.PublicObject;
import uchicago.src.sim.util.RepastException;

/**
 * Thrown whenever a class is registered with the local 
 * database, but it does not have the right hierarchy (ie
 * does not extends {@link PublicObject}).
 * 
 * @author Rob
 */
public class BadClassTreeException extends RepastException {

	private Class offendingClass;
	
	public BadClassTreeException(Class c, String message) {
	
		super(message);
		this.offendingClass = c;
	}

	public BadClassTreeException(String message) {
	
		this(null, message);
	}

	public Class getOffendingClass() {
	
		return offendingClass;
	}
}
