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
 * Created on 27-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package io;

import java.util.Vector;

import uchicago.src.sim.engine.BatchController;
import uchicago.src.sim.engine.SimEvent;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RemoteController extends BatchController {

	public RemoteController(Vector arg0) {
		super(arg0);
	}
	
	protected void setupSchedule() {
	  simRun = new Runnable() {

		public void run() {

		  while (go) {

			schedule.preExecute();
			time = schedule.getCurrentTimeDouble();
			schedule.execute();
		  }

		  // if reach here simulation has ended
		  //timeMod = 0;
		  schedule.executeEndActions();
		  model.fireSimEvent(new SimEvent(this, SimEvent.STOP_EVENT));
		  time = 0;
		  runFinished = true;
		  notifyMonitor();
		}
	  };

	  runThread = null;
	}	
}