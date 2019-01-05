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

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Random;
import java.awt.Point;
import uchicago.src.sim.util.*;


/**
*   A planning class that uses recursive column-/row-scans to find viable
*   Routes between two points in environments with arbitrary blocking
*   objects.
*/
public class RoutePlanner {
	
	private TileWorld world;
	private Agent agent;
	private static long MAX_RECURSION = 100;
	
	/**
	*   Constructs a RoutePlanner that will construct Routes subjective
	*   to the specified Agent in the specified TileWorld
	*   @param agent the Agent for whom to plan Routes
	*   @param world the TileWorld through which to plan the Routes
	*/
	public RoutePlanner(TileWorld world, Agent agent) {
		
		this.world = world;
		this.agent = agent;
		uchicago.src.sim.util.Random.createUniform();
		System.out.println("" + Runtime.getRuntime().freeMemory());
	}
	
	/**
	*   Generates a Route from the Agent to the specified Point
	*   @param p the Point which the Route should lead to
	*   @return a Route whose origin should be the point that
	*   the agent currently occupies and whose destination should
	*   be p. If no Route can be found the method returns null.
	*/
	public Route generateRouteTo(Point p) {
				
		try {
			return doSearch(new Point(agent.getX(), agent.getY()), p);
		}
		catch (UnreachableException e) {
			return null;
		}			
	}
	
	/**
	*   Generates a Route from the Agent to the specified object
	*   @param ob the SpatialObject which the Route should lead to
	*   @return a Route whose origin should be the point that
	*   the agent currently occupies and whose destination should
	*   be ob. If no Route can be found the method returns null
	*/  
	public Route generateRouteTo(SpatialObject ob) {
		
		if (ob == null)
			return null;
		try {
			return doSearch(new Point(agent.getX(), agent.getY()), new Point(ob.getX(), ob.getY()));
		}
		catch (UnreachableException e) {
			return null;
		}	
	}
	
	/**
	*   Generates a Route from p1 to p2
	*   @param p1 the Point the Route should start at
	*   @param p2 the Point the Route should end at
	*   @return a Route whose origin should be p1 and whose
	*   destination should be p2. If no Route can be found the 
	*   method returns null.
	*/ 
	public Route generateRouteFromTo(Point p1, Point p2) {
				
		try {
			return doSearch(p1, p2);
		}
		catch (UnreachableException e) {
			return null;
		}			
	}	
	
	private Route doSearch(Point start, Point finish) throws UnreachableException {
		
		//System.out.println("route requested from (" + start.getX() + "/" + start.getY() + ") to (" + finish.getX() + "/" + finish.getY() + ")");
		if (start.equals(finish)) {
			Route r = new Route();
			r.addWayPoint(finish);
			return r;
		}
		Hashtable foundPoints = new Hashtable();
		foundPoints.put(finish, new Integer(0));
		List routes = new ArrayList();
		List complete = new ArrayList();
		List temp = new ArrayList();
		Route init = new Route();
		init.addWayPoint(finish);
		routes.add(init);
		while (routes.size() > 0) {
			temp.clear();			
			for (int i = 0; i < routes.size(); i++) {
				Route curr = (Route)routes.get(i);
				List hood = getNeighbours(curr.getStart());
				for (int n = 0; n < hood.size(); n++) {
					Point nextPoint = (Point)hood.get(n);
					Route nextStep = new Route(curr, nextPoint);
					boolean isNew = false;
					if (foundPoints.containsKey(nextPoint)) {
						int currCost = ((Integer)foundPoints.get(nextPoint)).intValue();
						if (currCost > nextStep.getLength()) {
							isNew = true;
							foundPoints.put(nextPoint, new Integer(nextStep.getLength()));
						}
					}
					else {
						isNew = true;
						foundPoints.put(nextPoint, new Integer(nextStep.getLength()));						
					}
					if (isNew) {
						if (nextPoint.equals(start))
							complete.add(nextStep);
						else
							temp.add(nextStep);
					}
				}
			}
			routes.clear();
			routes.addAll(temp);
			//System.out.println("routes.size() = " + routes.size());
			//System.out.println("complete.size() = " + complete.size());
		}
		if (complete.size() == 0)
			throw new UnreachableException();
		Collections.sort(complete);
		Route chosen = (Route)complete.get(0);
		//System.out.println("found a route !");
		//chosen.printRoute();
		return (Route)complete.get(0);
	}
	
	
	private List getNeighbours(Point p) {
		
		List hood = new ArrayList();
		int pX = (int)p.getX();
		int pY = (int)p.getY();
		int worldX = world.getSizeX();
		int worldY = world.getSizeY();
		if (pX < worldX - 1 && !world.isTerrain(pX + 1, pY))
			hood.add(new Point(pX + 1, pY));
		if (pY < worldY - 1 && !world.isTerrain(pX, pY + 1))
			hood.add(new Point(pX, pY + 1));
		if (pX > 0 && !world.isTerrain(pX - 1, pY))
			hood.add(new Point(pX - 1, pY));
		if (pY > 0 && !world.isTerrain(pX, pY - 1))
			hood.add(new Point(pX, pY - 1));
		return hood;
	}
}	
