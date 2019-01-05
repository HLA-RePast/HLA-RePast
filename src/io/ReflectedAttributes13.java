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
 * Created on 06-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package io;

import hla.rti13.java1.ArrayIndexOutOfBounds;
import hla.rti13.java1.ReflectedAttributes;

import java.util.ArrayList;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReflectedAttributes13 {

	class HVPair {
		int handle;
		byte[] value;
		HVPair(int handle, byte[] value) {
			this.handle = handle;
			this.value = value;
		}
	}
	
	private ArrayList<HVPair> pairs = new ArrayList<HVPair>();
	
	public ReflectedAttributes13(ReflectedAttributes attrs) {		
		try {
			for (int i = 0; i < attrs.size(); i++) {
				pairs.add(new HVPair(attrs.getHandle(i), attrs.getValue(i)));
			}
		}
		catch (ArrayIndexOutOfBounds e) {}
	}
	
	public int size() {
		
		return pairs.size();
	}
	
	public byte[] getValue(int index) {
		
		return ((HVPair)pairs.get(index)).value;
	}
	
	public int getHandle(int index) {
		
		return ((HVPair)pairs.get(index)).handle;
	}
}
