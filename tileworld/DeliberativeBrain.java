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
 * Created on 25-Jan-2004
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
public class DeliberativeBrain implements AgentBrain {

	protected Agent agent;
	protected TileWorld world;
	
	//brain elements
	protected SEUCalculator calculator;
	protected TaskFilter filter;
	protected TaskDeliberator deliberator;
	protected IntentionStructure structure;
	protected MeansEndsReasoner reasoner;
	protected LongTermPlanner planner;
	
	//threaded elements
	private Thread brainThread;
	private boolean thinking;
	private boolean threadAlive;
	private TileWorldObjects obs;
	
	private boolean doneTest = false;
	
	/**
	*   Constructs a ThreadedBrainImpl associated with the specified
	*   Agent in the specified TileWorld. 
	*   @param agent the Agent this Brain thinks for
	*   @param world the TileWorld the Agent populates
	*   @param calculationTolerance a threshold dictating how long a task will 
	*   take before certain reasoning functions attempt to look for alternative possibile
	*   tasks that achieve the same or similar ends. A high tolerance
	*   will produce a Brain that operates quite quickly but often will
	*   keep doing a task even though better alternatives exist, while a low tolerance
	*   will almost always end up doing the smartest thing at any moment but will also
	*   spend a long time thinking about what that should be - hence a low-tolerance brain
	*   often cannot produce information to an ExecutionEngine at an acceptable rate.
	*   @param filterThreshold a threshold dictating whether an incoming alternative
	*   task will be passed on for further analysis or ignored. Incoming alternatives
	*   are either a product of short-term planning reasoning or a product of perception
	*   of changes in the TileWorld. A high threshold will produce an agent that rarely,
	*   if ever, changes its current task, even if it is of low utility, a low threshold
	*   conversely, produces an agent that will constantly switch between tasks, 
	*   leaving many of them incomplete.
	*/
	public DeliberativeBrain(   Agent agent, 
								TileWorld world, 
								int calculationTolerance, 
								int filterThreshold) {
		
		this.agent = agent;
		this.world = world;
		
		calculator = new SEUCalculator(world, agent);
		deliberator = new TaskDeliberator(calculator);
		filter = new TaskFilter(deliberator, filterThreshold);
		reasoner = new MeansEndsReasoner(world, agent, calculationTolerance, filter, calculator);
		structure = new IntentionStructure(agent, world, reasoner, null, deliberator, calculator);
		filter.registerIntentionStructure(structure);
		planner = new LongTermPlanner(agent, world, calculator, structure);
		
		thinking = false;
		threadAlive = false;
	}
	
	/**
	*   Notifies the Brain that new objects have appeared in the TileWorld
	*   @param obs the TileWorldObjects datastructure of new Tiles and Holes
	*/
	public void updateEnvironment(TileWorldObjects obs) {

		this.obs = obs;
	}
	
	public void holeRemoved(Hole h) {
		
		this.structure.notifyHoleRemoval(h);
	}
	
	public void tileRemoved(Tile t) {
		
		this.structure.notifyTileRemoval(t);
	}

	/**
	*   Registers an ExecutionEngine that will be driven by intentions
	*   supplied by this Brain
	*   @param engine the ExecutionEngine to register
	*/
	public void registerExecutionEngine(ExecutionEngine engine) {
		
		structure.registerExecutionEngine(engine);
	}
	
	/**
	*   Requests that the Brain produce the next intention that it has 
	*   currently planned.
	*/
	public void produceNextIntention() {
		
		this.structure.nextIntention();
	}

	/* (non-Javadoc)
	 * @see tileworld.AgentBrain#think()
	 */
	public void think() {
				
		if (structure.getCurrentPlan() == null)
			reasoner.propose(structure);
		else if (structure.getActivityStatus() == IntentionStructure.FETCHING_TILE) {
			/*
			if (structure.getTileQueue().size() > 0)
				reasoner.propose((Tile)structure.getTileQueue().get(0), structure);
			*/
		}
		else if (structure.getActivityStatus() == IntentionStructure.GOING_TO_HOLE) {
			reasoner.propose(structure.getHole(), structure);
		}
		if (obs != null) {
			filter.filterHoles(obs.getNewHoles());
			obs = null;
		}
		deliberator.deliberate();
		planner.planStep();
	}
	
	/**
	*   Notifies the brain that the specified hole has been filled
	*   @param h the Hole that has been filled
	*/
	public void notifyHoleFill(Hole h) {
		
		this.structure.notifyHoleRemoval(h);
	}
	
	/**
	*   Gets a valid Route between p1 and p2
	*   @param p1 the origin of the desired Route
	*   @param p2 the destination of the desired Route
	*   @return the Route from p1 to p2
	*/
	public Route getRouteFromTo(Point p1, Point p2) {

		return reasoner.getRouteFromTo(p1, p2);
	}
	
	double testThink(int iterations) {
		
		if (doneTest == false)
			doneTest = true;
		long start = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++)
			thinkLots();
		long finish = System.currentTimeMillis() - start;
		return ((double)(finish/iterations)) / 1000;
	}
	
	private void thinkLots() {
		
		think();
	}
}
