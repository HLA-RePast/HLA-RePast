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
 * Created on 24-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Just a logical-time sorted queue of events.
 * 
 * This version is <b>not</b> thread-safe and should
 * only be used in single-threaded execution models.
 * 
 * @author Rob Minson
 *
 */
class EventQueue {

	private List<LoggedCall> events;
	private double lastTime = Double.POSITIVE_INFINITY;
	
	protected EventQueue() {	
		events = new ArrayList<LoggedCall>();
	}

	protected int size() {
		return events.size();
	}
	
	@SuppressWarnings("unchecked")
	protected void enque(Call c, double logTime) {
		events.add(new LoggedCall(c, logTime));
		Collections.sort(events);
	}

	protected boolean isEmpty() {	
		return events.isEmpty();
	}

	protected double peekTime() {	
		return ((LoggedCall)events.get(0)).getTime();
	}
	
	protected double findLast() {
		return lastTime;
	}

	protected Call next() {
		LoggedCall next = (LoggedCall)events.remove(0);
		lastTime = next.logTime;
		return next;
	}

	class LoggedCall implements Call, Comparable {		
		Call c;
		double logTime;
		LoggedCall(Call c, double logTime) {
			this.c = c;
			this.logTime = logTime;
		}
		public void execute() {
			c.execute();
		}
		
		double getTime() {
			return logTime;
		}
		public int compareTo(Object o) {
			double otherTime = ((LoggedCall)o).getTime();
			if (otherTime > logTime)
				return 1;
			else if (otherTime == logTime)
				return 0;
			else
				return -1;
		}
	}
}
