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
*   A class that facilitates economising in the computation necessary
*   for computing utility scores. Instead of comparing scores of various
*   Holes (the generation of which requires finding lists of viable tiles)
*   and then finding the right Tiles for one of them. The SEUProfile supplies
*   a datastructure for packaging a score, a list of tiles and a reference to
*   the Hole to which the profile relates.
*/
public class SEUProfile {
	
	private Hole hole;
	private double rating;
	private ArrayList tiles;
	
	/**
	*   Constructs an SEUProfile for the specified Hole with the specifed
	*   SEU rating and list of viable Tiles.
	*   @param h the Hole
	*   @param rating the SEU of the Hole
	*   @param tiles the list of n tiles, where n is the current depth of h
	*/
	public SEUProfile(Hole h, double rating, ArrayList tiles) {
		
		this.hole = h;
		this.rating = rating;
		this.tiles = tiles;
	}
	
	/**
	*   Gets the rating held in this SEUProfile
	*   @return the SEU of the Hole held by this profile
	*/
	public double getRating() {
		
		return this.rating;
	}
	
	/**
	*   Gets the Hole that this SEUProfile describes
	*   @return the Hole
	*/
	public Hole getHole() {
		
		return this.hole;
	}
	
	/**
	*   Gets the list of tiles that can be used to fill the Hole
	*   @return a list of tiles of size() n where n is the current depth of 
	*   the Hole
	*/
	public ArrayList getTiles() {
		
		return this.tiles;
	}
}
