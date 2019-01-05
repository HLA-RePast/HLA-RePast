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
import java.util.List;
import java.awt.*;

/**
*   An area of the agent brain which is capable of making high-level
*   recommendations about strategy on-the-fly. The MeansEndsReasoner
*   is invoked in two circumstances:<ul>
*   <li>When the brain is about to execute an action (i.e. taking a 
*   held tile to a hole or going to a tile to pick it up.) In this
*   instance the reasoner will decide whether changing either the 
*   target tile or hole will provide better utility than the currently
*   proposed one.</li>
*   <li>When the brain has run out of intentions altogether and new 
*   ones must be produced. In this circumstance the reasoner is capable
*   of checking the entire state of the world and, if potential options
*   still exist, creating a new set of high-level ends and passing them on.</li>
*   </ul>
*   As noted in the following documentation, a full re-appraisal of the 
*   situation should only be undertaken when all other options are exhausted
*   as the computation necessary for producing a the new ends is complex and
*   time consuming.<p>
*   The logic of the reasoner is as follows: making a decision fetch one
*   tile or another is not a big decision and can be made on-the-fly. Therefore
*   when a brain proposes fetching a Tile the MeansEndsReasoner is considered
*   to be in a good position (having a map of the world and a RoutePlanner) to
*   make the decision either to provide a route for that Tile, or to 
*   pre-empt the Brain and provide a different Tile, and a route for it.
*   Decisions made about which hole to be working on at any moment are more
*   significant and cannot be made by the reasoner alone. Therefore, if a 
*   Hole is proposed to the reasoner it returns a Route to it, however, during
*   this deliberation it may also find evidence that another Hole is worth 
*   considering, in which case it may propose this alternative to the 
*   Brain's central deliberator through the filter, but it may NEVER decide
*   to change the Hole on its own, it will always return a Route to that hole.
*/
public class MeansEndsReasoner {
	
	private SEUCalculator calc;
	private TaskFilter filter;
	private TileWorld world;
	private Agent agent;
	private RoutePlanner planner;
	private int agreementThreshold;
	
	/**
	*   Constructs a new MeansEndsReasoner for the sepecified Agent
	*   operating in the specified TileWorld. The reasoner has access to the specified 
	*   SEUCalculator (which sould also, of course, be registered to the same agent and
	*   TileWorld. The threshold indicates the degree of agreement that the reasoner
	*   will adopt when deciding whether to undertake lengthy utility calculations.
	*   Simply put, if an action is proposed, if it will take a number of moves more
	*   than the given threshold, computation will be undertaken to find a better choice
	*   this may not be below the threshold but it may be better than the current proposition
	*   A high threshold will result in a fast but often stupid reasoner while a low 
	*   threshold will result in a slow but accurate reasoner which will almost always
	*   find the best alternative to a proposition (if an alternative exists).
	*   @param agent an agent.
	*   @param world the TileWorld in which the agent exists
	*   @param SEUCalculator a calculator for this agent, in this world
	*   @param threshold the level of tolerance practiced by this reasoner (see above)
	*/
	public MeansEndsReasoner(   TileWorld world, 
								Agent agent, 
								int threshold,
								TaskFilter filter,
								SEUCalculator calc) {
		
		this.calc = calc;
		this.filter = filter;
		this.world = world;
		this.agent = agent;
		this.planner = new RoutePlanner(world, agent);
		this.agreementThreshold = threshold;
	}
	
	/**
	*   Supplies the state of an intentionStructure considering the given 
	*   TileWorld for evaluation by this reasoner. The reasoner will embark
	*   on a lengthy calculation, reviewing the entire state of the TileWorld
	*   and forwarding the best calculable plan to the other parts of the brain.
	*   @param intStruct the IntentionStructure to be analysed
	*/
	public void propose(IntentionStructure intStruct) {
		
		Hole topHole = calc.getTopRatedHole();
		if (topHole != null)
			filter.filterHole(topHole);
	}		
	
	/**
	*   Proposes the aim of the agent taking a tile to the specified hole
	*   This method will propose to a deliberator taking the tile to a different 
	*   hole if routing reveals a closer one is available.
	*   @param h The hole that is being proposed
	*   @param intStruct the intentionStructure that is proposing this
	*/
	public Route propose(Hole h, IntentionStructure intStruct) {
				
		Route routeToCurrent = planner.generateRouteTo(h);
		if (routeToCurrent == null || routeToCurrent.getLength() > this.agreementThreshold) {
			Hole closestHole = closestHoleToAgent(world.getHoles());
			if (closestHole == null)
				return null;
			if (closestHole != h) {
				Route routeToClosest = planner.generateRouteTo(closestHole);
				if (routeToClosest != null) {
					if (routeToCurrent == null)
						filter.filterHole(closestHole);
					else {
						if (routeToCurrent.getLength() > routeToClosest.getLength())
							filter.filterHole(closestHole);
					}
				}
			}
		}
		return routeToCurrent;
	}
	
	/**
	*   Proposes the aim of the agent picking up the specified tile.
	*   This will return a TaskSet to the invoker. This set will be composed
	*   of a tile and a route to that tile. If the tile proposed is reasonable
	*   then the invocation simply provides the best route to that tile.
	*   If the means-ends reasoner disagrees with the choice then the TaskSet
	*   will contain a different tile and the route to that tile.
	*   @param t the Tile currently being intended for picking up
	*   @param intStruct the intentionStructure that is proposing this
	*   @return a TaskSet containing either t or a different Tile if a better option
	*   was found. The set also contains a Route to the returned tile.
	*/
	public TaskSet propose(Tile t, IntentionStructure intStruct) {
		
		/*
		Route routeToCurrent = planner.generateRouteTo(t);
		if (routeToCurrent == null || routeToCurrent.getLength() > this.agreementThreshold) {
			Tile closestTile = closestTileToAgent(world.getTiles());
			if (closestTile != null && closestTile != t) {
				Route routeToClosest = planner.generateRouteTo(closestTile);
				if (routeToClosest != null) {
					if (routeToCurrent == null || routeToCurrent.getLength() > routeToClosest.getLength())
						return new TaskSet(routeToClosest, closestTile);
				}
			}
		}
		return new TaskSet(routeToCurrent, t);
		*/
		Tile closest = closestTileToAgent(world.getTiles());
		if (closest != t) {
			Route toOther = planner.generateRouteTo(closest);
			Route toCurrent = planner.generateRouteTo(t);
			if (toOther == null && toCurrent == null)
				return new TaskSet(null, t);
			int lengthOther = Integer.MAX_VALUE;
			int lengthCurrent = Integer.MAX_VALUE;
			if (toOther != null)
				lengthOther = toOther.getLength();
			if (toCurrent != null)
				lengthCurrent = toCurrent.getLength();
			if (lengthOther <= lengthCurrent) {
				return new TaskSet(toOther, closest);
			}
			else {
				return new TaskSet(toCurrent, t);
			}
		}
		else
			return new TaskSet(planner.generateRouteTo(t), t);
	}
	
	/**
	*   Gets a valid Route between the two specified Points.
	*   @param p1 the start of the Route
	*   @param p2 the destination of the Route
	*   @return the valid Route
	*/
	public Route getRouteFromTo(Point p1, Point p2) {
		
		return planner.generateRouteFromTo(p1, p2);
	}
	
	private Hole closestHoleToAgent(List holes) {
	
		if (holes.isEmpty())
			return null;
		int index = 0;
		Point agentPoint = new Point(agent.getX(), agent.getY());
		Hole firstHole = (Hole)holes.get(0);
		int closestDist = simpleDist(agentPoint, new Point(firstHole.getX(), firstHole.getY()));
		for (int i = 1; i < holes.size(); i++) {
			Hole nextHole = (Hole)holes.get(i);
			Point nextPoint = new Point(nextHole.getX(), nextHole.getY());
			if (simpleDist(agentPoint, nextPoint) < closestDist)
				index = i;
		}
		return (Hole)holes.get(index);
	}
	
	private Tile closestTileToAgent(List tiles) {
		
		if (tiles.isEmpty())
			return null;
		int index = 0;
		Point agentPoint = new Point(agent.getX(), agent.getY());
		Tile firstTile = (Tile)tiles.get(0);
		int closestDist = simpleDist(agentPoint, new Point(firstTile.getX(), firstTile.getY()));
		for (int i = 1; i < tiles.size(); i++) {
			Tile nextTile = (Tile)tiles.get(i);
			Point nextPoint = new Point(nextTile.getX(), nextTile.getY());
			if (simpleDist(agentPoint, nextPoint) < closestDist)
				index = i;
		}
		return (Tile)tiles.get(index);
	}
	
	private int simpleDist(Point p1, Point p2) {
		
		int xDist = Math.abs((int)p1.getX() - (int)p2.getX());
		int yDist = Math.abs((int)p1.getY() - (int)p2.getY());
		return xDist + yDist;
	}
}
