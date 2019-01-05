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

/**
*   A datastructure that encapsulates a Tile
*   and a Route between a Point and that
*   Tile.
*/
public class TaskSet {
	
	private Tile tile;
	private Route route;
	
	/**
	*   Constructs a TaskSet with the specified Route
	*   and the given Tile.
	*   @param t a Tile
	*   @param r a Route, the destination of which should
	*   be t.
	*/
	public TaskSet(Route r, Tile t) {
		
		this.tile = t;
		this.route = r;
	}
	
	/**
	*   Gets the Route to the Tile
	*   @return the Route
	*/
	public Route getRoute() {
		
		return this.route;
	}
	
	/**
	*   Gets the Tile specified by this TaskSet
	*   @return the Tile
	*/
	public Tile getTile() {
		
		return this.tile;
	}
}
