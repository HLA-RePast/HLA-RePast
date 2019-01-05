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
 * Created on 05-Apr-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tileworld;

import java.awt.Point;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReactiveBrain implements AgentBrain {

	private ExecutionEngine engine;	
	private SEUCalculator calc;
	private RoutePlanner planner;
	private Agent parent;

	public ReactiveBrain(Agent parent, RoutePlanner planner, SEUCalculator calc) {
		
		this.parent = parent;
		this.planner = planner;
		this.calc = calc;
	}

	public void updateEnvironment(TileWorldObjects obs) {
		//nothing to do for a reactive brain
	}


	public void registerExecutionEngine(ExecutionEngine engine) {
		
		this.engine = engine;
	}


	public void produceNextIntention() {
		
		if (parent.isHoldingTile()) {
			doHoleFill();
		}
		else {
			doTileFetch();
		}
	}
	
	private void doHoleFill() {
		
		Hole h = calc.getTopRatedHole();
		Route r = planner.generateRouteTo(h);
		engine.doHoleFill(h, r);
	}
	
	private void doTileFetch() {
		
		try {
			Tile t = calc.getNearestTile();
			Route r =planner.generateRouteTo(t);
			engine.doTileFetch(t, r);
		}
		catch (InsufficientTilesException e) {}
	}

	public void think() {
		// TODO Auto-generated method stub
		//nothing to do here
	}


	public void notifyHoleFill(Hole h) {
		// TODO Auto-generated method stub
		//don't care
	}


	public Route getRouteFromTo(Point p1, Point p2) {
		// TODO Auto-generated method stub
		return planner.generateRouteFromTo(p1, p2);
	}


	public void tileRemoved(Tile t) {
		// TODO Auto-generated method stub
		//don't care
	}

	public void holeRemoved(Hole h) {
		// TODO Auto-generated method stub
		//don't care
	}

}
