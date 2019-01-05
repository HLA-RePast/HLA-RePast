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

import java.awt.Point;
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
public class ViewablePoint extends ViewableVariable {

	private Point internalVal;
	
	public ViewablePoint(Point p) {
		
		internalVal = p;
	}
	
	public ViewablePoint(int x, int y) {
		
		internalVal = new Point(x, y);
	}

	public void set(Point newValue) throws RTIexception, IOException {
		
		if (super.checkOwnership()) {
			internalVal = newValue;
			obLook.internalUpdate(this);
		}
		else
			throw new VariableException("viewable variable not owned", null);		
	}
	
	public Point get() {
		
		return internalVal;
	}
	
	public void update(byte[] newValue) throws RTIinternalError, FederateInternalError {
			super.update(newValue);		
			internalVal = (Point)Bytes.objectValue(newValue);
	}

	public byte[] getBytes() throws RTIinternalError {
		
		return Bytes.getBytes(internalVal); 
	}
}
