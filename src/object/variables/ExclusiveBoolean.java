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
 * Created on 20-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package object.variables;

import hla.rti13.java1.RTIexception;
import io.Bytes;
import hla.rti13.java1.RTIinternalError;
import hla.rti13.java1.FederateInternalError;

import java.io.IOException;


import exceptions.ExcludedException;
import exceptions.VariableException;


/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExclusiveBoolean extends ExclusiveVariable {

	private boolean internalValue;
	
	public ExclusiveBoolean() {
		
		internalValue = false;
	}
	
	public ExclusiveBoolean(boolean initVal) {
		super();
		internalValue = initVal;
	}
	
	public boolean get() {
		
		return internalValue;
	}
	
	public void set(boolean value) throws ExcludedException, RTIexception {
		
		if (super.checkOwnership()) {
			internalValue = value;
			try {
				obLook.internalUpdate(this);
			}
			catch (IOException e) {
				throw new VariableException("serialisation error", e);
			}
		}
		else
			throw new ExcludedException();
	}
	
	public void update(byte[] newValue) throws RTIinternalError, FederateInternalError {
		
		super.update(newValue);
		internalValue = Bytes.booleanValue(newValue);
	}
	
	public byte[] getBytes() throws RTIinternalError{
		
		return Bytes.getBytes(internalValue);
	}
}
