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

import java.awt.*;
import java.util.ArrayList;

import testrti.DEV_TOOLS;

public class ExecutionEngineImpl implements ExecutionEngine {

	private Agent agent;
	private TileWorld world;
	private AgentBrain brain;
	
	private Hole targetHole;
	private Tile targetTile;
	
	private Route currentRoute;
	private Route tempRoute;	
	private Point currentTarget;
	
	private boolean busy;
	private boolean movingToStartOfRoute;
	
	protected static int DOING_NOTHING = 0;
	protected static int GETTING_TILE = 1;
	protected static int FILLING_HOLE = 2;
	private int currentActivity;
	
	/**
	*   Creates an ExecutionEngineImpl to drive the specified agent through
	*   the specified TileWorld according to the commands of the specified brain
	*   @param agent the Agent to act upon
	*   @param world the TileWorld that the agent exists in
	*   @param brain the AgentBrain that this engine will accept instructions from
	*/
	public ExecutionEngineImpl(Agent agent, TileWorld world, AgentBrain brain) {
		
		this.agent = agent;
		this.world = world;
		this.brain = brain;
		busy = false;
		movingToStartOfRoute = false;
		brain.registerExecutionEngine(this);
		targetHole = null;
		targetTile = null;
		currentRoute = null;
		currentActivity = DOING_NOTHING;
	}
	
	/**
	*   Grants permission for this engine to drive the actions of its associated
	*   Agent through one time step. In this implementation the engine will either
	*   perform the next action in its itinerary or, if it has no actions registered,
	*   will request the brain to deliver the next intention to it.
	*/
	public void act() {
		
		brain.think();
		if (!busy) {
			brain.produceNextIntention();
		}
		else {
			nextAction();
		}

	}
	
	/**
	*   Instructs this engine to move to the specified Tile using the specified
	*   Route and pick the tile up
	*   @param t the Tile to be picked up
	*   @param r the Route leading from the current position to the position of 
	*   the Tile
	*/
	public void doTileFetch(Tile t, Route r) {
		
		if (!busy) {
			if (agent.getTile() != null) {
				brain.produceNextIntention();
				return;
			}
			targetTile = t;
			currentRoute = r;
			busy = true;
			Point start = currentRoute.getStart();
			currentActivity = GETTING_TILE;
			if (agent.getX() != start.getX() || agent.getY() != start.getY()) {
				tempRoute = currentRoute;
				currentRoute = brain.getRouteFromTo(new Point(agent.getX(), agent.getY()), start);
				movingToStartOfRoute = true;
			}
		}
	}
	
	/**
	*   Instructs this engine to move to the specified Hole using the specified
	*   Route and drop the current tile in it.
	*   @param h the Hole to be filled
	*   @param r the Route leading to the Hole, starting at the position of the Agent
	*   and ending at the position of the Hole.
	*/
	public void doHoleFill(Hole h, Route r) {
		
		if (!busy) {
			targetHole = h;
			currentRoute = r;
			busy = true;
			Point start = currentRoute.getStart();
			currentActivity = FILLING_HOLE;
			if (agent.getX() != start.getX() || agent.getY() != start.getY()) {
				tempRoute = currentRoute;
				currentRoute = brain.getRouteFromTo(new Point(agent.getX(), agent.getY()), start);
				movingToStartOfRoute = true;
			}
				
		}
	}
	
	private void nextAction() {
			
		if ((targetTile == null && targetHole == null) || currentRoute == null) {
			targetTile = null;
			targetHole = null;
			currentRoute = null;
			busy = false;
			currentActivity = DOING_NOTHING;
			brain.produceNextIntention();
			return;
		}			
		
		if (currentTarget == null) {
			//then we arrived at a way-point last turn or this is the start of the route
			if (!currentRoute.hasMoreWayPoints()) {
				//then we are either at the start of the actual route or the end of it
				if (movingToStartOfRoute) {
					//then we are now at the start of the actual route
					currentRoute = tempRoute;
					movingToStartOfRoute = false;
				}
				else {
					//then we are at the end of the actual route and should execute the current command
					if (currentActivity == GETTING_TILE) {
						try {
							world.pickupTile(agent.getX(), agent.getY(), targetTile, agent);
							currentActivity = DOING_NOTHING;
							busy = false;   //then set our variables back to the neutral state
							return;
						}
						catch (BadTileCoordsException e) {
							//the brain gave us a wierd route, so refresh it, don't change anything else and carry
							//on as normal.
							Point correctTilePoint = new Point(e.getX(), e.getY());
							currentRoute = 
								brain.getRouteFromTo(	new Point(agent.getX(), agent.getY()), 
																		correctTilePoint);
							return;
						}
						catch (NotOnBoardException e) {
							//at this point we're screwed - best bet is to go back to neutral and hope the brain sorts
							//things out.
							currentActivity = DOING_NOTHING;
							currentRoute = null;
							currentTarget = null;
							busy = false;
							return;
						}
						catch (TileOwnershipException e) {
							//The agent already has a tile so go back to neutral and let the brain work out that
							//we should be hole-filling, not tile-fetching
							currentActivity = DOING_NOTHING;
							currentRoute = null;
							currentTarget = null;
							busy = false;
							return;
						}						
					}
					else if (currentActivity == FILLING_HOLE) {
						try {
							world.fillHole(agent.getX(), agent.getY(), agent.getTile(), targetHole, agent);
							targetHole = null;
							currentActivity = DOING_NOTHING;
							busy = false;
							return;
						}
						catch (BadHoleCoordsException e) {
							//As above, the brain has made a booboo, so refresh the route and try
							//again.
							Point correctHolePoint = new Point(e.getX(), e.getY());
							currentRoute = brain.getRouteFromTo(new Point(agent.getX(), agent.getY()), correctHolePoint);
							return;
						}
						catch (NotOnBoardException e) {
							//as above, we're screwed at this point
							currentActivity = DOING_NOTHING;
							currentRoute = null;
							currentTarget = null;
							busy = false;
							return;
						}
						catch (TileOwnershipException e) {
							//either we don't own that tile or we don't own ANY tile, in either case
							//we should not try and be clever, just reset to neutral and let the brain
							//figure things out.
							currentActivity = DOING_NOTHING;
							currentRoute = null;
							currentTarget = null;
							busy = false;
							return;
						}
					}
				}
			}
			else {
				//then we have not completed a route yet and just get its next waypoint
				currentTarget = currentRoute.getNextWayPoint();
			}
		}
		//if we have not yet returned we cannot be at the end of a route 
		//and know that we must have a non-null way point
		try {
			boolean arrived = moveTowards((int)currentTarget.getX(), (int)currentTarget.getY());
			if (arrived)
				currentTarget = null;
		}
		catch (BlockedException e) {
			System.out.println("blocked");
			tempRoute = null;
			currentRoute = null;
			int targetX = 0;
			int targetY = 0;
			if (currentActivity == GETTING_TILE) {
				targetX = targetTile.getX();
				targetY = targetTile.getY();
			}
			else {
				targetX = targetHole.getX();
				targetY = targetHole.getY();
			}
			currentRoute = brain.getRouteFromTo(new Point(agent.getX(), agent.getY()),
												new Point(targetX, targetY));
			if (currentRoute != null)
				movingToStartOfRoute = false;
			else {
				currentTarget = null;
				busy = false;
			}
		}
	}	

	private boolean moveTowards(int x, int y) throws BlockedException {
		
		if (agent.getX() == x && agent.getY() == y)
			return true;
		world.removeObjectAt(agent.getX(), agent.getY(), agent);
		int xDist = (int)Math.abs(x - agent.getX());
		int yDist = (int)Math.abs(y - agent.getY());
		if (xDist >= yDist) {
			try {
				moveToX(x, y);
			}
			catch (BlockedException e) {
				try {
					moveToY(x, y);
				}
				catch (BlockedException ex) {
					world.putObjectAt(agent.getX(), agent.getY(), agent);
					throw ex;
				}
			}
			return false;
		}
		else {
			try {
				moveToY(x, y);
			}
			catch (BlockedException e) {
				try {
					moveToX(x, y);
				}
				catch (BlockedException ex) {
					world.putObjectAt(agent.getX(), agent.getY(), agent);
					throw ex;
				}
			}
		}
		if (agent.getX() == x && agent.getY() == y)
			return true;
		else
			return false;
	}
	
	private void moveToX(int x, int y) throws BlockedException {
		
		if (x > agent.getX()) {
			if (!world.isTerrain(agent.getX() + 1, agent.getY())) {
				agent.setPosition(new Point(agent.getX() + 1, agent.getY()));
				world.putObjectAt(agent.getX(), agent.getY(), agent);
				//System.out.println("agent placed at " + agent.getX() + "/" + agent.getY());
			}
			else
				throw new BlockedException();
		}
		else {
			if (!world.isTerrain(agent.getX() - 1, agent.getY())) {
				agent.setPosition(new Point(agent.getX() - 1, agent.getY()));
				world.putObjectAt(agent.getX(), agent.getY(), agent);
				//System.out.println("agent placed at " + agent.getX() + "/" + agent.getY());
			}
			else
				throw new BlockedException();
		}
	}
	
	private void moveToY(int x, int y) throws BlockedException {
		
		if (y > agent.getY()) {
			if (!world.isTerrain(agent.getX(), agent.getY() + 1)) {
				agent.setPosition(new Point(agent.getX(), agent.getY() + 1));
				world.putObjectAt(agent.getX(), agent.getY(), agent);
				//System.out.println("agent placed at " + agent.getX() + "/" + agent.getY());
			}
			else
				throw new BlockedException();
		}
		else {
			if (!world.isTerrain(agent.getX(), agent.getY() - 1)) {
				agent.setPosition(new Point(agent.getX(), agent.getY() - 1));
				world.putObjectAt(agent.getX(), agent.getY(), agent);
				//System.out.println("agent placed at " + agent.getX() + "/" + agent.getY());
			}
			else
				throw new BlockedException();
		}
	}
}
