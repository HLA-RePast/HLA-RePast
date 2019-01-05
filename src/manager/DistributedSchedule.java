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
 * Created on 27-Oct-2003
 */
package manager;

import io.DEV_TOOLS;
import hla.rti13.java1.RTIexception;
import logging.StopWatch;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;

/**
 * This class is a modified version of the standard RePast
 * {@link Schedule} object.
 * 
 * The distributed schedule inserts synchronisation points
 * in to the standard execution algorithm and ensures that
 * the model is in a state consistent with the current
 * logical time before {@link BasicAction} events scheduled
 * for this tick.
 * 
 * @author Rob Minson
 */
public class DistributedSchedule extends Schedule {

	private static boolean LOCAL_QUEUE = true;
	private static boolean EXTERNAL_QUEUE = false; 

	Advancer13 advancer;
	EventQueue extQ;
	BasicAction displayAction = null;
	boolean freshestQueue = LOCAL_QUEUE;
	
	public static StopWatch repastTimer = new StopWatch();
	public static StopWatch hlaTimer = new StopWatch();

	/**
	 * Create a DistributedSchedule that will use the given datastructures
	 * to ensure stable time advance.
	 * @param adv The advancer is used to ensure the schedule is granted 
	 * 	advance to a given logical time before any locally scheduled events 
	 * 	at that time are executed.
	 * @param evtQ This is a queue for external events (usually held by the
	 * 	{@link LocalManager} of this JVM. This queue is incorporated in to 
	 * 	the standard discrete event engine of the RePast schedule to ensure 
	 * 	that any incoming external events which occured before the next local 
	 * 	event are committed in the local model before the local event
	 * 	executes. 
	 * @param flushCall This is a 
	 */
	DistributedSchedule(Advancer13 adv, EventQueue evtQ) {
		super();
		advancer = adv;
		this.extQ = evtQ;
	}
	
	/**
	 * Performs the standard ScheduleBase job of preExecute (determining
	 * the group of events (including LAST, FIRST and RANDOM events) should
	 * be executed.
	 * 
	 * The distributed version also ensures that the federate has permission
	 * from the synchronisation algorithm to advance to the time of the next
	 * local event. In the process of this advance the federate will receive 
	 * a set of events from other federates which will be buffered by the time
	 * the preExecute call returns.
	 * 
	 * The first act that the execute() method performs is to apply these 
	 * external events to the local model (see the flushCall references below)
	 */
	public void preExecute() {

//		System.out.println("<DistributedSchedule> preExecute()");
		
		//ascertain the earliest time of any event in each of the queues
		double queueMin = Double.POSITIVE_INFINITY;
		double lastMin = Double.POSITIVE_INFINITY;		
		if (!actionQueue.isEmpty())
			queueMin = actionQueue.peekMin().getNextTime();
		if (!lastQueue.isEmpty())
			lastMin = lastQueue.peekMin().getNextTime(); 
		
		//The queues don't need to be used again so we can now pre-execute.
		//The groupToExecute variable will then be full of BasicActions all with
		//a timestamp equal to the nextEventTime value we obtain below
		super.preExecute();
		
		//this is simply the min of the lowest timestamp values from each queue
		double nextEventTime = Math.min(queueMin, lastMin);
		
		//now do a blocking advance to this time, when this returns the LocalManager
		//will have an external event queue full of events between the original time 
		//and nextEventTime (inclusive):
		try {
			DEV_TOOLS.print("<DistributedSchedule.preExecute> advancing to " + nextEventTime);		
			
			hlaTimer.start();
			advancer.advanceTo(nextEventTime, true);
			hlaTimer.stop();
		}
		catch (Exception e) {
			DEV_TOOLS.showException(e);
		}
	}
	
	public void execute() {
		
		//this is exactly the same as the normal Schedule class' execute
		//method but it misses out the preExecute call, we simply assume 
		//this has taken place because the local manager should always de-queue
		//and execute all events in the external queue (populated during a call to 
		//preExecute)
				
		if (!preExecuted)
			this.preExecute();
		if (!extQ.isEmpty())
			freshestQueue = EXTERNAL_QUEUE;
		/* at this point the rti has sent us a bunch of events taking us up to 'now */
		
//		System.out.println("<DistributedSchedule> flushCall.execute()");
		hlaTimer.start();
		try {
				DEV_TOOLS.print("<DistributedSchedule::execute> flushing external events at time " + LocalManager.getManager().getTick());
				DEV_TOOLS.indent();
			LocalManager.getManager().flushExternalEventQueueSngThrd();
				DEV_TOOLS.undent();
				DEV_TOOLS.print("<DistributedSchedule::execute> events flushed");
			LocalManager.getManager().resetOwnership();
		} catch (RTIexception e) {
			e.printStackTrace();
		}
		hlaTimer.stop();		
		/* at this point all those events have been applied in the local model */
		
		if (displayAction != null) {
			repastTimer.start(); 
			displayAction.execute();
			repastTimer.stop();
		}
		/* at this point all those effects have been rendered/logged/etc. */
		
		//END OF PREVIOUS TICK ^
		
			/* if we wanted to do some kind of barrier synchronisation we would do it here */
		
		//START OF THIS TICK v
		
//		System.out.println("<DistributedSchedule> groupToExecute.execute()");
		if (groupToExecute.size() > 0)
			freshestQueue = LOCAL_QUEUE; 
		repastTimer.start();
		groupToExecute.execute();				
		groupToExecute.reSchedule(null);
		
		
		/* delete garbage collected objects */
		if (LocalManager.LAZY_DELETION) {
			try { 
				LocalManager.getManager().obLookup.cleanPublicObjects();
			} catch (RTIexception e) {
				e.printStackTrace();
			}
		}
		repastTimer.stop();
		/* at this point all local events at 'now' have been executed */
		
		preExecuted = false;		
	}
	
	/**
	 * This allows for the scheduling of a single BasicAction that should be used
	 * for refreshing the display surface during a gui-based run. It is extremely 
	 * important that this action should not attempt to modify the state of any shared
	 * objects.<p>
	 * Generally local events are integrated in to the global scheduling mechanism, 
	 * however, were this to be true of screen-update events it would dramatically
	 * reduce the performance of the system without any gain in fidelity or correctness.
	 * @param action an event (or nested sequence of events) which updates the 
	 * display of the simulation at this node.
	 */
	public void addDisplayAction(BasicAction action) {		
		displayAction = action;
	}
	
	/**
	 * Returns the timestamp of the most recently executed event.
	 */
	public synchronized long getCurrentTime() {
		
		long extQMin = (long)extQ.findLast();
		if (!extQ.isEmpty())
			extQMin = Math.min((long)extQ.peekTime(), extQMin);
		if (freshestQueue == EXTERNAL_QUEUE)
			return extQMin;
		else
			return super.getCurrentTime();
	}
}
