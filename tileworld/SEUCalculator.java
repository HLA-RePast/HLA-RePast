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
import java.awt.Point;

/**
* A general utility object which is capable of providing 
* Subjective Estimated Utility data for various options 
* provided by other cognitive modules
*/
public class SEUCalculator {
	
	private TileWorld world;
	private Agent agent;
	private RoutePlanner planner;
	
	/**
	* Creates a new SEUCalculator which will assess SEU scores for
	* the specified Agent operating in the specified TileWorld
	* @param world the TileWorld being observed by this SEUCalculator
	* @param agent the Agent to which the produced scores are subjective
	*/
	public SEUCalculator(TileWorld world, Agent agent) {
		
		this.world = world;
		this.agent = agent;
		this.planner = new RoutePlanner(world, agent);
	}
	
	/**
	* Get the SEU score for a specified hole
	* @param h the Hole for which to produce the score
	* @return the specified hole's SEU score, relative to the parent agent
	* @throws InsufficientTilesException if the TileWorld does not contain enough
	* tiles to fill the specified hole.
	*/
	public double getSEURating(Hole h) throws InsufficientTilesException {
	
		double totalTravel = dist(agent, h) + getTileFetchTime(h);
		double utility = h.getValue() / totalTravel;
		//System.out.println("\ndist = " + totalTravel + "\nval = " + h.getValue() + "\nseu = " + utility);
		return utility;
	}
	
	/**
	*   Get the SEU score for a specified hole making two assumptions:<ul>
	*   <li>That the agent concerned will start at the specified point, rather 
	*   than the point it is currently occupying</li>
	*   <li>That the specified list of Tiles will not be available to the agent</li>
	*   </ul>
	*   @param h the Hole for which to produce the score
	*   @param p the Point that the agent will start from
	*   @param tiles the list of Tiles which will NOT be available to the agent for
	*   filling the specified Hole (h)
	*   @return an SEUProfile including the hole, the rating for the hole
	*   and the list of tiles closest to the hole at the time of computation, excluding
	*   any tiles in the specified list.
	*   @throws InsufficientTilesException if the TileWorld does not contain enough
	*   tiles to fill the specified hole, assuming the specified tiles are not available
	*/
	public SEUProfile getSEUProfileAssuming(Hole h, Point p, ArrayList tiles)
		throws InsufficientTilesException {
			
		ArrayList availTiles = getNearestTilesExcluding(h, tiles);
		double totalTravel = dist(p, h) + getTileFetchTime(h, availTiles);
		double utility = h.getValue() / totalTravel;
		return new SEUProfile(h, utility, availTiles);
	}
	
	/**
	*   Get an SEUProfile for the specified Hole. This method should be
	*   used to save time if possible. Instead of finding out a hole's 
	*   rating, deciding to set it as a goal and <i>then</i> finding out
	*   the best tiles to get, the SEUProfile takes advantage of the fact
	*   that these tiles are computed in the process of finding the rating
	*   anyway, so they are returned with the computation
	*   @param h the Hole to be rated.
	*   @return an SEUProfile that includes the hole, the rating for the 
	*   hole and the list of tiles closest to the hole at the time of
	*   computation.
	*   @throws InsufficientTilesException if the TileWorld does not contain enough
	*   tiles to fill the specified hole.
	*/
	public SEUProfile getSEUProfile(Hole h) throws InsufficientTilesException {
		
		ArrayList tiles = getNearestTiles(h);
		double totalTravel = dist(agent, h) + getTileFetchTime(h, tiles);
		double utility = h.getValue() / totalTravel;
		return new SEUProfile(h, utility, tiles);
	}
		
	
	/**
	*   Get the Hole in this TileWorld for this agent with the top rated SEU. 
	*   A very computationally intensive exercise whose use should be minimised 
	*   by the model.   
	*   @return the Hole in this TileWorld with the highest current SEU rating or null
	*   if there are either no holes, or not enough tiles to fill any of the holes.
	*/
	public Hole getTopRatedHole() {
		
		List holes = world.getHoles();
		if (holes.size() < 1)
			return null;
		double highestRating = 0;
		Hole pickedHole = null;
		for (int i = 0; i < holes.size(); i++) {
			Hole thisHole = (Hole)holes.get(i);
			if (thisHole == null)
				continue;
			try {
				double thisRating = getSEURating(thisHole);
				if (thisRating > highestRating) {
					highestRating = thisRating;
					pickedHole = thisHole;
				}
			}
			catch (InsufficientTilesException e) {}
		}
		return pickedHole;
	}
	
	/**
	*   Gets the SEUProfile of the Hole in this TileWorld with the highest SEU 
	*   rating assuming three things:<ul>
	*   <li>The agent will start from the specified point</li>
	*   <li>The given list of tiles will not be available</li>
	*   <li>The given list of holes will not be viable targets</li>
	*   @param p the Point at which the agent will start
	*   @param tiles the tiles which will not be available
	*   @param holes the holes which will not be viable targets
	*   @return the SEUProfile of the top rated hole or null if there are either
	*   no holes, or not enough tiles to fill any of the available holes
	*/
	public SEUProfile getTopRatedHoleAssuming(  Point p, 
												ArrayList excludedTiles, 
												ArrayList excludedHoles) {
	
		ArrayList holes = new ArrayList();
		holes.addAll(world.getHoles());
		holes.removeAll(excludedHoles);
		if (holes.size() < 1)
			return null;
		double highestRating = 0;
		SEUProfile pickedProfile = null;
		for (int i = 0; i < holes.size(); i++) {
			Hole thisHole = (Hole)holes.get(i);
			try {
				SEUProfile thisProfile = getSEUProfileAssuming(thisHole, p, excludedTiles);
				double thisRating = thisProfile.getRating();
				if (thisRating > highestRating) {
					highestRating = thisRating;
					pickedProfile = thisProfile;
				}
			}
			catch (InsufficientTilesException e) {}
		}
		return pickedProfile;
	}
	
	/**
	*   Gets a more accurate SEU for a hole, based on evidence provided by a more
	*   sophisticated RoutePlanner. The invoker must provide a List of Routes so that 
	*   this can be verified. The list provided should consist of a 'n' long list of
	*   routes, all of whose origin is the position of the hole, and all of whose
	*   destination is the position of a Tile, where 'n' is the number of tiles 
	*   required to fill the hole.
	*   The produced SEU factors in the Routes which provide a more accurate utility 
	*   score of a hole.
	*   @param hole the Hole for which to produce a utility score.
	*   @param routes the List of Routes which more accurately represent the availability
	*   of tiles with which to fill this hole.
	*/
	public double getRoutedSEURating(Hole hole, List routes) throws InsufficientTilesException {
		
		if (hole == null || routes == null)
			return -1;
		int tileRouteSummation = 0;
		for (int i = 0; i < routes.size(); i++) {
			Route nextTileRoute = (Route)routes.get(i);
			if (nextTileRoute == null)
				throw new InsufficientTilesException();
			tileRouteSummation += nextTileRoute.getLength() * 2;
		}
		double totalDistance = dist(agent, hole) + tileRouteSummation;
		double utility = hole.getValue() / totalDistance;
		return utility;
	}
	
	public double getRoutedSEURating(Hole h) throws InsufficientTilesException {
		
		if (h == null)
			return -1;
		List routes = new ArrayList();
		List tiles = getNearestTiles(h);
		for (int i = 0; i < tiles.size(); i++) {
			Route r = planner.generateRouteTo((SpatialObject)tiles.get(i));
			if (r == null)
				throw new InsufficientTilesException();
			routes.add(planner.generateRouteTo((SpatialObject)tiles.get(i)));
		}			
		return getRoutedSEURating(h, routes);
	}
	
	public Tile getNearestTile() throws InsufficientTilesException {
		
		List tiles = world.getTiles();
		Tile chosen = null;
		int distanceToChosen = Integer.MAX_VALUE;
		if (tiles.size() > 0) {
			chosen = (Tile)tiles.get(0);
			distanceToChosen = (int) dist(agent, chosen);
		}
		else
			throw new InsufficientTilesException();
		for (int i = 0; i < tiles.size(); i++) {
				Tile current = (Tile)tiles.get(i);				
				int distanceToCurrent = (int)dist(agent, current);
				if (chosen != null && distanceToCurrent < distanceToChosen) {
					chosen = current;
					distanceToChosen = distanceToCurrent;
				}					
		}
		return chosen;
	}
	
	/**
	*   Gets an ArrayList of Tiles to be used to fill the specified hole.
	*   The tiles will be the n closest tiles to the specified hole, where n is the
	*   remaining depth of the hole at time of calculation.
	*   @param h the Hole for which the Tiles should be found
	*   @return the ArrayList of n nearest tiles.	
	*   @throws InsufficientTilesException if the TileWorld does not contain enough
	*   tiles to fill the specified hole.
	*/
	public ArrayList getNearestTiles(Hole h) throws InsufficientTilesException {
		
		int tilesRequired = h.getCurrentDepth();
		List tilesAvailable = world.getTiles();
		if (tilesAvailable.size() < tilesRequired) 
			throw new InsufficientTilesException();
		ArrayList chosenTiles = new ArrayList();
		for (int i = 0; i < tilesAvailable.size(); i++) {
			Tile nextTile = (Tile)tilesAvailable.get(i);
			double distance = dist(h, nextTile);
			//no elements to compare to, add the first tile
			if (chosenTiles.size() == 0)
				chosenTiles.add(nextTile);
			//are elements to compare to, add in, retaining a distance-order
			else {
				for (int j = 0; j < chosenTiles.size(); j++) {
					Tile nextCandidate = (Tile)chosenTiles.get(j);
					if (distance < dist(h, nextCandidate)) {
						chosenTiles.add(j, nextTile);
						while (chosenTiles.size() > tilesRequired) {
							chosenTiles.remove(tilesRequired);
						}
					}
				}
			}
		}
		return chosenTiles;
	}
	
	/**
	*   Gets an ArrayList of Tiles to be used to fill the specified hole, 
	*   assuming that the specified tiles will not be available at the time.
	*   @param h the Hole for which the Tiles should be found
	*   @param tiles the list of tiles to exclude from the search
	*   @return the ArrayList of n nearest tiles not in tiles
	*   @throws InsufficientTilesException if the TileWorld does not contain
	*   enough tiles to fill the specified hole, assuming that no Tile in 
	*   tiles will be available.
	*/
	public ArrayList getNearestTilesExcluding(Hole h, ArrayList tiles)
		throws InsufficientTilesException {
		
		int tilesRequired = h.getCurrentDepth();
		ArrayList tilesAvailable = new ArrayList();
		tilesAvailable.addAll(world.getTiles());		
		tilesAvailable.removeAll(tiles);
		if (tilesAvailable.size() < tilesRequired) 
			throw new InsufficientTilesException();
		ArrayList chosenTiles = new ArrayList();
		for (int i = 0; i < tilesAvailable.size(); i++) {
			Tile nextTile = (Tile)tilesAvailable.get(i);
			double distance = dist(h, nextTile);
			//no elements to compare to, add the first tile
			if (chosenTiles.size() == 0)
				chosenTiles.add(nextTile);
			//are elements to compare to, add in, retaining a distance-order
			else {
				for (int j = 0; j < chosenTiles.size(); j++) {
					Tile nextCandidate = (Tile)chosenTiles.get(j);
					if (distance < dist(h, nextCandidate)) {
						chosenTiles.add(j, nextTile);
						while (chosenTiles.size() > tilesRequired) {
							chosenTiles.remove(tilesRequired);
						}
					}
				}
			}
		}
		return chosenTiles;
	}
	
	private int getTileFetchTime(Hole h) throws InsufficientTilesException {
		
		int totalDist = 0;
		ArrayList tiles = getNearestTiles(h);
		for (int i = 0; i < tiles.size(); i++) {
			Tile nextTile = (Tile)tiles.get(i);
			totalDist += 2 * dist(h, nextTile);
		}
		return totalDist;
	}
	
	private int getTileFetchTime(Hole h, ArrayList tiles) {
		
		int totalDist = 0;
		for (int i = 0; i < tiles.size(); i++) {
			Tile nextTile = (Tile)tiles.get(i);
			totalDist += 2 * dist(h, nextTile);
		}
		return totalDist;
	}
	
	private double dist(Point p, SpatialObject ob) {
		
		double xDist = (double)(Math.abs(p.getX() - ob.getX()));
		double yDist = (double)(Math.abs(p.getY() - ob.getY()));
		return xDist + yDist;
	}
	
	private double dist(SpatialObject ob1, SpatialObject ob2) {
		
		double xDist = (double)(Math.abs(ob1.getX() - ob2.getX()));
		double yDist = (double)(Math.abs(ob1.getY() - ob2.getY()));
		return xDist + yDist;
	}	
}
