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
package tileworld;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class LongTermPlanner implements Runnable {
	
	private Agent agent;
	private TileWorld world;
	private SEUCalculator calculator;
	private IntentionStructure intStruct;
	
	private ArrayList plan;
	
	private ArrayList plannedTiles;
	private ArrayList plannedHoles;
	
	private Thread planningThread;
	private boolean threadAlive;
	private boolean planning;
	
	public LongTermPlanner( Agent agent,
							TileWorld world,
							SEUCalculator calc,
							IntentionStructure struct) {
						
		this.agent = agent;
		this.world = world;
		this.calculator = calc;
		this.intStruct = struct;
		
		plan = new ArrayList();
		plannedTiles = new ArrayList();
		plannedHoles = new ArrayList();
		
		threadAlive = false;
		planning = false;
	}	
	
	private ArrayList getPlan() {

		return this.plan;
	}
	
	public synchronized void planStep() {
				
		if (!plannedHoles.containsAll(world.getHoles())) {
			SEUProfile nextProfile = calculator.getTopRatedHoleAssuming(new Point(agent.getX(), agent.getY()),
																		plannedTiles, 
																		plannedHoles);
			if (nextProfile != null) {
				plan.add(nextProfile);
				plannedHoles.add(nextProfile.getHole());
				plannedTiles.addAll(nextProfile.getTiles());
				intStruct.addPlan(nextProfile);
			}
		}
	}
	
	public void run() {
		
		while(threadAlive) {
			if (planning) {
				planStep();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
	
	public void startPlanning() {
		
		threadAlive = true;
		planning = true;
		if (planningThread == null) {
			planningThread = new Thread(this);
			planningThread.start();
		}
	}
	
	public void stopPlanning() {
		
		threadAlive = false;
	}
	
	public void pausePlanning() {
		
		planning = false;
	}
	
	public void unPausePlanning() {
		
		planning = true;
	}
}
