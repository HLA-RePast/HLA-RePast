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
 * Created on 16-Dec-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package rtidep;

import io.DEV_TOOLS;
import hla.rti13.java1.AttributeAlreadyBeingAcquired;
import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeHandleSetFactory;
import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.FederateOwnsAttributes;
import hla.rti13.java1.RTIambassador;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RTIDep {

	public static byte[] getLogTime(double time) {		
		try {
			return EncodingHelpers.encodeDouble(time);
		}
		catch (Exception e) {
			DEV_TOOLS.showException("RTIDep.getLogTime(" + time + ")", e);
			return null;		
		}
	}
	
	public static Object getHandleSet(int handle) {
		
		try {
			AttributeHandleSet ahs = AttributeHandleSetFactory.create(1);
			ahs.add(handle);
			return ahs;
		} catch (Exception e) {
			DEV_TOOLS.showException("RTIDep.getHandleSet(" + handle + ")", e);
			return null;
		}
	}
	
	public static void doOwnershipRequest(int object, int attribute, Object rtiAmb) throws FederateOwnsAttributes, AttributeAlreadyBeingAcquired {
		
		try {
			((RTIambassador)rtiAmb).attributeOwnershipAcquisitionIfAvailable(object, (hla.rti13.java1.AttributeHandleSet)getHandleSet(attribute));
		}
		catch (FederateOwnsAttributes e) {
			throw e;
		}
		catch (AttributeAlreadyBeingAcquired e) {
			throw e;
		}
		catch (Exception e) {
			DEV_TOOLS.showException("doOwnershipRequest("+object+","+attribute+")",e);
		}
	}
}
