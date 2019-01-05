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
 * Created on 01-Dec-2003
 */
package manager;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeHandleSetFactory;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.ReflectedAttributes;
import hla.rti13.java1.SuppliedAttributes;
import hla.rti13.java1.SuppliedAttributesFactory;
import io.Bytes;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;

import java.sql.Timestamp;

import rtidep.Threads;
/**
 * The coupler class transparently couples the federate to the federation in a controlled
 * manner. The general use for a Coupler is:
 * <ol>
 * 	<li>Create a federation execution, if succesful then you are leader (boolean)</li>
 * 	<li>Create Coupler using boolean from 1 (and some <i>n</i> for num_federates)</li>
 * 	<li>Initilalise the coupler</li>
 * 	<li>'Turn on' (in some way) delegation of callbacks to delegation methods </li>
 * 	<li>Call finalise</li>
 * 	<li>Once finalise returns the federate will be in a federation of exactly <i>n</i>
 * 	federates all of which will share the same Timestamp value as the fedexid
 * 	which is retrievable by the call to getFedExId</li>
 * </ol>
 */
public class Coupler implements LogicalTimeListener {

	private boolean coupled = false;
	static String NODE = "node";
	int nodeHandle;
	static String NAME = "name";
	int nameHandle;
	  
	Timestamp fedexid = null;
	static String FEDEXID = "fedexID";
	int fedexidHandle;
	static String TS = "ts";
	int tsHandle;
	int fedexidInstHandle = -1;
	  
	boolean isLeader = false;
	int numFederates;
	
	private boolean granted = true;
	private double time;
	
	/**
	 * Creates a Coupler with the given details
	 * @param isLeader if the parent federate created the fedex
	 * @param numFederates the number of federates which should be coupled to 
	 * @throws RTIexception usually if the .fed file does not contain the correct
	 * classes.
	 */
	public Coupler(boolean isLeader, int numFederates) 
		throws RTIexception {
		
		this.isLeader = isLeader;
		this.numFederates = numFederates;
		if (isLeader)
			this.numFederates--;
	}

	/**
	 * Sets up the master/slave relationship by ensuring exactly 1 federate 
	 * has the job of waiting for all federates to join, then sending the
	 * fedexid (for logging/debugging) whilst n-1 federates
	 * have the job of joining, then listening for the fedexid.
	 * 
	 * This is done by the leader subscribing to nodeHandle objects and 
	 * publishing fedexID objects, whilst everyone else does the converse.
	 *
	 * @throws RTIException usually if the RTI doesn't support 
	 * publication/subscription management services, or if the .fed file
	 * is not correctly configured with the HLA_RePast admin types:
	 * <ul>
	 * 	<li>
	 * 		node
	 * 		<ul>
	 * 			<li>
	 * 				name
	 * 			</li>
	 * 		</ul>
	 * 	</li>
	 * 	<li>
	 * 		fedexID
	 * 		<ul>
	 * 			<li>
	 * 				ts
	 * 			</li>
	 * 		</ul>
	 * 	</li>
	 * </ul>
	 */
	public void initialise() throws RTIexception {
		
		nodeHandle = LocalManager.getRTI().getObjectClassHandle(NODE);
		nameHandle = LocalManager.getRTI().getAttributeHandle(NAME, nodeHandle);
		AttributeHandleSet handles = AttributeHandleSetFactory.create(1);
		handles.add(nameHandle);
		if (isLeader)
			LocalManager.getRTI().subscribeObjectClassAttributes(nodeHandle, handles);
		else
			LocalManager.getRTI().publishObjectClass(nodeHandle, handles);

		fedexidHandle = LocalManager.getRTI().getObjectClassHandle(FEDEXID);
		tsHandle = LocalManager.getRTI().getAttributeHandle(TS, fedexidHandle);
		handles = AttributeHandleSetFactory.create(1);
		handles.add(tsHandle);
		if (!isLeader)
			LocalManager.getRTI().subscribeObjectClassAttributes(fedexidHandle, handles);
		else
			LocalManager.getRTI().publishObjectClass(fedexidHandle, handles);
	}
	
	/**
	 * Finalise the coupling procedure, once this method returns the federate will
	 * be coupled to exactly <i>n</i> other federates in a single federation all of 
	 * whom will share the given fedexid.
	 * @param time the time the federate is currently at (usually 0.0)
	 * @return the fedexid of the federation to which the Federate is now coupled. Useful for
	 * logging, distributed debugging, etc.
	 * @throws Exception
	 */
	public Timestamp finalise(double time) throws RTIexception {
		
		this.time = time;
		
		if (isLeader) {
			while (numFederates > 0) {
				Threads.RTI_LONG_TICK();
			}
			SuppliedAttributes supAtt = SuppliedAttributesFactory.create(1);
			fedexid = new Timestamp(System.currentTimeMillis());
			byte[] tsSerial = Bytes.getBytes(fedexid);
			supAtt.add(tsHandle, tsSerial);
			byte[] encTime = Bytes.getBytes(time);
			fedexidInstHandle = 
				LocalManager.getRTI().registerObjectInstance(fedexidHandle);
			Threads.RTI_SHORT_TICK();
			LocalManager.getRTI().updateAttributeValues(fedexidInstHandle, 
														supAtt, encTime, "");
			Threads.RTI_LONG_TICK();
			DEV_TOOLS.print("leader finalised");  		
		}  	
		else {
			LocalManager.getRTI().registerObjectInstance(nodeHandle);
			while (fedexidInstHandle == -1) {  			
				Threads.RTI_SHORT_TICK();
			}
			DEV_TOOLS.print("discovered fedexidInstance");
			NERACall(time);
			while (fedexid == null) {
				if (granted)
					NERACall(time);
				Threads.RTI_SHORT_TICK();
			}
			while (!granted) {
				Threads.RTI_SHORT_TICK();
			}
			DEV_TOOLS.print("follower finalised");
		}
		coupled = true;
		return fedexid;
	}
	
	/**
	 * Delegates a discoverObjectInstance callback to the coupler. If the callback is
	 * involving objects used in the coupling procedure they will be processed (usually
	 * moving the finalise method on in some way).
	 * @param id the instanceID obtained through the callback
	 * @param classID the classID obtained through the callback
	 * @return true if the callback concerned coupling (if not you're in trouble)
	 */
	public boolean delegatedDiscover(int id, int classID) {
		
		DEV_TOOLS.print("delegated discover: ");		
		if (isLeader && classID == nodeHandle) {
			DEV_TOOLS.print("(node)");
			numFederates--;
		}
		else if (!isLeader && classID == fedexidHandle) {
			DEV_TOOLS.print("(fedexid)");
			fedexidInstHandle = id;
		}
		else
			return false;
		return true;
	}
	
	/**
	 * Delegates a reflectAttributeValues callback to the coupler. If the callback is
	 * coupler business then it will process the events, moving the finalise method on
	 * in some way.
	 * @param instID the instanceHandle from the callback
	 * @param attrs the values from the callback
	 * @return true if the callback was coupler business
	 * @throws Exception usually during de-serialisation or if the attrs object is bad
	 */
	public boolean delegatedReflect(int instID, ReflectedAttributes attrs) 
		throws Exception {
		
		DEV_TOOLS.print("delegated reflect");
		if (!isLeader && instID == fedexidInstHandle) {
			DEV_TOOLS.print("extracting timestamp");
			DEV_TOOLS.print("size of attrs = " + attrs.size());
			fedexid = (Timestamp)Bytes.objectValue(attrs.getValue(0));
			return true;
		}
		return false;
	}
	
	public boolean delegatedReflect(int instID, ReflectedAttributes13 attrs) 
		throws Exception {
	
		DEV_TOOLS.print("delegated reflect13");
		if (!isLeader && instID == fedexidInstHandle) {
			DEV_TOOLS.print("extracting timestamp");
			DEV_TOOLS.print("size of attrs = " + attrs.size());
			fedexid = (Timestamp)Bytes.objectValue(attrs.getValue(0));
			return true;
		}
		return false;
	}
	
	public void grantTo(double newTime) {
		
		this.time = newTime;
		granted = true;
	}
	
	private void NERACall(double targetTime) throws RTIexception {
		
		granted = false;
		byte[] t = Bytes.getBytes(targetTime);
		LocalManager.getRTI().nextEventRequestAvailable(t);
		Threads.RTI_LONG_TICK();
	}
	
	public boolean isCoupled() {
		
		return coupled;
	}
}
