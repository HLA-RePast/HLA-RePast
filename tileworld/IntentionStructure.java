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

import java.util.*;

/**
*   A simplistic part of the Brain which serves as a holder of some state about the 
*   future intentions which are being produced by the cognitive functiosn.
*   The state is held, basically, as a Hole and a list of Tiles which will be used
*   to fill the Hole. The IntentionStructure can be thought of as the interface to
*   the ExecutionEngine as it is from here that the next intention for the engine
*   to perform originates. This class is also the destination of callbacks from the
*   engine when it is requesting its next job. The IntentionStructure communicates
*   with the MeansEndsReasoner through 'propositions' which propose, for example the
*   picking up of a certain Tile. The reasoner serves two purposes, to provide the
*   IntentionStructure (and ultimately the ExecutionEngine) with Routes and to 
*   arbitrate with the IntentionStructure over its current choice of action. It may
*   decide to override the IntentionStructure's decision to go for a certain Tile, or
*   it may suggest to the deliberator that the IntentionStructure's entire strategy
*   (in the form of its current targetted hole) is incorrect.
*/
public class IntentionStructure {
	
	private Agent agent;
	private TileWorld world;
	
	private Hole currentHole;
	private Tile currentTile;
	private SEUProfile currentProfile;
	private ArrayList tileQueue;
	private ArrayList planQueue;
	
	private int activityStatus;
	private ExecutionEngine engine;
	private MeansEndsReasoner reasoner;
	private TaskDeliberator deliberator;
	private SEUCalculator calc;
	
	public static final int FETCHING_TILE = 0;
	public static final int GOING_TO_HOLE = 1;
	public static final int DELIBERATING = 2;
	public static final int START_OF_HOLE = 3;
	
	/**
	*   Creates an IntentionStructure to be used by a TileWorld agent. The intention structure
	*   will initially contain no values and hence no intentions. Its initial 'state' will be
	*   deliberating.
	*   @param reasoner the MeansEndsReasoner associated with the agent, this reasoner will be
	*   passed future intentions for deliberation
	*   @param engine the ExecutionEngine associated with the agent, this engine will be passed
	*   intentions which will be executed.
	*/
	public IntentionStructure(  Agent agent,
								TileWorld world,
								MeansEndsReasoner reasoner, 
								ExecutionEngine engine, 
								TaskDeliberator deliberator,
								SEUCalculator calc) {
		
		this.agent = agent;
		this.world = world;
		this.currentHole = null;
		this.currentTile = null;
		this.currentProfile = null;
		this.tileQueue = new ArrayList();
		this.planQueue = new ArrayList();
		this.activityStatus = DELIBERATING;
		
		this.engine = engine;
		this.reasoner = reasoner;
		this.deliberator = deliberator;
		this.calc = calc;
		deliberator.registerIntentionStructure(this);
	}
	
	/**
	*   Invokes the next intention on the ExecutionEngine (be that to fetch a tile
	*   or, if a tile is already held, to fill a hole). The intended action is first
	*   proposed to the MeansEndsReasoner and then invoked on the associated engine.
	*   If no intentions are found the IntentionStructure sets its state to 'DELIBERATING'
	*   and asks the rest of the brain to help it out. Note that this method is 
	*   synchronized to prevent the instability that can be caused by a very fast
	*   ExecutionEngine registered to a very slow brain, in which case nextIntention
	*   is constantly called by the engine, causing problems with certain data structures
	*   herein.
	*/
	public synchronized void nextIntention() {
				
			
		if (engine == null)
			return;
		//we execute this block if:
		//This is the start of the queue
		//Or
		//The previous plan has been completed and we have been notified of the Hole fill
		//Or
		//The hole in the previous plan has been filled by another or died of old age
		//Or
		//The previous plan has been found to be inaccurate
		if (currentProfile == null) { //get the next SEUProfile out of the plan queue
			if (planQueue.isEmpty()) { //ran out of plans, revert to deliberator
				activityStatus = DELIBERATING;
				deliberator.requestNewGoals();
				return;
			}
			currentProfile = (SEUProfile)planQueue.get(0);
			planQueue.remove(0);
			currentHole = currentProfile.getHole();
			tileQueue = currentProfile.getTiles();
		}
		//we execute this block if
		//We have a plan (and therefore the currentHole and tileQueue are not null)
		//And
		//We have a tile
		if (agent.getTile() != null) { //already have a tile, go to the hole
			Route routeToHole = reasoner.propose(currentHole, this);
			if (routeToHole == null || !world.getHoles().contains(currentHole)) { 
				//hole is unreachable, remove the profile from the plan queue
				currentProfile = null;
				currentHole = null;
				tileQueue = null;
				return;
			}
			activityStatus = GOING_TO_HOLE;
			engine.doHoleFill(currentHole, routeToHole);
			return;
		}
		//we execute this block if 
		//We have a plan 
		//And
		//We don't have a tile
		//And
		//We don't have a list of candidate tiles
		if (tileQueue.isEmpty()) { //all of the tiles we planned have dissapeared
			try {
				currentProfile = calc.getSEUProfile(currentHole);				
				tileQueue = currentProfile.getTiles();
				for (int i = 0; i < planQueue.size(); i++) {
					SEUProfile nextProfile = (SEUProfile)planQueue.get(i);
					nextProfile.getTiles().removeAll(tileQueue);
				}
			}
			catch (InsufficientTilesException e) {
				currentProfile = null;
				currentHole = null;
				tileQueue = null;
			}
			return;
		}
		//we execute this block if
		//We have a plan
		//And
		//We are not holding a tile
		//And
		//We have at least one candidate tile in our tileQueue
		TaskSet toTile = reasoner.propose((Tile)tileQueue.get(0), this);
		currentTile = toTile.getTile();
		boolean gotRoute = (toTile.getRoute() != null);
		while (!gotRoute && !tileQueue.isEmpty()) {
			currentTile = (Tile)tileQueue.get(0);
			toTile = reasoner.propose(currentTile, this);
			if (toTile.getRoute() != null) {
				currentTile = toTile.getTile();
				gotRoute = true;
			}
			else
				tileQueue.remove(0);
		}
		//We execute this block if
		//We have a plan but no route can be found to ANY of the tiles in the tileQueue
		if (!gotRoute) { //then we have failed to find a reachable Tile, panic !
			try {
				currentProfile = calc.getSEUProfile(currentHole);
				tileQueue = currentProfile.getTiles();
				for (int i = 0; i < planQueue.size(); i++) {
					SEUProfile nextProfile = (SEUProfile)planQueue.get(i);
					nextProfile.getTiles().removeAll(tileQueue);
				}
			}
			catch (InsufficientTilesException e) {
				currentProfile = null;
				currentHole = null;
				tileQueue = null;
			}
			return;
		}
		//We execute this block if
		//We have a plan
		//And
		//We are not holding a tile
		//But
		//We have a route to a tile
		else {
			while (tileQueue.contains(currentTile))
				tileQueue.remove(currentTile);
			engine.doTileFetch(currentTile, toTile.getRoute());
		}					
	}
	
	/**
	*   Updates the queue of Tiles to fetch. The intention structure will after this attempt to 
	*   fetch the tiles in the order they appear in this list, from 0 to queue.size() - 1.
	*   @param tiles the new queue of tiles.
	*/
	public synchronized void setTileQueue(ArrayList tiles) {
		
		this.tileQueue = tiles;		
	}
	
	/**
	*   Updates the Hole at which this intention structure is aiming. 
	*   @param the new hole to aim for
	*/
	public synchronized void setHole(Hole hole) {
		
		this.currentHole = hole;
	}
	
	/**
	*   Gets the current Hole at which this IntentionStructure is aiming the agent
	*   @return the current Hole
	*/
	public Hole getHole() {
		
		return this.currentHole;
	}
	
	/**
	*   Gets the current list of Tiles highlighted for using to fill the current Hole
	*   @return the queue of currently intended tiles
	*/
	public List getTileQueue() {
		
		return this.tileQueue;
	}
	
	/**
	*   Gets the current plan (in the form of an SEUProfile) being pursued by this
	*   IntentionStructure
	*   @return the current SEUProfile
	*/
	public SEUProfile getCurrentPlan() {
		
		return this.currentProfile;
	}
	
	/**
	*   Pre-empts the current plan being pursued by this IntentionStructure. This meaning
	*   that the current Hole and list of Tiles (being a subtree of the IntentionStructure's
	*   plan tree provided by the LongTermPlanner) has been found less desireable than 
	*   another hole and list of tiles by the deliberator. The pre-empting plan is placed
	*   at the front of the queue of plans being executed.
	*   @param newPlan an SEUProfile containing a Hole and list of Tiles for that hole
	*   which will pre-empt the plan currently being executed by this IntentionStructure
	*/
	public void preemptPlan(SEUProfile newPlan) {
		
		if (currentProfile != null) {
			System.out.println("********PREMPTED**********");
			planQueue.add(0, currentProfile);
		}
		currentProfile = newPlan;
		currentHole = newPlan.getHole();
		tileQueue = newPlan.getTiles();
		for (int i = 0; i < planQueue.size(); i++) {
			SEUProfile nextPlan = (SEUProfile)planQueue.get(i);
			if (nextPlan.getHole() == newPlan.getHole()) {
				planQueue.remove(i);
				i--;
			}
		}
	}
	
	/**
	*   Gets the (bitmasked) activity status - will be one of:<ul>
	*   <li>IntentionStructure.FETCHING_TILE</li>
	*   <li>IntentionStructure.GOING_TO_HOLE</li>
	*   <li>IntentionStructure.DELIBERATING</li>
	*   @return the current activity status of this IntentionStructure
	*/
	public int getActivityStatus() {
		
		return this.activityStatus;
	}
	
	/**
	*   Notifies an intentionStructure that the agent to which the brain
	*   is registered has successfully filled the given Hole ! If it finds that
	*   the hole specified is the one it is currently trying to fill then it 
	*   will move on to the next hole in its intention tree.
	*   @param h the Hole that the agent has filled.
	*/
	public synchronized void notifyHoleRemoval(Hole h) {
		
		if (currentHole == h) {
			currentProfile = null;
			currentHole = null;
			tileQueue = null;
		}
		else {
			for (int i = 0; i < planQueue.size(); i++) {
				SEUProfile nextProfile = (SEUProfile)planQueue.get(i);
				if (nextProfile.getHole() == h) {
					planQueue.remove(i);
					i--;
				}
			}
		}					
	}
	
	/**
	*   Notifies an intentionStructure that a Tile has been removed from the 
	*   world.
	*   @param t the Tile that was removed
	*/
	public synchronized void notifyTileRemoval(Tile t) {
		
		if (currentTile == t) {
			currentTile = null;
		}
		if (tileQueue != null) {
			while (tileQueue.contains(t))
				tileQueue.remove(t);
		}
		for (int i = 0; i < planQueue.size(); i++) {
			SEUProfile nextProfile = (SEUProfile)planQueue.get(i);
			while(nextProfile.getTiles().contains(t))
				nextProfile.getTiles().remove(t);
		}
	}

	/**
	*   Sets the ExecutionEngine that will be issued actions from this 
	*   IntentionStructure.
	*   @param engine the ExecutionEngine to be registered
	*/
	public synchronized void registerExecutionEngine(ExecutionEngine engine) {
		
		this.engine = engine;
	}
	
	public void addPlan(SEUProfile profile) {
		
		if (world.getHoles().contains(profile.getHole())) {
			planQueue.add(profile);
		}
	}
}
