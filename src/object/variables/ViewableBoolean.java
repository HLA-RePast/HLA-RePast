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
 * Created on 21-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package object.variables;

import hla.rti13.java1.RTIexception;
import io.Bytes;

import java.io.IOException;


import exceptions.VariableException;

import hla.rti13.java1.RTIinternalError;
import hla.rti13.java1.FederateInternalError;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ViewableBoolean extends ViewableVariable {

	private boolean internalValue;
	
	public ViewableBoolean(boolean value) {
		
		internalValue = value;
	}
	
	public byte[] getBytes() throws RTIinternalError {
		
		return Bytes.getBytes(internalValue);
	}	

	public void set(boolean newValue) throws RTIexception, IOException {
		
		if (super.checkOwnership()) {
			internalValue = newValue;
			obLook.internalUpdate(this);
		}
		else
			throw new VariableException("viewable variable not owned", null);		
	}
	
	public boolean get() {
		
		return internalValue;
	}
	
	public void update(byte[] newValue) throws RTIinternalError, FederateInternalError {
		super.update(newValue);
		internalValue = Bytes.booleanValue(newValue);
	}
}
