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
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package manager;

import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;

/**
 * A class which encapsulates the management of time advance with the RTI.
 * 
 * Provides facilities to make time-advance requests in a blocking or 
 * non-blocking manner.
 * 
 * @see Advancer13
 * 
 * @author Rob Minson
 *
 */
public abstract class Advancer implements LogicalTimeListener {
	
	/**
	 * Perform any setup steps (eg. registering as time-regulating of
	 * time-constrained).
	 * 
	 * The purpose of placing this in a separate method as opposed
	 * to assuming it will be done in the constructor is to allow calls
	 * to {@link RTIambassador#tick()} which may re-enter the local
	 * federate code and (potentially) re-enter this Advancer object itself
	 * (eg. to notifying time-regulating).
	 *
	 * If such a re-entrant call were placed in a constructor, the thread
	 * would attempt to use a null variable.
	 */
	public abstract void initialise() throws RTIexception;
	
	/**
	 * Request this advancer to advance to the given time. The call can
	 * be blocking or non-blocking.
	 * 
	 * A non-blocking call will request the RTI to advance to the given time
	 * and hand the thread to the RTI briefly. This *may* result in advance
	 * to the requested time being completed or may not (a call to {@link #getTime()}
	 * will clear this up).
	 * 
	 * A blocking call will request the RTI to advance to the given time and 
	 * will repeatedly make the call and give the RTI time on the thread until
	 * it is succesfully granted to the requested time. This will result in 
	 * receipt of all external events with earlier timestamps than the requested
	 * time and may take a large amount of real time if other federates are
	 * doing considerable processing
	 * 
	 * @param time the time to which the federate wants to advance
	 * @param block if true, the method will return only once the local time is
	 * equal to 'time', if false, the method will return immediately after 
	 * requesting advance with the RTI, the requested time may have been reached
	 * or not.
	 * @throws RTIexception barring catastrophic RTI failure, the most likely 
	 * cause would be a request to advance to a time &lt; the current local time
	 */
	public abstract void advanceTo(double time, boolean block) throws RTIexception;
	
	/**
	 * Gives the current local time. This is strictly the time to which the 
	 * advancer has been granted by the RTI, not the time that it is currently 
	 * advancing to (not that this method is not explicitly synchronised with
	 * the methods performing time advance requests and time advance grants, 
	 * due to this the result may be incorrect even as it returns.
	 * 
	 * @return the current local time, as granted by the RTI.
	 */
	public abstract double getTime();
	
	/**
	 * Called when the RTI has granted advance to the specified time. This will
	 * be equal to or less than the most recently requested time advance target.
	 * 
	 * Note that, since this method is called by the RTI callback thread, it 
	 * should not be used to enter the RTI itself. If for some reason interaction
	 * with the RTI is necessary in response to this, use semaphores and a 
	 * separate thread to handle this (see the source for the {@link Advancer13}
	 * class for an example of this).
	 */
	public abstract void grantTo(double newTime);
}
