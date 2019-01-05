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
 * Created on 05-Dec-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package manager;

import io.DEV_TOOLS;
import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.RTIexception;
import rtidep.Threads;

/**
 * An implementation of the {@link Advancer} class specific to 
 * the RTI-NG 1.3 DMSO RTI implementation.
 */
public class Advancer13 extends Advancer {

	private boolean regulating;
	private boolean constrained;
	
	private boolean advancing = false;
	
//	private RTIambassador amb;
	private double time;
	private double expectedTime;

	public Advancer13() throws RTIexception {		
		time = 0;
		expectedTime = 0;
	}
	
	public void initialise() throws RTIexception {
		
		DEV_TOOLS.print("<Advancer13> starting initialise");
		LocalManager.getRTI().enableTimeConstrained();
		while (!constrained) {
			Threads.RTI_SHORT_TICK();
		}
		/* current time, lookahead */
		LocalManager.getRTI().enableTimeRegulation(	EncodingHelpers.encodeDouble(time), 
													EncodingHelpers.encodeDouble(0));
		while (!regulating) {
			Threads.RTI_SHORT_TICK();
		}
		DEV_TOOLS.print("<Advancer13> done initialise");
	}
	
	public void advanceTo(double time, boolean block) throws RTIexception {
		expectedTime = time;		
		if (block) {
			while (getTime() != expectedTime) {
				if (!advancing)
					advanceTo(time, false);
				else
					Threads.RTI_SHORT_TICK();
			}
		}
		else {
			advanceTo(EncodingHelpers.encodeDouble(time));
			Threads.RTI_SHORT_TICK();
		}
	}
	
	protected void advanceTo(byte[] serTime) throws RTIexception {		
		LocalManager.getRTI().nextEventRequestAvailable(serTime);
		advancing = true;
	}
	
	public double getTime() {		
		return this.time;
	}
	
	/**
	 * Although Advancers do implement the LogicalTimeListener interface they
	 * cannot be used like normal listeners of this type. The invocation of the Advancer's
	 * grantTo may cause it to immediately issue another advance request.
	 * If the grant events are fired to other LogicalTimeListeners after this has occured
	 * then they may attempt to perform actions only valid in the time_granted phase
	 * causing RTIexceptions.<p>
	 * Advancers should then occupy a special place in the federate ambassador code,
	 * either always shuffling to the end of the listener list, or occupying a separate
	 * variable which is always accessed last during any event firing.
	 */
	public void grantTo(double newTime) {		
		time = newTime;
		advancing = false;		
	}
	
	public void constrain(double time) {
		DEV_TOOLS.print("<Advancer13> constrained (time = " + time + ")");
		constrained = true;
		this.time = time;
	}
	
	public void regulating(double time) {
		DEV_TOOLS.print("<Advancer13> regulating (time = " + time + ")");
		regulating = true;
		this.time = time;
	}
}