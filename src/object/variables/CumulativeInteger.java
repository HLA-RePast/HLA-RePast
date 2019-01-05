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

import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import io.Bytes;
import io.DEV_TOOLS;

import java.io.IOException;



import manager.LocalManager;


/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CumulativeInteger extends CumulativeVariable {

	private int internalValue;
	
	public CumulativeInteger(int val) {
		super();
		internalValue = val;
	}
	
	public CumulativeInteger() {
		
		internalValue = 0;
	}
	
	public int get() {
		
		return internalValue;
	}
	
	public void set(int modifier) throws RTIexception {
		
		boolean modSuccess = super.checkOwnership();
		if (!modSuccess || this.ownership != PublicVariable.OwnershipState.OWNED) //will only happen if the object is deleted - trouble !
			return;
		this.internalValue += modifier;
		try {
//			obLook.internalUpdate(this);
			obLook.sendUpdate(this, Bytes.getBytes(modifier));
			obLook.returnOwnership(this);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public byte[] getBytes() throws RTIinternalError {
		return Bytes.getBytes(internalValue);
	}
	
	public void update(byte[] newValue) throws FederateInternalError, RTIinternalError {
		super.update(newValue);		
		
		int modifier = Bytes.intValue(newValue);
		internalValue += modifier;
		DEV_TOOLS.print("<CumulativeInteger::update> " + (internalValue - modifier) + " + " + modifier + " = " + internalValue + " at time " + LocalManager.getManager().getTick());
	}
	
	
}
