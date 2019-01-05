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
 * Created on 25-Oct-2003
 */
package manager;


import java.util.LinkedList;
import java.util.List;


import hla.rti13.java1.AttributeAcquisitionWasNotRequested;
import hla.rti13.java1.AttributeAlreadyOwned;
import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeNotKnown;
import hla.rti13.java1.AttributeNotOwned;
import hla.rti13.java1.AttributeNotPublished;
import hla.rti13.java1.CouldNotDiscover;
import hla.rti13.java1.EnableTimeConstrainedWasNotPending;
import hla.rti13.java1.EnableTimeRegulationWasNotPending;
import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.EventRetractionHandle;
import hla.rti13.java1.FederateAmbassador;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.FederateOwnsAttributes;
import hla.rti13.java1.InvalidFederationTime;
import hla.rti13.java1.NullFederateAmbassador;
import hla.rti13.java1.ObjectClassNotKnown;
import hla.rti13.java1.ObjectNotKnown;
import hla.rti13.java1.ReflectedAttributes;
import hla.rti13.java1.TimeAdvanceWasNotInProgress;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;


/**
 * A class to do asynchronous handling of callbacks.
 * 
 * Often when the RTI performs a callback on this federate,
 * we need to respond in some way (eg. a time advance grant or
 * an ownership grant). Because many RTIs are non-reentrant
 * we can't use the callback thread to do this. 
 * 
 * Instead we insert the appropriate handler (in the form of
 * a {@link Call} object) in to a synchronised queue which is
 * continually flushed on a separate thread.
 * 
 * This class also acts as the {@link FederateAmbassador} 
 * implementation in HLA_RePast, it is the only component to 
 * which the RTI directly talks.
 * 
 * @see 
 * 
 * @author Rob Minson
 */
class CallbackBuffer extends NullFederateAmbassador {

	private boolean timeLogging;
	private boolean updateLogging;
	private boolean ownershipLogging;	

	private List<Call> callbacks = new LinkedList<Call>();
	
	private LocalManager manager;
	
	public CallbackBuffer(LocalManager manager) {
		this.manager = manager;
	}

	public boolean isEmpty() {	
		return callbacks.isEmpty();
	}
	
	public void executeNext() {
		callbacks.remove(0).execute();
	}

	protected void enque(Call c) {
		callbacks.add(c);
	}
	
	/////////////////////// FederateAmbassador implementation. /////////////////


		//////////////////// Registration and Update methods //////////////////////

	public void discoverObjectInstance(	int theObject, 
										int theObjectClass, 
										String objectName)
		throws CouldNotDiscover, ObjectClassNotKnown, FederateInternalError {
		
		//impl
		//printUpdateRelated(	"DiscoverObjectInstance class: " + 
			//				theObjectClass + 
			//				" / instance " + 
			//				theObject);
	
		class DiscoverInstanceCall implements Call {
			private int theObject;
			private int theClass;
			DiscoverInstanceCall(int o, int c) {
				this.theObject = o;
				this.theClass = c;
			}
			public void execute() {
				manager.discoverInstance(this.theObject, this.theClass);
			}
		}
	
		enque(new DiscoverInstanceCall(theObject, theObjectClass));
	}	

	public void reflectAttributeValues(	int theObject,
										ReflectedAttributes attrs,
										byte[] theTime,
										String userTag,
										EventRetractionHandle retractionHandle)
											throws 	ObjectNotKnown,
													AttributeNotKnown,
													FederateOwnsAttributes,
													InvalidFederationTime,
													FederateInternalError {
				
		//printUpdateRelated("REFLECT: object: " + theObject + " time = " + EncodingHelpers.decodeDouble(theTime));
		class ReflectValuesCall implements Call {
			private int theObject;
			private ReflectedAttributes13 theAttrs;
			private double time;
			ReflectValuesCall(int o, ReflectedAttributes13 attrs, double time) {
				this.theObject = o;
				this.theAttrs = attrs;
				this.time = time;
			}
			public void execute() {
				manager.reflectValues(this.theObject, this.theAttrs, this.time);
			}
		}

		Call call = new ReflectValuesCall(	theObject, 
											new ReflectedAttributes13(attrs),
											EncodingHelpers.decodeDouble(theTime));
		enque(call);
	}

	
	public void removeObjectInstance(
			int theObject,
			byte[] theTime,
			String userMessage,
			EventRetractionHandle retractionHandle)
			throws ObjectNotKnown, InvalidFederationTime, FederateInternalError {
			
		//printUpdateRelated("removeObjectInstance object = " + theObject);
		
		class RemoveInstanceCall implements Call {
			private int theObject;
			private double time;
			RemoveInstanceCall(int o, double time) {
				this.theObject = o;
				this.time = time;
			}
			public void execute() {
				manager.removeInstance(this.theObject, this.time);
			}
		}

		enque(new RemoveInstanceCall(	
									theObject, 
									EncodingHelpers.decodeDouble(theTime)));
		
	}

	
	public void provideAttributeValueUpdate(int handle, AttributeHandleSet arg1)
		throws
			ObjectNotKnown,
			AttributeNotKnown,
			AttributeNotOwned,
			FederateInternalError {
		
			class RefreshValuesCall implements Call {
				private int theObject;
				RefreshValuesCall(int o) {
					this.theObject = o;
				}
				public void execute() {
					manager.refreshRequest(theObject);
				}
			}

			Call call = new RefreshValuesCall(handle);
			enque(call);
	}




		////////////////////////// Object Ownership Methods ////////////////////////
		///////////////// (note we only bother with 'pulling' negotiations) ////////



	public void 
		attributeOwnershipAcquisitionNotification(	
										int theObject, 
										AttributeHandleSet securedAttributes)
		throws 	ObjectNotKnown, 
				AttributeNotKnown, 
				AttributeAcquisitionWasNotRequested, 
				AttributeAlreadyOwned, 
				AttributeNotPublished, 
				FederateInternalError {
	
		//printOwnershipRelated("attributeOwnershipAcquisition " + theObject);
	
		class OwnershipAcquisitionCall implements Call {
			private int object;
			private AttributeHandleSet attrs;
			OwnershipAcquisitionCall(int ob, AttributeHandleSet attrs) {
				this.object = ob;
				this.attrs = (AttributeHandleSet)attrs.clone();
			}
			public void execute() {
				manager.ownershipAcquired(this.object, this.attrs);
			}
		}
	
		enque(new OwnershipAcquisitionCall(theObject, securedAttributes));
	}

	public void 
		attributeOwnershipUnavailable(	int theObject, 
										AttributeHandleSet theAttributes) 
		throws 	ObjectNotKnown, 
				AttributeNotKnown, 
				AttributeAlreadyOwned, 
				AttributeAcquisitionWasNotRequested, 
				FederateInternalError {
	
		//printOwnershipRelated("attributeOwnershipUnavailable " + theObject);
	
		class AcquisitionFailureCall implements Call {
			private int object;
			private AttributeHandleSet attrs;
			AcquisitionFailureCall(int ob, AttributeHandleSet attrs) {
				this.object = ob;
				this.attrs = (AttributeHandleSet)attrs.clone();
			}
			public void execute() {
				manager.ownershipFailed(this.object, this.attrs);
			}
		}	
		enque(new AcquisitionFailureCall(theObject, theAttributes));
	}

	public void 
		requestAttributeOwnershipRelease(int theObject, 
										AttributeHandleSet candidateAttributes, 
										byte userSuppliedTag[]) 
		throws 	ObjectNotKnown, 
				AttributeNotKnown, 
				AttributeNotOwned, 
				FederateInternalError {		
		//printOwnershipRelated("requestAttributeOwnershipRelease " + theObject);	
		class ReleaseRequestCall implements Call {
			private int ob;
			private AttributeHandleSet attrs;
			ReleaseRequestCall(int ob, AttributeHandleSet attrs) {
				this.ob = ob;
				this.attrs = attrs;
			}
			public void execute() {
				manager.releaseRequest(ob, attrs);
			}
		}
	
		enque(new ReleaseRequestCall(theObject, candidateAttributes));
	}

		

	public void timeRegulationEnabled(byte[] theFederateTime) 
		throws 	InvalidFederationTime, 
				EnableTimeRegulationWasNotPending, 
				FederateInternalError {
		//printTimeRelated("timeRegulationEnabled");
		manager.regulationEnabled(decodeTime(theFederateTime));
	}

	public void timeConstrainedEnabled(byte[] theFederateTime)
		throws 	InvalidFederationTime, 
				EnableTimeConstrainedWasNotPending, 
				FederateInternalError {		
		//printTimeRelated("timeConstrainedEnabled");
		manager.constrainedEnabled(decodeTime(theFederateTime));
	}

	public void timeAdvanceGrant(byte[] time) 
		throws 	InvalidFederationTime, 
				TimeAdvanceWasNotInProgress, 
				FederateInternalError {
		//printTimeRelated("timeAdvanceGrant");
		manager.timeAdvanceGrant(decodeTime(time));
	}
	
	private double decodeTime(Object theTime) throws FederateInternalError {		
		return EncodingHelpers.decodeDouble((byte[])theTime);
	}
	
	
	
	
	//bottlenecked for easy turn-on/turn-off de-bugging
	private void print(String message) {				
		DEV_TOOLS.print(message + " <callbackModule>");	
	}
	
	private void printTimeRelated(String message) {		
		if (timeLogging) {
			//print("TIME" + message);
		}
	}
	
	void timeLoggingOn(boolean logging) {		
		timeLogging = logging;
	}
	
	private void printUpdateRelated(String message) {		
		if (updateLogging) {			
			//print(message);
		}
	}
		
	void udpateLoggingOn(boolean logging) {		
		updateLogging = logging;
	}
	
	private void printOwnershipRelated(String message) {		
		if (ownershipLogging) {
			//print("OWNERSHIP" + message);
		}
	}
	
	void ownershipLoggingOn(boolean logging) {		
		this.ownershipLogging = logging;
	}	


	
	
	
	
	
	
	
	
	
	
	/* BELOW IS OLD, MULTI-THREADED CODE */
	
//	public void start() {		
//		//print("started");
//		queue.start();
//	}
//
//	public void reset() {	
//		//print("reset");
//		queue.stop();
//		queue = new CallBackQueue();
//		System.gc();
//	}
//
//	public void destroy() {	
//		queue.stop();
//		this.manager = null;
//		System.gc();
//	}

}
