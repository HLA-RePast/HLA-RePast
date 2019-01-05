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

public interface ExecutionEngine {
	
	/**
	*   Requests that this ExecutionEngine cause the Agent to which
	*   it is registered to perform the next action. Depending upon
	*   implementation this method may or may not actually do anything
	*   in all circumstances. In most implementations this method will
	*   either perform the next action or, if one is not found, will
	*   invoke the agent's brain to provide it with a new action (usually
	*   by a method call to<p><code> 
	*   brain.produceNextIntention();
	*   </code>
	*/
	public void act();
	
	/**
	*   Sets the next action for this ExecutionEngine to be the fetching
	*   of a Tile from the world. The Tile and the Route to it must be
	*   provided. It is acceptable for some degree of intellegence in the 
	*   engine here - namely, if a tile is already held by the agent, it 
	*   may simply ignore the command and immediately perform a callback
	*   for the next task.
	*   @param t the Tile to be fetched
	*   @param r the Route to the specified Tile
	*/
	public void doTileFetch(Tile t, Route r);
	
	/**
	*   Sets the next action for this ExecutionEngine to be the filling
	*   of a Tile from the TileWorld. The Hole and the Route to it must
	*   be provided.
	*   @param h the Hole to attempt to fill
	*   @param r the Route to the Hole
	*/
	public void doHoleFill(Hole h, Route r);
}
