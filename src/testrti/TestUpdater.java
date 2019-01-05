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
 * Created on 10-Dec-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package testrti;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.SuppliedAttributes;
import io.Bytes;
import manager.LocalManager;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestUpdater {

	int instanceHandle;
	
	int countryClassHandle;
	int popHandle;
	
	long pop;
	byte[] serialPop;

	public TestUpdater(RTIambassador amb) throws RTIexception {
		
		countryClassHandle = amb.getObjectClassHandle("Country");
		AttributeHandleSet handles = LocalManager.getHandleSet(1);
		popHandle = amb.getAttributeHandle("Population", countryClassHandle);
		handles.add(popHandle);
		
		amb.publishObjectClass(countryClassHandle, handles);
		amb.subscribeObjectClassAttributes(countryClassHandle, handles);
		
		instanceHandle = amb.registerObjectInstance(countryClassHandle);
		
		pop = (long)(Math.random() * 1000);
	}
	
	public long doUpdate(RTIambassador amb, double time) throws Exception {
		
		SuppliedAttributes attrs = LocalManager.getSuppliedAttributes(2);
		pop = (long)(pop * 1.1);
		serialPop = Bytes.getBytes(pop);
		attrs.add(popHandle, serialPop);
		amb.updateAttributeValues(instanceHandle, attrs, EncodingHelpers.encodeDouble(time), "");				
		return pop;
	}
	
	public int getInstanceHandle() {
		
		return instanceHandle;
	}
	
	public int getPopHandle() {
		
		return popHandle;
	}
	
	public int getClassHandle() {
		
		return countryClassHandle;
	}
	
	public long getPopulation() {
		
		return pop;
	}
	
	public byte[] getLoggingValue() {
		
		return serialPop;
	}
}