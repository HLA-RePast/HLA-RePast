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
 * Created on 29-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tileworld;

import java.sql.Timestamp;

import hla_past.io.RemoteInit;
import hla_past.logging.DBLogger;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JunkModel extends SimModelImpl {

	Schedule scedge;
	long startTime;
	DBLogger logger;
	Timestamp id;
	
	Object2DGrid grid;
	Object2DDisplay dispGrid;
	DisplaySurface dsurf;
	
	String domain;
	
	public JunkModel(String domain) {
		logger = new DBLogger();
		this.domain = domain;
	}

	public String[] getInitParam() {
		return new String[0];
	}
	
	public void begin() {
		id = new Timestamp(System.currentTimeMillis());
		dsurf.display();
	}

	private void populateSchedule() {
		
		if (scedge == null)
			scedge = new Schedule();
			
		scedge.scheduleActionAt(0, new BasicAction() {			
			public void execute() {
				startTime = System.currentTimeMillis();
			}
		});
		
		scedge.scheduleActionAtInterval(1.0, new BasicAction() {
			public void execute() {
				dsurf.updateDisplay();
			}
		});
		
		scedge.scheduleActionAt(10000, new BasicAction() {
			public void execute() {
				int totalTime = (int)(System.currentTimeMillis() - startTime);
				try {
					logger.open();
					logger.logJunkRun(id.toString(), totalTime, domain);
					logger.close();
				}
				catch (Exception e) {
					e.printStackTrace();				
				}
				System.out.println("ID: " + id.toString() + " // total time = " + totalTime);
				getController().stopSim();
				getController().exitSim();
			}
		});
	}
	
	public void setup() {
		scedge = null;
		if (dsurf != null)
			dsurf.dispose();
		grid = new Object2DGrid(50, 50);
		dispGrid = new Object2DDisplay(grid);
		dsurf = new DisplaySurface(this, "Junk Model");
		dsurf.addDisplayable(dispGrid, "junk");
	}

	/* (non-Javadoc)
	 * @see uchicago.src.sim.engine.SimModel#getSchedule()
	 */
	public Schedule getSchedule() {
		if (scedge == null)
			populateSchedule();
		return scedge;
	}

	/* (non-Javadoc)
	 * @see uchicago.src.sim.engine.SimModel#getName()
	 */
	public String getName() {
		return "Junk Model";
	}

	public static void main(String[] args) {
		
		SimInit init = new SimInit();
		init.loadModel(new JunkModel(args[0]), null, false);
	}
}
