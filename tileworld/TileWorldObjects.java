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

import java.util.List;

/**
*   A datastructure used for issuing update to TileWorldListeners
*/
public class TileWorldObjects {
	
	private List newTiles;
	private List newHoles;
	
	/**
	*   Construct a new TileWorldObjects object with the list of
	*   new tiles and list of new holes
	*   @param tiles the new Tiles that have appeared in the TileWorld
	*   @param holes the new Holes that have appeared in the TileWorld
	*/
	public TileWorldObjects(List tiles, List holes) {
		
		this.newTiles = tiles;
		this.newHoles = holes;
	}
	
	/**
	*   Get the new tiles held in this TileWorldObjects
	*   @return an ArrayList of Tile objects which have appeared
	*   in the TileWorld since the last update
	*/
	public List getNewTiles() {
		
		return this.newTiles;
	}
	
	/**
	*   Get the new holes held in this TileWorldObjects
	*   @return an ArrayList of Hole objects which have appeared
	*   in the TileWorld since the last update
	*/
	public List getNewHoles() {
		
		return this.newHoles;
	}
}
